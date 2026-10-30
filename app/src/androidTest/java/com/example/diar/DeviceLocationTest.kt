package com.example.diar

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.view.View
import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceLocationTest {
    private fun shell(command: String) {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
    }
    @Suppress("DEPRECATION")
    @Test fun currentLocationUsesFreshFixAndSurvivesRecreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val permission = Manifest.permission.ACCESS_COARSE_LOCATION
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        shell("pm grant ${context.packageName} $permission")
        shell("appops set ${context.packageName} android:mock_location allow")
        try {
            manager.addTestProvider(LocationManager.GPS_PROVIDER, false, false, false, false, true, true, true, 1, 1)
            manager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { a ->
                    a.navigate(View(a).apply { tag = "sell" })
                    a.findViewById<EditText>(R.id.property_title).setText("Location test")
                    a.findViewById<EditText>(R.id.property_area).setText("120")
                    a.findViewById<View>(R.id.composer_next).performClick()
                    a.findViewById<View>(R.id.use_current_location).performClick()
                }
                var selected = false
                for (attempt in 0..40) {
                    manager.setTestProviderLocation(LocationManager.GPS_PROVIDER, Location(LocationManager.GPS_PROVIDER).apply {
                        latitude = 31.77; longitude = 35.21; accuracy = 5f
                        time = System.currentTimeMillis(); elapsedRealtimeNanos = android.os.SystemClock.elapsedRealtimeNanos()
                    })
                    android.os.SystemClock.sleep(150)
                    scenario.onActivity { selected = it.hasPropertyPin() }
                    if (selected) break
                }
                assertTrue("Fresh device location should select a pin", selected)
                scenario.recreate()
                scenario.onActivity { assertTrue(it.hasPropertyPin()) }
            }
        } finally {
            try { manager.removeTestProvider(LocationManager.GPS_PROVIDER) } catch (_: Exception) { }
            shell("appops set ${context.packageName} android:mock_location default")
            // Runtime permission is intentionally retained for the emulator test app:
            // revoking it here kills the instrumentation process on Android.
        }
    }
}
