package com.lucky.aikeyboard

import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LuckyKeyboardService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private lateinit var keyboardView: KeyboardView
    private lateinit var aiPanel: LinearLayout
    private lateinit var promptInput: EditText
    private lateinit var toneSpinner: Spinner
    private lateinit var generateButton: Button
    private lateinit var cancelButton: Button
    private lateinit var loadingProgress: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var keyboard: Keyboard
    private val geminiClient = GeminiClient(this)
    private var isShifted = false
    private var currentEditorInfo: EditorInfo? = null

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        keyboardView = view.findViewById(R.id.keyboard_view)
        aiPanel = view.findViewById(R.id.ai_panel)
        promptInput = view.findViewById(R.id.prompt_input)
        toneSpinner = view.findViewById(R.id.tone_spinner)
        generateButton = view.findViewById(R.id.generate_button)
        cancelButton = view.findViewById(R.id.cancel_button)
        loadingProgress = view.findViewById(R.id.loading_progress)
        errorText = view.findViewById(R.id.error_text)

        val tones = arrayOf("Neutral", "Formal", "Friendly")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tones)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        toneSpinner.adapter = adapter

        generateButton.setOnClickListener { generateReply() }
        cancelButton.setOnClickListener { hideAIPanel() }

        keyboard = Keyboard(this, R.xml.keyboard_qwerty)
        keyboardView.keyboard = keyboard
        keyboardView.setOnKeyboardActionListener(this)
        return view
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        currentEditorInfo = info
        aiPanel.visibility = View.GONE
    }

    override fun onKey(primaryCode: Int, intArray: IntArray?) {
        val ic = currentInputConnection ?: return
        when {
            primaryCode == Keyboard.KEYCODE_DELETE -> {
                ic.deleteSurroundingText(1, 0)
            }
            primaryCode == Keyboard.KEYCODE_SHIFT -> {
                isShifted = !isShifted
                keyboard.isShifted = isShifted
                keyboardView.invalidateAllKeys()
            }
            primaryCode == 10 -> {
                ic.commitText("\n", 1)
            }
            primaryCode == 32 -> {
                ic.commitText(" ", 1)
            }
            primaryCode == -11 -> {
                toggleAIPanel()
            }
            primaryCode > 0 -> {
                var keyChar = primaryCode.toChar()
                if (isShifted || keyboard.isShifted) {
                    keyChar = keyChar.uppercaseChar()
                }
                ic.commitText(keyChar.toString(), 1)
                if (isShifted) {
                    isShifted = false
                    keyboard.isShifted = false
                    keyboardView.invalidateAllKeys()
                }
            }
        }
    }

    private fun toggleAIPanel() {
        if (aiPanel.visibility == View.VISIBLE) {
            hideAIPanel()
        } else {
            showAIPanel()
        }
    }

    private fun showAIPanel() {
        if (currentEditorInfo?.inputType?.and(EditorInfo.TYPE_MASK_CLASS) == EditorInfo.TYPE_CLASS_TEXT &&
            currentEditorInfo?.inputType?.and(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD) != EditorInfo.TYPE_TEXT_VARIATION_PASSWORD) {
            aiPanel.visibility = View.VISIBLE
        }
    }

    private fun hideAIPanel() {
        aiPanel.visibility = View.GONE
        loadingProgress.visibility = View.GONE
        errorText.visibility = View.GONE
        promptInput.text.clear()
    }

    private fun generateReply() {
        val prompt = promptInput.text.toString()
        if (prompt.isEmpty()) return
        val tone = toneSpinner.selectedItem.toString()
        loadingProgress.visibility = View.VISIBLE
        errorText.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val context = if (SettingsActivity.getEnableContext(this@LuckyKeyboardService)) {
                    getLastNChars(100)
                } else null
                val reply = geminiClient.generateReply(prompt, tone, context)
                currentInputConnection?.commitText(reply, 1)
                hideAIPanel()
            } catch (e: Exception) {
                errorText.text = getString(R.string.error_api) + ": ${e.message}"
                errorText.visibility = View.VISIBLE
            } finally {
                loadingProgress.visibility = View.GONE
            }
        }
    }

    private fun getLastNChars(n: Int): String? {
        val ic = currentInputConnection ?: return null
        val editable = ic.extractText(EditorInfo.CURSOR_AFTER, n, 0)
        return editable.toString()
    }

    override fun onPress(primaryCode: Int) {}
    override fun onRelease(primaryCode: Int) {}
    override fun onText(text: CharSequence?) {}
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}
}
