package com.example.diar

import android.view.View
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapWrapTest {
    @Test fun mapClicksOnRepeatedWorldCopiesConfirmTheSameLocation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var selectedLatitude: Double? = null
            var selectedLongitude: Double? = null
            var picker: MapPicker? = null
            try {
                scenario.onActivity { activity ->
                    picker = MapPicker(activity).also { it.show(null, null) { lat, lon ->
                        selectedLatitude = lat; selectedLongitude = lon
                    } }
                }
                for (longitude in listOf(395.21, -324.79, 35.21)) {
                    onView(withId(R.id.location_web_map)).perform(object : ViewAction {
                        override fun getConstraints(): org.hamcrest.Matcher<View> = isAssignableFrom(WebView::class.java)
                        override fun getDescription() = "Tap a map world copy at longitude $longitude"
                        override fun perform(ui: UiController, view: View) {
                            var ready = false
                            val web = view as WebView
                            for (attempt in 0..40) {
                                web.evaluateJavascript("typeof choose") { ready = it == "\"function\"" }
                                ui.loopMainThreadForAtLeast(100)
                                if (ready) break
                            }
                            check(ready) { "Bundled map did not initialize" }
                            web.evaluateJavascript("map.fire('click',{latlng:L.latLng(31.77,$longitude)})", null)
                            ui.loopMainThreadForAtLeast(300)
                            var markerLongitude: Double? = null
                            web.evaluateJavascript("pin.getLatLng().lng") { markerLongitude = it.toDouble() }
                            ui.loopMainThreadForAtLeast(100)
                            assertEquals(longitude, markerLongitude!!, 0.000001)
                        }
                    })
                    onView(withId(R.id.map_latitude)).check(matches(withText("31.770000")))
                    onView(withId(R.id.map_longitude)).check(matches(withText("35.210000")))
                }
                onView(withId(android.R.id.button1)).perform(click())
                assertEquals(31.77, selectedLatitude!!, 0.000001)
                assertEquals(35.21, selectedLongitude!!, 0.000001)
            } finally { scenario.onActivity { picker?.dismiss() } }
        }
    }
}
