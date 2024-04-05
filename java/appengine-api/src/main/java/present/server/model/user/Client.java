package present.server.model.user;

import com.google.common.geometry.S2CellId;
import com.google.common.geometry.S2LatLng;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.condition.IfNotNull;
import java.lang.reflect.Field;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.Platform;
import present.proto.RequestHeader;
import present.proto.UnreadCounts;
import present.proto.UnreadState;
import present.server.Cities;
import present.server.KeysOnly;
import present.server.Uuids;
import present.server.environment.Environment;
import present.server.model.geocoding.GeocodeClient;
import present.wire.rpc.client.RpcClient;
import present.wire.rpc.core.RpcFilter;
import present.wire.rpc.core.RpcInvocation;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static present.server.model.util.Coordinates.fromProto;

/**
 * Represents an installation of the client application. A client is always associated with
 * one user. A user can have many clients.
 *
 * @author Bob Lee (bob@present.co)
 */
@Entity @Cache public class Client {

  private static final Logger logger = LoggerFactory.getLogger(Client.class);

  @Id public String uuid;

  public long createdTime = System.currentTimeMillis();

  /** User associated with this client. */
  @Load(unless = KeysOnly.class) @Index public Ref<User> user;

  /** Device token for push notifications. */
  @Index(IfNotNull.class) public String deviceToken;

  /** The update time for the device token. */
  public long deviceTokenUpdateTime = 0;

  /** See {@link present.proto.Platform}. */
  @Index public int platform;
  public String clientVersion;
  public String platformVersion;

  /** S2 leaf cell ID for last known location. See S2CellId. */
  @Index(IfNotNull.class) public Long location;

  /** Time when location was captured. Only changes when the user moves. */
  public long locationTimestamp;

  private static final long MIN_DISTANCE_CHANGE = 100; // m

  /** Sets location. Returns true if the location changed. */
  public boolean setLocation(present.server.model.util.Coordinates newLocation) {
    if (newLocation == null) return false;

    // TODO: If we have location before associating a user with the client, we aren't currently
    // capturing it until the subsequent request, though it is on the Client.

    // Capture signup location if we haven't already.
    if (this.user != null) {
      // Load cached User.
      User user = this.user.get();
      if (user != null && user.zip == null && user.signupLocation == null) {
        ofy().transact(() -> {
          User latest = user.reload();
          if (latest.signupLocation == null) {
            logger.info("Setting signup location.");
            latest.setSignupLocation(newLocation);
            latest.signupLocationSource = User.SignupLocationSource.GPS;
            latest.save();
            GeocodeClient.queueGeotagRequest(user);
          }
        });
      }
    }

    // Update Client if necessary.
    S2LatLng newLatLng = newLocation.toS2LatLng();
    if (this.location != null) {
      S2LatLng oldLatLng = new S2CellId(this.location).toLatLng();
      if (newLatLng.getEarthDistance(oldLatLng) < MIN_DISTANCE_CHANGE) return false;
    }
    logger.info("Setting location.");
    this.location = S2CellId.fromLatLng(newLatLng).id();
    this.locationTimestamp = System.currentTimeMillis();
    return true;
  }

  public S2LatLng location() {
    if (this.location == null) return null;
    return new S2CellId(this.location).toLatLng();
  }

  public long locationTimestamp() {
    return locationTimestamp;
  }

  /** Captures fields from the header. */
  public void updateWith(RequestHeader header) {
    boolean dirty = false;

    // Populate missing platforms (for legacy clients).
    if (platform == 0) {
      logger.info("Setting platform.");
      this.platform = header.platform.getValue();
      dirty = true;
    }
    if (header.platformVersion != null
        && !Objects.equals(this.platformVersion, header.platformVersion)) {
      logger.info("Setting platform version.");
      this.platformVersion = header.platformVersion;
      dirty = true;
    }
    if (header.clientVersion != null
        && !Objects.equals(this.clientVersion, header.clientVersion)) {
      logger.info("Setting client version.");
      this.clientVersion = header.clientVersion;
      dirty = true;
    }
    if (header.deviceName != null
        && !Objects.equals(this.deviceName, header.deviceName)) {
      logger.info("Setting device name.");
      this.deviceName = header.deviceName;
      dirty = true;
    }

    // Check API version.
    if (!header.apiVersion.equals(this.apiVersion)) {
      logger.info("Setting API version.");
      this.apiVersion = header.apiVersion;
      dirty = true;
    }

    // Update last known location.
    if (setLocation(fromProto(header.location))) {
      dirty = true;
    }

    if (dirty) {
      logger.info("Saving updated Client...");
      save().now();
    }
  }

  public boolean isIos() {
    return platform() == Platform.IOS;
  }

  public boolean isAndroid() {
    return platform() == Platform.ANDROID;
  }

  public boolean isWeb() {
    return platform() == Platform.WEB;
  }

  /** Returns the client's platform. Can return {@code null} for legacy clients. */
  public Platform platform() {
    return Platform.fromValue(this.platform);
  }

