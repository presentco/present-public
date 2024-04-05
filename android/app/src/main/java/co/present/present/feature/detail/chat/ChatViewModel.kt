package co.present.present.feature.detail.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import android.util.Log
import co.present.present.db.CircleDao
import co.present.present.extensions.Optional
import co.present.present.feature.common.GetCircle
import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.feature.common.viewmodel.UploadPhoto
import co.present.present.feature.detail.info.JoinCircle
import co.present.present.location.LocationDataProvider
import co.present.present.model.Chat
import co.present.present.model.Circle
import co.present.present.model.CurrentUser
import co.present.present.service.RpcManager
import co.present.present.service.rpc.deleteComment
import co.present.present.service.rpc.markRead
import co.present.present.service.rpc.postComment
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.rxkotlin.combineLatest
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import present.proto.GroupMemberPreapproval
import present.proto.GroupMembershipState
import present.proto.GroupService
import javax.inject.Inject

class  ChatViewModel @Inject constructor(val groupService: GroupService,
                                        val rpcManager: RpcManager,
                                        val locationDataProvider: LocationDataProvider,
                                        val circleDao: CircleDao,
                                        val getComments: GetComments,
                                        val getCircle: GetCircle,
                                        val photoUploader: ChatUploadPhotoImpl,
                                        val joinCircle: JoinCircle,
                                        getCurrentUser: GetCurrentUser,
                                        application: Application) :
        AndroidViewModel(application),
        GetCurrentUser by getCurrentUser,
        GetComments by getComments,
        GetCircle by getCircle,
        UploadPhoto by photoUploader,
        JoinCircle by joinCircle {

    private val TAG = javaClass.simpleName

    val context get() = getApplication<Application>().applicationContext

    sealed class State(val groupMembershipState: GroupMembershipState?, val currentUser: CurrentUser?) {

        // User is either logged out or not joined, and Circle has anything other than "Anyone" pre-approval, so they cannot
        // read content or post, only request membership.
        // Group membership might be NONE, UNJOINED, REQUESTED, or REJECTED in this state.
        // Depending on group membership state, the user might be shown join prompts, or different messaging.
        class CantReadCantPost(groupMembershipState: GroupMembershipState?, currentUser: CurrentUser?): State(groupMembershipState, currentUser)

        // User is a member no matter what privacy the group has.  OR,
        // User has not joined or possibly even logged in (so they should see join prompts) but Circle has "Anyone" pre-approval so they can read and post seamlessly.
        // If they post, we first auto-join them to the group so their post will succeed.
        class CanReadCanPost(groupMembershipState: GroupMembershipState?, currentUser: CurrentUser?): State(groupMembershipState, currentUser)
    }

    fun getState(circleId: String): Flowable<ChatViewModel.State> {
        return getCircle(circleId).combineLatest(currentUserOptional).map { (circle, currentUserOptional) ->
            val currentUser = currentUserOptional.value
            if (circle.getGroupMembershipState() == GroupMembershipState.ACTIVE
                || circle.preapproval == GroupMemberPreapproval.ANYONE.value) {
                State.CanReadCanPost(circle.getGroupMembershipState(), currentUser)
            }
            else State.CantReadCantPost(circle.getGroupMembershipState(), currentUser)
        }
    }

    fun deleteComment(commentId: String): Completable {
        return groupService.deleteComment(commentId)
    }

    fun postComment(messageId: String, circleId: String, message: String): Completable {
        return groupService.postComment(messageId, circleId, message, photoUploader.uuid)
    }

    fun getCommentUpdates(circleId: String): Flowable<Chat> {
        return getLiveServerUpdates(circleId).map { Chat(it) }
    }

    fun canDeleteComment(currentUser: CurrentUser, chat: Chat): Boolean {
        return currentUser.isAdmin || chat.user.uuid == currentUser.id
    }

    fun getCircleAndCurrentUser(circleId: String): Flowable<Pair<Circle, Optional<CurrentUser>>> {
        return getCircle(circleId).combineLatest(currentUserOptional)
    }

    fun setRead(circleId: String): Completable {
        return getLatestCommentIndex(circleId)
                .combineLatest(getVisible())
                // Only set read if the screen is currently visible
                .filter { (index, visible) -> visible }
                .flatMapCompletable { (index, visible) ->
                    markRead(circleId, index).subscribeOn(Schedulers.io())
                }
    }

    fun forceSetRead(circleId: String): Completable {
        return getLatestCommentIndex(circleId)
                .flatMapCompletable { index -> markRead(circleId, index).subscribeOn(Schedulers.io()) }
    }

    fun setVisible(visibleToUser: Boolean) {
        visiblePublishSubject.onNext(visibleToUser)
    }

    fun onComposeFocusChanged(hasFocus: Boolean) {
        composeFocusPublishSubject.onNext(hasFocus)
    }

    fun getJoinButtonVisibility(circleId: String): Flowable<Boolean> {
        return getComposeFocus().combineLatest(getVisible(), getCircle(circleId))
                .map { (composeFocused, visible, circle) ->
                    circle.isNotJoinedOrRequested() && visible && !composeFocused
                }
    }

    fun getComposeVisibility(circleId: String): Flowable<Boolean> {
        return getState(circleId).map { it is State.CanReadCanPost }
    }

    fun shouldHideKeyboard(): Flowable<Boolean> {
        return getComposeFocus().combineLatest(getVisible())
                .map { (composeFocused, visible) ->
                    !composeFocused || !visible
                }
    }

    /**
     * Mark the circle read on the server as of the given comment index, then update the circle's
     * read state in the database.
     */
    private fun markRead(circleId: String, lastReadCommentIndex: Int): Completable {
        return groupService.markRead(circleId, lastReadCommentIndex).toSingleDefault(true)
                .doOnSuccess { Log.d(TAG, "Marked circle read on server") }
                .map { circleDao.markRead(circleId) }
                .doOnSuccess { Log.d(TAG, "Marked circle read in database") }
                .toCompletable()
    }

    private fun getLatestCommentIndex(circleId: String): Flowable<Int> {
        return getComments(circleId).map { if (it.isEmpty()) -1 else it.first().index }
                .mergeWith { getLiveServerUpdates(circleId).filter { !it.deleted }.map { it.index } }
    }

    private fun getVisible(): Flowable<Boolean> = visiblePublishSubject.toFlowable(BackpressureStrategy.LATEST)

    private val visiblePublishSubject = BehaviorSubject.createDefault(true)

    private val composeFocusPublishSubject = BehaviorSubject.createDefault(false)

    private fun getComposeFocus(): Flowable<Boolean> = composeFocusPublishSubject.toFlowable(BackpressureStrategy.LATEST)



}
