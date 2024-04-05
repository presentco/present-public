package cast.placeholder.test;

import present.proto.RequestHeader;
import present.wire.rpc.client.RpcClient;
import present.wire.rpc.core.RpcFilter;
import present.wire.rpc.core.RpcInvocation;
import cast.placeholder.proto.CastResponse;
import cast.placeholder.proto.CastService;
import cast.placeholder.proto.Coordinates;
import cast.placeholder.proto.DeleteCastRequest;
import cast.placeholder.proto.FlagCastRequest;
import cast.placeholder.proto.NearbyCastsRequest;
import cast.placeholder.proto.NearbyCastsResponse;
import cast.placeholder.proto.PutCastRequest;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import okio.ByteString;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public abstract class AcceptanceTest {

  private final CastService cs;

  private final String clientUuid = UUID.randomUUID().toString();

  public AcceptanceTest(String apiUrl) {
    RpcFilter filter = new RpcFilter() {
      @Override public Object filter(RpcInvocation invocation) throws Exception {
        invocation.setHeader(newHeader());
        return invocation.proceed();
      }
    };

    this.cs = RpcClient.create(apiUrl, RequestHeader.class, CastService.class, filter);
  }

  private RequestHeader newHeader() {
    return new RequestHeader(clientUuid, UUID.randomUUID().toString(), "not implemented",
        RequestHeader.Platform.TEST, 1, "1", "1");
  }

  private Coordinates location = new Coordinates(1.0, 2.0, 0.0);

  @Test public void simple() throws IOException {
    String castId = UUID.randomUUID().toString();

    // Try a query before we store anything.
    cs.nearbyCasts(new NearbyCastsRequest(location));

    // Put it.
    cs.putCast(new PutCastRequest(castId, location, IMAGE, "Bob's iPhone"));

    // Find it.
    NearbyCastsResponse response = cs.nearbyCasts(new NearbyCastsRequest(location));
    CastResponse found = findCast(response.nearbyCasts, castId);
    assertTrue(System.currentTimeMillis() - found.creationTime < 1000 * 60 * 60);
    assertTrue(found.mine);
    assertEquals(IMAGE, ByteString.of(Resources.toByteArray(new URL(found.image))));

    // Flag it.
    cs.flagCast(new FlagCastRequest(castId));

    // Delete it.
    cs.deleteCast(new DeleteCastRequest(castId));
    response = cs.nearbyCasts(new NearbyCastsRequest(location));
    for (CastResponse cast : response.nearbyCasts) {
      assertFalse(cast.id.equals(castId));
    }
  }

  private CastResponse findCast(final List<CastResponse> casts, final String id) {
    return retry(new Supplier<CastResponse>() {
      @Override public CastResponse get() {
        for (CastResponse cast : casts) {
          if (cast.id.equals(id)) return cast;
        }
        throw new AssertionError();
      }
    });
  }

  /**
   * Retry until the contained assertions pass. Necessary for eventually consistent data.
   */
  private void retry(final Runnable r) {
    retry((Supplier<Void>) () -> {
      r.run();
      return null;
    });
  }

  /**
   * Retry until the contained assertions pass. Necessary for eventually consistent data.
   */
  private <T> T retry(Supplier<T> s) {
    int tries = 0;
    while (true) {
      try {
        return s.get();
      } catch (AssertionError e) {
        if (++tries == 10) throw e;
      }
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) { /* ignored */ }
    }
  }

  private static final SecureRandom random = new SecureRandom();

  public static String randomNumber(int length) {
    int value = random.nextInt(Integer.MAX_VALUE);
    String code = Integer.toString(value);
    if (code.length() > length) code = code.substring(0, length);
    return Strings.padStart(code, length, '0');
  }

  // A 1x1 JPEG.
  private static final ByteString IMAGE = ByteString.of(
      (byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe0, (byte) 0x00, (byte) 0x10,
      (byte) 0x4a, (byte) 0x46, (byte) 0x49, (byte) 0x46, (byte) 0x00, (byte) 0x01, (byte) 0x01,
      (byte) 0x01, (byte) 0x00, (byte) 0x60, (byte) 0x00, (byte) 0x60, (byte) 0x00, (byte) 0x00,
      (byte) 0xff, (byte) 0xdb, (byte) 0x00, (byte) 0x43, (byte) 0x00, (byte) 0x08, (byte) 0x06,
      (byte) 0x06, (byte) 0x07, (byte) 0x06, (byte) 0x05, (byte) 0x08, (byte) 0x07, (byte) 0x07,
      (byte) 0x07, (byte) 0x09, (byte) 0x09, (byte) 0x08, (byte) 0x0a, (byte) 0x0c, (byte) 0x14,
      (byte) 0x0d, (byte) 0x0c, (byte) 0x0b, (byte) 0x0b, (byte) 0x0c, (byte) 0x19, (byte) 0x12,
      (byte) 0x13, (byte) 0x0f, (byte) 0x14, (byte) 0x1d, (byte) 0x1a, (byte) 0x1f, (byte) 0x1e,
      (byte) 0x1d, (byte) 0x1a, (byte) 0x1c, (byte) 0x1c, (byte) 0x20, (byte) 0x24, (byte) 0x2e,
      (byte) 0x27, (byte) 0x20, (byte) 0x22, (byte) 0x2c, (byte) 0x23, (byte) 0x1c, (byte) 0x1c,
      (byte) 0x28, (byte) 0x37, (byte) 0x29, (byte) 0x2c, (byte) 0x30, (byte) 0x31, (byte) 0x34,
      (byte) 0x34, (byte) 0x34, (byte) 0x1f, (byte) 0x27, (byte) 0x39, (byte) 0x3d, (byte) 0x38,
      (byte) 0x32, (byte) 0x3c, (byte) 0x2e, (byte) 0x33, (byte) 0x34, (byte) 0x32, (byte) 0xff,
      (byte) 0xdb, (byte) 0x00, (byte) 0x43, (byte) 0x01, (byte) 0x09, (byte) 0x09, (byte) 0x09,
      (byte) 0x0c, (byte) 0x0b, (byte) 0x0c, (byte) 0x18, (byte) 0x0d, (byte) 0x0d, (byte) 0x18,
      (byte) 0x32, (byte) 0x21, (byte) 0x1c, (byte) 0x21, (byte) 0x32, (byte) 0x32, (byte) 0x32,
      (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32,
      (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32,
      (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32,
      (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32,
      (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32,
      (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32,
      (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0xff, (byte) 0xc0,
      (byte) 0x00, (byte) 0x11, (byte) 0x08, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x01,
      (byte) 0x03, (byte) 0x01, (byte) 0x22, (byte) 0x00, (byte) 0x02, (byte) 0x11, (byte) 0x01,
      (byte) 0x03, (byte) 0x11, (byte) 0x01, (byte) 0xff, (byte) 0xc4, (byte) 0x00, (byte) 0x15,
      (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0xff, (byte) 0xc4,
      (byte) 0x00, (byte) 0x14, (byte) 0x10, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xff,
      (byte) 0xc4, (byte) 0x00, (byte) 0x14, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x00,
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0xff, (byte) 0xc4, (byte) 0x00, (byte) 0x14, (byte) 0x11, (byte) 0x01, (byte) 0x00,
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x00, (byte) 0xff, (byte) 0xda, (byte) 0x00, (byte) 0x0c, (byte) 0x03, (byte) 0x01,
      (byte) 0x00, (byte) 0x02, (byte) 0x11, (byte) 0x03, (byte) 0x11, (byte) 0x00, (byte) 0x3f,
      (byte) 0x00, (byte) 0xbf, (byte) 0x80, (byte) 0x0f, (byte) 0xff, (byte) 0xd9
  );
}
