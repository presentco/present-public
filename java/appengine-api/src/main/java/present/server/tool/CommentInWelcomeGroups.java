package present.server.tool;

import present.proto.ContentReferenceRequest;
import present.proto.ContentType;
import present.proto.GroupService;
import present.proto.MuteGroupRequest;
import present.proto.PutCommentRequest;
import present.server.Uuids;
import present.server.model.group.WelcomeGroup;
import present.server.model.user.User;
import present.server.model.user.Users;

import static present.server.tool.RemoteTool.PRODUCTION_SERVER;
import static present.server.tool.RemoteTool.STAGING_SERVER;
import static present.server.tool.RemoteTool.against;

/**
 * Send comment to welcome groups.
 *
 * @author Bob Lee (bob@present.co)
 */
class CommentInWelcomeGroups {

  public static void main(String[] args) {
    against(PRODUCTION_SERVER, () -> {
      User user = Users.bob();
      GroupService gs = user.rpcClient(GroupService.class);
      for (WelcomeGroup group : WelcomeGroup.ALL) {
        String message = "Happy Thanksgiving! 🦃";
        PutCommentRequest commentRequest = new PutCommentRequest.Builder()
            .uuid(Uuids.fromName(message + " -> " + group.uuid))
            .comment(message)
            .groupId(group.uuid)
            .ignoreMuting(true)
            .build();
        gs.putComment(commentRequest);
        System.out.println(message);
        System.out.println(group.city);
      }
    });
  }
}
