package present.server.facebook;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.Gender;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * User data from the Facebook API.
 */
@Entity @Cache public class FacebookUserData {

  private static final Logger logger = LoggerFactory.getLogger(FacebookUserData.class);

  public static final int ONE_HOUR = 60 * 60 * 1000;


  @Id public String id;

  public long timestamp = System.currentTimeMillis();

  public boolean needsUpdate() {
    return System.currentTimeMillis() - timestamp > ONE_HOUR;
  }

  public String name;
  public String first_name;
  public String last_name;

  public static class AgeRange {
    public int min;

    @Override public String toString() {
      return toStringHelper(this).add("min", min).toString();
    }
  }

  public AgeRange age_range;

  public String link;
  public String gender;

  public Gender gender() {
    return Facebook.genderOf(this);
  }

  public int friendCount() {
    if (friends == null || friends.summary == null || friends.summary.total_count == null) {
      return -1;
    }

    try {
      return Integer.parseInt(friends.summary.total_count);
    } catch (Exception e) {
      logger.warn("Error parsing friend count.", e);
      return -1;
    }
  }

  public String locale;

  public static class Picture {
    public static class data {
      public boolean is_silhouette;
      public String url;

      @Override public String toString() {
        return toStringHelper(this).add("is_silhouette", is_silhouette)
            .add("url", url)
            .toString();
      }
    }

    public Picture.data data;

    @Override public String toString() {
      return toStringHelper(this).add("data", data).toString();
    }
  }

  public String profilePhotoUrl() {
    if (picture == null || picture.data == null || picture.data.url == null) return null;
    if (picture.data.is_silhouette) {
      logger.info("Ignoring silhouette.");
      return null;
    }
    return picture.data.url;
  }

  public Picture picture;

  public String timezone;
  public Date updated_time;
  public boolean verified;
  public String email;

  public static class Friends {

    public static class Friend {
      public String first_name;
      public String last_name;
      public String name;
      public String id;

      @Override public String toString() {
        return toStringHelper(this).add("first_name", first_name)
            .add("last_name", last_name)
            .add("name", name)
            .add("id", id)
            .toString();
      }
    }

    public Friends.Friend[] data;

    public static class Summary {
      public String total_count;

      @Override public String toString() {
        return toStringHelper(this).add("total_count", total_count).toString();
      }
    }

    public Friends.Summary summary;

    @Override public String toString() {
      return toStringHelper(this).add("data", data).add("summary", summary).toString();
    }
  }

  public Friends friends;

  public boolean hasFriends() {
    return friends != null && friends.data != null && friends.data.length > 0;
  }

  @Override public String toString() {
    return toStringHelper(this).add("id", id)
        .add("name", name)
        .add("first_name", first_name)
        .add("last_name", last_name)
        .add("age_range", age_range)
        .add("link", link)
        .add("gender", gender)
        .add("locale", locale)
        .add("picture", picture)
        .add("timezone", timezone)
        .add("updated_time", updated_time)
        .add("verified", verified)
        .add("email", email)
        .add("friends", friends)
        .toString();
  }

  /** Looks up the data for the given facebook ID. */
  public static FacebookUserData get(String facebookId) {
    if (facebookId == null) return null;
    return ofy().load().type(FacebookUserData.class).id(facebookId).now();
  }
}
