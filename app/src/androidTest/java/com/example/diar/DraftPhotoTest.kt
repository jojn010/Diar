package com.example.diar

import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DraftPhotoTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun photo(): File {
        val source = File(context.cacheDir, "draft-${UUID.randomUUID()}.jpg")
        val bitmap = Bitmap.createBitmap(40, 30, Bitmap.Config.ARGB_8888)
        try {
            source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            return PropertyPhotos(context).import(Uri.fromFile(source))
        } finally { bitmap.recycle(); source.delete() }
    }
    @Test fun replacementRotationAndStepBackPreserveOnlyTheActiveDraft() {
        val first = photo()
        val second = photo()
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { a ->
                    a.navigate(View(a).apply { tag = "home" })
                    a.navigate(View(a).apply { tag = "sell" })
                    a.adoptDraftPhoto(first.name)
                    a.adoptDraftPhoto(second.name)
                    assertFalse(first.exists())
                    assertTrue(second.exists())
                    a.findViewById<EditText>(R.id.property_title).setText("Draft home")
                    a.findViewById<EditText>(R.id.property_area).setText("120")
                    a.findViewById<View>(R.id.composer_next).performClick()
                }
                scenario.recreate()
                scenario.onActivity { a ->
                    assertTrue(second.exists())
                    a.onBackPressedDispatcher.onBackPressed() // Location -> home details.
                    assertTrue(second.exists())
                    a.onBackPressedDispatcher.onBackPressed() // Leave the composer.
                    assertFalse(second.exists())
                }
            }
        } finally { first.delete(); second.delete() }
    }
    @Test fun navigatingAwayOrFinishingDiscardsUnsavedPhotos() {
        val navigatedAway = photo()
        val finished = photo()
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { a ->
                    a.navigate(View(a).apply { tag = "sell" })
                    a.adoptDraftPhoto(navigatedAway.name)
                    a.navigate(View(a).apply { tag = "home" })
                    assertFalse(navigatedAway.exists())
                    a.navigate(View(a).apply { tag = "sell" })
                    a.adoptDraftPhoto(finished.name)
                }
            }
            assertFalse(finished.exists())
        } finally { navigatedAway.delete(); finished.delete() }
    }
    @Test fun discardNeverDeletesAPhotoReferencedByASavedListing() {
        val file = photo()
        val store = PropertyStore(context)
        val id = UUID.randomUUID().toString()
        try {
            assertTrue(store.save(Property(id, "Saved home", "Ramallah", "120", "200000", "", file.name)))
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { a ->
                    a.navigate(View(a).apply { tag = "sell" })
                    a.adoptDraftPhoto(file.name)
                    a.navigate(View(a).apply { tag = "home" })
                    assertTrue(file.exists())
                    assertEquals(file.name, store.all().first { it.id == id }.photo)
                }
            }
        } finally { store.delete(id); file.delete() }
    }
}
