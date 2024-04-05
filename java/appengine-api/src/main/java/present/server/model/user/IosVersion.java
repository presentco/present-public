package present.server.model.user;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A version of the iOS client.
 *
 * @author Bob Lee (bob@present.co)
 */
public class IosVersion implements Comparable<IosVersion> {

  private static final Pattern pattern = Pattern.compile(
      "(\\d+)\\.(\\d+)(?:\\.(\\d+))?b(\\d+)");

  public static final IosVersion V4_1_B2 = IosVersion.parse("4.1b2");
  public static final IosVersion V4_3_B6 = IosVersion.parse("4.3b6");

  public final int major;
  public final int minor;

  /** -1 if missing */
  public final int patch;

  public final int build;

  private IosVersion(int major, int minor, int patch, int build) {
    this.major = major;
    this.minor = minor;
    this.patch = patch;
    this.build = build;
  }

  @Override public int compareTo(IosVersion o) {
    if (major < o.major) return -1; if (major > o.major) return 1;
    if (minor < o.minor) return -1; if (minor > o.minor) return 1;
    if (patch < o.patch) return -1; if (patch > o.patch) return 1;
    if (build < o.build) return -1; if (build > o.build) return 1;
    return 0;
  }

  public String toString() {
    String s = major + "." + minor;
    if (patch != -1) s += "." + patch;
    return s + "b" + build;
  }

  /**
   * Format: MAJOR.MINOR[.PATCH]bBUILD
   *
   * @throws IllegalArgumentException if format unrecognized
   */
  public static IosVersion parse(String version) {
    Matcher matcher = pattern.matcher(version);
    if (!matcher.matches()) throw new IllegalArgumentException("Invalid version: " + version);
    String patch = matcher.group(3);
    return new IosVersion(
      Integer.parseInt(matcher.group(1)),
      Integer.parseInt(matcher.group(2)),
      patch == null ? -1 : Integer.parseInt(patch),
      Integer.parseInt(matcher.group(4))
    );
  }
}
