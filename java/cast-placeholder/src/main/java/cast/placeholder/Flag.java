package cast.placeholder;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.condition.IfFalse;

/**
 * A user flagged a cast.
 *
 * @author Bob Lee (bob@present.co)
 */
@Entity public class Flag {

  @Id public String id;
  @Index public Ref<Client> client;
  @Index public Ref<Cast> cast;
  @Index public long creationTime = System.currentTimeMillis();
}
