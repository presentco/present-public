package co.present.present.feature.activity

import co.present.present.R
import co.present.present.extensions.loadCircularImage
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import isSameDate
import kotlinx.android.synthetic.main.item_activity_feed.*
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import present.proto.EventResponse
import java.util.*


class ActivityFeedItem(val eventResponse: EventResponse): Item() {

    private val localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(eventResponse.`when`), ZoneId.systemDefault())

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.photo.loadCircularImage(eventResponse.icon)

        viewHolder.text.text = eventResponse.summary

        val format = if (localDateTime.isSameDate(LocalDateTime.now())) timeFormat else dateFormat
        viewHolder.date.text = format.format(localDateTime)
    }

    override fun getLayout() = R.layout.item_activity_feed

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        return other is ActivityFeedItem && other.eventResponse.uuid == eventResponse.uuid
    }

    companion object {
        val timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
        val dateFormat = DateTimeFormatter.ofPattern("MMM d", Locale.US)
    }
}