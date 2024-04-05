package cast.placeholder;

import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.gson.Gson;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * @author Bob Lee (bob@present.co)
 */
@Path(CastToSlack.PATH)
public class CastToSlack {

  static final String PATH = "/castToSlack";
  private static final String ID = "id";

  private static final Logger logger = LoggerFactory.getLogger(CastToSlack.class);
  private static final Gson gson = new Gson();

  public static TaskOptions optionsFor(Cast cast) {
    return TaskOptions.Builder.withUrl("/rest" + PATH).param(ID, cast.id);
  }

  @POST
  public Response castToSlack(@FormParam(ID) String id) {
    // TODO: Prevent public access.
    try {
      Cast cast = ofy().load().type(Cast.class).id(id).now();
      URL url = new URL(
          "https://hooks.slack.com/services/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");
      HttpURLConnection c = (HttpURLConnection) url.openConnection();
      c.setRequestMethod("POST");
      c.setDoOutput(true);
      byte[] payload = gson.toJson(new Payload(cast)).getBytes("UTF-8");
      c.setRequestProperty("Content-Type", "application/json");
      OutputStream out = c.getOutputStream();
      out.write(payload);
      out.close();
      int code = c.getResponseCode();
      if (code != 200) {
        logger.warn("Error posting to Slack. Code: {} Message: {}", code, c.getResponseMessage());
      }
      c.disconnect();
    } catch (Exception e) {
      logger.error("Error posting to Slack.", e);
    }
    return Response.ok().build();
  }

  private static class Payload {
    public final String username = "Cast Server";
    public final String text;
    public final Object[] attachments = new Object[2];

    public Payload(Cast cast) {
      Client creator = cast.creator.get();
      this.text = String.format("Cast <%s|%s> by <%s|%s>", cast.consoleUrl(),
          cast.id, creator.consoleUrl(), creator.internalName());
      attachments[0] = new ImageAttachment("Image", cast.imageUrl());
      attachments[1] = new ImageAttachment("Map", cast.mapImageUrl(600, 600));
    }
  }

  private static class ImageAttachment {
    public final String fallback;
    public final String image_url;

    public ImageAttachment(String fallback, String image_url) {
      this.fallback = fallback;
      this.image_url = image_url;
    }
  }
}
