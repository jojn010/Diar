package com.example.diar

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class FavoriteSyncTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun migratedFavoritesUploadOnceThenFollowCloudRemovals() {
        val account="qa-sync-${UUID.randomUUID()}"
        val prefs=context.getSharedPreferences("diar_favorites",Context.MODE_PRIVATE)
        prefs.edit().putStringSet(account,setOf("old-home")).commit()
        val store=FavoriteStore(context,account)
        assertEquals(mapOf("old-home" to true),store.pendingChanges())
        store.applyRemote(setOf("old-home"),mapOf("old-home" to true),emptySet())
        assertTrue(store.pendingChanges().isEmpty())
        store.applyRemote(emptySet(),emptyMap(),emptySet())
        assertTrue(store.ids().isEmpty())
        prefs.edit().remove(account).remove("$account.pending").remove("$account.cloudInitialized").commit()
    }
    @Test fun offlineTapsSurviveRecreationAndAnOlderResponseCannotEraseANewerTap() {
        val account="qa-sync-${UUID.randomUUID()}"
        val store=FavoriteStore(context,account)
        store.set("home",true)
        val sent=store.pendingChanges()
        store.set("home",false)
        FavoriteStore(context,account).applyRemote(setOf("home","web-home"),sent,emptySet())
        assertFalse(store.contains("home"));assertTrue(store.contains("web-home"))
        assertEquals(mapOf("home" to false),store.pendingChanges())
        store.applyRemote(setOf("web-home"),store.pendingChanges(),emptySet())
        assertTrue(store.pendingChanges().isEmpty())
        context.getSharedPreferences("diar_favorites",Context.MODE_PRIVATE).edit().remove(account).remove("$account.pending").remove("$account.cloudInitialized").commit()
    }
    @Test fun privateLocalHomesStayLocalAndOtherAccountsStaySeparate() {
        val account="qa-sync-${UUID.randomUUID()}"
        val store=FavoriteStore(context,account)
        store.set("local-home",true)
        store.applyRemote(setOf("cloud-home"),emptyMap(),setOf("local-home"))
        assertEquals(setOf("local-home","cloud-home"),store.ids())
        assertTrue(FavoriteStore(context,"other-$account").ids().isEmpty())
        context.getSharedPreferences("diar_favorites",Context.MODE_PRIVATE).edit().remove(account).remove("$account.pending").remove("$account.cloudInitialized").commit()
    }
}
