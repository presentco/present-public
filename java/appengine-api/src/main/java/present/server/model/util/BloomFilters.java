package present.server.model.util;

import com.google.common.hash.BloomFilter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.annotation.Nullable;
import okio.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author pat@pat.net
 */
public class BloomFilters {
  private static final Logger logger = LoggerFactory.getLogger(BloomFilters.class);

  /**
   * Serialize the BloomFilter using its writeTo() method.
   * @return the byte array or null if the bloom filter is null.
   */
  public static @Nullable byte [] toByteArray(@Nullable BloomFilter bf) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      bf.writeTo(baos);
      return baos.toByteArray();
    } catch (IOException e) {
      logger.error("error serializing bloom filter");
      throw new RuntimeException(e);
    }
  }

  public static @Nullable ByteString toByteString(@Nullable BloomFilter bf) {
    if (bf == null ) { return null; }
    return ByteString.of(toByteArray(bf));
  }
}
