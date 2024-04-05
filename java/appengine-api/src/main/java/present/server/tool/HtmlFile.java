package present.server.tool;

import com.google.common.base.Charsets;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Tools for working with HTML files.
 *
 * @author Bob Lee (bob@present.co)
 */
public class HtmlFile {

  /** Writes the given HTML to a file and opens it in a web browser. */
  public static void writeAndOpen(String html) {
    String path = "/tmp/test.html";
    new File(path).delete();
    try {
      Files.write(Paths.get(path), html.getBytes(Charsets.UTF_8), StandardOpenOption.CREATE);
      new ProcessBuilder("open", path).inheritIO().start().waitFor();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
