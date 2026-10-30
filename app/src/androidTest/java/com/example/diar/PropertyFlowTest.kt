package com.example.diar

import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PropertyFlowTest {
    private fun screenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(650)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val directory = java.io.File(instrumentation.targetContext.getExternalFilesDir(null), "listing-previews").apply { mkdirs() }
        java.io.FileOutputStream(java.io.File(directory, "$name.png")).use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
    @Test fun createValidatePersistAndShowBothLanguages() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = PropertyStore(context)
        val originalIds = store.all().map { it.id }.toSet()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            try {
                for (lang in listOf("en", "ar")) {
                    scenario.onActivity { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang)) }
                    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                    scenario.onActivity { a ->
                        a.navigate(View(a).apply { tag = "sell" })
                        a.findViewById<View>(R.id.listing_submit).performClick()
                        assertNotNull(a.findViewById<EditText>(R.id.property_title).error)
                        a.findViewById<EditText>(R.id.property_title).setText(if (lang == "ar") "بيت الاختبار" else "Test garden home")
                        a.findViewById<EditText>(R.id.property_location).setText(if (lang == "ar") "رام الله" else "Ramallah")
                        a.findViewById<EditText>(R.id.property_area).setText("0")
                        a.findViewById<EditText>(R.id.property_price).setText("450000")
                        a.findViewById<View>(R.id.listing_submit).performClick()
                        assertNotNull(a.findViewById<EditText>(R.id.property_area).error)
                        a.findViewById<EditText>(R.id.property_area).setText(if (lang == "ar") "١٨٥٫٥" else "185.5")
                    }
                    scenario.recreate()
                    screenshot("sell-$lang")
                    scenario.onActivity { a ->
                        assertTrue(a.findViewById<EditText>(R.id.property_title).text.isNotBlank())
                        a.findViewById<View>(R.id.listing_submit).performClick()
                        assertNotNull(a.findViewById<View>(R.id.property_content))
                        assertEquals("185.5", store.all().first().area)
                        assertEquals("450000", store.all().first().price)
                    }
                    screenshot("detail-$lang")
                    scenario.recreate()
                    scenario.onActivity { a ->
                        assertNotNull(a.findViewById<View>(R.id.property_content))
                        a.navigate(View(a).apply { tag = "listings" })
                        val collection = a.findViewById<LinearLayout>(R.id.own_listings)
                        assertTrue(collection.childCount >= 3)
                        collection.getChildAt(2).performClick()
                        assertNotNull(a.findViewById<View>(R.id.property_content))
                        a.navigate(View(a).apply { tag = "search" })
                        val query = a.findViewById<EditText>(R.id.search_query)
                        query.setText(if (lang == "ar") "بيت الاختبار" else "Test garden home")
                        val results = a.findViewById<LinearLayout>(R.id.search_results)
                        val matches = (0 until results.childCount).map { results.getChildAt(it) }
                            .filter { it.visibility == View.VISIBLE && it is android.widget.Button }
                        assertEquals(1, matches.size)
                        matches.first().performClick()
                        val content = a.findViewById<LinearLayout>(R.id.property_content)
                        content.getChildAt(content.childCount - 1).performClick()
                    }
                    androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(android.R.id.button1))
                        .perform(androidx.test.espresso.action.ViewActions.click())
                    assertEquals(originalIds, store.all().map { it.id }.toSet())
                }
            } finally {
                store.all().filterNot { it.id in originalIds }.forEach { store.delete(it.id) }
                scenario.onActivity { AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList()) }
            }
        }
    }
}
