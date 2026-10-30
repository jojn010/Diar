package com.example.diar

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PropertyStorageTest {
    @Test fun existingListingsMigrateWithoutLosingPhotosOrPrices() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("diar_properties", 0)
        val original = prefs.getString("items", "[]")
        try {
            prefs.edit().putString("items", """[{"id":"legacy","title":"Old home","location":"Ramallah","area":"120","price":"230000.50","description":"Kept","photo":"original.jpg"}]""").commit()
            val property = PropertyStore(context).all().single()
            assertEquals("original.jpg", property.photo)
            assertEquals("230000.50", property.price)
            assertFalse(property.negotiable)
            assertEquals("sale", property.purpose)
            assertEquals("unspecified", property.type)
            assertNull(property.latitude)
            assertTrue(PropertyStore(context).save(property.copy(negotiable = true, purpose = "rent", floors = 2)))
            assertTrue(PropertyStore(context).all().single().negotiable)
            assertEquals("original.jpg", PropertyStore(context).all().single().photo)
        } finally { prefs.edit().putString("items", original).commit() }
    }
    @Test fun pickedPhotosStayLocalAndCameraRotationIsApplied() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File(context.cacheDir, "photo-import-test.jpg")
        var output: File? = null
        val bitmap = Bitmap.createBitmap(2400, 1200, Bitmap.Config.ARGB_8888)
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        ExifInterface(source.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString()); saveAttributes()
        }
        try {
            output = PropertyPhotos(context).import(Uri.fromFile(source))
            assertEquals(context.filesDir, output.parentFile)
            source.delete()
            val decoded = BitmapFactory.decodeFile(output.absolutePath)
            assertEquals(600, decoded.width)
            assertEquals(1200, decoded.height)
            decoded.recycle()
        } finally { source.delete(); output?.delete() }
    }
}
