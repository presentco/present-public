package present.acceptance;

/**
 * @author Bob Lee (bob@present.co)
 */
public class DevelopmentTest extends AcceptanceTest {

  private static final int API_SERVER_PORT = 8081;

  public DevelopmentTest() {
    super("http://local.present.co:" + API_SERVER_PORT + "/api");
  }
}
