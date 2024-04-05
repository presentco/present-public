package co.present.present.view

import android.content.Context
import androidx.appcompat.widget.AppCompatEditText
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper

/**
 * A custom EditText that captures and reports the soft keyboard "back" or delete button.
 */
class BackButtonEditText: AppCompatEditText {

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context) : super(context) {}

    private lateinit var onBackspaceListener: () -> Unit

    fun onBackspace(listener: () -> Unit) {
        this.onBackspaceListener = listener
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val superConnection = super.onCreateInputConnection(outAttrs)
        return if (superConnection != null) {
            ZanyInputConnection(superConnection, true)
        } else {
            superConnection
        }
    }

    private inner class ZanyInputConnection(target: InputConnection, mutable: Boolean) : InputConnectionWrapper(target, mutable) {

        override fun sendKeyEvent(event: KeyEvent): Boolean {
            if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DEL) {
                this@BackButtonEditText.onBackspaceListener()
            }
            return super.sendKeyEvent(event)
        }

        override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
            // magic: in latest Android, deleteSurroundingText(1, 0) will be called for backspace
            return if (beforeLength == 1 && afterLength == 0) {
                // backspace
                sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)) && sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
            } else super.deleteSurroundingText(beforeLength, afterLength)

        }
    }

}