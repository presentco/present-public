package co.present.present.feature.detail.chat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.*
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import co.present.present.BaseFragment
import co.present.present.R
import co.present.present.ViewModelFactory
import co.present.present.analytics.AmplitudeEvents
import co.present.present.extensions.*
import co.present.present.feature.SignUpDialogActivity
import co.present.present.feature.detail.CircleActivity
import co.present.present.feature.image.ImageGalleryActivity
import co.present.present.feature.invite.AddToCircleActivity
import co.present.present.model.Chat
import co.present.present.model.Circle
import co.present.present.model.CurrentUser
import co.present.present.view.AfterTextChangedWatcher
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_chat.*
import present.wire.rpc.core.ClientException
import java.util.*
import javax.inject.Inject


open class ChatFragment : BaseFragment(), ChatItem.OnPhotoClickedListener, ChatItem.OnUserClickedListener {

    val circleId: String by lazy { activity!!.intent.getStringExtra(Circle.ARG_CIRCLE) }

    @Inject lateinit var viewModelFactory: ViewModelFactory
    lateinit var viewModel: ChatViewModel

    private var adapter = ChatAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        performInjection()
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(ChatViewModel::class.java)
        analytics.log(AmplitudeEvents.CIRCLE_CHAT_VIEW)

        // The Chat fragment is the first tab, so if we're ever created, mark read initially
        viewModel.forceSetRead(circleId).subscribeOn(Schedulers.io()).subscribeBy(
                onError = {
                    Log.e(TAG, "Network error setting circle read")
                },
                onComplete = {
                    Log.d(TAG, "Successfully set circle  read on server")
                }
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, true)
        recyclerView.adapter = adapter
        photoButton.setOnClickListener { launchImagePicker(photoButton.context) }
        imagePreviewDiscard.setOnClickListener { clearImage() }
        compose.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            viewModel.onComposeFocusChanged(hasFocus)
        }
        joinButton.drawableLeft(R.drawable.ic_not_joined)
    }

    private fun inviteFriends(currentUser: CurrentUser?) {
        baseActivity.doIfLoggedIn {
            baseActivity.startActivityForResult(AddToCircleActivity.newIntent(baseActivity, circleId), CircleActivity.ADD_FRIENDS_REQUEST)
        }
    }

    override fun onUserClicked(item: ChatItem) {
        analytics.log(AmplitudeEvents.CIRCLE_CHAT_TAP_USER)
        baseActivity.launchUser(item.chat.user.uuid)
    }

    private fun launchImagePicker(context: Context) {
        CropImage.activity()
                .setCropShape(CropImageView.CropShape.RECTANGLE)
                .setCropMenuCropButtonTitle("Done")
                .setAllowFlipping(false)
                .setAllowRotation(false)
                .setAutoZoomEnabled(false)
                .setRequestedSize(1080, 1080)
                .setOutputUri(viewModel.temporaryPhotoUri)
                .start(context, this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode) {
            CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    viewModel.uploadPhoto().compose(applySingleSchedulers())
                            .doOnSubscribe {
                                showLoadingImagePreview()
                            }
                            .subscribeBy(
                                    onSuccess = { uri ->
                                        Log.d(TAG, "Successfully uploaded new chat photo")
                                        updateSendButtonState()
                                        showLoadedImagePreview(uri)
                                        analytics.log(AmplitudeEvents.CIRCLE_CHAT_ADD_PHOTO)
                                    },
                                    onError = { e ->
                                        Log.e(TAG, "Error uploading chat photo", e)
                                        snackbar(R.string.photo_upload_error)
                                        hideImagePreview()
                                    }
                            )
                } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    snackbar(R.string.generic_error)
                }
            }
        }
    }

    private fun hideImagePreview() {
        imagePreview.hide()
        imageTopPadding.hide()
        imagePreviewSpinner.hide()
    }

    private fun showLoadedImagePreview(uri: Uri) {
        imagePreviewSpinner.hide()
        imagePreview.show()
        imageTopPadding.show()
        imagePreview.layoutParams.width = ConstraintLayout.LayoutParams.WRAP_CONTENT
        imagePreview.loadFromUri(uri, skipMemoryCache = true, diskCacheStrategy = DiskCacheStrategy.NONE)
        imagePreviewDiscard.show()
    }

    private fun showLoadingImagePreview() {
        imagePreview.setImageDrawable(null)
        imagePreview.show()
        imageTopPadding.show()
        imagePreview.layoutParams.width = resources.getDimensionPixelSize(R.dimen.chat_image_preview_default_width)
        imagePreviewSpinner.show()
    }

    private fun clearImage() {
        imagePreview.hide()
        imageTopPadding.hide()
        imagePreviewDiscard.hide()
        viewModel.clearUuid()
        updateSendButtonState()
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        // This can actually be called *before* onCreate(), which is where we initialize the ViewModel
        view?.let {
            if (isVisibleToUser) {
                Log.d(TAG, "Chat fragment is becoming visible.")
                viewModel.setVisible(isVisibleToUser)
            }
        }
    }

    private val emptyAdapterDataObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            updateEmptyState()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            updateEmptyState()
        }

        fun updateEmptyState() {
            if (adapter.itemCount == 0) {
                empty.show()
                recyclerView.hide()
            } else {
                empty.hide()
                recyclerView.show()
            }
        }
    }

    private val onInviteButtonClickListener = object : ChatEmptyView.InviteButtonClickListener {
        override fun onInviteButtonClicked(currentUser: CurrentUser?) {
            analytics.log(AmplitudeEvents.CIRCLE_CHAT_TAP_INVITE_FRIENDS)
            inviteFriends(currentUser)
        }
    }

    override fun onResume() {
        super.onResume()
        updateSendButtonState()
        compose.addTextChangedListener(composeTextWatcher)
        adapter.registerAdapterDataObserver(emptyAdapterDataObserver)

        disposable += viewModel.getState(circleId)
                .compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = { Log.e(TAG, "error", it) },
                        onNext = {
                            empty.bind(it, onInviteButtonClickListener)
                        }
                )

        disposable += viewModel.getComments(circleId)
                .compose(applySingleSchedulers())
                .doOnSubscribe {
                    empty.hide()
                    chatLoadingSpinner.show()
                }
                .subscribeBy(
                        onError = {
                            chatLoadingSpinner.hide()
                            if (it is ClientException) {
                                Log.d(TAG, "Not allowed to load previous comments.")
                                empty.show()
                            } else {
                                Log.e(TAG, "Failed to load previous comments. We should probably retry, but don't yet", it)
                            }
                        },
                        onSuccess = { comments ->
                            Log.d(TAG, "Got ${comments.size} previous chat comments")
                            val messages = comments.map { ChatItem(it, this, this@ChatFragment, this@ChatFragment) }
                            chatLoadingSpinner.hide()

                            adapter.clear()
                            adapter.addHistory(messages)
                            scrollToBottom()

                            // Manually trigger the empty state calculation; in case nothing was added, the observer won't run.
                            emptyAdapterDataObserver.updateEmptyState()
                        }
                )

        disposable += viewModel.getCommentUpdates(circleId)
                .compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = { e ->
                            Log.e(TAG, "Connection to live server failed, chat can't continue", e)
                        },
                        onNext = { chat ->
                            Log.d(TAG, "New event from live server")
                            if (chat.deleted) {
                                deleteComment(chat)
                            } else {
                                addComment(chat)
                            }
                        }
                )

        disposable += viewModel.currentUserOptional
                .firstElement()
                .compose(applyMaybeSchedulers())
                .subscribe { currentUserOptional ->
                    val currentUser = currentUserOptional.value
                    setChatEnterMessageListener(currentUser)
                    currentUser?.let { setChatLongClickListener(currentUser) }
                }

        disposable += viewModel.setRead(circleId).subscribeOn(Schedulers.io()).subscribeBy(
                onError = {
                    Log.e(TAG, "Network error: Couldn't set circle read")
                },
                onComplete = {
                    Log.d(TAG, "Successfully set circle read")
                }
        )

        viewModel.getJoinButtonVisibility(circleId).compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = {},
                        onNext = { visible ->
                            joinButton.setVisible(visible)
                        }
                ).addTo(disposable)

        viewModel.getComposeVisibility(circleId).compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = {},
                        onNext = { composeGroup.setVisible(it) }
                )

        viewModel.shouldHideKeyboard().compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = {},
                        onNext = { shouldHideKeyboard ->
                            if (shouldHideKeyboard) compose.hideKeyboard()
                        }
                ).addTo(disposable)

        viewModel.getCircleAndCurrentUser(circleId).compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = {},
                        onNext = { (circle, currentUserOptional) ->
                            joinButton.setOnClickListener {
                                analytics.log(AmplitudeEvents.CIRCLE_CHAT_TAP_JOIN)
                                if (currentUserOptional.value == null) {
                                    start<SignUpDialogActivity>()
                                } else {
                                    joinCircle(circle)
                                }
                            }
                        }
                ).addTo(disposable)
    }

    private fun joinCircle(circle: Circle) {
        viewModel.toggleCircleJoin(circle).compose(applyCompletableSchedulers())
                .doOnSubscribe {
                    // TODO: Make join button show a spinner
                }
                .subscribeBy(
                        onError = {
                            toast(R.string.network_error)
                            joinButton.drawableLeft(R.drawable.ic_not_joined)
                        },
                        onComplete = {
                            // No need to manipulate Join button here, its state is driven by the database / Rx.
                            analytics.log(AmplitudeEvents.CIRCLE_CHAT_JOIN)
                        }
                ).addTo(disposable)
    }

    private fun setChatLongClickListener(currentUser: CurrentUser) {
        adapter.setOnItemLongClickListener { item, view ->
            clearPreviousSelection()
            if (item is ChatItem) {
                showPopup(view, item, currentUser)
                lastSelectedView = view
                view.isSelected = true
            }
            true
        }
    }

    override fun onPhotoClicked(item: ChatItem) {
        activity?.apply {
            analytics.log(AmplitudeEvents.CIRCLE_CHAT_VIEW_ADD_PHOTO)
            start(ImageGalleryActivity.newIntent(this, circleId, item.chat.id))
        }
    }

    private fun addComment(chat: Chat) {
        adapter.addToStart(ChatItem(chat, this, this, this))
        scrollToBottom()
    }

    private fun scrollToBottom() {
        recyclerView.postDelayed({
            recyclerView?.smoothScrollToPosition(0)
        }, 200)
    }

    private fun deleteComment(chat: Chat) {
        adapter.delete(chat)
    }

    private fun setChatEnterMessageListener(currentUser: CurrentUser?) {
        sendButton.setOnClickListener {
            analytics.log(AmplitudeEvents.CIRCLE_CHAT_TAP_SEND_COMMENT)

            // Different behavior for logged out users
            if (currentUser == null) {
                start<SignUpDialogActivity>()
                return@setOnClickListener
            }

            val message = compose.text.toString()

            // Optimistically post comment.
            val messageId = UUID.randomUUID().toString()
            addComment(Chat(messageId, message, currentUser, viewModel.photoUploader.photoUrl))
            compose.text.clear()
            imagePreview.hide()
            imageTopPadding.hide()
            imagePreviewDiscard.hide()
            compose.hideKeyboard()

            disposable += viewModel.postComment(messageId, circleId, message)
                    .doOnSubscribe {
                        viewModel.clearUuid()
                        updateSendButtonState()
                    }
                    .compose(applyCompletableSchedulers())
                    .subscribeBy(
                            onError = { e ->
                                Log.e(TAG, "Couldn't post comment: \n    $message\nWe should indicate the problem visually and show a retry button", e)
                            },
                            onComplete = {
                                Log.d(TAG, "Comment posted successfully, no need to do anything.")
                                analytics.log(AmplitudeEvents.CIRCLE_CHAT_ADD_COMMENT)
                            }
                    )
        }
    }

    private val composeTextWatcher = object : AfterTextChangedWatcher() {
        override fun afterTextChanged(editable: Editable) {
            updateSendButtonState()
        }
    }

    private fun updateSendButtonState() {
        sendButton.post {
            sendButton.isEnabled = compose.text.isNotBlank() || viewModel.photoUploader.uuid != null
        }
    }

    override fun onPause() {
        compose.removeTextChangedListener(composeTextWatcher)
        adapter.unregisterAdapterDataObserver(emptyAdapterDataObserver)
        super.onPause()
    }

    protected open fun performInjection() {
        activityComponent.inject(this)
    }

    private var lastSelectedView: View? = null

    private fun showPopup(v: View, message: ChatItem, currentUser: CurrentUser) {
        context?.let {
            val popup = PopupMenu(it, v)
            popup.inflate(R.menu.chat_message)
            if (!viewModel.canDeleteComment(currentUser, message.chat)) {
                popup.menu.removeItem(R.id.delete)
            }
            popup.gravity = Gravity.TOP or Gravity.END
            popup.show()
            popup.setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.copy -> activity?.copyToClipboard(message.chat.comment)
                    R.id.delete -> deleteChatMessage(message.chat)
                }
                true
            }
            popup.setOnDismissListener {
                clearPreviousSelection()
            }
        }

    }

    private fun clearPreviousSelection() {
        lastSelectedView?.isSelected = false
        lastSelectedView = null
    }

    private fun deleteChatMessage(message: Chat) {
        dialog(R.string.delete_confirm, positiveButtonText = R.string.delete) {
            disposable += viewModel.deleteComment(message.id)
                    .compose(applyCompletableSchedulers())
                    .subscribeBy(
                            onError = {
                                snackbar(R.string.network_error)
                            },
                            onComplete = {
                                deleteComment(message)
                                analytics.log(AmplitudeEvents.CIRCLE_CHAT_DELETE_COMMENT)
                            }
                    )
        }
    }

    companion object {
        private val TAG = ChatFragment::class.java.simpleName

        fun newInstance(circleId: String): ChatFragment {
            val fragment = ChatFragment()
            val bundle = Bundle()
            bundle.putString(Circle.ARG_CIRCLE, circleId)
            fragment.arguments = bundle
            return fragment
        }
    }
}
