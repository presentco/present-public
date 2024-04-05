package co.present.present.feature.image

import androidx.lifecycle.ViewModel
import co.present.present.feature.detail.chat.GetComments
import co.present.present.model.Chat
import io.reactivex.Single
import javax.inject.Inject

class ImageGalleryViewModel @Inject constructor(getComments: GetComments): ViewModel(), GetComments by getComments {

    var selectedChatIndex : Int? = null

    data class ImageGalleryData(val chats: List<Chat>, val selectedChatIndex: Int)

    fun getImageGalleryData(circleId: String, chatId: String): Single<ImageGalleryData> {
        return imageChats(circleId)
                .map { chats -> ImageGalleryData(chats, selectedChatIndex(chats, chatId))}
    }

    /**
     * All chats to date which contain images.
     */
    private fun imageChats(circleId: String): Single<List<Chat>> {
        return getComments(circleId).map { comments -> comments.filter { it.photo != null }.reversed() }
    }

    private fun selectedChatIndex(chats: List<Chat>, chatId: String): Int {
        return selectedChatIndex ?: chats.indexOf(chats.find { it.id == chatId })
    }
}