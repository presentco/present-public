package present.server.tool;

import com.google.common.io.Resources;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import okio.ByteString;
import present.proto.ContentType;
import present.proto.ContentUploadRequest;
import present.server.model.comment.Comment;
import present.server.model.comment.Comments;
import present.server.model.content.Content;
import present.server.model.group.Group;
import present.server.model.group.Groups;
import present.server.model.user.User;
import present.server.model.user.Users;

import static com.googlecode.objectify.ObjectifyService.ofy;

public class CleanUpContent {

  public static void main(String[] args) {
    RemoteTool.against(RemoteTool.STAGING_SERVER, () -> {
      Set<Ref<Content>> bad = new HashSet<>();

      for (Content c : ofy().load().type(Content.class).list()) {
        if (c.type == ContentType.JPEG && c.servingUrl == null) {
          try {
            // getServingUrl() often works only after re-uploading the image to GCS.
            System.out.println("Fixing " + c + "...");
            ByteString bytes = ByteString.of(Resources.toByteArray(new URL(c.url())));
            Content.createAndUpload(
                new ContentUploadRequest(c.uuid, ContentType.JPEG, bytes, null));
            System.out.println("Done!");
          } catch (IOException e) {
            e.printStackTrace();
            bad.add(Ref.create(c));
          }
        }
      }

      if (bad.isEmpty()) return;

      // Fix up reference to missing content.

      boolean readOnly = true;

      for (Group group : Groups.all()) {
        if (bad.contains(group.coverContent)) {
          System.out.println(group);
          if (!readOnly) {
            group.coverContent = null;
            group.save();
          }
        }
      }

      for (User user : Users.all()) {
        if (bad.contains(user.photo)) {
          System.out.println(user);
          if (!readOnly) {
            user.photo = Ref.create(Key.create(Content.class, "00000000-0000-0000-0000-000000000000"));
            user.save();
          }
        }
      }

      for (Comment comment : Comments.all()) {
        if (bad.contains(comment.contentRef)) {
          System.out.println(comment);
          if (!readOnly) {
            comment.contentRef = null;
            comment.save();
          }
        }
      }

      if (!readOnly) ofy().delete().entities(bad);
    });
  }
}
