package present.server.tool;

import present.proto.ContentType;
import present.server.model.content.Content;

import static com.googlecode.objectify.ObjectifyService.ofy;

public class SetServingUrls {
  public static void main(String[] args) {
    RemoteTool.against(RemoteTool.PRODUCTION_SERVER, () -> {
      for (Content content : ofy().load().type(Content.class).list()) {
        if (content.type == ContentType.JPEG
            && (content.servingUrl == null || content.servingUrl.startsWith("http:"))) {
          try {
            content.setServingUrl();
          } catch (Exception e) {
            e.printStackTrace();
            continue;
          }
          ofy().save().entity(content);
          System.out.println(content.servingUrl);
        }
      }
    });
  }
}
