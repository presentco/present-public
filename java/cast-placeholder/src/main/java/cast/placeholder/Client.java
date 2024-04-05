package cast.placeholder;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

/**
 * @author Bob Lee (bob@present.co)
 */
@Entity @Cache public class Client extends BaseEntity {

  @Id public String privateId;

  @Index public String publicId;

  public long creationTime;

  public boolean censored;

  public String deviceName;

  /** Can be set to something human readable to aid internal tracking. */
  private String internalName;

  public String internalName() {
    if (internalName != null) return internalName;
    if (deviceName != null) return deviceName;
    return "Client " + privateId;
  }
}
