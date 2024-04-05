package co.present.present.feature.detail.chat

import co.present.present.model.Chat
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import isSameDate
import org.threeten.bp.LocalDateTime


class ChatAdapter: GroupAdapter<ViewHolder>() {

    fun addToStart(chatItem: ChatItem) {
        val isNewMessageToday = isPreviousSameDate(chatItem.chat.localCreatedDateTime)
        if (!isNewMessageToday) {
            add(0, DateHeaderItem(chatItem.chat.localCreatedDateTime))
        }
        add(0, chatItem)
    }

    private fun isPreviousSameDate(dateToCompare: LocalDateTime): Boolean {
        if (itemCount == 0) return false
        getItem(0).let { firstItem ->
            return if (firstItem is ChatItem) {
                firstItem.chat.localCreatedDateTime.isSameDate(dateToCompare)
            } else false
        }
    }

    fun addHistory(chatItems: List<ChatItem>) {
        addAll(generateDateHeaders(chatItems))
    }

    private fun generateDateHeaders(chatItems: List<ChatItem>): List<Item<*>> {
        val items = mutableListOf<Item<*>>()
        chatItems.forEachIndexed { i, chatItem ->
            items.add(chatItem)
            if (chatItems.size > i + 1) {
                val nextMessage = chatItems[i + 1]
                if (!chatItem.chat.localCreatedDateTime.isSameDate(nextMessage.chat.localCreatedDateTime)) {
                    items.add(DateHeaderItem(chatItem.chat.localCreatedDateTime))
                }
            } else {
                items.add(DateHeaderItem(chatItem.chat.localCreatedDateTime))
            }
        }
        return items
    }

    fun delete(chat: Chat) {
        for (i in 0..itemCount) {
            val item = getItem(i)
            if (item is ChatItem && item.chat == chat) {
                remove(item)
                // TODO: Evaluate whether the date headers are still appropriate
                return
            }
        }
    }

}