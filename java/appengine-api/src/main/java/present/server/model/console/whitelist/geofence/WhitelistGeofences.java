package present.server.model.console.whitelist.geofence;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import present.server.model.SingletonEntity;
import present.server.model.util.Coordinates;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Represents the list of whitelist geofences.
 * This class must remain JSON friendly.
 *
 * @author Pat Niemeyer (pat@pat.net)
 */
// Note: Normally our SingletonEntity would have a parent to scope it.
// Note: Just keeping one of these globally for now to simplify.
@Entity @Cache public class WhitelistGeofences extends SingletonEntity<WhitelistGeofences> {

  public List<WhitelistGeofence> geofences = new ArrayList<>();

  public WhitelistGeofences() { }

  public WhitelistGeofences(List<WhitelistGeofence> geofences) {
    this.geofences = geofences;
  }

  public static void add(WhitelistGeofence gf) {
    WhitelistGeofences whitelist = load();
    whitelist.geofences.add(gf);
    whitelist.save();
  }

  public static void remove(String uuid) {
    WhitelistGeofences whitelist = load();
    Iterator<WhitelistGeofence> it = whitelist.geofences.iterator();
    while (it.hasNext()) {
      WhitelistGeofence geofence = it.next();
      if (geofence.uuid.equals(uuid)) {
        it.remove();
        break;
      }
    }
    whitelist.save();
  }

  // Load or create
  public static WhitelistGeofences load() {
    Key<WhitelistGeofences> key = Key.create(WhitelistGeofences.class, ONLY_ID);
    WhitelistGeofences geofences = ofy().load().key(key).now();
    if (geofences == null) {
      geofences = new WhitelistGeofences();
      ofy().save().entity(geofences).now();
    }
    return geofences;
  }

  public boolean contains(Coordinates coordinates) {
    for (WhitelistGeofence geofence : geofences) {
      if (geofence.contains(coordinates)) {
        return true;
      }
    }
    return false;
  }

  @Override protected WhitelistGeofences getThis() {
    return this;
  }
}



