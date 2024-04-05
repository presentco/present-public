package present.server.environment;

import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
import com.google.apphosting.api.ApiProxy;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Exposes information specific to the current server environment.
 */
public enum Environment {

  TEST {
    @Override public String webUrl() {
      return "http://local.present.co:8080";
    }

    @Override public String apiUrl() {
      return "http://" + apiHost() + "/api";
    }

    @Override public String apiHost() {
      return "localhost:8081";
    }
  },

  DEVELOPMENT {
    @Override public String webUrl() {
      return "http://local.present.co:8080";
    }

    @Override public String apiUrl() {
      return "http://" + apiHost() + "/api";
    }

    @Override public String apiHost() {
      return "localhost:8081";
    }
  },

  STAGING {
    @Override public String webUrl() {
      return "https://staging.present.co";
    }

    @Override public String apiHost() {
      return "api.staging.present.co";
    }
  },

  PRODUCTION {
    @Override public String webUrl() {
      return "https://present.co";
    }

    @Override public String apiHost() {
      return "api.present.co";
    }
  };

  private static final Map<String, Environment> byId = Arrays.stream(values())
      .collect(Collectors.toMap(e -> "present-" + e.name().toLowerCase(), e -> e));

  /** Returns the current environment. */
  public static Environment current() {
    String applicationId = applicationId();
    if (applicationId == null) {
      throw new RuntimeException("Environment not set. See RemoteTool.against().");
    }
    if (applicationId.equals("test")) return TEST;
    Environment deployment = byId.get(applicationId);
    if (deployment == null) {
      throw new RuntimeException("Unrecognized application ID: " + applicationId);
    }
    return deployment;
  }

  /** True if this is the development server. */
  public static boolean isDevelopment() {
    return current() == DEVELOPMENT;
  }

  /** True if this is the production server. */
  public static boolean isProduction() {
    return current() == PRODUCTION;
  }

  /** True if this is the staging server. */
  public static boolean isStaging() {
    return current() == STAGING;
  }

  /** True if this is a test. */
  public static boolean isTest() {
    return applicationId() == null || current() == TEST;
  }

  /**
   * Get the deployment application id, e.g. "present-staging", "present-production",
   * "present-development".
   *
   * Return an application id similar to the one reported by SystemProperty.applicationId, but which
   * can be used from remote code for migration.
   */
  public static String applicationId() {
    // Use ApiProxy instead of SystemProperty.applicationId so this works with RemoteTool.
    String id = ApiProxy.getCurrentEnvironment().getAppId();
    // Work around a bug in App Engine.
    if (id.startsWith("s~")) id = id.substring(2);
    return id;
  }

  /** Returns the URL for our web app. */
  public abstract String webUrl();

  /** Returns the URL for our RPC endpoint. */
  public String apiUrl() {
    return "https://" + current().apiHost() + "/api";
  }

  /**
   * Returns the host for our API server.
   *
   * Set this as the "Host" header on task queue tasks so they work properly in development:
   * https://code.google.com/p/googleappengine/issues/detail?id=10457
   */
  public abstract String apiHost();

  /** Adds an "Authorization" header to the given HTTP connection. */
  public static void authorize(HttpURLConnection c, List<String> scopes) {
    // The access token asserts the identity reported by appIdentity.getServiceAccountName()
    String token = AppIdentityServiceFactory.getAppIdentityService()
        .getAccessToken(scopes)
        .getAccessToken();
    c.addRequestProperty("Authorization", "Bearer " + token);
  }
}
