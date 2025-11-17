package com.lucky.aikeyboard

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.*

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val apiKeyEdit = findViewById<EditText>(R.id.api_key_edit)
        val saveButton = findViewById<Button>(R.id.save_button)
        val contextSwitch = findViewById<Switch>(R.id.context_switch)
        val themeSpinner = findViewById<Spinner>(R.id.theme_spinner)
        val privacyStatement = findViewById<TextView>(R.id.privacy_statement)
        val enableKeyboardButton = findViewById<Button>(R.id.enable_keyboard_button)

        val sharedPref = getSharedPreferences("settings", MODE_PRIVATE)

        apiKeyEdit.setText(sharedPref.getString("api_key", ""))
        contextSwitch.isChecked = sharedPref.getBoolean("enable_context", false)

        val themes = arrayOf("Light", "Dark", "System")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, themes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        themeSpinner.adapter = adapter
        val themeMode = sharedPref.getString("theme", "System")
        themeSpinner.setSelection(themes.indexOf(themeMode))

        privacyStatement.text = getString(R.string.privacy_statement)

        saveButton.setOnClickListener {
            val editor = sharedPref.edit()
            editor.putString("api_key", apiKeyEdit.text.toString().trim())
            editor.putBoolean("enable_context", contextSwitch.isChecked)
            editor.putString("theme", themeSpinner.selectedItem as String)
            editor.apply()
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        }

        enableKeyboardButton.setOnClickListener {
            startActivity(android.content.Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
    }

    companion object {
        fun getEnableContext(context: android.content.Context): Boolean {
            val sharedPref = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
            return sharedPref.getBoolean("enable_context", false)
        }
    }
}
