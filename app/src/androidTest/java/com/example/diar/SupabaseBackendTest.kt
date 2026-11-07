package com.example.diar

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Fake transport: no accounts, email or listing data are sent to a live project. */
@RunWith(AndroidJUnit4::class)
class SupabaseBackendTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun reset() { context.getSharedPreferences("diar_test_cloud_session", Context.MODE_PRIVATE).edit().clear().commit() }
    private fun session(expires: Int = 3600) = JSONObject().put("access_token", "test-access")
        .put("refresh_token", "test-refresh").put("expires_in", expires)
        .put("user", JSONObject().put("id", "owner-one").put("email", "owner@example.invalid")).toString()
    private fun property(owner: String = "") = Property("listing-one", "Test home", "Test location", "125.5", "250000", "", "",
        true, "sale", "house", 2, 31.7, 35.2, "map", owner)

    @Test fun encryptedSessionSurvivesRecreationAndRefreshesBeforeWrite() {
        reset()
        try {
            val calls = mutableListOf<String>()
            val transport: (String, String, ByteArray?, String?, String, Boolean) -> String = { method, path, body, token, _, _ ->
                calls.add(path)
                when {
                    path.contains("grant_type=password") -> session(0)
                    path.contains("grant_type=refresh_token") -> {
                        assertEquals("test-refresh", JSONObject(String(body!!)).getString("refresh_token")); session()
                    }
                    method == "POST" && path == "/rest/v1/listings" -> {
                        assertEquals("test-access", token)
                        val row = JSONObject(String(body!!))
                        assertEquals("owner-one", row.getString("owner_id"))
                        assertEquals("map", row.getString("location_source"))
                        assertFalse(row.has("photo")); assertFalse(row.has("ownerId"))
                        JSONArray().put(row).toString()
                    }
                    else -> error("Unexpected request")
                }
            }
            val first = SupabaseBackend(context, "https://example.invalid", "test-publishable", "diar_test_cloud_session", transport)
            first.signIn("owner@example.invalid", "test-password")
            val raw = context.getSharedPreferences("diar_test_cloud_session", Context.MODE_PRIVATE).getString("session", "")!!
            assertFalse(raw.contains("test-refresh")); assertFalse(raw.contains("test-access")); assertFalse(raw.contains("owner@example.invalid"))
            val restored = SupabaseBackend(context, "https://example.invalid", "test-publishable", "diar_test_cloud_session", transport)
            assertEquals("owner-one", restored.userId)
            val saved = restored.save(property(), false)
            assertEquals("owner-one", saved.ownerId)
            assertTrue(calls.any { it.contains("refresh_token") })
        } finally { reset() }
    }
    @Test fun otherOwnersCannotMutateAndOfflineLogoutClearsSession() {
        reset()
        try {
            var mutations = 0
            val backend = SupabaseBackend(context, "https://example.invalid", "test-publishable", "diar_test_cloud_session") { method, path, _, _, _, _ ->
                if (path.contains("grant_type=password")) session()
                else { if (method != "GET") mutations++; throw java.io.IOException("Offline") }
            }
            backend.signIn("owner@example.invalid", "test-password")
            for (delete in listOf(false, true)) {
                try {
                    if (delete) backend.delete(property("owner-two")) else backend.save(property("owner-two"), true)
                    fail("Wrong owner was accepted")
                } catch (_: SupabaseBackend.BackendException) { }
            }
            assertEquals(0, mutations)
            backend.signOut()
            assertEquals("", backend.userId)
        } finally { reset() }
    }
    @Test fun signupNeedingConfirmationDoesNotCreateSession() {
        reset()
        try {
            val backend = SupabaseBackend(context, "https://example.invalid", "test-publishable", "diar_test_cloud_session") { _, _, _, _, _, _ ->
                JSONObject().put("id", "pending-user").toString()
            }
            assertFalse(backend.signUp("Test", "owner@example.invalid", "test-password"))
            assertEquals("", backend.userId)
        } finally { reset() }
    }
    @Test fun failedListingWriteRemovesNewUploadAndPreservesOriginalFile() {
        reset()
        val file = java.io.File(context.filesDir, "cloud_test_photo.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        try {
            var uploaded = ""
            var removed = ""
            val backend = SupabaseBackend(context, "https://example.invalid", "test-publishable", "diar_test_cloud_session") { method, path, body, _, type, _ ->
                when {
                    path.contains("grant_type=password") -> session()
                    method == "POST" && path.startsWith("/storage/") -> {
                        uploaded = path.removePrefix("/storage/v1/object/listing-photos/")
                        assertEquals("image/jpeg", type); "{}"
                    }
                    method == "DELETE" && path.startsWith("/storage/") -> {
                        removed = JSONObject(String(body!!)).getJSONArray("prefixes").getString(0); "[]"
                    }
                    else -> throw SupabaseBackend.HttpFailure(500)
                }
            }
            backend.signIn("owner@example.invalid", "test-password")
            try { backend.save(property().copy(photo = file.name), false); fail("Failed write accepted") }
            catch (_: SupabaseBackend.HttpFailure) { }
            assertTrue(uploaded.startsWith("owner-one/listing-one/"))
            assertEquals(uploaded, removed)
            assertTrue(file.isFile)
        } finally { file.delete(); reset() }
    }
    @Test fun remoteReferencesRoundTripWithoutOverwritingLegacyListings() {
        val cache = PropertyStore(context, "diar_test_cloud_cache")
        try {
            val remote = property("owner-one").copy(photoPath = "owner-one/listing-one/photo.jpg")
            assertTrue(cache.replace(listOf(remote)))
            assertEquals(remote, PropertyStore(context, "diar_test_cloud_cache").all().single())
            assertTrue(cache.replace(emptyList()))
            assertTrue(cache.all().isEmpty())
        } finally { context.getSharedPreferences("diar_test_cloud_cache", Context.MODE_PRIVATE).edit().clear().commit() }
    }
}
