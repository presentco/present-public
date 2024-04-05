package present.server;

import com.squareup.wire.Message;
import okio.ByteString;
import present.proto.Empty;

/**
 * @author Pat Niemeyer (pat@pat.net)
 */
public class Protos {

  public static final Empty EMPTY = new Empty();

  public static String encodeBase64( Message proto ) {
    return ByteString.of(proto.encode()).base64();
  }

}
