package present.server.slack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.wire.WireTypeAdapterFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.Empty;
import present.proto.SlackPostRequest;
import present.proto.SlackService;
import present.server.Internal;
import present.server.Protos;
import present.server.RequestHeaders;
import present.server.environment.Environment;

public class SlackServiceImpl implements SlackService {

  private static final Logger logger = LoggerFactory.getLogger(SlackServiceImpl.class);

  private static Gson gson = new GsonBuilder().registerTypeAdapterFactory(
      new WireTypeAdapterFactory()).create();

  private static String TEST_CHANNEL = "#slack-tests";

  // The integration was created for the #monitoring channel but can be overridden with the "channel" field. e.g.
  // curl -X POST --data-urlencode 'payload={"channel": "#general", "username": "webhookbot",
  //   "text": "This is posted to #general and comes from a bot named webhookbot.", "icon_emoji": ":ghost:"}'
  //    https://hooks.slack.com/services/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
  private static final String URL = "https://hooks.slack.com/services/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";

  @Override @Internal public Empty post(SlackPostRequest request) throws IOException {
    SlackPostRequest.Builder builder = request.newBuilder();
    if (request.username == null) builder.username(SlackPostRequest.DEFAULT_USERNAME);
    if (request.emoji == null) builder.emoji(SlackPostRequest.DEFAULT_EMOJI);
    request = builder.build();

    // TODO: Disallow public access.
    if (RequestHeaders.isTest() || !Environment.isProduction()) {
      request = request.newBuilder().channel(TEST_CHANNEL).build();
    }
    byte[] bytes;
    try {
      bytes = gson.toJson(request).getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    try {
      HttpURLConnection c = (HttpURLConnection)new URL(URL).openConnection();
      c.setRequestMethod("POST");
      c.setDoOutput(true);
      c.setRequestProperty("Content-Type", "application/json");
      OutputStream out = c.getOutputStream();
      out.write(bytes);
      out.close();
      int code = c.getResponseCode();
      if (code != 200) {
        logger.warn("Error posting to Slack. Code: {} Message: {}", code, c.getResponseMessage());
      }
      c.disconnect();
    } catch (Exception e) {
      logger.error("Error posting to Slack.", e);
    }
    return Protos.EMPTY;
  }
}
