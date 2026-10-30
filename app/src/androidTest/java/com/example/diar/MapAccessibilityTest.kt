package com.example.diar

import android.view.View
import android.webkit.WebView
import android.widget.EditText
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapAccessibilityTest {
    @Test fun mapZoomLabelsAndDocumentLanguageFollowTheAppLanguage() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            try {
                for ((language, zoomIn, zoomOut) in listOf(Triple("ar", "تكبير", "تصغير"), Triple("en", "Zoom in", "Zoom out"))) {
                    scenario.onActivity { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language)) }
                    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                    var applied = false
                    for (attempt in 0..40) {
                        scenario.onActivity { applied = it.resources.configuration.locales[0].language == language }
                        if (applied) break
                        android.os.SystemClock.sleep(100)
                    }
                    check(applied) { "Language did not apply: $language" }
                    scenario.recreate()
                    scenario.onActivity { activity ->
                        activity.navigate(View(activity).apply { tag = "sell" })
                        activity.findViewById<EditText>(R.id.property_title).setText("Accessibility home")
                        activity.findViewById<EditText>(R.id.property_area).setText("120")
                        activity.findViewById<View>(R.id.composer_next).performClick()
                        activity.findViewById<View>(R.id.choose_map_location).performClick()
                    }
                    onView(withId(R.id.location_web_map)).perform(object : ViewAction {
                        override fun getConstraints(): org.hamcrest.Matcher<View> = isAssignableFrom(WebView::class.java)
                        override fun getDescription() = "Inspect the map's accessible zoom labels in $language"
                        override fun perform(ui: UiController, view: View) {
                            val web = view as WebView
                            var ready = false
                            for (attempt in 0..40) {
                                web.evaluateJavascript("typeof choose") { ready = it == "\"function\"" }
                                ui.loopMainThreadForAtLeast(100)
                                if (ready) break
                            }
                            check(ready) { "Map did not initialize" }
                            var labels: JSONObject? = null
                            web.evaluateJavascript("""({
                                zoomIn: document.querySelector('.leaflet-control-zoom-in').getAttribute('aria-label'),
                                zoomOut: document.querySelector('.leaflet-control-zoom-out').getAttribute('aria-label'),
                                inTitle: document.querySelector('.leaflet-control-zoom-in').title,
                                outTitle: document.querySelector('.leaflet-control-zoom-out').title,
                                language: document.documentElement.lang
                            })""") { labels = JSONObject(it) }
                            for (attempt in 0..20) {
                                ui.loopMainThreadForAtLeast(50)
                                if (labels != null) break
                            }
                            val actual = checkNotNull(labels)
                            assertEquals(zoomIn, actual.getString("zoomIn"))
                            assertEquals(zoomOut, actual.getString("zoomOut"))
                            assertEquals(zoomIn, actual.getString("inTitle"))
                            assertEquals(zoomOut, actual.getString("outTitle"))
                            assertEquals(language, actual.getString("language"))
                        }
                    })
                    onView(withId(android.R.id.button2)).perform(androidx.test.espresso.action.ViewActions.click())
                }
            } finally {
                scenario.onActivity {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                }
            }
        }
    }
}
