package present.shortid;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;
import org.hashids.Hashids;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static present.shortid.ShortId.newIdFor;

public class ShortIdTest {

  private static final Logger logger = LoggerFactory.getLogger(ShortIdTest.class);

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  @Before public void setUp() {
    helper.setUp();
  }

  @After public void tearDown() {
    helper.tearDown();
  }

  @Test public void testShortId() {
    try (Closeable closeable = ObjectifyService.begin()) {
      assertEquals(1024, newIdFor(Foo.class, 0));
      assertEquals(1024 << 1, newIdFor(Foo.class, 0));
      assertEquals(1024 + 1023, newIdFor(Foo.class, 1023));
      assertEquals((1024 << 1) + 1023, newIdFor(Foo.class, 1023));
      assertEquals(1024, newIdFor(Bar.class, 0));
      assertEquals(1024 << 1, newIdFor(Bar.class, 0));
      assertEquals(3 << 10, newIdFor(Foo.class, 0));
      assertEquals((3 << 10) + 1023, newIdFor(Foo.class, 1023));
    }
  }

  @Test public void testHashids() {
    long userId = 0;
    long groupId = 1;
    Hashids hashids = new Hashids();
    String hash = hashids.encode(userId, groupId);
    logger.info("Hash: " + hash); // 43Yi8r
    long[] ids = hashids.decode(hash);
    assertEquals(userId, ids[0]);
    assertEquals(groupId, ids[1]);
    System.out.println(hashids.encode(1024 * 1024));
    System.out.println(hashids.encode(1025 * 1024));
  }

  static class Foo {}
  static class Bar {}
}
