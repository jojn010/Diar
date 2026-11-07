package com.example.diar

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import java.util.UUID
import java.util.concurrent.Executors
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Small HTTPS REST adapter. Only a publishable/anon key belongs in the Android build. */
class SupabaseBackend(context: Context,
    apiUrl: String = BuildConfig.SUPABASE_URL,
    private val publishableKey: String = BuildConfig.SUPABASE_KEY,
    sessionPreferences: String = "diar_cloud_session",
    private val transport: ((String, String, ByteArray?, String?, String, Boolean) -> String)? = null) {
    private val context = context.applicationContext
    private val session = SessionVault(this.context, sessionPreferences)
    private val base = apiUrl.trimEnd('/')
    val enabled get() = base.startsWith("https://") && publishableKey.isNotBlank()
    val userId get() = session.read()?.optJSONObject("user")?.optString("id").orEmpty()
    val email get() = session.read()?.optJSONObject("user")?.optString("email").orEmpty()

    fun signIn(email: String, password: String) {
        val response = request("POST", "/auth/v1/token?grant_type=password", JSONObject()
            .put("email", email).put("password", password).toString().toByteArray())
        session.write(JSONObject(response).withExpiry())
    }
    /** False means email confirmation is required; a successful signup is not a fake login. */
    fun signUp(name: String, email: String, password: String): Boolean {
        val result = JSONObject(request("POST", "/auth/v1/signup", JSONObject().put("email", email)
            .put("password", password).put("data", JSONObject().put("full_name", name)).toString().toByteArray()))
        if (result.optString("access_token").isBlank()) { session.clear(); return false }
        session.write(result.withExpiry())
        return true
    }
    fun signOut() {
        // Local logout always works, including offline. Remote sessions expire normally.
        val token = session.read()?.optString("access_token").orEmpty()
        runCatching { if (token.isNotBlank()) request("POST", "/auth/v1/logout?scope=local", token = token) }
        session.clear()
    }
    private fun JSONObject.withExpiry(): JSONObject = put("expires_at",
        System.currentTimeMillis() / 1000 + optLong("expires_in", 3600))
    private fun token(): String {
        val current = session.read() ?: throw BackendException(R.string.cloud_sign_in_required)
        if (current.optLong("expires_at") > System.currentTimeMillis() / 1000 + 60)
            return current.getString("access_token")
        val refreshed = try {
            JSONObject(request("POST", "/auth/v1/token?grant_type=refresh_token",
                JSONObject().put("refresh_token", current.getString("refresh_token")).toString().toByteArray())).withExpiry()
        } catch (error: HttpFailure) {
            if (error.status in listOf(400, 401, 403)) session.clear()
            throw error
        }
        session.write(refreshed)
        return refreshed.getString("access_token")
    }
    fun listings(): List<Property> {
        val rows = JSONArray()
        var offset = 0
        do {
            val page = JSONArray(request("GET", "/rest/v1/listings?select=*&order=created_at.desc,id&limit=200&offset=$offset"))
            for (index in 0 until page.length()) rows.put(page.getJSONObject(index))
            offset += page.length()
        } while (page.length() == 200)
        return (0 until rows.length()).map { i -> val row = rows.getJSONObject(i)
            Property(row.getString("id"), row.getString("title"), row.getString("location"),
                row.getString("area"), row.getString("price"), row.optString("description"), "",
                row.getBoolean("negotiable"), row.getString("purpose"), row.getString("type"), row.getInt("floors"),
                row.getDouble("latitude"), row.getDouble("longitude"), row.getString("location_source"),
                row.getString("owner_id"), row.optString("photo_path"))
        }
    }
    fun save(property: Property, existing: Boolean): Property {
        val access = token()
        val owner = userId
        if (existing && property.ownerId != owner) throw BackendException(R.string.cloud_not_owner)
        var photoPath = property.photoPath
        var uploaded = ""
        if (property.photo.isNotEmpty()) {
            val file = File(context.filesDir, property.photo)
            require(file.parentFile?.canonicalFile == context.filesDir.canonicalFile && file.isFile)
            require(file.length() <= 5 * 1024 * 1024)
            uploaded = "$owner/${property.id}/${UUID.randomUUID()}.jpg"
            request("POST", "/storage/v1/object/listing-photos/$uploaded", file.readBytes(), access, "image/jpeg")
            photoPath = uploaded
        }
        val row = property.json().apply {
            remove("photo"); remove("locationSource"); remove("ownerId"); remove("photoPath")
            put("owner_id", owner); put("location_source", property.locationSource); put("photo_path", photoPath)
        }
        try {
            request(if (existing) "PATCH" else "POST",
                "/rest/v1/listings" + if (existing) "?id=eq.${property.id}&owner_id=eq.$owner" else "",
                row.toString().toByteArray(), access, representation = true).also {
                    if (JSONArray(it).length() != 1) throw BackendException(R.string.cloud_not_owner)
                }
        } catch (error: Exception) {
            if (uploaded.isNotEmpty()) runCatching { removePhoto(uploaded, access) }
            throw error
        }
        if (uploaded.isNotEmpty() && property.photoPath.isNotEmpty()) runCatching { removePhoto(property.photoPath, access) }
        return property.copy(photo = "", ownerId = owner, photoPath = photoPath)
    }
    fun delete(property: Property) {
        val access = token()
        if (property.ownerId != userId) throw BackendException(R.string.cloud_not_owner)
        val result = JSONArray(request("DELETE", "/rest/v1/listings?id=eq.${property.id}&owner_id=eq.$userId",
            token = access, representation = true))
        if (result.length() != 1) throw BackendException(R.string.cloud_not_owner)
        // The listing is gone even if optional orphan cleanup is temporarily unavailable.
        if (property.photoPath.isNotEmpty()) runCatching { removePhoto(property.photoPath, access) }
    }
    private fun removePhoto(path: String, access: String) {
        request("DELETE", "/storage/v1/object/listing-photos", JSONObject()
            .put("prefixes", JSONArray().put(path)).toString().toByteArray(), access)
    }
    fun photoUrl(path: String) = "$base/storage/v1/object/public/listing-photos/$path"

    private fun request(method: String, path: String, body: ByteArray? = null, token: String? = null,
                        contentType: String = "application/json", representation: Boolean = false): String {
        check(enabled)
        transport?.let { return it(method, path, body, token, contentType, representation) }
        val connection = URL(base + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15000; connection.readTimeout = 20000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("apikey", publishableKey)
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", contentType)
            if (representation) connection.setRequestProperty("Prefer", "return=representation")
            if (body != null) { connection.doOutput = true; connection.outputStream.use { it.write(body) } }
            val status = connection.responseCode
            if (status !in 200..299) throw HttpFailure(status) // Never log passwords, tokens, or response bodies.
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally { connection.disconnect() }
    }
    class HttpFailure(val status: Int) : Exception("Supabase HTTP $status")
    class BackendException(val messageId: Int) : Exception()
    companion object {
        // Serialize token refresh and mutations across Activity recreation.
        val worker = Executors.newSingleThreadExecutor()
    }
}

/** Refresh/access tokens are encrypted using an app-specific Android Keystore key. */
private class SessionVault(context: Context, name: String) {
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    private val alias = "diar.supabase.$name"
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (store.getKey(alias, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun write(value: JSONObject) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encoded = Base64.encodeToString(cipher.iv + cipher.doFinal(value.toString().toByteArray()), Base64.NO_WRAP)
        check(prefs.edit().putString("session", encoded).commit())
    }
    fun read(): JSONObject? = try {
        prefs.getString("session", null)?.let { raw ->
            val data = Base64.decode(raw, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, data.copyOfRange(0, 12)))
            }
            JSONObject(String(cipher.doFinal(data.copyOfRange(12, data.size)), Charsets.UTF_8))
        }
    } catch (_: Exception) { clear(); null }
    fun clear() { prefs.edit().clear().commit() }
}
