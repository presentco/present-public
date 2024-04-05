package present.acceptance;

import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.GroupReferralResponse;
import present.proto.GroupService;
import present.server.Protos;

import static org.junit.Assert.*;

public class GroupReferralTest {
    private static final Logger logger = LoggerFactory.getLogger(AutoJoinGroupTest.class);

    private AcceptanceTest tests;

    public GroupReferralTest(AcceptanceTest tests) {
        this.tests = tests;
    }

    public void testGroupReferral() throws IOException {
        GroupService groupService = tests.groupService;

        AcceptanceTest.TestUser testUser = tests.signUp(new AcceptanceTest.TestUser("Jane", "Villanueva"));

        // Check that response from CountGroupReferrals is zero
        int membersReferred = groupService.countGroupReferrals(Protos.EMPTY).membersReferred;
        assertEquals(membersReferred, 0);

        // Check that response from GetGroupReferrals is an empty list
        List<GroupReferralResponse> groupReferralResponses = groupService.getGroupReferrals(Protos.EMPTY).referrals;
        assertTrue(groupReferralResponses.isEmpty());
    }
}
