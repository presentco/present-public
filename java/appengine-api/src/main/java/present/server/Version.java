package present.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.Platform;
import present.proto.RequestHeader;
import present.proto.VersionCheckResult;
import present.server.model.user.Client;
import present.wire.rpc.core.ServerException;

import static present.proto.VersionCheckResult.Status.*;

/**
 * Version information related to client applications.
 *
 * @author Pat Niemeyer (pat@pat.net)
 */
public class Version {
  private static final Logger logger = LoggerFactory.getLogger(Version.class);

  private Platform platform;
  private String clientVersion;
  private Integer apiVersion;
  private Client client;

  public Version(RequestHeader requestHeader, Client client) {
    this.platform = requestHeader.platform;
    this.clientVersion = requestHeader.clientVersion;
    this.apiVersion = requestHeader.apiVersion;
    this.client = client;
  }

  public VersionCheckResult versionCheck() {
    switch(platform) {
      case ANDROID:
        return new Android().versionCheck();
      case IOS:
        return new iOS().versionCheck();
      case WEB:
      case TEST:
      default:
        return new Web().versionCheck();
    }
  }

  class Android {
    String latestVersionUrl = "http://present.co"; // TODO: Play store URL
    int minAPIVersion = 1;

    VersionCheckResult versionCheck() {
      if (apiVersion < minAPIVersion) {
        logger.info("Version check: require upgrade for client: "+client);
        return new VersionCheckResult(UpgradeRequired, latestVersionUrl);
      }
      return new VersionCheckResult(Current, latestVersionUrl);
    }
  }

  class iOS {
    String latestVersionUrl = "https://apps-ios.crashlytics.com/projects/58deb92e785c7f7e18bfdf61";

    VersionCheckResult versionCheck() {
      ClientVersion client = parseClientVersion(Version.this.clientVersion);
      if (
          // TODO: Need to implement semantic versioning scheme
          (client.version.equals("0.1") && client.build < 31) ||
          (client.version.equals("0.1.1") && client.build < 1)
        ) {
        logger.info("Version check: require upgrade for client: "+client);
        return new VersionCheckResult(UpgradeRequired, latestVersionUrl);
      }
      return new VersionCheckResult(Current, latestVersionUrl);
    }
  }

  static class ClientVersion {
    public String version;
    public int build;

    public ClientVersion(String version, int build) {
      this.version = version;
      this.build = build;
    }

    @Override public String toString() {
      return "ClientVersion{" + "version=" + version + ", build=" + build + '}';
    }
  }
  private static ClientVersion parseClientVersion(String clientVersion) {
    String[] parts = clientVersion.split("b");
    if(parts.length!=2) {
      throw new ServerException("Invalid client version string: " + clientVersion);
    }
    try {
      return new ClientVersion(parts[0], Integer.parseInt(parts[1]));
    } catch (NumberFormatException e) {
      throw new ServerException("Invalid version or version numbers: " + clientVersion);
    }
  }


  class Web {
    String latestVersionUrl = "http://present.co"; // TODO:

    VersionCheckResult versionCheck() {
      return new VersionCheckResult(Current, latestVersionUrl);
    }
  }

}
