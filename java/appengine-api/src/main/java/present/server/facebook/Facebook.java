package present.server.facebook;

import com.google.api.client.util.Charsets;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.annotation.Nullable;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.Gender;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Facebook utilities
 *
 * @author Pat Niemeyer (pat@pat.net)
 */
public class Facebook {
  private static final Logger logger = LoggerFactory.getLogger(Facebook.class);
  private static final Gson gson = new Gson();

  // The requested size of the user's profile photo
  private static int PHOTO_SIZE = 1024;
  private static int FRIENDS_LIMIT = 5000;

  private static String profileFields =
      "id,cover,name,first_name,last_name,age_range,link,gender,locale,"
          + "picture.width("
          + PHOTO_SIZE
          + ").height("
          + PHOTO_SIZE
          + "),"
          + "timezone,updated_time,verified";
  private static String addFields = "email,friends.limit(" + FRIENDS_LIMIT + ")";
  private static String fbBaseUrlFormat =
      "https://graph.facebook.com/v2.9/me?fields=%s&access_token=%s";

  /**
   * Gets Facebook data for the given Facebook user token.
   *
   * @throws IOException if an error occurs contacting Facebook
   * @throws FacebookException if the Facebook API returns an error
   */
  public static FacebookUserData getUserData(String userToken) throws FacebookException,
      IOException {
    String url = String.format(fbBaseUrlFormat, profileFields + "," + addFields, userToken);
    logger.debug("URL: {}", url);
    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
    try {
      int code = c.getResponseCode();
      if (code == HttpURLConnection.HTTP_OK) {
        try (Reader in = new InputStreamReader(c.getInputStream(), Charsets.UTF_8)) {
          FacebookUserData facebookData = gson.fromJson(in, FacebookUserData.class);
          logger.info("Retrieved Facebook data for {} ({}).", facebookData.name, facebookData.email);
          return facebookData;
        }
      } else {
        Error error = readErrorFrom(c);
        logger.info("Bad response from Facebook. HTTP error code: {}, Facebook error: {}", code,
            error);
        throw new FacebookException(code, error);
      }
    } finally {
      c.disconnect();
    }
  }

  private static final OkHttpClient httpClient = new OkHttpClient();

  /**
   * Revokes a user's Facebook login permissions.
   */
  public static void revokeLogin(String userId) throws IOException {
    Request request = new Request.Builder()
        .delete()
        .url("https://graph.facebook.com/v3.0/" + userId + "/permissions"
            + "?access_token=xxx")
        .build();
    Response response = httpClient.newCall(request).execute();
    if (!response.isSuccessful()) {
      throw new IOException("Http " + response.code() + " error: " + response.body().toString());
    }
  }

  private static Facebook.Error readErrorFrom(HttpURLConnection c) {
    try {
      try (InputStream in = c.getErrorStream()) {
        return in == null ? null
            : gson.fromJson(new InputStreamReader(in, Charsets.UTF_8), ErrorResponse.class).error;
      }
    } catch (Exception e) {
      logger.warn("Error reading Facebook error.", e);
      return null;
    }
  }

  public static Gender genderOf(@Nullable FacebookUserData user) {
    if (user == null || user.gender == null) {
      return Gender.UNKNOWN;
    }
    String gender = user.gender;
    if (gender.equalsIgnoreCase("male")) {
      return Gender.MAN;
    }
    if (gender.equalsIgnoreCase("female")) {
      return Gender.WOMAN;
    }
    return Gender.OTHER;
  }

  public static class ErrorResponse {
    public Error error;
  }

  public static class Error {
    public String type;
    public int code;
    public int error_subcode;
    public String message;
    public String fbtrace_id;

    @Override public String toString() {
      return toStringHelper(this)
          .add("type", type)
          .add("code", code)
          .add("error_subcode", error_subcode)
          .add("message", message)
          .add("fbtrace_id", fbtrace_id)
          .toString();
    }

    /** Returns true if we were throttled by Facebook. */
    public boolean throttled() {
      return code == 4 // "API Too Many Calls"
          || code == 17 // "API User Too Many Calls"
          || code == 341; // "Application limit reached"
    }

    /** Returns true if this is a temporary error. */
    public boolean temporary() {
      return throttled()
          || code == 1 // "API Unknown"
          || code == 2 // "API Service"
          || code == 368; // "Temporarily blocked for policies violations"
    }

    /** Returns true if we should ask the user to log in again. */
    public boolean requestLogin() {
      return !temporary();
    }
  }

  public static void main(String[] args) throws Exception {
    // Analyze graph requests here: https://developers.facebook.com/tools/explorer/
    // Analyze access tokens here:  https://developers.facebook.com/tools/debug/accesstoken"
    // Note: The token that we use is the user access token scoped to the Present app.
    // You can generate one for testing by using the graph API explorer above and selecting Present
    // as the application before generating the user token.
    String testToken = "xxx";
    //String testToken = "EAAJUZCN95rQ0BAKWvmxNncXx9XPsLb04FvZAVIEErDZAXwqFZBgxbRZA2dtfXBbPEejMbWPCW5mN6d6XVZAQubkst0krqmmRDUiZAgKzlFknMKMBGsVnblNOIchP4lZAVMR2FWHjZAx4kZB1QZB73tAMDGx4PnJkYSdtmpX86rUPasZA14dDs85XmSfUeaBALPI2KdnBn7NceKpHGrTZCpvHCZA89IALIposfuPLNc3j6PtXI8MAZDZD";
    FacebookUserData userData = getUserData(testToken);
    System.out.println("userData = " + userData);
  }
}
