package co.present.present.feature.detail.chat

import co.present.present.model.Chat
import io.reactivex.Flowable
import io.reactivex.Single
import present.proto.CommentResponse

interface GetComments {

    fun getComments(circleId: String): Single<List<Chat>>

    fun getLiveServerUpdates(circleId: String): Flowable<CommentResponse>

}