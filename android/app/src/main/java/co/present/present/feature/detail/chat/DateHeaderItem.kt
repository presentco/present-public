package co.present.present.feature.detail.chat

import co.present.present.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import isCurrentYear
import isToday
import isYesterday
import kotlinx.android.synthetic.main.item_header.*
import longDateFormat
import org.threeten.bp.LocalDateTime
import shortDateFormat

class DateHeaderItem(val date: LocalDateTime): Item() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        when {
            date.isToday() -> viewHolder.headerText.setText(R.string.chat_header_today)
            date.isYesterday() -> viewHolder.headerText.setText(R.string.chat_header_yesterday)
            date.isCurrentYear() -> viewHolder.headerText.text = dateFormatCurrentYear.format(date)
            else -> viewHolder.headerText.text = dateFormatPreviousYear.format(date)
        }
    }

    override fun getLayout() = R.layout.item_header

    companion object {
        private val dateFormatCurrentYear = shortDateFormat
        private val dateFormatPreviousYear = longDateFormat
    }
}