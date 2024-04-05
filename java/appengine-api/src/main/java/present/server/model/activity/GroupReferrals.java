package present.server.model.activity;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.PropertyProjection;
import com.google.appengine.api.datastore.Query;
import com.googlecode.objectify.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.server.model.group.Group;
import present.server.model.user.Clients;
import present.server.model.user.User;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * GroupReferral Utilities
 *
 * @author Gabrielle Taylor {gabrielle@present.co}
 */
public class GroupReferrals {
  private static final AsyncDatastoreService datastore = DatastoreServiceFactory.getAsyncDatastoreService();

  /** Generates a uuid for a group referral from the user and group uuids */
  public static String id(User to, Group g) {
    return to.uuid() + ":" + g.uuid();
  }

  /** Generates a key for a group referral from the user and group uuids */
  public static com.googlecode.objectify.Key<GroupReferral> keyFor(User to, Group g) {
    return com.googlecode.objectify.Key.create(GroupReferral.class, id(to, g));
  }

  /** Gets referrals to the given group for the given user. */
  public static GroupReferral get(User to, Group g) {
    return ofy().load().key(keyFor(to, g)).now();
  }

  public static GroupReferral getOrCreate(User from, User to, Group group) {
    return ofy().transact(() -> {
      GroupReferral referral = ofy().load().key(keyFor(to, group)).now();
      if (referral == null) {
        referral = new GroupReferral(from, to, group);
      } else if (!referral.referrers().contains(from)) {
        referral.addFrom(from);
      }
      referral.save();
      return referral;
    });
  }

  // Counts all referrals from a user
  public static long countReferrals(User from) {
    return ofy().load().type(GroupReferral.class).filter("referrers", from).count();
  }

  // Lists all referrals from a user
  public static List<GroupReferral> getReferrals(User from) {
    List<GroupReferral> referrals = ofy().load()
        .type(GroupReferral.class)
        .filter("referrers", from)
        .order("saveTimestamp")
        .list();
    return referrals;
  }

  // Batch loads all referrals and generates a map of user to number of referrals
  public static class ReportQuery {
    private final List<Entity> results = new ArrayList<>();
    
    public ReportQuery() {
      PreparedQuery q = datastore.prepare(
          new Query("GroupReferral").addProjection(new PropertyProjection("referrers",
              com.google.appengine.api.datastore.Key.class))
              .addProjection(new PropertyProjection("to", com.google.appengine.api.datastore.Key.class))
              .addSort("saveTimestamp"));
      results.addAll(q.asList(FetchOptions.Builder.withDefaults()));
    }

    public Map<User, Long> report() {
      List<Key<GroupReferral>> keys = results.stream().map(e -> Key.create(GroupReferral.class, e.getKey().getName())).collect(Collectors.toList());
      Map<Key<GroupReferral>, GroupReferral> referrals = ofy().load().keys(keys);
      Map<User, Long> referralCounts = new HashMap<>();
      for (GroupReferral r : referrals.values()) {
        if (referralCounts.containsKey(r.referrers().get(0))) {
          long count = referralCounts.get(r.referrers().get(0));
          referralCounts.replace(r.referrers().get(0), count, ++count);
        } else {
          referralCounts.put(r.referrers().get(0), 1L);
        }
      }
      return referralCounts;
    }
  }

}
