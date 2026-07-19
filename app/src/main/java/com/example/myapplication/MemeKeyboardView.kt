package com.example.myapplication

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.Button
import android.widget.GridLayout
import android.view.Gravity

class MemeKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private var emojiClickListener: ((emoji: String, dialogue: String) -> Unit)? = null

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        setPadding(8, 8, 8, 8)
        setupKeyboard()
    }

    private fun setupKeyboard() {
        // Grid layout for meme emojis and their messages
        val gridLayout = GridLayout(context).apply {
            columnCount = 4
            rowCount = 3
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        val memeData = listOf(
            "😂" to "Hahahaha! 😂",
            "🤔" to "Hmmm sochne do 🤔",
            "😎" to "Bhaiya main cool hu 😎",
            "🎉" to "Celebration mode ON! 🎉",
            "🔥" to "Yeh to fire hai! 🔥",
            "💯" to "100% sahi hai bro 💯",
            "👍" to "Thumbs up! 👍",
            "😭" to "Roona aa gaya! 😭",
            "🤣" to "Jaldi se hasne do! 🤣",
            "❤️" to "Dil se pyaar 💕",
            "😍" to "Ladki dekhna 😍",
            "🥰" to "Cute lagta hai! 🥰"
        )

        memeData.forEach { (emoji, message) ->
            val button = Button(context).apply {
                text = emoji
                textSize = 28f
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 80
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(4, 4, 4, 4)
                }
                setOnClickListener {
                    emojiClickListener?.invoke(emoji, message)
                }
            }
            gridLayout.addView(button)
        }

        addView(gridLayout)

        // Add a spacer view at the bottom
        val spacer = LinearLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 20)
        }
        addView(spacer)
    }

    fun setOnEmojiClickListener(listener: (emoji: String, dialogue: String) -> Unit) {
        emojiClickListener = listener
    }
}
