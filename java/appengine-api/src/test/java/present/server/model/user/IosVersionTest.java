package present.server.model.user;

import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IosVersionTest {

  @Test public void testIosVersions() {
    for (String version : VERSIONS) {
      assertEquals(version, IosVersion.parse(version).toString());
    }

    assertEquals(-1, compare("1.3.1b1", "3.0b4"));
    assertEquals(0, compare("1.3.1b1", "1.3.1b1"));
    assertEquals(1, compare("3.0b5", "3.0b4"));
    assertEquals(1, compare("3.1b4", "3.0b4"));
    assertEquals(1, compare("3.1b4", "3.0b4"));
    assertEquals(1, compare("4.0b4", "3.0b4"));
    assertEquals(1, compare("4.0b4", "3.0.1b4"));
  }

  private int compare(String a, String b) {
    return IosVersion.parse(a).compareTo(IosVersion.parse(b));
  }

  public static void main(String[] args) {
    Arrays.stream(VERSIONS).map(IosVersion::parse).sorted().forEach(System.out::println);
  }

  private static final String[] VERSIONS = {
      "1.3.1b1",
      "3.0b4",
      "2.4b3",
      "3.1b14",
      "3.2b2",
      "3.5b4",
      "2.6b6",
      "1.2b9",
      "0.1.11b3",
      "0.1.13b10",
      "2.2b5",
      "0.1.1b5",
      "2.0b1",
      "2.1b4",
      "1.0b9",
      "2.3b3",
      "1.1b9",
      "3.3b3",
      "3.1b10",
      "0.1.12b4",
      "0.1.8b3",
      "1.3b2",
      "1.0b3",
      "3.5b2",
      "3.5b5",
      "3.3b1",
      "1.0b2",
      "3.1b8",
      "0.1.13b4",
      "0.1.13b6",
      "3.3b2",
      "2.2b3",
      "2.5b6",
      "3.1b15",
      "1.4b9",
      "0.1.10b8",
      "3.0b1",
      "2.6b1",
      "0.1.13b5",
      "2.3b2",
      "3.5b3",
      "2.5b5",
      "0.1.9b2",
      "0.1.4b7",
      "3.5b8",
      "0.1.6b3",
      "1.1b3",
      "2.5b4",
      "3.5b1",
      "3.1b11",
      "1.4b8",
      "0.1.13b2",
      "1.0b8",
      "1.4b5",
      "2.5b8",
      "2.3b1",
      "3.1b12",
      "0.1.13b3",
      "3.1b2",
      "2.5b7",
      "1.0b4",
      "3.5b7",
      "1.1b10",
      "2.1b2",
      "0.1.13b1",
      "1.2b2",
      "1.4b6",
      "3.1b5",
      "0.1.7b9",
      "3.0b3",
      "2.6b5",
      "2.4b4",
      "2.2b1",
      "2.0b2",
      "3.1b13",
      "2.6b4",
      "2.7b1",
      "3.0b2",
      "2.1b3",
      "2.5b3",
      "1.2b3",
      "0.1.13b8",
      "3.1b7"
  };
}
