package present.live.server;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import okio.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author pat@pat.net
 *         Date: 2/6/17
 */
public class BloomFilters {
  private static final Logger logger = LoggerFactory.getLogger(BloomFilters.class);

  /**
   * Deserialize the BloomFilter using its readFrom() method.
   * @return the filter or null if the original byte stream is null
   */
  public static BloomFilter<String> fromByteArray(byte [] ba) {
    if ( ba == null ) { return null; }
    try {
      return BloomFilter.readFrom(new ByteArrayInputStream(ba), UserIdFunnel.INSTANCE);
    } catch (IOException e) {
      logger.error("error deserializing bloom filter: "+e);
      throw new RuntimeException(e);
    }
  }

  public static BloomFilter<String> fromByteString(ByteString bs) {
    if (bs == null) { return null; }
    return fromByteArray(bs.toByteArray());
  }

  // TODO: Can't be null but never used in this context...
  private enum UserIdFunnel implements Funnel<String> {
    INSTANCE;
    public void funnel(String userId, PrimitiveSink into) {
      into.putUnencodedChars(userId);
    }
  }

}
