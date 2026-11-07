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
@LiveProjectTest
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
    @Test fun anotherOwnersPublishedHomeCanBeSavedWithoutShowingOwnerControls() {
        assumeTrue(BuildConfig.SUPABASE_URL.isNotEmpty())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val backend = SupabaseBackend(context)
        val property = backend.listings().firstOrNull { it.ownerId != backend.userId }
        assumeTrue(property != null)
        property!!
        PropertyStore(context, "diar_public_cache").save(property)
        val favorites = FavoriteStore(context, backend.userId.ifEmpty { "guest" })
        val wasSaved = favorites.contains(property.id)
        fun texts(view: View): List<android.widget.TextView> = when (view) {
            is android.view.ViewGroup -> (0 until view.childCount).flatMap { texts(view.getChildAt(it)) }
            is android.widget.TextView -> listOf(view)
            else -> emptyList()
        }
        try {
            favorites.set(property.id, false)
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { a ->
                    a.navigate(View(a).apply { tag = "home" })
                    val title = texts(a.findViewById(R.id.own_listings)).single { it.text.toString() == property.title }
                    (title.parent as View).performClick()
                    val detail = texts(a.findViewById(R.id.property_content))
                    assertFalse(detail.any { it.text == a.getString(R.string.listing_remove) })
                    assertFalse(detail.any { it.text == a.getString(R.string.listing_photo) })
                    assertFalse(detail.any { it.text == a.getString(R.string.location_details) })
                    assertTrue(detail.any { it.text == a.getString(R.string.listing_share) })
                    assertTrue(detail.any { it.text == a.getString(R.string.listing_view_map) })
                    a.findViewById<View>(R.id.property_favorite).performClick()
                    assertTrue(favorites.contains(property.id))
                    a.navigate(View(a).apply { tag = "favorites" })
                    assertTrue(texts(a.findViewById(R.id.saved_items)).any { it.text.toString() == property.title })
                }
            }
        } finally { favorites.set(property.id, wasSaved) }
    }

}
