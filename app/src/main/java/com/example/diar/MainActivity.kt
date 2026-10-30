package com.example.diar

import android.os.Bundle
import android.animation.ValueAnimator
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import java.io.File
import java.text.NumberFormat
import java.util.UUID
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AlertDialog
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged

/** Offline UI prototype: no authentication, payment or messaging service is contacted. */
class MainActivity : AppCompatActivity() {
    private val screens = mapOf(
        "sell" to R.layout.screen_sell, "my_property" to R.layout.screen_my_property,
        "splash" to R.layout.screen_splash, "welcome" to R.layout.screen_welcome,
        "signup" to R.layout.screen_signup, "login" to R.layout.screen_login,
        "home" to R.layout.screen_home, "listings" to R.layout.screen_listings,
        "notifications" to R.layout.screen_notifications, "tour" to R.layout.screen_tour,
        "discover" to R.layout.screen_discover, "search" to R.layout.screen_search,
        "search_empty" to R.layout.screen_search_empty, "map" to R.layout.screen_map,
        "chat" to R.layout.screen_chat, "detail" to R.layout.screen_detail,
        "payment" to R.layout.screen_payment, "success" to R.layout.screen_success,
        "favorites" to R.layout.screen_favorites, "account" to R.layout.screen_account,
        "conversation" to R.layout.screen_conversation, "screens" to R.layout.screen_screens
    )
    private val history = mutableListOf<String>()
    private var current = "splash"
    private val prefs by lazy { getSharedPreferences("diar_demo", MODE_PRIVATE) }
    private val messages = mutableListOf<String>()
    private val propertyStore by lazy { PropertyStore(this) }
    private var selectedProperty: String? = null
    private var selectedPhoto = ""
    private var tone: ToneGenerator? = null
    private val photoPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                // Decode bounds first to reject non-images and sample large camera photos safely.
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
                require(bounds.outWidth > 0 && bounds.outHeight > 0)
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 1
                    while (bounds.outWidth / inSampleSize > 1600 || bounds.outHeight / inSampleSize > 1600) inSampleSize *= 2
                }
                val bitmap = contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
                    ?: error("Invalid image")
                val file = File(filesDir, "property_${UUID.randomUUID()}.jpg")
                try { file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, it) } }
                finally { bitmap.recycle() }
                selectedPhoto = file.name
                updatePhotoPreview()
            } catch (_: Exception) { toast(getString(R.string.listing_photo_error)) }
        }
    }
    override fun onDestroy() { tone?.release(); tone = null; super.onDestroy() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        selectedProperty = savedInstanceState?.getString("selectedProperty")
        selectedPhoto = savedInstanceState?.getString("selectedPhoto") ?: ""
        history.addAll(savedInstanceState?.getStringArrayList("history") ?: emptyList())
        messages.addAll(savedInstanceState?.getStringArrayList("messages") ?: emptyList())
        show(savedInstanceState?.getString("screen") ?: "splash", false)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (history.isNotEmpty()) show(history.removeAt(history.lastIndex), false) else finish()
            }
        })
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("screen", current)
        outState.putString("selectedProperty", selectedProperty)
        outState.putString("selectedPhoto", selectedPhoto)
        outState.putStringArrayList("history", ArrayList(history))
        outState.putStringArrayList("messages", ArrayList(messages))
    }
    fun navigate(view: View) {
        if (view.tag == "sell" && current != "sell") selectedPhoto = ""
        show(view.tag.toString())
    }
    fun goBack(view: View) { onBackPressedDispatcher.onBackPressed() }
    private fun show(destination: String, push: Boolean = true) {
        val layout = screens[destination] ?: return
        val host = findViewById<FrameLayout>(R.id.main)
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(host.windowToken, 0)
        if (push && destination != current) history.add(current)
        current = destination
        host.removeAllViews()
        layoutInflater.inflate(layout, host, true)
        bind()
        animateEntrance(host.getChildAt(0))
    }
    private fun click(id: Int, action: () -> Unit) { findViewById<View>(id)?.setOnClickListener { action() } }
    private fun value(id: Int) = findViewById<EditText>(id).text.toString().trim()
    // Accept Arabic-Indic digits in the same demo validators as Western digits.
    private fun numericValue(id: Int) = value(id).map { character ->
        Character.digit(character, 10).takeIf { it >= 0 }?.let { ('0'.code + it).toChar() } ?: character
    }.joinToString("")
    private fun valid(id: Int, condition: Boolean, message: String): Boolean {
        if (!condition) { findViewById<EditText>(id).error = message; findViewById<EditText>(id).requestFocus() }
        return condition
    }
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    private fun chooseLanguage() {
        val languages = arrayOf(getString(R.string.language_system), getString(R.string.language_english), getString(R.string.language_arabic))
        val selected = when (AppCompatDelegate.getApplicationLocales().toLanguageTags()) { "en" -> 1; "ar" -> 2; else -> 0 }
        AlertDialog.Builder(this).setTitle(R.string.language_title)
            .setSingleChoiceItems(languages, selected) { dialog, index ->
                dialog.dismiss()
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(arrayOf("", "en", "ar")[index]))
            }.show()
    }
    private fun bind() {
        bindProperties()
        click(R.id.change_language) { chooseLanguage() }
        click(R.id.google) { toast(getString(R.string.google_demo)) }
        click(R.id.facebook) { toast(getString(R.string.facebook_demo)) }
        click(R.id.login_submit) {
            if (valid(R.id.login_phone, value(R.id.login_phone).count { it.isDigit() } >= 8, getString(R.string.error_phone))) show("home")
        }
        click(R.id.signup_submit) {
            when {
                !valid(R.id.full_name, value(R.id.full_name).length >= 2, getString(R.string.error_name)) -> Unit
                !valid(R.id.signup_phone, value(R.id.signup_phone).count { it.isDigit() } >= 8, getString(R.string.error_phone)) -> Unit
                !valid(R.id.email, android.util.Patterns.EMAIL_ADDRESS.matcher(value(R.id.email)).matches(), getString(R.string.error_email)) -> Unit
                !valid(R.id.password, value(R.id.password).length >= 6, getString(R.string.error_password)) -> Unit
                !findViewById<CheckBox>(R.id.terms).isChecked -> toast(getString(R.string.accept_terms))
                else -> show("home")
            }
        }
        findViewById<Button>(R.id.save_home)?.let { button ->
            fun update() { button.text = if (prefs.getBoolean("saved", false)) getString(R.string.home_saved) else getString(R.string.home_save) }
            update()
            button.setOnClickListener { prefs.edit().putBoolean("saved", !prefs.getBoolean("saved", false)).apply(); update()
                if (motionEnabled()) button.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).withEndAction {
                    button.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
                }.start() }
        }
        findViewById<LinearLayout>(R.id.saved_items)?.let { items ->
            if (!prefs.getBoolean("saved", false)) {
                items.removeAllViews()
                items.addView(TextView(this).apply { text = getString(R.string.favorites_empty); textSize = 18f; setPadding(0, 32, 0, 32) })
            }
        }
        click(R.id.clear_notifications) {
            findViewById<LinearLayout>(R.id.notification_items).removeAllViews()
            toast(getString(R.string.notifications_cleared))
        }
        findViewById<EditText>(R.id.search_query)?.let { query ->
            val results = findViewById<LinearLayout>(R.id.search_results)
            val rows = (0 until results.childCount).map { results.getChildAt(it) as Button }.toMutableList()
            rows.forEach { it.contentDescription = it.text.toString() + " · Jerusalem · القدس" }
            val empty = TextView(this).apply { text = getString(R.string.search_no_matches); textSize = 20f; setPadding(0, 40, 0, 40) }
            results.addView(empty)
            propertyStore.all().forEach { property ->
                rows.add(MaterialButton(this).apply {
                    text = property.title + " · " + property.location + " · " + propertyPrice(property)
                    contentDescription = text
                    minHeight = dp(56)
                    setOnClickListener { selectedProperty = property.id; show("my_property") }
                    results.addView(this)
                })
            }
            fun filter() {
                val term = query.text.toString().trim()
                var count = 0
                rows.forEach { row ->
                    val visible = term.isBlank() || row.text.contains(term, true) || row.contentDescription.toString().contains(term, true)
                    row.visibility = if (visible) View.VISIBLE else View.GONE
                    if (visible) count++
                }
                empty.visibility = if (count == 0) View.VISIBLE else View.GONE
            }
            query.doAfterTextChanged { filter() }
            filter()
        }
        click(R.id.payment_submit) {
            when {
                !valid(R.id.card_name, value(R.id.card_name).length >= 2, getString(R.string.error_card_name)) -> Unit
                !valid(R.id.card_number, value(R.id.card_number).filter { it.isDigit() }.length in 13..19, getString(R.string.error_card_number)) -> Unit
                !valid(R.id.card_expiry, Regex("(0[1-9]|1[0-2])/[0-9]{2}").matches(numericValue(R.id.card_expiry)), getString(R.string.error_expiry)) -> Unit
                !valid(R.id.card_cvc, value(R.id.card_cvc).length in 3..4, getString(R.string.error_cvc)) -> Unit
                !valid(R.id.country, value(R.id.country).isNotBlank(), getString(R.string.error_country)) -> Unit
                else -> show("success", false)
            }
        }
        if (current == "conversation") {
            messages.forEach { addMessage(it) }
            click(R.id.send_message) {
                val message = value(R.id.message_input)
                if (message.isNotEmpty()) {
                    messages.add(message); addMessage(message)
                    findViewById<EditText>(R.id.message_input).text.clear()
                    toast(getString(R.string.message_added))
                }
            }
        }
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun motionEnabled() = android.os.Build.VERSION.SDK_INT < 26 || ValueAnimator.areAnimatorsEnabled()
    private fun animateEntrance(root: View) {
        if (!motionEnabled()) return
        root.alpha = 0f
        root.translationY = dp(14).toFloat()
        root.animate().alpha(1f).translationY(0f).setDuration(280)
            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
        val content = if (root is android.view.ViewGroup) {
            val scroll = (0 until root.childCount).map { root.getChildAt(it) }.filterIsInstance<ScrollView>().firstOrNull()
            scroll?.getChildAt(0) as? android.view.ViewGroup
        } else null
        content?.let { group ->
            for (i in 0 until minOf(group.childCount, 7)) {
                group.getChildAt(i).apply {
                    alpha = 0f; translationY = dp(12).toFloat()
                    animate().alpha(1f).translationY(0f).setStartDelay(i * 35L).setDuration(280)
                        .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
                }
            }
        }
    }
    private fun updatePhotoPreview() {
        findViewById<ImageView>(R.id.listing_photo_preview)?.let { image ->
            loadPhoto(image, selectedPhoto)
            image.contentDescription = getString(if (selectedPhoto.isEmpty()) R.string.listing_sample else R.string.listing_photo_chosen)
        }
        findViewById<TextView>(R.id.listing_photo_status)?.setText(
            if (selectedPhoto.isEmpty()) R.string.listing_sample else R.string.listing_photo_chosen)
    }
    private fun loadPhoto(image: ImageView, path: String) {
        val file = File(filesDir, path)
        if (path.isNotEmpty() && file.isFile) image.setImageURI(android.net.Uri.fromFile(file))
        else image.setImageResource(R.drawable.house_garden)
    }
    private fun text(parent: LinearLayout, value: String, size: Float = 16f): TextView = TextView(this).apply {
        text = value; textSize = size; setTextColor(getColor(R.color.ink))
        setPadding(0, dp(8), 0, dp(8)); parent.addView(this)
    }
    private fun button(parent: LinearLayout, label: Int, action: () -> Unit) = MaterialButton(this).apply {
        setText(label); minHeight = dp(56); setOnClickListener { action() }; parent.addView(this)
    }
    private fun photo(parent: LinearLayout, property: Property, height: Int) {
        parent.addView(ShapeableImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, dp(height))
            scaleType = ImageView.ScaleType.CENTER_CROP
            shapeAppearanceModel = shapeAppearanceModel.toBuilder().setAllCornerSizes(dp(20).toFloat()).build()
            contentDescription = if (property.photo.isEmpty()) getString(R.string.listing_sample) else property.title
            loadPhoto(this, property.photo)
        })
        if (property.photo.isEmpty()) text(parent, getString(R.string.listing_sample), 12f)
    }
    private fun propertyPrice(property: Property) = getString(R.string.listing_price_value,
        NumberFormat.getNumberInstance(resources.configuration.locales[0]).format(property.price.toBigDecimal()))
    private fun propertyArea(property: Property) = getString(R.string.listing_area_value,
        NumberFormat.getNumberInstance(resources.configuration.locales[0]).format(property.area.toBigDecimal()))
    private fun bindProperties() {
        findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.sound_feedback)?.apply {
            isChecked = prefs.getBoolean("sound", false)
            setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean("sound", checked).apply() }
        }
        if (current == "sell") {
            listOf(R.id.property_title, R.id.property_location, R.id.property_area,
                R.id.property_price, R.id.property_description).forEach { id ->
                findViewById<EditText>(id).apply { doAfterTextChanged { error = null } }
            }
            updatePhotoPreview()
            click(R.id.listing_choose_photo) { photoPicker.launch(arrayOf("image/*")) }
            click(R.id.listing_submit) { saveProperty() }
        }
        findViewById<LinearLayout>(R.id.own_listings)?.let { parent ->
            val items = propertyStore.all()
            text(parent, getString(R.string.your_listings), 22f)
            text(parent, if (items.isEmpty()) getString(R.string.listing_empty)
                else getString(R.string.listing_saved_count, items.size))
            items.forEach { property ->
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(12), dp(12), dp(16))
                    setBackgroundResource(R.drawable.card)
                    layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16) }
                    isFocusable = true
                    setOnClickListener { selectedProperty = property.id; show("my_property") }
                }
                photo(card, property, 180)
                text(card, property.title, 22f)
                text(card, propertyPrice(property), 20f)
                text(card, property.location + " · " + propertyArea(property))
                parent.addView(card)
            }
        }
        findViewById<LinearLayout>(R.id.property_content)?.let { parent ->
            val property = propertyStore.all().find { it.id == selectedProperty }
            if (property == null) { text(parent, getString(R.string.listing_empty)); return@let }
            photo(parent, property, 260)
            text(parent, getString(R.string.listing_for_sale), 12f)
            text(parent, property.title, 30f)
            text(parent, propertyPrice(property), 26f)
            text(parent, property.location, 20f)
            text(parent, propertyArea(property), 20f)
            if (property.description.isNotBlank()) text(parent, property.description)
            text(parent, getString(R.string.listing_local), 13f)
            button(parent, R.string.listing_remove) {
                AlertDialog.Builder(this).setMessage(R.string.listing_delete_confirm)
                    .setNegativeButton(R.string.listing_cancel, null)
                    .setPositiveButton(R.string.listing_remove) { _, _ ->
                        if (propertyStore.delete(property.id)) {
                            if (property.photo.isNotEmpty()) File(filesDir, property.photo).delete()
                            show("listings")
                        }
                    }.show()
            }
        }
    }
    private fun saveProperty() {
        for (id in listOf(R.id.property_title, R.id.property_location)) {
            if (!valid(id, value(id).isNotBlank(), getString(R.string.listing_required))) return
        }
        val area = PropertyStore.number(value(R.id.property_area))
        val price = PropertyStore.number(value(R.id.property_price))
        if (!valid(R.id.property_area, area != null, getString(R.string.listing_invalid))) return
        if (!valid(R.id.property_price, price != null, getString(R.string.listing_invalid))) return
        val property = Property(UUID.randomUUID().toString(), value(R.id.property_title), value(R.id.property_location),
            area!!.toPlainString(), price!!.toPlainString(), value(R.id.property_description), selectedPhoto)
        if (!propertyStore.save(property)) { toast(getString(R.string.listing_save_error)); return }
        selectedProperty = property.id
        selectedPhoto = ""
        if (prefs.getBoolean("sound", false)) {
            try {
                if (tone == null) tone = ToneGenerator(AudioManager.STREAM_MUSIC, 30)
                tone?.startTone(ToneGenerator.TONE_PROP_ACK, 120)
            } catch (_: RuntimeException) { /* Audio must never block saving. */ }
        }
        // Back returns to the collection rather than the already submitted form.
        history.removeAll { it == "sell" || it == "my_property" }
        if (history.lastOrNull() != "listings") history.add("listings")
        show("my_property", false)
        toast(getString(R.string.listing_created))
    }
    private fun addMessage(message: String) {
        findViewById<LinearLayout>(R.id.messages).addView(TextView(this).apply {
            text = message; textSize = 16f; setPadding(24, 20, 24, 20)
            setBackgroundResource(R.drawable.pill)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 16 }
        })
    }
}
