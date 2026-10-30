package com.example.diar

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import java.util.Locale

/** Bundled map code; only standard map tiles are fetched online. No backend or API key. */
class MapPicker(private val activity: Activity) {
    private var dialog: AlertDialog? = null
    fun dismiss() { dialog?.dismiss(); dialog = null }
    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    fun show(latitude: Double?, longitude: Double?, onPick: (Double, Double) -> Unit) {
        dismiss()
        val root = activity.layoutInflater.inflate(R.layout.dialog_location_map, null)
        val lat = root.findViewById<EditText>(R.id.map_latitude)
        val lon = root.findViewById<EditText>(R.id.map_longitude)
        val status = root.findViewById<TextView>(R.id.map_status)
        val web = root.findViewById<WebView>(R.id.location_web_map)
        if (latitude != null && longitude != null) {
            lat.setText(String.format(Locale.US, "%.6f", latitude))
            lon.setText(String.format(Locale.US, "%.6f", longitude))
        }
        val latStart = latitude ?: 31.7683
        val lonStart = longitude ?: 35.2137
        web.settings.apply {
            javaScriptEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            userAgentString += " Diar/1.2 (+https://github.com/jojn010/Diar)"
        }
        web.setOnTouchListener { view, event ->
            view.parent.requestDisallowInterceptTouchEvent(event.actionMasked != android.view.MotionEvent.ACTION_UP && event.actionMasked != android.view.MotionEvent.ACTION_CANCEL)
            false
        }
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = true
        }
        web.addJavascriptInterface(object {
            @JavascriptInterface fun select(latitude: Double, longitude: Double) {
                if (!validCoordinates(latitude, longitude)) return
                activity.runOnUiThread {
                    lat.setText(String.format(Locale.US, "%.6f", latitude))
                    lon.setText(String.format(Locale.US, "%.6f", longitude))
                    lat.error = null; lon.error = null
                }
            }
            @JavascriptInterface fun tilesLoaded() { activity.runOnUiThread { status.visibility = View.GONE } }
            @JavascriptInterface fun tilesFailed() { activity.runOnUiThread { status.setText(R.string.map_offline); status.visibility = View.VISIBLE } }
        }, "DiarPin")
        val css = activity.assets.open("leaflet.css").bufferedReader().use { it.readText() }
        val js = activity.assets.open("leaflet.js").bufferedReader().use { it.readText() }
        val html = """<!doctype html><html><head><meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1"><style>
            html,body,#map{height:100%;margin:0;background:#e0ebdd;} $css
            </style></head><body><div id="map"></div><script>$js</script><script>
            const map=L.map('map').setView([$latStart,$lonStart],14);
            let pin=null;
            function choose(lat,lon){
              if(pin)map.removeLayer(pin);
              pin=L.circleMarker([lat,lon],{radius:10,color:'#195D60',weight:4,fillColor:'#EBC46D',fillOpacity:1}).addTo(map);
              DiarPin.select(lat,lon);
            }
            const tiles=L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,
              attribution:'&copy; OpenStreetMap contributors'}).addTo(map);
            tiles.on('tileload',()=>DiarPin.tilesLoaded());tiles.on('tileerror',()=>DiarPin.tilesFailed());
            map.on('click',e=>choose(e.latlng.lat,e.latlng.lng));
            ${if (latitude != null && longitude != null) "choose($latStart,$lonStart);" else ""}
            </script></body></html>"""
        // All executable content is bundled; navigation is blocked and the bridge accepts coordinates only.
        web.loadDataWithBaseURL("https://github.com/jojn010/Diar/", html, "text/html", "UTF-8", null)
        val popup = AlertDialog.Builder(activity).setTitle(R.string.map_title).setView(root)
            .setNegativeButton(R.string.listing_cancel, null).setPositiveButton(R.string.map_use, null).create()
        dialog = popup
        popup.setOnShowListener {
            popup.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val latitude = coordinate(lat.text.toString())
                val longitude = coordinate(lon.text.toString())
                if (latitude == null || longitude == null || !validCoordinates(latitude, longitude)) {
                    lat.error = activity.getString(R.string.map_invalid); lat.requestFocus()
                } else { onPick(latitude, longitude); popup.dismiss() }
            }
        }
        popup.setOnDismissListener { web.removeJavascriptInterface("DiarPin"); web.stopLoading(); web.destroy(); dialog = null }
        popup.show()
        popup.window?.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    companion object {
        fun validCoordinates(latitude: Double, longitude: Double) = latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0
        fun coordinate(text: String): Double? = text.trim().map { c ->
            when { c == '٫' -> '.'; c == '−' -> '-'; Character.digit(c, 10) >= 0 -> ('0'.code + Character.digit(c, 10)).toChar(); else -> c }
        }.joinToString("").toDoubleOrNull()?.takeIf { it.isFinite() }
    }
}
