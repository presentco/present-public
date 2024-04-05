package co.present.present.feature.create

import androidx.annotation.StringRes
import co.present.present.R
import co.present.present.feature.common.Payload
import co.present.present.feature.common.item.GrayHeaderItem
import com.xwray.groupie.Section
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_date_time.*
import longDateFormat
import org.threeten.bp.temporal.TemporalAccessor
import timeDateFormat
import toLocalDateTime
import zonedDateNow12Am


sealed class DateTimeItem(@StringRes private val label: Int, val timeMillis: Long) : Item() {

    // I can't figure out how to make this a val
    lateinit var listener: OnDateTimeClickedListener

    override fun bind(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(TimePayload)) {
            bind(holder, position)
            return
        } else if (payloads.contains(Payload.DontUpdate)) return
        super.bind(holder, position, payloads)
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        with(viewHolder) {
            text.setText(label)
            val localDateTime = timeMillis.toLocalDateTime()
            date.setOnClickListener { listener.onDateClicked(this@DateTimeItem) }
            time.setOnClickListener { listener.onTimeClicked(this@DateTimeItem) }

            if (timeMillis > 0) {
                date.text = longDateFormat.format(localDateTime)

                // There's currently a bug in CL (5/31/2018) where it caches the width of the TextView.
                // For some reason, setting the hint (but not the text) forces measurement, so
                // when we set the text, also set the hint to the same value, otherwise date will be
                // misaligned because time may be incorrectly measured
                val formattedTime = timeDateFormat.format(localDateTime)
                time.apply { hint = formattedTime; text = formattedTime }
            }
        }
    }

    protected fun showDateAsHint(viewHolder: ViewHolder, localDateTime: TemporalAccessor) {
        with(viewHolder) {
            date.hint = longDateFormat.format(localDateTime)
            time.hint = timeDateFormat.format(localDateTime)
            listOf(date, time).forEach { it.text = "" }
        }
    }

    override fun getLayout() = R.layout.item_date_time

    interface OnDateTimeClickedListener {
        fun onDateClicked(item: DateTimeItem)
        fun onTimeClicked(item: DateTimeItem)
    }

    companion object {
        const val TimePayload = "timePayload"
    }
}

class StartDateTimeItem(val start: Long) : DateTimeItem(R.string.starts, start) {

    override fun bind(viewHolder: ViewHolder, position: Int) {
        super.bind(viewHolder, position)
        if (timeMillis <= 0) {
            showDateAsHint(viewHolder, zonedDateNow12Am())
        }
    }

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        return other is StartDateTimeItem
    }

    override fun getChangePayload(newItem: com.xwray.groupie.Item<*>?): Any? {
       return if ((newItem as? StartDateTimeItem)?.start != start) TimePayload else Payload.DontUpdate
    }
}

class EndDateTimeItem(val start: Long, val end: Long) : DateTimeItem(R.string.ends, end) {

    override fun bind(viewHolder: ViewHolder, position: Int) {
        super.bind(viewHolder, position)
        if (start <= 0) {
            // Start date not picked yet, so don't even show a hint for the end date
            with(viewHolder) {
                listOf(date, time).forEach { it.hint = ""; it.text = "" }
            }
        } else if (start > 0 && timeMillis <= 0) {
            // Start date has been picked, but not end date. Show the start date as the hint
            showDateAsHint(viewHolder, start.toLocalDateTime())
        }
    }

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        return other is EndDateTimeItem
    }

    override fun getChangePayload(newItem: com.xwray.groupie.Item<*>?): Any? {
        return if ((newItem as? EndDateTimeItem)?.start != start || (newItem as? EndDateTimeItem)?.end != end) TimePayload else null
    }
}

data class DateTimeGroup(val start: Long, val end: Long, val listener: DateTimeGroup.OnDateTimeClickedListener)
    : Section(), DateTimeItem.OnDateTimeClickedListener {

    init {
        setHeader(GrayHeaderItem(stringRes = R.string.time))
        val groups = listOf(StartDateTimeItem(start), EndDateTimeItem(start, end))
        groups.forEach { (it as? DateTimeItem)?.listener = this }
        addAll(groups)
    }

    override fun onDateClicked(item: DateTimeItem) {
        when(item) {
            is StartDateTimeItem -> listener.onStartDateClicked(start)
            is EndDateTimeItem -> listener.onEndDateClicked(start, end)
        }
    }

    override fun onTimeClicked(item: DateTimeItem) {
        when(item) {
            is StartDateTimeItem -> listener.onStartTimeClicked(start)
            is EndDateTimeItem -> listener.onEndTimeClicked(start, end)
        }
    }

    interface OnDateTimeClickedListener {
        fun onEndDateClicked(currentStartDate: Long, currentEndDate: Long)
        fun onStartDateClicked(currentStartDate: Long)
        fun onEndTimeClicked(currentStartDate: Long, currentEndDate: Long)
        fun onStartTimeClicked(currentStartDate: Long)
    }

}