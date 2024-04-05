package co.present.present.feature.detail.info

import co.present.present.db.CircleDao
import co.present.present.db.UserDao
import co.present.present.feature.common.GetMembers
import co.present.present.support.DataHelper
import co.present.present.support.getUser
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Flowable
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import present.proto.*


class JoinCircleImplTest {
    lateinit var testObject: JoinCircle

    @Mock lateinit var groupService: GroupService
    @Mock lateinit var circleDao: CircleDao
    @Mock lateinit var userDao: UserDao
    @Mock lateinit var getMembers: GetMembers

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        testObject = JoinCircleImpl(groupService, circleDao, userDao, getMembers)
    }

    @Test
    @Throws(Exception::class)
    fun viewModelCallsSaveGroup() {
        val circle = DataHelper.getCircle(false)
        whenever(groupService.joinGroup(any(JoinGroupRequest::class.java))).thenReturn(JoinGroupResponse(GroupMembershipState.ACTIVE))
        val testObserver = testObject.toggleCircleJoin(circle).test()
        testObserver.awaitTerminalEvent()

        verify(groupService).joinGroup(JoinGroupRequest.Builder().groupId(circle.id).build())
    }

    @Test
    @Throws(Exception::class)
    fun viewModelCallsUnsaveGroup() {
        val circle = DataHelper.getCircle(true)
        whenever(userDao.getCurrentUser()).thenReturn(Flowable.just(getUser()))
        whenever(groupService.leaveGroup(any(LeaveGroupRequest::class.java))).thenReturn(Empty())
        val testObserver = testObject.toggleCircleJoin(circle).test()
        testObserver.awaitTerminalEvent()

        verify(groupService).leaveGroup(LeaveGroupRequest(circle.id))
    }

    @Test
    @Throws(Exception::class)
    fun whenGroupJoinIsRequestedOnServer_viewModelOptimisticallySavesNewCircleAsRequestedToDatabase() {
        val circle = DataHelper.getCircle(false)
        whenever(groupService.joinGroup(any(JoinGroupRequest::class.java))).thenReturn(JoinGroupResponse(GroupMembershipState.ACTIVE))
        val testObserver = testObject.toggleCircleJoin(circle).test()
        testObserver.awaitTerminalEvent()

        // First it's saved to database as state = requested
        val requestedCircle = circle.copy(membershipState = GroupMembershipState.REQUESTED.value, joined = false)
        verify<CircleDao>(circleDao).update(requestedCircle)

        // When we get confirmation from server, save the final state of the group
        val finalCircle = circle.copy(membershipState = GroupMembershipState.ACTIVE.value, joined = true, participantCount = circle.participantCount + 1)
        verify<CircleDao>(circleDao).update(finalCircle)
    }

    @Test
    @Throws(Exception::class)
    fun whenGroupIsUnSavedOnServer_viewModelSavesNewCircleToDatabase() {
        val circle = DataHelper.getCircle(true)
        whenever(userDao.getCurrentUser()).thenReturn(Flowable.just(getUser()))
        val testObserver = testObject.toggleCircleJoin(circle).test()
        testObserver.awaitTerminalEvent()

        val newCircle = circle.copy(joined = false, membershipState = GroupMembershipState.UNJOINED.value, participantCount = circle.participantCount - 1)
        verify<CircleDao>(circleDao).update(newCircle)
    }

//    @Test
//    @Throws(Exception::class)
//    fun whenGroupIsNotSavedOnServer_circleIsOptimisticallySavedAsRequested_thenCircleIsResetInDatabase() {
//        val circle = DataHelper.getCircle(false)
//        whenever(groupService.joinGroup(any(JoinGroupRequest::class.java))).thenThrow(RuntimeException("Network error!"))
//        val testObserver = testObject.toggleCircleJoin(circle).test()
//        testObserver.awaitTerminalEvent()
//
//        // First it's saved to database as state = requested
//        val requestedCircle = circle.copy(membershipState = GroupMembershipState.REQUESTED.value, joined = false)
//        verify<CircleDao>(circleDao).update(requestedCircle)
//
//        // When the server error occurs, reset the group
//        verify<CircleDao>(circleDao).update(circle)
//    }

    @Test
    @Throws(Exception::class)
    fun whenGroupIsNotUnSavedOnServer_circleIsOptimisticallyUnsaved_butThenRevertedInDatabase() {
        val circle = DataHelper.getCircle(true)
        whenever(groupService.leaveGroup(any<LeaveGroupRequest>())).thenThrow(RuntimeException("Network error!"))
        whenever(userDao.getCurrentUser()).thenReturn(Flowable.just(getUser()))

        val testObserver = testObject.toggleCircleJoin(circle).test()
        testObserver.awaitTerminalEvent()

        // Optimistically unsave the circle
        val newCircle = circle.copy(joined = false, membershipState = GroupMembershipState.UNJOINED.value, participantCount = circle.participantCount - 1)
        verify<CircleDao>(circleDao).update(newCircle)

        // When the server error occurs, reset the circle in DB
        verify<CircleDao>(circleDao).update(circle)
    }
}