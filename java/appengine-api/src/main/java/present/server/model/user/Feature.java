package present.server.model.user;

/**
 * An optional app feature.
 *
 * @author Bob Lee (bob@present.co)
 */
public enum Feature {

  /** Used to suppress client update requests. */
  LATEST("4.5b0", 156),

  CIRCLE_CREATE_URL("0.0b0", 79),
  CHANGE_LOCATION_URL("3.6b0", 130),
  PRIVATE_GROUPS("4.0b0", 141),
  PHONE_SIGNUP("4.3b0", 150),

  /** /app/addFriends, /app/login, and /app/linkFacebook */
  EXPLORE_URLS("4.5b0", 156);

  final IosVersion iosVersion;
  final int androidVersion;

  Feature(String iosVersion, int androidVersion) {
    this.iosVersion = IosVersion.parse(iosVersion);
    this.androidVersion = androidVersion;
  }
}
