package com.example.diar

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal

/** Device-local listings. No remote publication or account is implied. */
data class Property(val id: String, val title: String, val location: String,
    val area: String, val price: String, val description: String, val photo: String) {
    fun json() = JSONObject().put("id", id).put("title", title).put("location", location)
        .put("area", area).put("price", price).put("description", description).put("photo", photo)
}

class PropertyStore(context: Context) {
    private val prefs = context.getSharedPreferences("diar_properties", Context.MODE_PRIVATE)
    fun all(): List<Property> {
        val array = JSONArray(prefs.getString("items", "[]"))
        return (0 until array.length()).map { i -> array.getJSONObject(i).let {
            Property(it.getString("id"), it.getString("title"), it.getString("location"),
                it.getString("area"), it.getString("price"), it.optString("description"), it.optString("photo"))
        } }
    }
    fun save(property: Property) = write(listOf(property) + all().filterNot { it.id == property.id })
    fun delete(id: String) = write(all().filterNot { it.id == id })
    private fun write(items: List<Property>): Boolean = prefs.edit()
        .putString("items", JSONArray().apply { items.forEach { put(it.json()) } }.toString()).commit()
    companion object {
        fun number(text: String): BigDecimal? {
            val normalized = text.trim().map { c ->
                when { c == '٫' -> '.'; Character.digit(c, 10) >= 0 -> ('0'.code + Character.digit(c, 10)).toChar(); else -> c }
            }.joinToString("")
            if (!Regex("[0-9]+(\\.[0-9]{1,2})?").matches(normalized)) return null
            return normalized.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO && it <= BigDecimal("1000000000") }
        }
    }
}
