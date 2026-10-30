package com.example.diar

import android.view.View
import android.webkit.WebView
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.assertion.ViewAssertions.matches
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
        val directory = java.io.File(instrumentation.targetContext.getExternalFilesDir(null), "composer-previews").apply { mkdirs() }
        java.io.FileOutputStream(java.io.File(directory, "$name.png")).use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
    private fun chooseOnMap() {
        // Exercise the real bundled map JavaScript -> native pin bridge, including invalid input.
        onView(withId(R.id.map_latitude)).perform(replaceText("95"), closeSoftKeyboard())
        onView(withId(R.id.map_longitude)).perform(replaceText("35"), closeSoftKeyboard())
        onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
        onView(withId(R.id.map_latitude)).check(matches(hasErrorText(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.map_invalid))))
        onView(withId(R.id.location_web_map)).perform(object : androidx.test.espresso.ViewAction {
            override fun getConstraints(): org.hamcrest.Matcher<View> = isAssignableFrom(WebView::class.java)
            override fun getDescription() = "Choose a coordinate through the live map"
            override fun perform(ui: androidx.test.espresso.UiController, view: View) {
                var ready = false
                for (attempt in 0..40) {
                    (view as WebView).evaluateJavascript("typeof choose") { ready = it == "\"function\"" }
                    ui.loopMainThreadForAtLeast(100)
                    if (ready) break
                }
                check(ready) { "Bundled map did not initialize" }
                (view as WebView).evaluateJavascript("map.fire('click',{latlng:L.latLng(31.77,35.21)})", null)
                ui.loopMainThreadForAtLeast(300)
            }
        })
        onView(withId(R.id.map_latitude)).check(matches(withText("31.770000")))
        screenshot("map-picker")
        onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
    }
    @Test fun createValidatePersistSearchAndDeleteBothLanguages() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = PropertyStore(context)
        val originalIds = store.all().map { it.id }.toSet()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            try {
                val cases = listOf(Triple("en", R.id.type_house, R.id.floor_two), Triple("ar", R.id.type_villa, R.id.floor_more), Triple("en", R.id.type_apartment, R.id.floor_one))
                for ((index, data) in cases.withIndex()) {
                    val (lang, type, floors) = data
                    val title = if (lang == "ar") "بيت الاختبار" else "Test garden home"
                    scenario.onActivity { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang)) }
                    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                    scenario.onActivity { a ->
                        a.navigate(View(a).apply { tag = "sell" })
                        a.findViewById<View>(R.id.composer_next).performClick()
                        assertNotNull(a.findViewById<EditText>(R.id.property_title).error)
                        a.findViewById<EditText>(R.id.property_title).setText(title)
                        a.findViewById<RadioGroup>(R.id.property_type).check(type)
                        a.findViewById<RadioGroup>(R.id.property_floors).check(floors)
                        a.findViewById<RadioGroup>(R.id.property_purpose).check(if (lang == "ar") R.id.purpose_rent else R.id.purpose_sale)
                        a.findViewById<EditText>(R.id.property_area).setText("0")
                        a.findViewById<View>(R.id.composer_next).performClick()
                        assertNotNull(a.findViewById<EditText>(R.id.property_area).error)
                        a.findViewById<EditText>(R.id.property_area).setText(if (lang == "ar") "١٨٥٫٥" else "185.5")
                    }
                    screenshot("home-$lang-$index")
                    scenario.onActivity { a ->
                        a.findViewById<View>(R.id.composer_next).performClick()
                        a.findViewById<View>(R.id.composer_next).performClick()
                        assertEquals(View.VISIBLE, a.findViewById<View>(R.id.composer_location).visibility)
                        assertFalse(a.hasPropertyPin())
                        a.findViewById<View>(R.id.choose_map_location).performClick()
                    }
                    chooseOnMap()
                    scenario.onActivity { a ->
                        assertTrue(a.hasPropertyPin())
                        a.findViewById<EditText>(R.id.property_location).setText(if (lang == "ar") "رام الله" else "Ramallah")
                    }
                    scenario.recreate()
                    scenario.onActivity { a ->
                        assertTrue(a.hasPropertyPin())
                        assertEquals(View.VISIBLE, a.findViewById<View>(R.id.composer_location).visibility)
                        a.findViewById<View>(R.id.composer_next).performClick()
                        a.findViewById<EditText>(R.id.property_price).setText("1500.5")
                        a.findViewById<View>(R.id.listing_submit).performClick()
                        assertNotNull(a.findViewById<EditText>(R.id.property_price).error)
                        a.findViewById<EditText>(R.id.property_price).setText(if (lang == "ar") "١٥٠٠" else "450000")
                        a.findViewById<CheckBox>(R.id.property_negotiable).isChecked = true
                    }
                    scenario.recreate()
                    screenshot("finish-$lang-$index")
                    scenario.onActivity { a ->
                        assertTrue(a.findViewById<CheckBox>(R.id.property_negotiable).isChecked)
                        assertEquals(View.VISIBLE, a.findViewById<View>(R.id.composer_finish).visibility)
                        a.findViewById<View>(R.id.listing_submit).performClick()
                        assertNotNull(a.findViewById<View>(R.id.property_content))
                        val property = store.all().first()
                        assertEquals("185.5", property.area)
                        assertEquals(if (lang == "ar") "1500" else "450000", property.price)
                        assertTrue(property.negotiable)
                        assertEquals(if (lang == "ar") "rent" else "sale", property.purpose)
                        assertEquals(listOf("house", "villa", "apartment")[index], property.type)
                        assertEquals(listOf(2, 3, 1)[index], property.floors)
                        assertEquals(31.77, property.latitude!!, 0.000001)
                        assertEquals(35.21, property.longitude!!, 0.000001)
                        assertEquals("map", property.locationSource)
                    }
                    screenshot("detail-$lang-$index")
                    scenario.recreate()
                    scenario.onActivity { a ->
                        assertNotNull(a.findViewById<View>(R.id.property_content))
                        a.navigate(View(a).apply { tag = "my_listings" })
                        val collection = a.findViewById<LinearLayout>(R.id.own_listings)
                        assertTrue(collection.childCount >= 3)
                    }
                    screenshot("collection-$lang-$index")
                    scenario.onActivity { a ->
                        a.findViewById<LinearLayout>(R.id.own_listings).getChildAt(2).performClick()
                        assertNotNull(a.findViewById<View>(R.id.property_content))
                        a.navigate(View(a).apply { tag = "search" })
                        a.findViewById<EditText>(R.id.search_query).setText(title)
                        val results = a.findViewById<LinearLayout>(R.id.search_results)
                        val matches = (0 until results.childCount).map { results.getChildAt(it) }
                            .filter { it.visibility == View.VISIBLE && it is Button }
                        assertEquals(1, matches.size)
                        matches.first().performClick()
                        val content = a.findViewById<LinearLayout>(R.id.property_content)
                        content.getChildAt(content.childCount - 1).performClick()
                    }
                    onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
                    assertEquals(originalIds, store.all().map { it.id }.toSet())
                }
            } finally {
                store.all().filterNot { it.id in originalIds }.forEach { store.delete(it.id) }
                scenario.onActivity { AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList()) }
            }
        }
    }
}
