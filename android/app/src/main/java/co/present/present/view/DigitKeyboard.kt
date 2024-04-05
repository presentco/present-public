package co.present.present.view

import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.TextView
import co.present.present.R
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.digits_keyboard.view.*

class DigitKeyboard(context: Context, attributeSet: AttributeSet): ConstraintLayout(context, attributeSet), LayoutContainer {
    override val containerView: View? = this

    init {
        View.inflate(context, R.layout.digits_keyboard, this)
        listOf(zero, one, two, three, four, five, six, seven, eight, nine).forEachIndexed { index, textView ->
            textView.setOnClickListener {
                Log.d("DigitKeyboard", "Clicked view ${(it as TextView).text} with index $index")
                listener?.onDigit(index)
            }
        }

        delete.setOnClickListener { listener?.onDelete() }
    }

    private var listener: KeyListener? = null

    fun setListener(listener: KeyListener) {
        this.listener = listener
    }

    interface KeyListener {
        fun onDigit(digit: Int)
        fun onDelete()
    }
}