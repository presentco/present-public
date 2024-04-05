
import android.content.Context
import android.text.format.DateUtils
import android.text.format.DateUtils.*
import co.present.present.PresentApplication
import co.present.present.R
import org.threeten.bp.*
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.TextStyle
import java.text.SimpleDateFormat
import java.util.*


/**
 * Just now
 * 15 mins
 * 10:22AM
 * Thursday
 * Feb 21
 *
 */
fun Context.getShortRelativeDate(localDateTime: LocalDateTime): String? {

    val duration = Duration.between(localDateTime, localDateTimeNow())
    return if (duration.toMinutes() < 1) {
        getString(R.string.justNow)
    } else if (duration.toHours() < 1) {
        val min = duration.toMinutes()
        resources.getQuantityString(R.plurals.min, min.toInt(), min)
    } else if (duration.toDays() < 1) {
        timeDateFormat.format(localDateTime)
    } else if (duration.toDays() < 7) {
        localDateTime.dayOfWeek.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
    } else {
        shortDateFormat.format(localDateTime)
    }
}

/**
 * Now
 * 15m
 * 10:22AM
 * Thurs
 * Feb 21
 *
 */
fun Context.getXShortRelativeDate(localDateTime: LocalDateTime): String? {

    val duration = Duration.between(localDateTime, localDateTimeNow())
    return if (duration.toMinutes() < 1) {
        getString(R.string.now)
    } else if (duration.toHours() < 1) {
        val min = duration.toMinutes()
        "${min}m"
    } else if (duration.toDays() < 1) {
        timeDateFormat.format(localDateTime)
    } else if (duration.toDays() < 7) {
        localDateTime.dayOfWeek.getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault())
    } else {
        shortDateFormat.format(localDateTime)
    }
}



fun Duration.toMinutesPart(): Long {
    return minusHours(toHours()).toMinutes()
}

fun Duration.toSecondsPart(): Long {
    return minusMinutes(toMinutes()).seconds
}

fun LocalDateTime.isToday(): Boolean {
    val zonedNow = ZonedDateTime.now(ZoneId.systemDefault())
    val nowDate = LocalDate.from(zonedNow)
    return LocalDate.from(this) == nowDate
}

fun LocalDateTime.isYesterday(): Boolean {
    val zonedYesterday = ZonedDateTime.now(ZoneId.systemDefault()).minusDays(1)
    val yesterdayDate = LocalDate.from(zonedYesterday)
    return LocalDate.from(this) == yesterdayDate
}

fun LocalDateTime.isCurrentYear(): Boolean {
    val zonedNow = ZonedDateTime.now(ZoneId.systemDefault())
    return this.year == zonedNow.year
}

fun localDateTimeNow(): LocalDateTime = LocalDateTime.from(zonedDateNow())

private fun zonedDateNow(): ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())

fun zonedDateNow12Am(): ZonedDateTime {
    val currentTime = ZonedDateTime.now()
    val hours = currentTime.hour
    val minutes = currentTime.minute
    return currentTime.minusMinutes(minutes.toLong()).minusHours(hours.toLong())
}

fun LocalDateTime.isSameDate(other: LocalDateTime): Boolean {
    return this.toLocalDate() == other.toLocalDate()
}

fun Long.toLocalDateTime(): LocalDateTime {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())
}

val timeFormat = SimpleDateFormat("h:mm a", Locale.US)                      // 1:12PM
val timeDateFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.US)           // 1:12PM

val shortDateFormat = DateTimeFormatter.ofPattern("MMM d", Locale.US)       // Feb 25
val longDateFormat = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)  // Feb 25, 2014

/**
 * Feb 25, 4PM
 * Feb 25, 4:00PM - 4:30PM
 * Feb 25, 4PM - Feb 26, 11AM
 * Will only drop zeroes if both times are exact hours, unfortunately
 */
fun getShortDateTimeRange(startMillis: Long, endTime: Long): String {
    val flags = FORMAT_ABBREV_TIME or FORMAT_ABBREV_ALL or FORMAT_SHOW_TIME or FORMAT_SHOW_DATE or FORMAT_CAP_AMPM
    return formatRange(startMillis, endTime, flags)
}

/**
 * Monday, February 25
 * Monday, February 25 - Tuesday, February 26
 */
fun getDateRangeWithFullWeekday(startMillis: Long, endTime: Long): String {
    val flags = FORMAT_SHOW_WEEKDAY or FORMAT_SHOW_DATE
    return formatRange(startMillis, endTime, flags)
}

/**
 * 9:00 - 10:00AM
 * 11:00AM - 1:30PM
 */
fun getTimeRange(startMillis: Long, endTime: Long): String {
    val flags = FORMAT_SHOW_TIME or FORMAT_CAP_AMPM
    return formatRange(startMillis, endTime, flags)
}

/**
 * Formats a date range using the user's local date / time prefs.  If the start and end
 * are the same, shows only start date.  If end is before start, assumes invalid end and shows
 * only start date.
 */
private fun formatRange(startMillis: Long, endMillis: Long, flags: Int): String {
    val sb = StringBuilder()
    val formatter = Formatter(sb, Locale.getDefault())
    DateUtils.formatDateRange(
            PresentApplication.staticAppComponent.application,
            formatter,
            startMillis,
            if (endMillis < startMillis) startMillis else endMillis,
            flags,
            TimeZone.getDefault().id)
    return sb.toString()
}


