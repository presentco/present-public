package co.present.present.view

import android.content.Context
import androidx.viewpager.widget.ViewPager
import android.util.AttributeSet
import android.view.View

/**
 * A ViewPager which can wrap its children vertically. Based on this SO answer:
 * https://stackoverflow.com/a/20784791
 *
 * Because a normal viewpager doesn't inflate (and therefore can't measure) anything except the
 * first child, if your items have different heights, you need to use it with
 * setOffscreenPageLimit(totalPages).  Yes, this destroys the performance benefits
 * of a view pager, so don't put too many things in it.
 */
class WrapContentViewPager(context: Context, attributeSet: AttributeSet): ViewPager(context, attributeSet) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var heightMeasureSpec = heightMeasureSpec

        var height = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.measure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            val h = child.measuredHeight
            if (h > height) height = h
        }

        if (height != 0) {
            heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}