package com.example.diar

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal

/** Device-local listings. No remote publication or account is implied. */
data class Property(val id: String, val title: String, val location: String,
    val area: String, val price: String, val description: String, val photo: String,
    val negotiable: Boolean = false, val purpose: String = "sale", val type: String = "unspecified",
    val floors: Int = 0, val latitude: Double? = null, val longitude: Double? = null,
    val locationSource: String = "") {
    fun json() = JSONObject().put("id", id).put("title", title).put("location", location)
        .put("area", area).put("price", price).put("description", description).put("photo", photo)
        .put("negotiable", negotiable).put("purpose", purpose).put("type", type).put("floors", floors)
        .put("latitude", latitude).put("longitude", longitude).put("locationSource", locationSource)
}

class PropertyStore(context: Context) {
    private val prefs = context.getSharedPreferences("diar_properties", Context.MODE_PRIVATE)
    fun all(): List<Property> {
        val array = JSONArray(prefs.getString("items", "[]"))
        return (0 until array.length()).map { i -> array.getJSONObject(i).let {
            Property(it.getString("id"), it.getString("title"), it.getString("location"),
                it.getString("area"), it.getString("price"), it.optString("description"), it.optString("photo"),
                it.optBoolean("negotiable", false), it.optString("purpose", "sale"),
                it.optString("type", "unspecified"), it.optInt("floors", 0),
                if (it.has("latitude") && !it.isNull("latitude")) it.getDouble("latitude") else null,
                if (it.has("longitude") && !it.isNull("longitude")) it.getDouble("longitude") else null,
                it.optString("locationSource"))
        } }
    }
    fun save(property: Property) = write(listOf(property) + all().filterNot { it.id == property.id })
    fun delete(id: String) = write(all().filterNot { it.id == id })
    private fun write(items: List<Property>): Boolean = prefs.edit()
        .putString("items", JSONArray().apply { items.forEach { put(it.json()) } }.toString()).commit()
    companion object {
        fun integer(text: String): Long? {
            val normalized = text.trim().map { c ->
                Character.digit(c, 10).takeIf { it >= 0 }?.let { ('0'.code + it).toChar() } ?: c
            }.joinToString("")
            if (!Regex("[0-9]+").matches(normalized)) return null
            return normalized.toLongOrNull()?.takeIf { it in 1..1_000_000_000L }
        }
        fun number(text: String): BigDecimal? {
            val normalized = text.trim().map { c ->
                when { c == '٫' -> '.'; Character.digit(c, 10) >= 0 -> ('0'.code + Character.digit(c, 10)).toChar(); else -> c }
            }.joinToString("")
            if (!Regex("[0-9]+(\\.[0-9]{1,2})?").matches(normalized)) return null
            return normalized.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO && it <= BigDecimal("1000000000") }
        }
    }
}
