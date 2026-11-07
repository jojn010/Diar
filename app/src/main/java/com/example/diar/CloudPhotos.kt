package com.example.diar

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.widget.ImageView
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/** Images only come from this build's Supabase storage. Download and decode off the UI thread. */
object CloudPhotos {
    private val workers = Executors.newFixedThreadPool(2)
    private val main = Handler(Looper.getMainLooper())
    private val cache = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    fun load(view: ImageView, url: String) {
        view.tag = url
        view.setImageResource(R.drawable.house_garden)
        cache.get(url)?.let { view.setImageBitmap(it); return }
        workers.execute {
            val bitmap = runCatching {
                val connection = URL(url).openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = 10000; connection.readTimeout = 15000
                    connection.instanceFollowRedirects = false
                    check(connection.responseCode == 200)
                    val bytes = connection.inputStream.use { it.readBytesCapped(5 * 1024 * 1024) }
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                    check(options.outWidth > 0 && options.outHeight > 0)
                    options.inSampleSize = 1
                    while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > 1600) options.inSampleSize *= 2
                    options.inJustDecodeBounds = false
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                } finally { connection.disconnect() }
            }.getOrNull()
            if (bitmap != null) { cache.put(url, bitmap); main.post { if (view.tag == url) view.setImageBitmap(bitmap) } }
        }
    }
    private fun java.io.InputStream.readBytesCapped(max: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            check(output.size() + count <= max)
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
}
