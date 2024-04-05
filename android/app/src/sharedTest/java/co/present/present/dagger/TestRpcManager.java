package co.present.present.dagger;

import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import co.present.present.service.RpcManager;
import present.proto.GroupService;
import present.proto.JoinedGroupsResponse;
import present.proto.NearbyGroupsResponse;
import present.proto.UserService;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * RpcManager that mocks services.
 */
public class TestRpcManager extends RpcManager {

    private final GroupService groupService;
    private final UserService userService;

    public TestRpcManager(UUID uuid) {
        super(uuid);
        groupService = Mockito.mock(GroupService.class);
        userService = Mockito.mock(UserService.class);
    }

    @Override
    public <T> T getService(Class<T> serviceType) {
        if (serviceType == GroupService.class) {
            return (T) groupService;
        } else if (serviceType == UserService.class) {
            return (T) userService;
        }
        throw new UnsupportedOperationException(
                "Not supporting class: " + serviceType.getName());
    }

    public void setJoinedGroups(JoinedGroupsResponse savedGroupsResponse) {
        try {
            when(groupService.getJoinedGroups(any()))
                    .thenReturn(savedGroupsResponse);
        } catch (IOException e) {
            // no-op
        }
    }

    public void setNearbyGroupsResponse(NearbyGroupsResponse groupsResponse) {
        try {
            when(groupService.getNearbyGroups(any()))
                    .thenReturn(groupsResponse);
        } catch (IOException e) {
            // no-op
        }
    }

    public void setDefaultResponse() {
        setJoinedGroups(new JoinedGroupsResponse(new ArrayList<>(), null));
        setNearbyGroupsResponse(new NearbyGroupsResponse(new ArrayList<>(), null));
    }

    public void reset() {
        Mockito.reset(groupService);
    }

    public GroupService getGroupService() {
        return groupService;
    }
}
