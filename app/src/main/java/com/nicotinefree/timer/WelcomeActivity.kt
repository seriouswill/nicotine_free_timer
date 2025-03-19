package com.nicotinefree.timer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var nicotineTypeGroup: RadioGroup
    private lateinit var amountEditText: EditText
    private lateinit var continueButton: Button

    companion object {
        const val PREFS_NAME = "TimerAppPrefs"
        const val KEY_USER_NAME = "user_name"
        const val KEY_NICOTINE_TYPE = "nicotine_type"
        const val KEY_DAILY_AMOUNT = "daily_amount"
        
        // Constants for the different nicotine types
        const val TYPE_CIGARETTES = "Cigarettes"
        const val TYPE_VAPING = "Vaping"
        const val TYPE_POUCHES = "Snoos/Pouches"
        const val TYPE_GUM = "Nicotine Gum"
        const val TYPE_OTHER = "Nicotine Products"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        nameEditText = findViewById(R.id.name_input)
        nicotineTypeGroup = findViewById(R.id.nicotine_type_group)
        amountEditText = findViewById(R.id.amount_input)
        continueButton = findViewById(R.id.continue_button)

        // Check if user has already entered name before
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedName = sharedPref.getString(KEY_USER_NAME, "") ?: ""
        val savedNicotineType = sharedPref.getString(KEY_NICOTINE_TYPE, "") ?: ""
        val savedAmount = sharedPref.getInt(KEY_DAILY_AMOUNT, 0)
        
        if (savedName.isNotEmpty() && savedNicotineType.isNotEmpty() && savedAmount > 0) {
            // If user already registered with name, nicotine type, and amount, go directly to main activity
            startMainActivity()
            return
        }
        
        // If we have saved values, pre-fill the fields
        if (savedName.isNotEmpty()) {
            nameEditText.setText(savedName)
        }
        
        if (savedAmount > 0) {
            amountEditText.setText(savedAmount.toString())
        }

        // Make sure EditText gets focus and shows keyboard
        nameEditText.setOnClickListener {
            requestFocusAndShowKeyboard(nameEditText)
        }
        
        nameEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showKeyboard(nameEditText)
            }
        }

        continueButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val amountStr = amountEditText.text.toString().trim()
            
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                requestFocusAndShowKeyboard(nameEditText)
                return@setOnClickListener
            }
            
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Please enter your daily amount", Toast.LENGTH_SHORT).show()
                requestFocusAndShowKeyboard(amountEditText)
                return@setOnClickListener
            }
            
            val amount = try {
                amountStr.toInt()
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
                requestFocusAndShowKeyboard(amountEditText)
                return@setOnClickListener
            }
            
            if (amount <= 0) {
                Toast.makeText(this, "Amount must be greater than zero", Toast.LENGTH_SHORT).show()
                requestFocusAndShowKeyboard(amountEditText)
                return@setOnClickListener
            }
            
            // Get the selected nicotine type
            val nicotineType = getNicotineTypeFromSelection()
            
            // Save name, nicotine type, and amount
            with(sharedPref.edit()) {
                putString(KEY_USER_NAME, name)
                putString(KEY_NICOTINE_TYPE, nicotineType)
                putInt(KEY_DAILY_AMOUNT, amount)
                apply()
            }
            
            startMainActivity()
        }
    }
    
    private fun getNicotineTypeFromSelection(): String {
        return when (nicotineTypeGroup.checkedRadioButtonId) {
            R.id.radio_cigarettes -> TYPE_CIGARETTES
            R.id.radio_vaping -> TYPE_VAPING
            R.id.radio_pouches -> TYPE_POUCHES
            R.id.radio_gum -> TYPE_GUM
            R.id.radio_other -> TYPE_OTHER
            else -> TYPE_CIGARETTES // Default to cigarettes if somehow nothing is selected
        }
    }
    
    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    private fun requestFocusAndShowKeyboard(editText: EditText) {
        editText.requestFocus()
        showKeyboard(editText)
    }
    
    private fun showKeyboard(editText: EditText) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }
} 