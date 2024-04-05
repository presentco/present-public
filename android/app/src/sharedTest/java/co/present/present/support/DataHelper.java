package co.present.present.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import androidx.annotation.NonNull;
import co.present.present.model.Circle;
import co.present.present.model.CurrentUser;
import co.present.present.model.Interest;
import co.present.present.model.Name;
import co.present.present.model.NotificationSettings;
import present.proto.CommentResponse;
import present.proto.ContentResponse;
import present.proto.ContentType;
import present.proto.Coordinates;
import present.proto.FindLiveServerResponse;
import present.proto.GroupMemberPreapproval;
import present.proto.GroupMembershipState;
import present.proto.GroupResponse;
import present.proto.SpaceResponse;
import present.proto.UserResponse;

import static co.present.present.support.DataHelper.TestCircle.CIRCLE_ID;
import static co.present.present.support.DataHelper.TestCircle.COMMENT_COUNT;
import static co.present.present.support.DataHelper.TestCircle.COMMENT_TIME;
import static co.present.present.support.DataHelper.TestCircle.COORDINATES;
import static co.present.present.support.DataHelper.TestCircle.CREATION_TIME;
import static co.present.present.support.DataHelper.TestCircle.DESCRIPTION;
import static co.present.present.support.DataHelper.TestCircle.MEMBER_COUNT;
import static co.present.present.support.DataHelper.TestCircle.NEIGHBORHOOD;
import static co.present.present.support.DataHelper.TestCircle.RADIUS;
import static co.present.present.support.DataHelper.TestCircle.SPACE_ID;
import static co.present.present.support.DataHelper.TestCircle.TITLE;
import static co.present.present.support.DataHelper.TestComment.COMMENT;
import static co.present.present.support.DataHelper.TestComment.COMMENT_ID;
import static co.present.present.support.DataHelper.TestUser.APP_SHARE_URL;
import static co.present.present.support.DataHelper.TestUser.BIO;
import static co.present.present.support.DataHelper.TestUser.FIRST_NAME;
import static co.present.present.support.DataHelper.TestUser.INTERESTS;
import static co.present.present.support.DataHelper.TestUser.LAST_NAME;
import static co.present.present.support.DataHelper.TestUser.LINK;
import static co.present.present.support.DataHelper.TestUser.NAME;
import static co.present.present.support.DataHelper.TestUser.PHOTO_URL;
import static co.present.present.support.DataHelper.TestUser.USER_ID;
import static co.present.present.support.DataHelper.TestUser.USER_LINK;
import static java.util.Collections.emptyList;

public class DataHelper {
    public static final String TEST_ERROR_MESSAGE = "This is an error message";

    private static final String HOST = "http://internet.com";
    private static final int PORT = 1234;

    public static class TestUser {
        static final String USER_ID = UUID.randomUUID().toString();
        public static final String FIRST_NAME = "Corey";
        public static final String LAST_NAME = "Latislaw";
        public static final String NAME = "Corey Latislaw";
        static final String USER_LINK = "https://present.co/userUrl";
        static final String PHOTO_URL = "http://avatar.com/me.jpg";
        static final String BIO = "Bio goes here";
        static final List<Interest> INTERESTS = Arrays.asList(Interest.Work, Interest.Live);
        public static final String TOKEN = "Token";
        public static final String APP_SHARE_URL = "https://present.co/a/appshareurl";
        public static final String LINK = "https://present.co/u/usershareurl";
    }

    public static class TestCircle {
        static final String CIRCLE_ID = UUID.randomUUID().toString();
        public static final String TITLE = "Hello, this is a card";
        static final String NEIGHBORHOOD = "Cole Valley";
        private static final String URL = "http://internet.com";
        static final Coordinates COORDINATES = new Coordinates(108.44, 108.56, 60.0);
        static final double RADIUS = 0.29;
        static final long CREATION_TIME = System.currentTimeMillis();
        static final long COMMENT_TIME = System.currentTimeMillis();
        static final int MEMBER_COUNT = 3534;
        static final int COMMENT_COUNT = 11034;
        static final String DESCRIPTION = "Fabulous exciting circle";
        static final String SPACE_ID = "everyone";
    }

