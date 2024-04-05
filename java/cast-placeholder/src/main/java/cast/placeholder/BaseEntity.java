package cast.placeholder;

import com.googlecode.objectify.Key;

/**
 * @author Bob Lee (bob@present.co)
 */
public class BaseEntity {

  public String consoleUrl() {
    Key<?> key = Key.create(this);
    return "https://console.cloud.google.com/datastore/entities/edit?key="
        + key.toWebSafeString() + "&project=cast-placeholder&kind=" + key.getKind()
        + "&authuser=1";
  }
}
