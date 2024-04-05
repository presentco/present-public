package co.present.present.feature.detail.chat

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.util.AttributeSet


class ChatRecyclerView(context: Context, attrs: AttributeSet) : RecyclerView(context, attrs) {

    // Force scroll the list to bottom (latest message) on a size change.  The reason to do
    // this is to handle the keyboard appearing, which the RV doesn't seem to handle itself
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (oldh != h) {
            smoothScrollToPosition(0)
        }
    }
}