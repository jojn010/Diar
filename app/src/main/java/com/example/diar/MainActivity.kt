package com.example.diar

import android.os.Bundle
import android.animation.ValueAnimator
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

/** Supabase listings/auth when configured; payments and messaging remain demonstrations. */
class MainActivity : AppCompatActivity() {
    private val screens = mapOf(
        "my_listings" to R.layout.screen_my_listings, "sell" to R.layout.screen_sell, "my_property" to R.layout.screen_my_property,
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
    private val backend by lazy { SupabaseBackend(this) }
    private val cloudCache by lazy { PropertyStore(this, "diar_public_cache") }
    private var cloudBusy = false
    private var refreshing = false
    private var cloudDialog: AlertDialog? = null
    private fun properties(): List<Property> {
        if (!backend.enabled) return propertyStore.all()
        val remote = cloudCache.all()
        val remoteIds = remote.map { it.id }.toSet()
        return remote + propertyStore.all().filterNot { it.id in remoteIds }
    }
    private fun owns(property: Property) = !backend.enabled || property.ownerId.isEmpty() || property.ownerId == backend.userId
    private var selectedProperty: String? = null
    private var selectedPhoto = ""
    private var tone: ToneGenerator? = null
    private var composer: HomeComposer? = null
    private var composerStep = 0
    private var draftPropertyId = UUID.randomUUID().toString()
    private var propertyLatitude: Double? = null
    private var propertyLongitude: Double? = null
    private var propertyLocationSource = ""
    private var mapPicker: MapPicker? = null
    private var locationCancellation: androidx.core.os.CancellationSignal? = null
    private val locationHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && current == "sell") requestDeviceLocation()
        else if (current == "sell") toast(getString(R.string.location_denied))
    }
    private val photoPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val file = PropertyPhotos(this).import(uri)
                if (current == "my_property") {
                    val property = properties().find { it.id == selectedProperty }
                    if (property != null && owns(property)) {
                        saveListing(property.copy(photo = file.name), true, failed = { file.delete() }) {
                            if (property.photo.isNotEmpty()) File(filesDir, property.photo).delete()
                            if (backend.enabled && property.ownerId.isNotEmpty()) file.delete()
                            show("my_property", false)
                        }
                    } else { file.delete(); toast(getString(R.string.listing_save_error)) }
                } else {
                    adoptDraftPhoto(file.name)
                }
            } catch (_: Exception) { toast(getString(R.string.listing_photo_error)) }
        }
    }
    override fun onDestroy() {
        if (isFinishing && current == "sell") discardPropertyDraft()
        cloudDialog?.dismiss(); cloudDialog = null
        mapPicker?.dismiss(); locationCancellation?.cancel(); locationHandler.removeCallbacksAndMessages(null)
        tone?.release(); tone = null; super.onDestroy()
    }
    override fun onStop() {
        super.onStop(); locationCancellation?.cancel(); locationCancellation = null
        if (current == "sell") updatePinStatus()
    }
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        composer?.updatePrice()
        if (current == "sell") updatePinStatus()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        composerStep = savedInstanceState?.getInt("composerStep") ?: 0
        draftPropertyId = savedInstanceState?.getString("draftPropertyId") ?: draftPropertyId
        propertyLatitude = savedInstanceState?.takeIf { it.containsKey("propertyLatitude") }?.getDouble("propertyLatitude")
        propertyLongitude = savedInstanceState?.takeIf { it.containsKey("propertyLongitude") }?.getDouble("propertyLongitude")
        propertyLocationSource = savedInstanceState?.getString("propertyLocationSource") ?: ""
        selectedProperty = savedInstanceState?.getString("selectedProperty")
        selectedPhoto = savedInstanceState?.getString("selectedPhoto") ?: ""
        history.addAll(savedInstanceState?.getStringArrayList("history") ?: emptyList())
        messages.addAll(savedInstanceState?.getStringArrayList("messages") ?: emptyList())
        show(savedInstanceState?.getString("screen") ?: "splash", false)
        if (backend.enabled) refreshListings(false)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (cloudBusy) return
                if (current == "sell" && composerStep > 0) { composer?.showStep(composerStep - 1); return }
                if (current == "sell") discardPropertyDraft()
                if (history.isNotEmpty()) show(history.removeAt(history.lastIndex), false) else finish()
            }
        })
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("screen", current)
        outState.putInt("composerStep", composerStep)
        outState.putString("draftPropertyId", draftPropertyId)
        propertyLatitude?.let { outState.putDouble("propertyLatitude", it) }
        propertyLongitude?.let { outState.putDouble("propertyLongitude", it) }
        outState.putString("propertyLocationSource", propertyLocationSource)
        outState.putString("selectedProperty", selectedProperty)
        outState.putString("selectedPhoto", selectedPhoto)
        outState.putStringArrayList("history", ArrayList(history))
        outState.putStringArrayList("messages", ArrayList(messages))
    }
    fun navigate(view: View) {
        if (cloudBusy) return
        if (backend.enabled && current == "account" && view.tag == "welcome") {
            cloudAction({ backend.signOut() }) { show("welcome") }; return
        }
        if (backend.enabled && view.tag == "sell" && backend.userId.isEmpty()) {
            toast(getString(R.string.cloud_sign_in_required)); show("login"); return
        }
        if ((current == "sell" && view.tag != "sell") || (view.tag == "sell" && current != "sell")) discardPropertyDraft()
        show(view.tag.toString())
    }
    fun goBack(view: View) { onBackPressedDispatcher.onBackPressed() }
    private fun show(destination: String, push: Boolean = true) {
        val layout = screens[destination] ?: return
        val host = findViewById<FrameLayout>(R.id.main)
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(host.windowToken, 0)
        if (push && destination != current) history.add(current)
        locationCancellation?.cancel(); locationCancellation = null
        composer = null
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
        bindCloudControls()
        click(R.id.change_language) { chooseLanguage() }
        click(R.id.google) { toast(getString(R.string.google_demo)) }
        click(R.id.facebook) { toast(getString(R.string.facebook_demo)) }
        click(R.id.login_submit) {
            if (backend.enabled) { authenticate(false); return@click }
            if (valid(R.id.login_phone, value(R.id.login_phone).count { it.isDigit() } >= 8, getString(R.string.error_phone))) show("home")
        }
        click(R.id.signup_submit) {
            if (backend.enabled) { authenticate(true); return@click }
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
            properties().forEach { property ->
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
    internal fun adoptDraftPhoto(name: String) {
        if (selectedPhoto != name) deleteDraftPhoto()
        selectedPhoto = name
        updatePhotoPreview()
    }
    private fun deleteDraftPhoto() {
        if (selectedPhoto.isNotEmpty() && propertyStore.all().none { it.photo == selectedPhoto })
            File(filesDir, selectedPhoto).delete()
    }
    private fun discardPropertyDraft() {
        deleteDraftPhoto()
        selectedPhoto = ""; composerStep = 0; draftPropertyId = UUID.randomUUID().toString()
        propertyLatitude = null; propertyLongitude = null; propertyLocationSource = ""
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
            contentDescription = if (property.photo.isEmpty() && property.photoPath.isEmpty()) getString(R.string.listing_sample) else property.title
            if (backend.enabled && property.photoPath.isNotEmpty()) CloudPhotos.load(this, backend.photoUrl(property.photoPath))
            else loadPhoto(this, property.photo)
        })
        if (property.photo.isEmpty() && property.photoPath.isEmpty()) text(parent, getString(R.string.listing_sample), 12f)
    }
    private fun propertyPrice(property: Property): String {
        val format = NumberFormat.getNumberInstance(resources.configuration.locales[0])
        val high = format.format(property.price.toBigDecimal())
        val amount = getString(R.string.listing_price_value, high)
        return if (property.purpose == "rent") getString(R.string.rent_range, amount) else amount
    }
    private fun propertyAttributes(property: Property): String {
        val type = when (property.type) { "apartment" -> R.string.type_apartment; "house" -> R.string.type_house; "villa" -> R.string.type_villa; else -> null }
        val floors = when (property.floors) { 1 -> R.string.floor_one; 2 -> R.string.floor_two; 3 -> R.string.floor_more; else -> null }
        return listOfNotNull(type?.let { getString(it) }, floors?.let { getString(it) }).joinToString(" · ")
    }
    private fun propertyArea(property: Property) = getString(R.string.listing_area_value,
        NumberFormat.getNumberInstance(resources.configuration.locales[0]).format(property.area.toBigDecimal()))
    private fun bindProperties() {
        findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.sound_feedback)?.apply {
            isChecked = prefs.getBoolean("sound", false)
            setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean("sound", checked).apply() }
        }
        click(R.id.collection_add) { navigate(View(this).apply { tag = "sell" }) }
        if (current == "sell") {
            composer = HomeComposer(this, composerStep, { composerStep = it }, { animateEntrance(it) })
            updatePhotoPreview()
            updatePinStatus()
            click(R.id.use_current_location) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED)
                    requestDeviceLocation()
                else locationPermission.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            click(R.id.choose_map_location) { openPropertyMap() }
            click(R.id.listing_choose_photo) { photoPicker.launch(arrayOf("image/*")) }
            click(R.id.listing_submit) { saveProperty() }
        }
        findViewById<LinearLayout>(R.id.own_listings)?.let { parent ->
            val items = properties().filter { current != "my_listings" || owns(it) }
            text(parent, getString(if (backend.enabled && current != "my_listings") R.string.cloud_listings else R.string.your_listings), 22f)
            if (backend.enabled) {
                button(parent, R.string.cloud_refresh) { refreshListings(true) }
                text(parent, getString(R.string.cloud_cached), 12f)
            }
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
                text(card, getString(if (property.negotiable) R.string.negotiable_badge else R.string.price_fixed), 13f)
                text(card, getString(if (property.purpose == "rent") R.string.purpose_rent else R.string.purpose_sale), 13f)
                text(card, property.location + " · " + propertyArea(property))
                if (propertyAttributes(property).isNotBlank()) text(card, propertyAttributes(property), 14f)
                parent.addView(card)
            }
        }
        findViewById<LinearLayout>(R.id.property_content)?.let { parent ->
            val property = properties().find { it.id == selectedProperty }
            if (property == null) { text(parent, getString(R.string.listing_empty)); return@let }
            photo(parent, property, 260)
            text(parent, getString(if (property.purpose == "rent") R.string.listing_for_rent else R.string.listing_for_sale), 12f)
            text(parent, property.title, 30f)
            text(parent, propertyPrice(property), 26f)
            text(parent, getString(if (property.negotiable) R.string.negotiable_badge else R.string.price_fixed), 14f)
            text(parent, property.location, 20f)
            text(parent, propertyArea(property), 20f)
            if (propertyAttributes(property).isNotBlank()) text(parent, propertyAttributes(property), 18f)
            if (property.latitude != null && property.longitude != null) {
                text(parent, pinLabel(property.latitude, property.longitude), 14f)
                if (owns(property)) button(parent, R.string.location_details) {
                    mapPicker = MapPicker(this).also { it.show(property.latitude, property.longitude) { lat, lon ->
                        saveListing(property.copy(latitude = lat, longitude = lon, locationSource = "map"), true) {
                            show("my_property", false)
                        }
                    } }
                }
            }
            if (owns(property)) button(parent, R.string.listing_photo) { photoPicker.launch(arrayOf("image/*")) }
            if (property.description.isNotBlank()) text(parent, property.description)
            text(parent, getString(if (property.ownerId.isEmpty()) R.string.listing_local else R.string.cloud_published), 13f)
            if (backend.enabled && property.ownerId.isEmpty()) button(parent, R.string.cloud_publish_local) {
                confirmPublish { saveListing(property, false, publishLocal = true) { show("my_property", false) } }
            }
            if (owns(property)) button(parent, R.string.listing_remove) {
                AlertDialog.Builder(this).setMessage(R.string.listing_delete_confirm)
                    .setNegativeButton(R.string.listing_cancel, null)
                    .setPositiveButton(R.string.listing_remove) { _, _ ->
                        deleteListing(property)
                    }.show()
            }
        }
    }
    private fun saveProperty() {
        if (composer?.validateAll() != true) return
        val property = Property(draftPropertyId, value(R.id.property_title), value(R.id.property_location),
            PropertyStore.number(value(R.id.property_area))!!.toPlainString(),
            PropertyStore.integer(value(R.id.property_price))!!.toString(), value(R.id.property_description), selectedPhoto,
            negotiable = findViewById<CheckBox>(R.id.property_negotiable).isChecked,
            purpose = if (composer?.choice(R.id.property_purpose) == R.id.purpose_rent) "rent" else "sale",
            type = when (composer?.choice(R.id.property_type)) { R.id.type_house -> "house"; R.id.type_villa -> "villa"; else -> "apartment" },
            floors = when (composer?.choice(R.id.property_floors)) { R.id.floor_two -> 2; R.id.floor_more -> 3; else -> 1 },
            latitude = propertyLatitude, longitude = propertyLongitude, locationSource = propertyLocationSource)
        if (backend.enabled) confirmPublish { saveListing(property, false) { listingSaved(property) } }
        else saveListing(property, false) { listingSaved(property) }
    }
    private fun listingSaved(property: Property) {
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
        if (history.lastOrNull() != "my_listings") history.add("my_listings")
        show("my_property", false)
        toast(getString(R.string.listing_created))
    }
    private fun bindCloudControls() {
        if (!backend.enabled) return
        if (current == "signup") {
            findViewById<View>(R.id.signup_phone)?.visibility = View.GONE
            findViewById<View>(R.id.signup_phone_label)?.visibility = View.GONE
        }
        if (current == "login") {
            findViewById<TextView>(R.id.login_title)?.setText(R.string.cloud_sign_in)
            findViewById<EditText>(R.id.login_phone)?.apply {
                setHint(R.string.cloud_email); inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                if (android.os.Build.VERSION.SDK_INT >= 26) setAutofillHints(View.AUTOFILL_HINT_EMAIL_ADDRESS)
            }
            findViewById<View>(R.id.login_password)?.visibility = View.VISIBLE
            findViewById<TextView>(R.id.login_label)?.setText(R.string.cloud_email)
            findViewById<TextView>(R.id.login_intro)?.setText(R.string.cloud_login_hint)
            findViewById<TextView>(R.id.login_footer)?.setText(R.string.cloud_login_hint)
        }
        findViewById<LinearLayout>(R.id.cloud_account_panel)?.let { panel ->
            findViewById<TextView>(R.id.account_intro)?.setText(R.string.cloud_account_hint)
            text(panel, if (backend.userId.isEmpty()) getString(R.string.cloud_sign_in_required) else backend.email, 18f)
            button(panel, if (backend.userId.isEmpty()) R.string.cloud_sign_in else R.string.cloud_sign_out) {
                if (backend.userId.isEmpty()) show("login")
                else cloudAction({ backend.signOut() }) { show("account", false) }
            }
        }
    }
    private fun authenticate(signUp: Boolean) {
        val emailId = if (signUp) R.id.email else R.id.login_phone
        val passwordId = if (signUp) R.id.password else R.id.login_password
        if (signUp && !valid(R.id.full_name, value(R.id.full_name).length >= 2, getString(R.string.error_name))) return
        if (!valid(emailId, android.util.Patterns.EMAIL_ADDRESS.matcher(value(emailId)).matches(), getString(R.string.error_email))) return
        val password = findViewById<EditText>(passwordId).text.toString()
        if (!valid(passwordId, password.length >= 6, getString(R.string.error_password))) return
        if (signUp && !findViewById<CheckBox>(R.id.terms).isChecked) { toast(getString(R.string.accept_terms)); return }
        val email = value(emailId)
        val name = if (signUp) value(R.id.full_name) else ""
        cloudAction({ if (signUp) backend.signUp(name, email, password) else { backend.signIn(email, password); true } }) { signedIn ->
            if (signedIn) { show("home"); refreshListings(false) }
            else { show("login"); AlertDialog.Builder(this).setMessage(R.string.cloud_confirm_email).setPositiveButton(android.R.string.ok, null).show() }
        }
    }
    private fun confirmPublish(action: () -> Unit) {
        if (backend.userId.isEmpty()) { toast(getString(R.string.cloud_sign_in_required)); return }
        AlertDialog.Builder(this).setTitle(R.string.cloud_publish_title).setMessage(R.string.cloud_publish_warning)
            .setNegativeButton(R.string.listing_cancel, null).setPositiveButton(R.string.cloud_publish) { _, _ -> action() }.show()
    }
    private fun saveListing(property: Property, existing: Boolean, publishLocal: Boolean = false, failed: () -> Unit = {}, done: () -> Unit) {
        if (!backend.enabled || (existing && property.ownerId.isEmpty() && !publishLocal)) {
            if (propertyStore.save(property)) done() else { failed(); toast(getString(R.string.listing_save_error)) }
            return
        }
        // Legacy records may predate required map/type/floor fields. Keep them private for correction.
        if (property.latitude == null || property.longitude == null || property.type !in listOf("apartment", "house", "villa") ||
            property.floors !in 1..3 || PropertyStore.integer(property.price) == null) {
            failed(); toast(getString(R.string.cloud_legacy_incomplete)); return
        }
        cloudAction({
            val saved = backend.save(property, existing && property.ownerId.isNotEmpty())
            cloudCache.save(saved)
            if (publishLocal) propertyStore.delete(property.id)
            if (property.photo.isNotEmpty() && propertyStore.all().none { it.photo == property.photo }) File(filesDir, property.photo).delete()
        }, failed = failed) { done() }
    }
    private fun deleteListing(property: Property) {
        if (!backend.enabled || property.ownerId.isEmpty()) {
            if (propertyStore.delete(property.id)) {
                if (property.photo.isNotEmpty()) File(filesDir, property.photo).delete()
                show("my_listings")
            } else toast(getString(R.string.listing_save_error))
            return
        }
        cloudAction({ backend.delete(property); cloudCache.delete(property.id) }) { show("my_listings") }
    }
    private fun refreshListings(report: Boolean) {
        if (refreshing || cloudBusy) return
        refreshing = true
        val screen = current
        SupabaseBackend.worker.execute {
            val result = runCatching { cloudCache.replace(backend.listings()) }
            runOnUiThread {
                refreshing = false
                if (isDestroyed || isFinishing) return@runOnUiThread
                if (result.isSuccess) {
                    if (current == screen && current in listOf("home", "listings", "my_listings", "my_property")) show(current, false)
                    if (report) toast(getString(R.string.cloud_refreshed))
                } else if (report) toast(getString(R.string.cloud_error))
            }
        }
    }
    private fun <T> cloudAction(work: () -> T, showProgress: Boolean = true, reportError: Boolean = true, failed: () -> Unit = {}, done: (T) -> Unit) {
        if (cloudBusy) return
        cloudBusy = true
        val dialog = if (showProgress) AlertDialog.Builder(this).setMessage(R.string.cloud_working).setCancelable(false).show() else null
        cloudDialog = dialog
        SupabaseBackend.worker.execute {
            val result = runCatching(work)
            runOnUiThread {
                cloudBusy = false
                if (isDestroyed || isFinishing) return@runOnUiThread
                dialog?.dismiss(); cloudDialog = null
                result.fold(done) { error ->
                    failed()
                    if (reportError) {
                        val message = when (error) {
                            is SupabaseBackend.BackendException -> error.messageId
                            is SupabaseBackend.HttpFailure -> error.messageId()
                            else -> R.string.cloud_error
                        }
                        AlertDialog.Builder(this).setMessage(message).setPositiveButton(android.R.string.ok, null).show()
                    }
                }
            }
        }
    }
    fun hasPropertyPin() = propertyLatitude != null && propertyLongitude != null
    private fun pinLabel(latitude: Double, longitude: Double): String {
        val format = NumberFormat.getNumberInstance(resources.configuration.locales[0]).apply { maximumFractionDigits = 5 }
        return getString(R.string.location_pin, format.format(latitude), format.format(longitude))
    }
    private fun updatePinStatus() {
        findViewById<TextView>(R.id.location_status)?.text = if (hasPropertyPin())
            pinLabel(propertyLatitude!!, propertyLongitude!!) else getString(R.string.location_none)
    }
    private fun openPropertyMap() {
        locationCancellation?.cancel(); locationCancellation = null
        mapPicker = MapPicker(this).also { it.show(propertyLatitude, propertyLongitude) { lat, lon ->
            propertyLatitude = lat; propertyLongitude = lon; propertyLocationSource = "map"; updatePinStatus()
        } }
    }
    @android.annotation.SuppressLint("MissingPermission")
    private fun requestDeviceLocation() {
        val manager = getSystemService(LOCATION_SERVICE) as android.location.LocationManager
        val providers = listOf(android.location.LocationManager.NETWORK_PROVIDER, android.location.LocationManager.GPS_PROVIDER)
            .filter { manager.isProviderEnabled(it) }
        if (providers.isEmpty()) { toast(getString(R.string.location_unavailable)); return }
        locationCancellation?.cancel()
        val cancellation = androidx.core.os.CancellationSignal()
        val children = providers.map { androidx.core.os.CancellationSignal() }
        cancellation.setOnCancelListener { children.forEach { it.cancel() } }
        locationCancellation = cancellation
        findViewById<TextView>(R.id.location_status)?.setText(R.string.location_wait)
        var pending = providers.size
        val timeout = Runnable {
            if (locationCancellation === cancellation) {
                locationCancellation = null; cancellation.cancel()
                if (current == "sell") { updatePinStatus(); toast(getString(R.string.location_unavailable)) }
            }
        }
        locationHandler.postDelayed(timeout, 20000)
        fun result(location: android.location.Location?) {
            if (locationCancellation !== cancellation || current != "sell") return
            pending--
            if (location != null && MapPicker.validCoordinates(location.latitude, location.longitude)) {
                locationCancellation = null; locationHandler.removeCallbacks(timeout); cancellation.cancel()
                propertyLatitude = location.latitude; propertyLongitude = location.longitude; propertyLocationSource = "current"
                updatePinStatus(); toast(getString(R.string.location_approximate))
            } else if (pending == 0) {
                locationCancellation = null; locationHandler.removeCallbacks(timeout); cancellation.cancel()
                updatePinStatus(); toast(getString(R.string.location_unavailable))
            }
        }
        providers.forEachIndexed { index, provider ->
            if (locationCancellation === cancellation) {
                try {
                    androidx.core.location.LocationManagerCompat.getCurrentLocation(manager, provider, children[index],
                        androidx.core.content.ContextCompat.getMainExecutor(this)) { result(it) }
                } catch (_: Exception) { result(null) }
            }
        }
    }
    private fun addMessage(message: String) {
        findViewById<LinearLayout>(R.id.messages).addView(TextView(this).apply {
            text = message; textSize = 16f; setPadding(24, 20, 24, 20)
            setBackgroundResource(R.drawable.pill)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 16 }
        })
    }
}
