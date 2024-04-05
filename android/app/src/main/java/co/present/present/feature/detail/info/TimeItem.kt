package co.present.present.feature.detail.info

import co.present.present.R
import co.present.present.model.Circle
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import getDateRangeWithFullWeekday
import getTimeRange
import kotlinx.android.synthetic.main.item_time.*

data class TimeItem(val circle: Circle, val listener: OnAddToCalendarListener) : Item() {

    override fun getLayout() = R.layout.item_time

    override fun bind(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        bind(holder, position)
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        // TODO: figure out some way to enforce in the compiler
        val startTime = circle.startTime!!
        val endTime = circle.endTime ?: startTime

        viewHolder.date.text = getDateRangeWithFullWeekday(startTime, endTime)
        viewHolder.time.text = getTimeRange(startTime, endTime)

        viewHolder.addToCalendar.setOnClickListener {
            listener.onAddToCalendarClicked(this, circle)
        }
    }

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        return other is TimeItem
    }

    override fun getId() = hashCode().toLong()

    interface OnAddToCalendarListener {
        fun onAddToCalendarClicked(item: TimeItem, circle: Circle)
    }
}