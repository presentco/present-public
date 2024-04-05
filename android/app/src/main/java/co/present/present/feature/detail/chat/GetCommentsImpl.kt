package co.present.present.feature.detail.chat

import android.util.Log
import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.model.Chat
import co.present.present.model.CurrentUser
import co.present.present.service.RpcManager
import co.present.present.service.rpc.getPastComments
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import present.live.client.LiveClient
import present.proto.CommentResponse
import present.proto.GroupService
import javax.inject.Inject

class GetCommentsImpl @Inject constructor(val groupService: GroupService,
                                          val getCurrentUser: GetCurrentUser,
                                          val rpcManager: RpcManager): GetComments, GetCurrentUser by getCurrentUser {

    private val oldCommentsMap = mutableMapOf<String, Flowable<List<Chat>>>()

    override fun getComments(circleId: String): Single<List<Chat>> {
        return groupService.getPastComments(circleId).map { it.comments.map { Chat(it) } }
    }

    override fun getLiveServerUpdates(circleId: String): Flowable<CommentResponse> {
        return currentUser.flatMap { currentUser ->
            liveServerUpdates(currentUser, circleId)
        }
    }

    private fun liveServerUpdates(currentUser: CurrentUser, circleId: String): Flowable<CommentResponse> {
        return Flowable.create<CommentResponse>({ emitter ->
            val liveClient = LiveClient(groupService, rpcManager.generateHeader(), currentUser.id, circleId, object : LiveClient.Listener {
                override fun ready() {

                }

                override fun comment(commentResponse: CommentResponse) {
                    emitter.onNext(commentResponse)
                    invalidatePastComments()
                }

                override fun deleted(commentResponse: CommentResponse) {
                    emitter.onNext(commentResponse)
                    invalidatePastComments()
                }

                override fun closed() {
                    emitter.onComplete()
                }

                override fun networkError(throwable: Throwable) {
                    emitter.onError(throwable)
                }
            })
            emitter.setCancellable {
                Log.d("LiveClient", "onClose()")
                liveClient.close()
            }
        }, BackpressureStrategy.LATEST)
    }

    private fun invalidatePastComments() {
        oldCommentsMap.clear()
    }


}