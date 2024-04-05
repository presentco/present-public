package co.present.present.feature.detail.chat

import android.app.Application
import co.present.present.db.CircleDao
import co.present.present.feature.common.GetCircle
import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.feature.detail.info.JoinCircle
import co.present.present.location.LocationDataProvider
import co.present.present.model.Chat
import co.present.present.service.RpcManager
import co.present.present.support.DataHelper
import co.present.present.view.OnLinkClickedListener
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import present.proto.GroupService
import java.util.*

class ChatViewModelTest {

    @Mock lateinit var groupService: GroupService
    @Mock lateinit var rpcManager: RpcManager
    @Mock lateinit var application: Application
    @Mock lateinit var getCurrentUser: GetCurrentUser
    @Mock lateinit var locationProvider: LocationDataProvider
    @Mock lateinit var circleDao: CircleDao
    @Mock lateinit var getComments: GetComments
    @Mock lateinit var photoClickedListener: ChatItem.OnPhotoClickedListener
    @Mock lateinit var userClickedListener: ChatItem.OnUserClickedListener
    @Mock lateinit var onLinkClickedListener: OnLinkClickedListener
    @Mock lateinit var chatUploadPhotoImpl: ChatUploadPhotoImpl
    @Mock lateinit var getCircle: GetCircle
    @Mock lateinit var joinCircle: JoinCircle
    lateinit var testObject: ChatViewModel
    private val testUuid = UUID.randomUUID().toString()

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        testObject = ChatViewModel(groupService, rpcManager, locationProvider, circleDao, getComments, getCircle, chatUploadPhotoImpl, joinCircle, getCurrentUser, application)
    }

    @Test
    fun normalUserCanDeleteOwnComment() {
        val id = "42"
        val currentUser = DataHelper.getCurrentUser().apply {
            this.id = id
            this.isAdmin = false
        }
        val message = ChatItem(Chat(testUuid, "blah", currentUser), onLinkClickedListener, photoClickedListener, userClickedListener)
        Assert.assertTrue(testObject.canDeleteComment(currentUser, message.chat))
    }

    @Test
    fun normalUserCannotDeleteOthersComment() {
        val id = "42"
        val currentUser = DataHelper.getCurrentUser().apply {
            this.id = id
            this.isAdmin = false
        }
        val message = ChatItem(Chat(testUuid, "blah", currentUser), onLinkClickedListener, photoClickedListener, userClickedListener)

        val differentId = "36"
        currentUser.id = differentId
        Assert.assertFalse(testObject.canDeleteComment(currentUser, message.chat))
    }

    @Test
    fun adminCanDeleteOthersComments() {
        val id = "42"
        val currentUser = DataHelper.getCurrentUser().apply {
            this.id = id
            this.isAdmin = true
        }
        val message = ChatItem(Chat(testUuid, "blah", currentUser), onLinkClickedListener, photoClickedListener, userClickedListener)

        val differentId = "36"
        currentUser.id = differentId
        Assert.assertTrue(testObject.canDeleteComment(currentUser, message.chat))
    }

}