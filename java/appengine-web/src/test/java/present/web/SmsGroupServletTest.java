package present.web;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static present.web.SmsServlet.urlPattern;

/**
 * @author Bob Lee (bob@present.co)
 */
public class SmsGroupServletTest {

  @Test public void urlPattern() {
    assertTrue(urlPattern.matcher("https://present.co/g/foo").matches());
    assertTrue(urlPattern.matcher("http://present.co/g/foo").matches());
    assertTrue(urlPattern.matcher("https://staging.present.co/g/foo").matches());
    assertTrue(urlPattern.matcher("http://staging.present.co/g/foo").matches());
  }
}
