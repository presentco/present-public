package co.present.present.feature.detail.chat

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import timeFormat
import java.util.*


class ChatItemTest {

    @Before
    fun setUp() {
        timeFormat.apply {  timeZone = TimeZone.getTimeZone("Etc/UTC") }
    }

    @Test
    fun morningTimeIsCorrect() {
        val date = Date.UTC(2017, 12, 1, 8, 27, 40)  // 2017-12-18 8:27:40
        Assert.assertEquals("8:27 AM", timeFormat.format(date))
    }

    @Test
    fun eveningTimeIsCorrect() {
        val date = Date.UTC(2017, 12, 1, 21, 27, 40)  // 2017-12-18 9:27:40
        Assert.assertEquals("9:27 PM", timeFormat.format(date))
    }

    @Test
    fun noonTimeIsCorrect() {
        val date = Date.UTC(2017, 12, 1, 12, 13, 40)  // 2017-12-18 12:13:40
        Assert.assertEquals("12:13 PM", timeFormat.format(date))
    }
}