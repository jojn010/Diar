package com.example.diar

import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged

/** Offline UI prototype: no authentication, payment or messaging service is contacted. */
class MainActivity : AppCompatActivity() {
    private val screens = mapOf(
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
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
        outState.putStringArrayList("history", ArrayList(history))
        outState.putStringArrayList("messages", ArrayList(messages))
    }
    fun navigate(view: View) { show(view.tag.toString()) }
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
    }
    private fun click(id: Int, action: () -> Unit) { findViewById<View>(id)?.setOnClickListener { action() } }
    private fun value(id: Int) = findViewById<EditText>(id).text.toString().trim()
    private fun valid(id: Int, condition: Boolean, message: String): Boolean {
        if (!condition) { findViewById<EditText>(id).error = message; findViewById<EditText>(id).requestFocus() }
        return condition
    }
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    private fun bind() {
        click(R.id.google) { toast("Google sign-in will be connected later. Use Explore homes for now.") }
        click(R.id.facebook) { toast("Facebook sign-in will be connected later. Use Explore homes for now.") }
        click(R.id.login_submit) {
            if (valid(R.id.login_phone, value(R.id.login_phone).count { it.isDigit() } >= 8, "Enter a valid phone number")) show("home")
        }
        click(R.id.signup_submit) {
            when {
                !valid(R.id.full_name, value(R.id.full_name).length >= 2, "Enter your name") -> Unit
                !valid(R.id.signup_phone, value(R.id.signup_phone).count { it.isDigit() } >= 8, "Enter a valid phone number") -> Unit
                !valid(R.id.email, android.util.Patterns.EMAIL_ADDRESS.matcher(value(R.id.email)).matches(), "Enter a valid email") -> Unit
                !valid(R.id.password, value(R.id.password).length >= 6, "Use at least 6 characters") -> Unit
                !findViewById<CheckBox>(R.id.terms).isChecked -> toast("Please accept the terms to continue")
                else -> show("home")
            }
        }
        findViewById<Button>(R.id.save_home)?.let { button ->
            fun update() { button.text = if (prefs.getBoolean("saved", false)) "♥  Saved to favorites" else "♡  Save this home" }
            update()
            button.setOnClickListener { prefs.edit().putBoolean("saved", !prefs.getBoolean("saved", false)).apply(); update() }
        }
        findViewById<LinearLayout>(R.id.saved_items)?.let { items ->
            if (!prefs.getBoolean("saved", false)) {
                items.removeAllViews()
                items.addView(TextView(this).apply { text = "No saved homes yet. Explore a property and tap Save."; textSize = 18f; setPadding(0, 32, 0, 32) })
            }
        }
        click(R.id.clear_notifications) {
            findViewById<LinearLayout>(R.id.notification_items).removeAllViews()
            toast("You’re all caught up")
        }
        findViewById<EditText>(R.id.search_query)?.let { query ->
            val results = findViewById<LinearLayout>(R.id.search_results)
            val rows = (0 until results.childCount).map { results.getChildAt(it) as Button }
            val empty = TextView(this).apply { text = "No homes found. Try another city or property name."; textSize = 20f; setPadding(0, 40, 0, 40) }
            results.addView(empty)
            fun filter() {
                val term = query.text.toString().trim()
                var count = 0
                rows.forEach { row ->
                    val visible = term.isBlank() || row.text.contains(term, true) || listOf("Jerusalem", "Ramallah").any { it.contains(term, true) }
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
                !valid(R.id.card_name, value(R.id.card_name).length >= 2, "Enter a sample name") -> Unit
                !valid(R.id.card_number, value(R.id.card_number).filter { it.isDigit() }.length in 13..19, "Use 13–19 sample digits") -> Unit
                !valid(R.id.card_expiry, Regex("(0[1-9]|1[0-2])/[0-9]{2}").matches(value(R.id.card_expiry)), "Use MM/YY") -> Unit
                !valid(R.id.card_cvc, value(R.id.card_cvc).length in 3..4, "Use 3 or 4 digits") -> Unit
                !valid(R.id.country, value(R.id.country).isNotBlank(), "Enter a country") -> Unit
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
                    toast("Message added to this local demo conversation")
                }
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
