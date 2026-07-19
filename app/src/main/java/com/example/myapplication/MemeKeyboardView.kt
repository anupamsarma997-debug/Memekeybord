package com.example.myapplication

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.Button

class MemeKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private var emojiClickListener: ((emoji: String, dialogue: String) -> Unit)? = null

    init {
        orientation = VERTICAL
        setupKeyboard()
    }

    private fun setupKeyboard() {
        // Define some meme emojis and their dialogues
        val memeData = listOf(
            "😂" to "That's hilarious!",
            "🤔" to "Thinking about it...",
            "😎" to "Cool, isn't it?",
            "🎉" to "Let's celebrate!",
            "🔥" to "This is fire!",
            "💯" to "That's perfect!",
            "👍" to "Great job!",
            "😭" to "Crying over this!"
        )

        memeData.forEach { (emoji, dialogue) ->
            val button = Button(context).apply {
                text = emoji
                setOnClickListener {
                    emojiClickListener?.invoke(emoji, dialogue)
                }
            }
            addView(button, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        }
    }

    fun setOnEmojiClickListener(listener: (emoji: String, dialogue: String) -> Unit) {
        emojiClickListener = listener
    }
}
