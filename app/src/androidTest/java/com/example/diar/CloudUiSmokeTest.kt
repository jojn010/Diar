package com.example.diar

import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Read-only smoke test against the configured project; never submits credentials or listings. */
@RunWith(AndroidJUnit4::class)
class CloudUiSmokeTest {
    @Test fun configuredCloudHasBilingualEmailLoginAndReachableListings() {
        assumeTrue(BuildConfig.SUPABASE_URL.isNotEmpty())
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assertTrue(SupabaseBackend(context).listings().all { it.ownerId.isNotEmpty() })
        val previous = AppCompatDelegate.getApplicationLocales()
        try {
            for (language in listOf("en", "ar")) {
                ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                    scenario.onActivity { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language)) }
                    instrumentation.waitForIdleSync()
                    var applied = false
                    for (attempt in 0..40) {
                        scenario.onActivity { applied = it.resources.configuration.locales[0].language == language }
                        if (applied) break
                        android.os.SystemClock.sleep(100)
                    }
                    check(applied) { "Language did not apply" }
                    scenario.recreate()
                    scenario.onActivity { activity ->
                        activity.navigate(View(activity).apply { tag = "login" })
                        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.login_password).visibility)
                        assertEquals(activity.getString(R.string.cloud_email), activity.findViewById<EditText>(R.id.login_phone).hint)
                        assertEquals(activity.getString(R.string.cloud_sign_in), activity.findViewById<TextView>(R.id.login_title).text)
                        assertEquals(if (language == "ar") View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR,
                            activity.findViewById<View>(R.id.main).layoutDirection)
                        activity.findViewById<View>(R.id.login_submit).performClick()
                        assertNotNull(activity.findViewById<EditText>(R.id.login_phone).error)
                        activity.navigate(View(activity).apply { tag = "signup" })
                        assertEquals(View.GONE, activity.findViewById<View>(R.id.signup_phone).visibility)
                        activity.navigate(View(activity).apply { tag = "home" })
                        assertNotNull(activity.findViewById<View>(R.id.own_listings))
                    }
                }
            }
        } finally { instrumentation.runOnMainSync { AppCompatDelegate.setApplicationLocales(previous) } }
    }
}
