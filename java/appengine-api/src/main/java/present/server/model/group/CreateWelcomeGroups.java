package present.server.model.group;

import java.util.Collections;
import present.proto.ContentReferenceRequest;
import present.proto.ContentType;
import present.proto.GroupService;
import present.proto.MuteGroupRequest;
import present.proto.PutCommentRequest;
import present.proto.PutGroupRequest;
import present.server.Uuids;
import present.server.model.Space;
import present.server.model.user.User;
import present.server.model.user.Users;

import static present.server.tool.RemoteTool.PRODUCTION_SERVER;
import static present.server.tool.RemoteTool.STAGING_SERVER;
import static present.server.tool.RemoteTool.against;

/**
 * Creates secondary welcome groups.
 *
 * @author Bob Lee (bob@present.co)
 */
class CreateWelcomeGroups {

  public static void main(String[] args) {
    against(PRODUCTION_SERVER, () -> {
      User user = Users.bob();
      GroupService gs = user.rpcClient(GroupService.class);
      for (WelcomeGroup group : WelcomeGroup.ALL) {
        PutGroupRequest groupRequest = new PutGroupRequest.Builder()
            .uuid(group.uuid)
            .title("Welcome to " + group.city + "!")
            .categories(Collections.singletonList("Communities"))
            .description("Connect with Present members near " + group.city + ".")
            .cover(COVER)
            .location(group.location.toProto())
            .locationName(group.city)
            .createdFrom(present.server.model.util.Coordinates.PRESENT_COMPANY.toProto())
            .spaceId(Space.EVERYONE.id)
            .build();
        PutCommentRequest commentRequest = new PutCommentRequest.Builder()
            .uuid(Uuids.fromName("Welcome comment for " + group.uuid))
            .comment(WELCOME_MESSAGE)
            .groupId(group.uuid)
            .build();
        gs.putGroup(groupRequest);
        gs.putComment(commentRequest);
        gs.muteGroup(new MuteGroupRequest(group.uuid));
        System.out.print(".");
      }
    });
  }

  private static final ContentReferenceRequest COVER = new ContentReferenceRequest(
      "40e9068d-b79a-8391-1acf-31f7f46836f2", ContentType.JPEG);


  private static String WELCOME_MESSAGE = "Hello! 👋 I’m Bob, one of Present's co-founders."
      + " Thank you for joining our community!"
      + " Our mission is to help you find fun things to do with people nearby. "
      + " Enjoy, and please let me know if you have any questions!";
}
