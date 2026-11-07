package com.example.diar

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class FavoriteFlowTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun texts(view: View): List<TextView> = when (view) {
        is ViewGroup -> (0 until view.childCount).flatMap { texts(view.getChildAt(it)) }
        is TextView -> listOf(view)
        else -> emptyList()
    }
    @Test fun savedListingsSurviveRecreationOpenTheSameDetailsAndCanBeRemovedInBothLanguages() {
        val property = Property(UUID.randomUUID().toString(), "Courtyard QA home", "QA location", "120", "250000", "", "",
            latitude = 0.0, longitude = 0.0, locationSource = "map")
        val store = PropertyStore(context)
        val favorites = FavoriteStore(context, "offline")
        val previous = AppCompatDelegate.getApplicationLocales()
        assertTrue(store.save(property))
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                for (language in listOf("en", "ar")) {
                    scenario.onActivity { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language)) }
                    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                    var applied = false
                    for (attempt in 0..40) {
                        scenario.onActivity { applied = it.resources.configuration.locales[0].language == language }
                        if (applied) break
                        android.os.SystemClock.sleep(100)
                    }
                    check(applied)
                    scenario.recreate()
                    scenario.onActivity { a -> a.navigate(View(a).apply { tag = "home" }) }
                    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                    scenario.onActivity { a ->
                        val title = texts(a.findViewById(R.id.own_listings)).single { it.text.toString() == property.title }
                        if (InstrumentationRegistry.getArguments().getString("diarScreenshots") == "true") {
                            val real = (title.parent as View).parent as View
                            val sample = a.findViewById<View>(R.id.sample_card_home_0)
                            assertEquals(sample.findViewById<View>(R.id.card_photo).height, real.findViewById<View>(R.id.card_photo).height)
                            for ((name, view) in listOf("real" to real, "sample" to sample)) {
                                val bitmap = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
                                view.draw(android.graphics.Canvas(bitmap))
                                java.io.File(a.getExternalFilesDir(null), "card-$name-$language.png").outputStream().use {
                                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                                }
                                bitmap.recycle()
                            }
                        }
                        (title.parent as View).performClick()
                        a.findViewById<View>(R.id.property_favorite).performClick()
                        assertTrue(favorites.contains(property.id))
                        val detail = texts(a.findViewById(R.id.property_content))
                        assertTrue(detail.any { it.text == a.getString(R.string.listing_share) })
                        assertTrue(detail.any { it.text == a.getString(R.string.listing_view_map) })
                    }
                    scenario.recreate()
                    scenario.onActivity { a ->
                        assertTrue(a.findViewById<View>(R.id.property_favorite).isSelected)
                        a.navigate(View(a).apply { tag = "favorites" })
                        val title = texts(a.findViewById(R.id.saved_items)).single { it.text.toString() == property.title }
                        (title.parent as View).performClick()
                        assertTrue(a.findViewById<View>(R.id.property_favorite).isSelected)
                        a.findViewById<View>(R.id.property_favorite).performClick()
                        assertFalse(favorites.contains(property.id))
                        a.navigate(View(a).apply { tag = "favorites" })
                        assertFalse(texts(a.findViewById(R.id.saved_items)).any { it.text.toString() == property.title })
                    }
                }
            }
        } finally {
            favorites.set(property.id, false); store.delete(property.id)
            InstrumentationRegistry.getInstrumentation().runOnMainSync { AppCompatDelegate.setApplicationLocales(previous) }
        }
    }
    @Test fun favoritesAreAccountScopedAndDoNotKeepDeletedListingCopies() {
        val id = UUID.randomUUID().toString()
        val first = FavoriteStore(context, "qa-a-$id")
        val second = FavoriteStore(context, "qa-b-$id")
        try {
            assertTrue(first.set(id, true))
            assertTrue(FavoriteStore(context, "qa-a-$id").contains(id))
            assertFalse(second.contains(id))
            assertTrue(first.set(id, false))
            assertFalse(first.contains(id))
        } finally { first.set(id, false); second.set(id, false) }
    }
}
