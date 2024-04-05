package co.present.present.feature.discovery.nearby

import android.content.Context
import android.location.Location
import co.present.present.PresentApplication
import co.present.present.config.FeatureDataProvider
import co.present.present.db.CircleDao
import co.present.present.extensions.Optional
import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.feature.detail.info.JoinCircle
import co.present.present.feature.discovery.CirclesViewModel
import co.present.present.feature.discovery.RefreshCircles
import co.present.present.location.LocationDataProvider
import co.present.present.model.CurrentUser
import co.present.present.support.DataHelper
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Flowable
import io.reactivex.Single
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import present.proto.*


class CirclesViewModelTest {

    private lateinit var testObject: CirclesViewModel

    @Mock lateinit var groupService: GroupService
    @Mock lateinit var circleDao: CircleDao
    @Mock lateinit var locationDataProvider: LocationDataProvider
    @Mock lateinit var featureDataProvider: FeatureDataProvider
    @Mock lateinit var application: PresentApplication
    @Mock lateinit var context: Context
    @Mock lateinit var location: Location
    @Mock lateinit var currentUser: CurrentUser
    @Mock lateinit var joinCircle: JoinCircle
    @Mock lateinit var refreshCircles: RefreshCircles
    lateinit var getCurrentUser: GetCurrentUser
    val circle = DataHelper.getCircle(true)


    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        whenever(application.applicationContext) doReturn (context)
        whenever(locationDataProvider.getLocation(context)) doReturn (Single.just(location))
        getCurrentUser = object: GetCurrentUser {
            override val currentUserOptional: Flowable<Optional<CurrentUser>>
                get() = Flowable.just(Optional(this@CirclesViewModelTest.currentUser))
            override val currentUser: Flowable<CurrentUser> get() = Flowable.just(this@CirclesViewModelTest.currentUser)
        }
        testObject = CirclesViewModel(locationDataProvider, featureDataProvider, joinCircle, getCurrentUser, refreshCircles, application)
    }

    @Test
    @Ignore
    fun getFromNetworkAndSaveDropsTableAndSavesInOneDatabaseTransaction() {
        whenever(circleDao.getNearbyCircles()).thenReturn(Flowable.just(listOf(circle, circle)))
        whenever(circleDao.getJoinedCircles()).thenReturn(Flowable.just(listOf(circle, circle)))
        val groupResponse = DataHelper.getGroupResponse()
        val nearbyGroupsResponse = NearbyGroupsResponse(
                listOf(groupResponse, groupResponse, groupResponse),
                MutedGroupsResponse(listOf()))
        whenever(groupService.getNearbyGroups(any(NearbyGroupsRequest::class.java))).thenReturn(nearbyGroupsResponse)

        val joinedGroupsResponse = JoinedGroupsResponse(listOf(groupResponse, groupResponse, groupResponse), MutedGroupsResponse(listOf()))
        whenever(groupService.getJoinedGroups(JoinedGroupsRequest(null))).thenReturn(joinedGroupsResponse)

        val testObserver = testObject.refreshCircles().test()
        testObserver.awaitTerminalEvent()
        verify(circleDao, times(1)).dropTableAndInsertAll(ArgumentMatchers.anyList())
    }

}