package present.server;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UuidsTest {

  private static final Logger logger = LoggerFactory.getLogger(UuidsTest.class);

  @Test public void uuidFromName() throws InterruptedException {
    String uuid = Uuids.fromName("Bob Lee");
    logger.info("UUID: {}", uuid);
    assertTrue(Uuids.isValid(uuid));
    assertEquals(uuid, Uuids.fromName("Bob Lee"));
    assertEquals(uuid, Uuids.fromName("Bob Lee"));
  }
}
