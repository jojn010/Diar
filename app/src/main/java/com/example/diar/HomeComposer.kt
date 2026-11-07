package com.example.diar

import android.view.View
import android.widget.*
import androidx.core.widget.doAfterTextChanged
import java.text.NumberFormat

/** Three short chapters; all fields stay mounted so Android can restore the draft. */
class HomeComposer(private val activity: MainActivity, initialStep: Int,
    private val stepChanged: (Int) -> Unit, private val animate: (View) -> Unit) {
    var step = initialStep.coerceIn(0, 2)
        private set
    private fun <T : View> view(id: Int): T = activity.findViewById(id)
    private fun value(id: Int) = view<EditText>(id).text.toString().trim()
    private val panels = listOf(R.id.composer_home, R.id.composer_location, R.id.composer_finish)
    init {
        listOf(R.id.property_title, R.id.property_location, R.id.property_area,
            R.id.property_price, R.id.property_description).forEach { id ->
            view<EditText>(id).doAfterTextChanged { view<EditText>(id).error = null; updateReview() }
        }
        view<View>(R.id.composer_next).setOnClickListener { if (validateStep(step)) showStep(step + 1) }
        view<View>(R.id.composer_previous).setOnClickListener { showStep(step - 1) }
        view<RadioGroup>(R.id.property_purpose).setOnCheckedChangeListener { _, _ -> updatePrice() }
        view<EditText>(R.id.property_price).doAfterTextChanged { updatePrice() }
        view<CheckBox>(R.id.property_negotiable).setOnCheckedChangeListener { _, _ -> updateReview() }
        showStep(step, false)
        updatePrice()
    }
    private fun number(value: Long) = NumberFormat.getIntegerInstance(activity.resources.configuration.locales[0]).format(value)
    fun showStep(requested: Int, motion: Boolean = true) {
        step = requested.coerceIn(0, 2); stepChanged(step)
        panels.forEachIndexed { i, id -> view<View>(id).visibility = if (i == step) View.VISIBLE else View.GONE }
        view<TextView>(R.id.composer_step_label).setText(listOf(R.string.step_home, R.string.step_location, R.string.step_finish)[step])
        view<ProgressBar>(R.id.composer_progress).progress = step + 1
        view<View>(R.id.composer_previous).visibility = if (step == 0) View.GONE else View.VISIBLE
        view<View>(R.id.composer_next).visibility = if (step == 2) View.GONE else View.VISIBLE
        view<ScrollView>(R.id.composer_scroll).scrollTo(0, 0)
        (activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager)
            .hideSoftInputFromWindow(view<View>(panels[step]).windowToken, 0)
        updateReview()
        if (motion) animate(view(panels[step]))
    }
    private fun invalid(id: Int, message: Int): Boolean {
        view<EditText>(id).apply { error = activity.getString(message); requestFocus() }
        return false
    }
    fun validateStep(step: Int): Boolean = when (step) {
        0 -> when {
            value(R.id.property_title).length !in 2..160 -> invalid(R.id.property_title, R.string.listing_title_length)
            PropertyStore.number(value(R.id.property_area)) == null -> invalid(R.id.property_area, R.string.listing_invalid)
            else -> true
        }
        1 -> when {
            !activity.hasPropertyPin() -> { Toast.makeText(activity, R.string.location_needed, Toast.LENGTH_SHORT).show(); false }
            value(R.id.property_location).isBlank() -> invalid(R.id.property_location, R.string.listing_required)
            else -> true
        }
        else -> if (PropertyStore.integer(value(R.id.property_price)) == null)
            invalid(R.id.property_price, R.string.price_error) else true
    }
    fun validateAll(): Boolean {
        for (index in 0..2) {
            showStep(index, false)
            if (!validateStep(index)) return false
        }
        return true
    }
    fun updatePrice() {
        val rent = view<RadioGroup>(R.id.property_purpose).checkedRadioButtonId == R.id.purpose_rent
        val field = view<EditText>(R.id.property_price)
        val parent = field.parent as LinearLayout
        (parent.getChildAt(parent.indexOfChild(field) - 1) as TextView).setText(if (rent) R.string.rent_label else R.string.price_label)
        val price = PropertyStore.integer(value(R.id.property_price))
        val amount = price?.let { activity.getString(R.string.listing_price_value, number(it)) }
        view<TextView>(R.id.price_summary).text = if (amount == null) activity.getString(R.string.price_play)
            else if (rent) activity.getString(R.string.rent_range, amount) else amount
        updateReview()
    }
    fun choice(id: Int) = view<RadioGroup>(id).checkedRadioButtonId
    private fun choiceLabel(id: Int): String = view<RadioButton>(choice(id)).text.toString()
    fun updateReview() {
        view<TextView>(R.id.listing_review).text = listOf(value(R.id.property_title),
            choiceLabel(R.id.property_purpose), choiceLabel(R.id.property_type), choiceLabel(R.id.property_floors),
            value(R.id.property_location), view<TextView>(R.id.price_summary).text.toString(),
            activity.getString(if (view<CheckBox>(R.id.property_negotiable).isChecked) R.string.negotiable_badge else R.string.price_fixed)).filter { it.isNotBlank() }.joinToString(" · ")
    }
}
