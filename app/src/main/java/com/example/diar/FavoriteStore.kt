package com.example.diar

import android.content.Context
import com.diar.shared.FavoriteSync

/** An optimistic cache with a durable per-account queue; guest favorites remain device-local. */
class FavoriteStore(context: Context, private val account: String) {
    private val prefs = context.getSharedPreferences("diar_favorites", Context.MODE_PRIVATE)
    fun ids(): Set<String> = synchronized(lock) { prefs.getStringSet(account, emptySet())!!.toSet() }
    fun contains(id: String) = id in ids()
    fun set(id: String, saved: Boolean): Boolean = synchronized(lock) {
        val updated = ids().toMutableSet().apply { if (saved) add(id) else remove(id) }
        val pending = prefs.getStringSet("$account.pending", emptySet())!!.toMutableSet().apply { add(id) }
        prefs.edit().putStringSet(account, updated).putStringSet("$account.pending", pending).commit()
    }
    fun pendingChanges(): Map<String, Boolean> = synchronized(lock) {
        val dirty = prefs.getStringSet("$account.pending", emptySet())!!.toMutableSet()
        if (!prefs.getBoolean("$account.cloudInitialized", false)) dirty.addAll(ids())
        val current = ids()
        dirty.associateWith { it in current }
    }
    fun applyRemote(remote: Set<String>, sent: Map<String, Boolean>, privateIds: Set<String>) = synchronized(lock) {
        val result = FavoriteSync.merge(ids(), pendingChanges(), remote, sent, privateIds)
        prefs.edit().putStringSet(account, result.ids).putStringSet("$account.pending", result.pending.keys)
            .putBoolean("$account.cloudInitialized", true).commit()
    }

    companion object { private val lock = Any() }
}
