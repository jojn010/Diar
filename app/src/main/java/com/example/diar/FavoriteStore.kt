package com.example.diar

import android.content.Context

/** Saved listing IDs, private to this device and separated by signed-in account. */
class FavoriteStore(context: Context, private val account: String) {
    private val prefs = context.getSharedPreferences("diar_favorites", Context.MODE_PRIVATE)
    fun ids(): Set<String> = prefs.getStringSet(account, emptySet())!!.toSet()
    fun contains(id: String) = id in ids()
    fun set(id: String, saved: Boolean): Boolean {
        val updated = ids().toMutableSet().apply { if (saved) add(id) else remove(id) }
        return prefs.edit().putStringSet(account, updated).commit()
    }
}
