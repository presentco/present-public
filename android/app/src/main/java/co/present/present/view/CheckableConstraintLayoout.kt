package co.present.present.view

import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import android.widget.Checkable

class CheckableConstraintLayout(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs), Checkable {

    private var mChecked = false

    private var mOnCheckedChangeListener: OnCheckedChangeListener? = null

    /**
     * Interface definition for a callback to be invoked when the checked state of this View is
     * changed.
     */
    interface OnCheckedChangeListener {

        /**
         * Called when the checked state of a compound button has changed.
         *
         * @param checkableView The view whose state has changed.
         * @param isChecked     The new checked state of checkableView.
         */
        fun onCheckedChanged(checkableView: View, isChecked: Boolean)
    }

    override fun isChecked(): Boolean {
        return mChecked
    }

    override fun setChecked(b: Boolean) {
        if (b != mChecked) {
            mChecked = b
            refreshDrawableState()

            if (mOnCheckedChangeListener != null) {
                mOnCheckedChangeListener!!.onCheckedChanged(this, mChecked)
            }
        }
    }

    override fun toggle() {
        isChecked = !mChecked
    }

    public override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        if (isChecked) {
            View.mergeDrawableStates(drawableState, CHECKED_STATE_SET)
        }
        return drawableState
    }

    /**
     * Register a callback to be invoked when the checked state of this view changes.
     *
     * @param listener the callback to call on checked state change
     */
    fun setOnCheckedChangeListener(listener: OnCheckedChangeListener) {
        mOnCheckedChangeListener = listener
    }

    companion object {

        private val CHECKED_STATE_SET = intArrayOf(android.R.attr.state_checked)
    }

}