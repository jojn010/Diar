package com.example.diar

import android.content.Intent
import android.view.View
import android.widget.FrameLayout
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreenNavigationTest {
    @Test fun allPrototypeScreensInflateAndNavigate() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val intent = Intent(instrumentation.targetContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val activity = instrumentation.startActivitySync(intent) as MainActivity
        val names = listOf("splash", "welcome", "signup", "login", "home", "listings",
            "notifications", "tour", "discover", "search", "search_empty", "map", "chat",
            "detail", "payment", "success", "favorites", "account", "conversation", "screens")
        names.forEach { name ->
            instrumentation.runOnMainSync {
                activity.navigate(View(activity).apply { tag = name })
                assertEquals("Screen $name must render", 1, activity.findViewById<FrameLayout>(R.id.main).childCount)
            }
            instrumentation.waitForIdleSync()
        }
        instrumentation.runOnMainSync { activity.finish() }
    }
}
