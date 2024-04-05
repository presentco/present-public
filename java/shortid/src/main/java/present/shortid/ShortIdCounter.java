package present.shortid;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@Entity @Cache class ShortIdCounter {
  @Id String id;
  long value;
}
