package present.server.tool.migrations;

import com.google.appengine.tools.remoteapi.RemoteApiInstaller;
import java.io.IOException;
import present.server.tool.RemoteTool;

public class MigrateCommentContent_05012017 extends RemoteTool {

  /**
   * Ran in staging: 05/01/2017
   * Ran in production : 05/01/2017
   */
  public static void main(String[] args) throws IOException
  {
    RemoteApiInstaller installer = installRemoteAPI(DEV_SERVER);
    //RemoteApiInstaller installer = installRemoteAPI(STAGING_SERVER);
    //RemoteApiInstaller installer = installRemoteAPI(PRODUCTION_SERVER);

    /*
    try (Closeable closeable = ObjectifyService.begin()) {
      PresentEntities.registerAll();

      List<CommentContainer> list = new ArrayList<>();
      list.addAll( Group.query().list() );
      list.addAll( Chat.query().list() );
      for (CommentContainer commentContainer : list) {
        List<Comment> comments = ofy().load().type(Comment.class).ancestor(commentContainer).list();
        if (comments.isEmpty()) { continue; }

        System.out.println("Updating "+comments.size()+" comments.");
        for (Comment comment : comments) {
          if (comment.content != null) {
            Content content = new Content(comment.content.uuid, comment.content.contentType);
            content.save().now();
            comment.contentRef = content.getRef();
            comment.save().now();
          }
        }
      }
    }*/
    installer.uninstall();
  }
}