  /** API version requested by the client. */
  public Integer apiVersion;

  /**
   * Returns the API version requested by the client. Currently returns 1 or 2:
   *
   * 1: Badge count = # of unread individual chat messages
   * 2: Badge count = # of unread groups and chats
   */
  public int apiVersion() {
    return apiVersion == null ? 1 : apiVersion;
  }

  /**
   * Returns the Android ordinal version or -1 if an ordinal version isn't known or the platform
   * isn't Android.
   */
  public int androidVersion() {
    if (clientVersion == null || clientVersion.indexOf('.') > -1) {
      return -1;
    }
    try {
      return Integer.parseInt(clientVersion);
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  public String deviceName;

  public static Client get(String uuid) {
    return get(Key.create(Client.class, uuid));
  }
  public static Client get(Key<Client> key) {
    return ofy().load().key(key).now();
  }

  /** Can be set to something human readable to aid internal tracking. */
  private String internalName;

  public String internalName() {
    if (internalName != null) return internalName;
    if (deviceName != null) return deviceName;
    return "Client " + uuid;
  }

  /** Pulls the badge count from the given UnreadStates. Returns -1 if not available. */
  public int badge(UnreadState unreadState) {
    if (unreadState != null && iosV2()) {
      UnreadCounts unreadCounts = UnreadStates.toCounts(user(), unreadState);
      return badge(unreadCounts);
    }
    return -1;
  }

  /** Pulls the badge count from the given UnreadCounts. Returns -1 if not available. */
  public int badge(UnreadCounts unreadCounts) {
    if (iosV2() && unreadCounts != null) {
      // TODO: Only return friend count for newer clients.
      return unreadCounts.total;
    }
    return -1;
  }

  private static final IosVersion INBOX_REMOVED = IosVersion.parse("3.5b0");

  /** Returns true if this is an older iOS client with inbox. */
  public boolean hasInbox() {
    return platform() == Platform.IOS && iosVersion().compareTo(INBOX_REMOVED) < 0;
  }

  /** Is this an iOS client supporting API v2 or later? */
  public boolean iosV2() {
    return platform() == Platform.IOS && apiVersion() >= 2;
  }

  /** Whether or not notifications are enabled on this client. {@code null} == unknown */
  public Boolean notificationsEnabled;

  @Override public String toString() {
    String userString = user == null ? null : String.valueOf(user.get());
    return "{" +
        "uuid='" + uuid + '\'' +
        ", user=" + userString +
        ", deviceToken='" + deviceToken + '\'' +
        ", deviceTokenUpdateTime=" + deviceTokenUpdateTime +
        ", platform=" + platform +
        ", clientVersion=" + clientVersion +
        ", platformVersion=" + platformVersion +
        ", apiVersion=" + apiVersion +
        ", deviceName='" + deviceName + '\'' +
        ", internalName='" + internalName + '\'' +
        ", notificationsEnabled='" + notificationsEnabled + '\'' +
        '}';
  }

  public Result<Key<Client>> save() {
    return ofy().save().entity(this);
  }

  /**
   * Simulates RPC from the given client.
   */
  public <T> T rpcSimulator(Class<T> service) {
    return RpcClient.create(Environment.current().apiUrl(), RequestHeader.class, service, new RpcFilter() {
      @Override public Object filter(RpcInvocation invocation) throws Exception {
        // TODO: location
        RequestHeader header = new RequestHeader.Builder()
            .clientUuid(Client.this.uuid)
            .requestUuid(Uuids.newUuid())
            .authorizationKey("not used")
            .platform(platform())
            .apiVersion(apiVersion == null ? 0 : apiVersion)
            .clientVersion(clientVersion)
            .platformVersion(platformVersion)
            .location(Cities.SAN_FRANCISCO.location)
            .build();
        invocation.setHeader(header);
        return invocation.proceed();
      }
    });
  }

  public boolean supportsCircleCreateUrl() {
    return supports(Feature.CIRCLE_CREATE_URL);
  }

  public IosVersion iosVersion() {
    if (platform() != Platform.IOS) return null;
    return IosVersion.parse(clientVersion);
  }

  /** Returns true if this client supports the given feature. */
  public boolean supports(Feature feature) {
    Platform platform = platform();

    if (platform == Platform.IOS) {
      return iosVersion().compareTo(feature.iosVersion) >= 0;
    }

    if (platform == Platform.ANDROID) {
      int version = androidVersion();
      return version >= feature.androidVersion;
    }

    return true;
  }

  public boolean isMobile() {
    return isIos() || isAndroid();
  }

  // Fields that may be used in filter queries as strings
  public static class Fields {

    private Fields() {}

    public static Field user = get("user");
    public static Field deviceToken = get("deviceToken");
    public static Field platform = get("platform");

    private static Field get(String fieldName) {
      try {
        return Client.class.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public String platformToString() {
    String s = this.platform().name();
    if (this.clientVersion != null) s = s + " " + this.clientVersion;
    return s;
  }

  public User user() {
    return this.user == null ? null : this.user.get();
  }
}
