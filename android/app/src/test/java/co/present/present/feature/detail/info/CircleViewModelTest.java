package co.present.present.feature.detail.info;

import android.app.Application;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import co.present.present.config.FeatureDataProvider;
import co.present.present.db.CircleDao;
import co.present.present.db.SpacesDao;
import co.present.present.feature.common.GetMembers;
import co.present.present.feature.common.GetCircle;
import co.present.present.feature.common.viewmodel.GetCurrentUser;
import co.present.present.feature.detail.GetMemberRequests;
import co.present.present.location.LocationDataProvider;
import present.proto.GroupService;


public class CircleViewModelTest {
    private CircleViewModel testObject;

    @Mock private GroupService groupService;
    @Mock private CircleDao circleDao;
    @Mock private SpacesDao spacesDao;

    @Mock private JoinCircle joinCircle;
    @Mock private GetCurrentUser getCurrentUser;
    @Mock private LocationDataProvider locationDataProvider;
    @Mock private Application application;
    @Mock private GetCircle getCircle;
    @Mock private GetMembers getMembers;
    @Mock private FeatureDataProvider featureDataProvider;
    @Mock private GetMemberRequests getMemberRequests;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        testObject = new CircleViewModel(groupService, circleDao, spacesDao, locationDataProvider,
                featureDataProvider, joinCircle, application, getCurrentUser, getCircle, getMembers,
                getMemberRequests);
    }

}