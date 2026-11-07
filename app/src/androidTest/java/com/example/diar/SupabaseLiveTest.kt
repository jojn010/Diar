package com.example.diar

import android.content.Context
import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Opt-in only. Use disposable, preconfirmed QA accounts; never real user credentials. */
@LiveProjectTest
@RunWith(AndroidJUnit4::class)
class SupabaseLiveTest {
    @Test fun disposableOwnersCanPublishPhotoEditAndDeleteAcrossSessions() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("diarLiveTest") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val password = checkNotNull(args.getString("diarTestPassword"))
        val first = SupabaseBackend(context, sessionPreferences = "diar_live_test_a")
        val second = SupabaseBackend(context, sessionPreferences = "diar_live_test_b")
        val file = File(context.filesDir, "qa_${UUID.randomUUID()}.jpg")
        var saved: Property? = null
        try {
            first.signIn(checkNotNull(args.getString("diarTestEmailA")), password)
            second.signIn(checkNotNull(args.getString("diarTestEmailB")), password)
            val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.GREEN)
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }; bitmap.recycle()
            val draft = Property(UUID.randomUUID().toString(), "QA only — disposable", "Synthetic test location", "120.5", "250000",
                "Automated QA record; removed after testing", file.name, true, "sale", "house", 2, 0.0, 0.0, "map")
            saved = first.save(draft, false)
            assertTrue(saved.photoPath.startsWith(first.userId + "/"))
            assertEquals(saved, second.listings().single { it.id == draft.id })
            val bytes = java.net.URL(first.photoUrl(saved.photoPath)).openStream().use { it.readBytes() }
            assertNotNull(android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            val changed = first.save(saved.copy(price = "260000", purpose = "rent"), true)
            assertEquals("260000", second.listings().single { it.id == changed.id }.price)
            try { second.delete(changed); fail("Another owner deleted the listing") }
            catch (_: SupabaseBackend.BackendException) { }
            first.delete(changed)
            assertFalse(second.listings().any { it.id == changed.id })
            saved = null
        } finally {
            saved?.let { runCatching { first.delete(it) } }
            runCatching { first.signOut() }; runCatching { second.signOut() }
            file.delete()
            for (name in listOf("diar_live_test_a", "diar_live_test_b"))
                context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
        }
    }
}
