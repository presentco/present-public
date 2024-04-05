package present.server;

import java.net.URI;
import java.net.URISyntaxException;
import org.hashids.Hashids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.AppResponse;
import present.proto.ResolveUrlResponse;
import present.proto.UserResponse;
import present.server.environment.Environment;
import present.server.model.activity.GroupReferrals;
import present.server.model.group.Group;
import present.server.model.group.Groups;
import present.server.model.user.User;
import present.server.model.user.Users;
import present.wire.rpc.core.ClientException;

/**
 * Generates short links of the form.
 *
 * @author Bob Lee (bob@present.co)
 */
public class ShortLinks {

  private static final Logger logger = LoggerFactory.getLogger(ShortLinks.class);

  private static final String APP_PATH = "/a/";
  private static final String USER_PATH = "/u/";
  private static final String GROUP_PATH = "/g/";

  private static final Hashids hashids = new Hashids("zV3uG$5_Z`hU&>(F", 8);

  public String to(User user) {
    return Environment.current().webUrl() + USER_PATH + hashWithReferrer(user.shortId);
  }

  public String to(Group group) {
    return Environment.current().webUrl() + GROUP_PATH + hashWithReferrer(group.shortId);
  }

  private String hashWithReferrer(long entityId) {
    User user = Users.current(false);
    return user == null ? hashids.encode(entityId)
        : hashids.encode(entityId, user.shortId);
  }

  public static String toApp() {
    String url = Environment.current().webUrl();
    User user = Users.current(false);
    if (user != null) url += APP_PATH + hashids.encode(user.shortId);
    return url;
  }

  public static ResolveUrlResponse resolve(String url) {
    try {
      return resolve(new URI(url));
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public static ResolveUrlResponse resolve(URI url) {
    String path = url.getRawPath();
    String prefix = path.substring(0, 3);
    String hash = path.substring(3);
    // Note: Referrer, if present, is always the last value.
    long[] ids = hashids.decode(hash);
    switch (prefix) {
      case APP_PATH: return appResponse(ids);
      case USER_PATH: return userResponse(ids);
      case GROUP_PATH: return groupResponse(ids);
      default:
        throw new ClientException("Invalid URL: " + url);
    }
  }

  public static ResolveUrlResponse appResponse() {
    return appResponse(new long[] {});
  }

  private static ResolveUrlResponse appResponse(long[] ids) {
    ResolveUrlResponse.Builder builder = new ResolveUrlResponse.Builder().type(
        ResolveUrlResponse.Type.APP);
    if (ids.length == 1) {
      User referrer = findReferrer(ids[0]);
      if (referrer != null) builder.referrer(referrer.toResponse());
    }
    return builder.app(new AppResponse()).build();
  }

  private static ResolveUrlResponse userResponse(long[] ids) {
    ResolveUrlResponse.Builder builder = new ResolveUrlResponse.Builder().type(
        ResolveUrlResponse.Type.USER);
    if (ids.length == 2) {
      User referrer = findReferrer(ids[1]);
      if (referrer != null) builder.referrer(referrer.toResponse());
    }
    User user = Users.query().filter("shortId", ids[0]).first().now();
    if (user == null) throw new ClientException("Not found.");
    UserResponse userResponse = user.toResponse();
    return builder.user(userResponse).build();
  }

  private static ResolveUrlResponse groupResponse(long[] ids) {
    ResolveUrlResponse.Builder builder = new ResolveUrlResponse.Builder().type(
        ResolveUrlResponse.Type.GROUP);
    User currentUser = Users.current(false);
    Group group = Groups.query().filter("shortId", ids[0]).first().now();
    // Note: We allow resolution from the mobile landing page, which has no logged in user.
    if (group == null) throw new ClientException("Not found.");
    if (ids.length == 2) {
      User referrer = findReferrer(ids[1]);
      if (referrer != null) {
        // We have a referrer!
        builder.referrer(referrer.toResponse());
        if (currentUser != null) {
          if (!referrer.equals(currentUser) && !currentUser.equals(group.owner.get())) {
            GroupReferrals.getOrCreate(referrer, currentUser, group);
          }
        }
      }
    }
    return builder.group(group.toResponseFor(currentUser)).build();
  }

  private static User findReferrer(long referrerId) {
    User referrer = Users.query().filter("shortId", referrerId).first().now();
    // This should only happen if we send a request to the wrong environment or we delete a user.
    if (referrer == null) logger.warn("User #{} not found.", referrerId);
    return referrer;
  }
}
