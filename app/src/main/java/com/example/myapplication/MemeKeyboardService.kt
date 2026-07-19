package com.example.myapplication

import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.LinearLayout
import android.widget.Button
import android.content.Context

class MemeKeyboardService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private lateinit var keyboardView: MemeKeyboardView
    private var inputConnection: InputConnection? = null

    override fun onCreateInputView(): View {
        return MemeKeyboardView(this).apply {
            keyboardView = this
            setOnEmojiClickListener { emoji, dialogue ->
                insertText(emoji, dialogue)
            }
        }
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        inputConnection = currentInputConnection
    }

    private fun insertText(emoji: String, dialogue: String) {
        inputConnection?.let { conn ->
            // Insert emoji first
            conn.commitText(emoji, 1)
            // Then insert the meme dialogue
            conn.commitText(" $dialogue", 1)
        }
    }

    override fun onPress(primaryCode: Int) {}
    override fun onRelease(primaryCode: Int) {}
    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {}
    override fun onText(text: CharSequence?) {}
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}
}
