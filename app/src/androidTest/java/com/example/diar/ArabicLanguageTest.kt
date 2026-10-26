package com.example.diar

import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArabicLanguageTest {
    private fun selectLanguage(scenario: ActivityScenario<MainActivity>, label: String, language: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        scenario.onActivity { it.findViewById<View>(R.id.change_language).performClick() }
        instrumentation.waitForIdleSync()
        var option: AccessibilityNodeInfo? = null
        val dialogDeadline = android.os.SystemClock.uptimeMillis() + 5000
        while (option == null && android.os.SystemClock.uptimeMillis() < dialogDeadline) {
            option = instrumentation.uiAutomation.rootInActiveWindow
                ?.findAccessibilityNodeInfosByText(label)?.firstOrNull { it.text?.toString() == label }
            if (option == null) android.os.SystemClock.sleep(100)
        }
        checkNotNull(option) { "Language option $label was not displayed" }
        val bounds = android.graphics.Rect()
        option.getBoundsInScreen(bounds)
        val time = android.os.SystemClock.uptimeMillis()
        for (action in listOf(android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_UP)) {
            val event = android.view.MotionEvent.obtain(time, android.os.SystemClock.uptimeMillis(), action,
                bounds.exactCenterX(), bounds.exactCenterY(), 0)
            event.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
            check(instrumentation.uiAutomation.injectInputEvent(event, true))
            event.recycle()
        }
        val deadline = android.os.SystemClock.uptimeMillis() + 5000
        var applied = false
        while (!applied && android.os.SystemClock.uptimeMillis() < deadline) {
            instrumentation.waitForIdleSync()
            scenario.onActivity { applied = it.resources.configuration.locales[0].language == language }
            if (!applied) android.os.SystemClock.sleep(100)
        }
        check(applied) { "Language switch did not apply $language" }
    }
    @Test fun languageSwitchRtlSearchValidationAndRecreation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            try {
                scenario.onActivity { it.navigate(View(it).apply { tag = "account" }) }
                selectLanguage(scenario, "العربية", "ar")
                scenario.recreate()
                scenario.onActivity { assertEquals("ar", it.resources.configuration.locales[0].language) }

                val screens = listOf("splash", "welcome", "signup", "login", "home", "listings",
                    "notifications", "tour", "discover", "search", "search_empty", "map", "chat",
                    "detail", "payment", "success", "favorites", "account", "conversation", "screens")
                for (name in screens) {
                    scenario.onActivity { activity ->
                        activity.navigate(View(activity).apply { tag = name })
                        assertEquals("ar", activity.resources.configuration.locales[0].language)
                        assertEquals(View.LAYOUT_DIRECTION_RTL, activity.resources.configuration.layoutDirection)
                        assertEquals(1, activity.findViewById<FrameLayout>(R.id.main).childCount)
                        assertEquals("الرئيسية", activity.getString(R.string.nav_home))
                    }
                    instrumentation.waitForIdleSync()
                    if (InstrumentationRegistry.getArguments().getString("screenshots") == "true") {
                        android.os.SystemClock.sleep(250)
                        val bitmap = instrumentation.uiAutomation.takeScreenshot()
                        val directory = java.io.File(instrumentation.targetContext.getExternalFilesDir(null), "arabic-previews").apply { mkdirs() }
                        java.io.FileOutputStream(java.io.File(directory, "$name.png")).use {
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                        }
                        bitmap.recycle()
                    }
                }
                scenario.onActivity { activity ->
                    activity.navigate(View(activity).apply { tag = "search" })
                    val query = activity.findViewById<EditText>(R.id.search_query)
                    val results = activity.findViewById<LinearLayout>(R.id.search_results)
                    query.setText("القدس")
                    assertEquals(View.VISIBLE, results.getChildAt(0).visibility)
                    query.setText("الزيتون")
                    assertEquals(View.VISIBLE, results.getChildAt(0).visibility)
                    assertEquals(View.GONE, results.getChildAt(1).visibility)
                    query.setText("لايوجدهذاالعقار")
                    assertEquals(activity.getString(R.string.search_no_matches), (results.getChildAt(3) as TextView).text)
                    assertEquals(View.VISIBLE, results.getChildAt(3).visibility)
                    activity.navigate(View(activity).apply { tag = "login" })
                    activity.findViewById<Button>(R.id.login_submit).performClick()
                    assertEquals("أدخل رقم هاتف صالحاً", activity.findViewById<EditText>(R.id.login_phone).error.toString())
                    activity.navigate(View(activity).apply { tag = "payment" })
                    mapOf(R.id.card_name to "اسم تجريبي", R.id.card_number to "٤٢٤٢٤٢٤٢٤٢٤٢٤٢٤٢",
                        R.id.card_expiry to "١٢/٣٠", R.id.card_cvc to "١٢٣", R.id.country to "فلسطين")
                        .forEach { (id, text) -> activity.findViewById<EditText>(id).setText(text) }
                    activity.findViewById<Button>(R.id.payment_submit).performClick()
                    assertEquals(null, activity.findViewById<View>(R.id.payment_submit))
                    activity.navigate(View(activity).apply { tag = "account" })
                }
                selectLanguage(scenario, "English", "en")
                scenario.onActivity {
                    assertEquals(View.LAYOUT_DIRECTION_LTR, it.resources.configuration.layoutDirection)
                    assertEquals("Home", it.getString(R.string.nav_home))
                    assertNotNull(it.findViewById<View>(R.id.change_language))
                }
            } finally {
                scenario.onActivity { AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList()) }
            }
        }
    }
}
