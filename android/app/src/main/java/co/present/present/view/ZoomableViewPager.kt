package co.present.present.view

import android.content.Context
import androidx.viewpager.widget.ViewPager
import android.util.AttributeSet
import android.view.MotionEvent

/**
 * This class exists to catch a framework error thrown in conjunction with zooming in PhotoView.
 *
 * Don't remove it unless you're not using PhotoView for image zooming anymore!
 *
 * https://github.com/chrisbanes/PhotoView#issues-with-viewgroups
 */
class ZoomableViewPager(context: Context, attributeSet: AttributeSet): ViewPager(context, attributeSet) {

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        try {
            return super.onInterceptTouchEvent(ev)
        } catch (e: IllegalArgumentException) {
            return false
        }

    }

}