    public static class TestComment {
        static final String COMMENT_ID = UUID.randomUUID().toString();
        public static final String COMMENT = "This is a string";
        static final long CREATION_TIME = System.currentTimeMillis();
    }

    public static CurrentUser getCurrentUser() {
        return new CurrentUser(USER_ID, BIO, new Name(FIRST_NAME, LAST_NAME), PHOTO_URL, INTERESTS,
                false, APP_SHARE_URL, false, false, new NotificationSettings(false, false),
                LINK, null);
    }

    public static ArrayList<Circle> populateList() {
        ArrayList<Circle> list = new ArrayList<>();
        list.add(getCircle(false));
        return list;
    }

    public static GroupResponse getGroupResponse() {
        GroupResponse.Builder builder = getGroupBuilder();
        return builder.build();
    }

    @NonNull
    public static GroupResponse.Builder getGroupBuilder() {
        GroupResponse.Builder builder = new GroupResponse.Builder();
        builder.uuid = CIRCLE_ID;
        builder.location = COORDINATES;
        builder.creationTime = CREATION_TIME;
        builder.commentCount = COMMENT_COUNT;
        builder.totalComments= COMMENT_COUNT;
        builder.lastCommentTime = COMMENT_TIME;
        builder.memberCount = MEMBER_COUNT;
        builder.title = TITLE;
        builder.locationName = NEIGHBORHOOD;
        builder.radius = RADIUS;
        builder.url(TestCircle.URL);
        builder.owner = getUserResponse();
        builder.categories(Collections.emptyList());
        builder.description(DESCRIPTION);
        builder.lastRead(0);
        builder.joinRequests(0);
        builder.muted(false);
        builder.joined(false);
        builder.unread(false);
        builder.space(new SpaceResponse.Builder().name("").id(SPACE_ID).build());
        builder.type(GroupResponse.Type.CIRCLE);
        builder.preapprove(GroupMemberPreapproval.ANYONE);
        builder.membershipState(GroupMembershipState.NONE);
        builder.deleted(false);
        builder.discoverable(true);
        return builder;
    }

    public static CommentResponse.Builder getCommentResponseBuilder() {
        return new CommentResponse.Builder().uuid(COMMENT_ID)
                .groupId(CIRCLE_ID)
                .comment(COMMENT)
                .content(null)
                .creationTime(TestComment.CREATION_TIME)
                .author(getUserResponse())
                .deleted(false);
    }

    public static CommentResponse getCommentResponse() {
        return getCommentResponseBuilder().build();
    }

    private static UserResponse getUserResponse() {
        return new UserResponse.Builder()
                .id(USER_ID)
                .name(NAME)
                .firstName(FIRST_NAME)
                .photo(PHOTO_URL)
                .bio(BIO)
                .member(true)
                .friends(emptyList())
                .interests(emptyList())
                .link(USER_LINK)
                .build();
        //return new UserResponse(USER_ID, NAME, FIRST_NAME, PHOTO_URL, BIO, emptyList(), emptyList());
    }

    private static ContentResponse getContentResponse() {
        return new ContentResponse(USER_ID, ContentType.JPEG, PHOTO_URL, PHOTO_URL);
    }

    public static FindLiveServerResponse getFindLiveServerResponse() {
        return new FindLiveServerResponse(HOST, PORT);
    }

    public static Circle getCircle(boolean hasJoined) {
        return new Circle(getGroupBuilder()
                .membershipState(hasJoined ? GroupMembershipState.ACTIVE : GroupMembershipState.NONE)
                .joined(hasJoined)
                .build());
    }

    public static Circle getPreviouslyJoinedCircle() {
        return new Circle(getGroupBuilder()
                .membershipState(GroupMembershipState.UNJOINED)
                .joined(false)
                .build());
    }

    public static Circle getCircle() {
        return new Circle(getGroupBuilder().build());
    }
}
