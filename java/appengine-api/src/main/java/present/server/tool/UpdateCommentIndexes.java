package present.server.tool;

import com.google.appengine.tools.remoteapi.RemoteApiInstaller;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import present.server.model.PresentEntities;
import present.server.model.comment.Comment;
import present.server.model.group.Group;

import static com.googlecode.objectify.ObjectifyService.ofy;

public class UpdateCommentIndexes extends RemoteTool {

  public static void main(String[] args) throws IOException
  {
    RemoteApiInstaller installer = installRemoteAPI(STAGING_SERVER);
    //RemoteApiInstaller installer = installRemoteAPI(DEV_SERVER);

    try (Closeable closeable = ObjectifyService.begin()) {
      PresentEntities.registerAll();

      // Reset the sequence for all comments based on their creationTime timestamps
      List<Group> list = new ArrayList<>();
      list.addAll( Group.query().list() );
      for (Group commentContainer : list) {
        List<Comment> comments = ofy().load().type(Comment.class).ancestor(commentContainer).list();
        if (comments.isEmpty()) { continue; }

        if (comments.size() == commentContainer.totalComments) {
          System.out.println("Count matches.");
          continue;
        }

        System.out.println("Sequencing "+comments.size()+" comments in: "+commentContainer);
        comments.sort(Comparator.comparingLong(o -> o.creationTime));
        int nextSequence = 0;
        for (Comment comment : comments) {
          comment.sequence = nextSequence++;
        }
        ((Group)commentContainer).totalComments = nextSequence;

        // Do the update
        ofy().transact(() -> {
          ofy().save().entity(commentContainer).now();
          ofy().save().entities(comments).now();
        } );
      }
    }

    installer.uninstall();
  }
}
