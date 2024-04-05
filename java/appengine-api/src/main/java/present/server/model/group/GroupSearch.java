package present.server.model.group;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.PropertyProjection;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.google.common.geometry.S2CellId;
import com.google.common.geometry.S2CellUnion;
import com.google.common.geometry.S2LatLng;
import com.google.common.geometry.S2RegionCoverer;
import com.googlecode.objectify.Key;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.GroupMemberPreapproval;
import present.proto.RequestHeader;
import present.s2.CircularRegion;
import present.s2.S2Visualizer;
import present.server.RequestHeaders;
import present.server.Time;
import present.server.facebook.FacebookFriendship;
import present.server.model.BasePresentEntity;
import present.server.model.Space;
import present.server.model.comment.GroupViews;
import present.server.model.group.Group.Fields;
import present.server.model.user.Client;
import present.server.model.user.Clients;
import present.server.model.user.Feature;
import present.server.model.user.User;
import present.server.model.user.Users;
import present.server.model.util.Coordinates;

import static com.google.appengine.api.datastore.Query.FilterOperator.EQUAL;
import static com.google.appengine.api.datastore.Query.FilterOperator.GREATER_THAN_OR_EQUAL;
import static com.google.appengine.api.datastore.Query.FilterOperator.IN;
import static com.google.appengine.api.datastore.Query.FilterOperator.LESS_THAN_OR_EQUAL;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static present.server.Time.restart;

/**
 * Searches for nearby groups.
 *
 * @author Bob Lee (bob@present.co)
 */
public class GroupSearch {

  private static final Logger logger = LoggerFactory.getLogger(GroupSearch.class);

  /** Don't return group further away than this. */
  public static final long MAX_RADIUS = 128_000; // 128km

  private static final AsyncDatastoreService datastore
      = DatastoreServiceFactory.getAsyncDatastoreService();

  private Coordinates origin;
  private GroupRanker ranker;
  private int limit;
  private Space space;

  private GroupSearch() {}

  /** Sets the location to search. */
  public static GroupSearch near(Coordinates origin) {
    GroupSearch search = new GroupSearch();
    search.origin = origin;
    return search;
  }

  /** Use the specified ranking algorithm. */
  public GroupSearch using(GroupRanker ranking) {
    this.ranker = ranking;
    return this;
  }

  /** Return the specified number of groups. */
  public GroupSearch limit(int limit) {
    this.limit = limit;
    return this;
  }

  /** Specifies the space. */
  public GroupSearch space(Space space) {
    this.space = space;
    return this;
  }

  /** Executes the search. Orders groups based on their ranking. */
  public List<RankedGroup> run() {
    Preconditions.checkNotNull(this.origin);
    Preconditions.checkNotNull(this.ranker);

    User user = Users.current(false);

    // Determine space[s] to query.
    Iterable<String> spaces;
    if (this.space == null) {
      // Women and admins see women-only circles, too.
      spaces = user != null && user.isWoman()
          ? ImmutableList.of(Space.EVERYONE.id, Space.WOMEN_ONLY.id)
          : Collections.singleton(Space.EVERYONE.id);
    } else {
      spaces = Collections.singleton(this.space.id);
    }

    Preconditions.checkState(this.limit > 0);
    int retrieve = ranker.retrieve(this.limit);

    Stopwatch stopwatch = Stopwatch.createStarted();

    // Retrieve and return twice as many groups to admins.
    int adminFactor = user != null && user.isAdmin() ? 2 : 1;

    // Asynchronously query IDs for friends.
    Iterable<Key<User>> friendKeys = user != null && ranker.usesFriends()
        ? Iterables.transform(FacebookFriendship.friendIdsFor(user),
            id -> Key.create(User.class, id))
        : null;

     /*
      * TODO: The client should drive expanding the radius so it can request results from further
      * and further away (infinitely). We should give the client a cursor it can pass back to us
      * when it asks for more results.
      *
      * Search 5 radii concurrently. Use the largest radius that doesn't exceed the result limit.
      * This triggers 100 queries total (5 radii * 20 queries each, max 3000 results).
      *
      * Note: If this proves to be too many queries, we should search each radius sequentially.
      * This will obviously result in higher latency.
      */
    List<ProximityQuery> queries = new ArrayList<>();
    long radius = 2_000; // 2km
    // Radii: 2km, 8km, 32km, 128km
    // Set a high limit for the smallest radius so we don't miss groups in dense areas.
    queries.add(new ProximityQuery(spaces, origin, radius, 1000));
    for (int i = 0; i < 3; i++) {
      radius *= 4;
      // Kick off the queries, but don't actually start fetching results until we need them.
      queries.add(new ProximityQuery(spaces, origin, radius, 500));
    }

    logger.info("Kicked off queries in {}.", restart(stopwatch));

    // TODO: Adapt this algorithm by region. We can either cache the number of circles in
    // a given region or we can cache the best radius by region.

    // Find the query returning the most results without going over the limit.
    Iterator<ProximityQuery> iterator = queries.iterator();
    ProximityQuery best = iterator.next();
    if (best.exceededLimit()) {
      logger.error("Exceeded query limit using a {}km radius @ {}! Lower the initial radius "
          + "and/or the number of weeks we search, or increase the limit.",
          best.radius / 1000, origin);
    } else {
      while (iterator.hasNext()) {
        ProximityQuery current = iterator.next();
        if (current.exceededLimit()) {
          logger.info("Exceeded limit with {}km radius @ {}.", best.radius / 1000, origin);
          // Add results from current group to the previous group. Ensures we have a
          // sufficient number of results without missing any nearby results.
          best.groupLists.addAll(current.groupLists);
          break;
        }
        best = current;
      }
      logger.info("Found {} groups using a {}km radius @ {}.",
          best.size(), best.radius / 1000, origin);
    }

    logger.info("Found best result set in {}.", restart(stopwatch));

    List<NearbyKey> results = best.results();

    logger.info("Computed {} distances in {}.", results.size(), restart(stopwatch));

    // Find the closest groups.
    if (results.size() > retrieve) {
      results = Ordering.from(NearbyKey.BY_DISTANCE).leastOf(results, retrieve * adminFactor);
    }

    logger.info("Found closest groups in {}.", restart(stopwatch));

    // Retrieve the groups and their logs from the datastore.

    List<Key<Group>> groupKeys;
    if (user != null && user.isWoman()) {
      groupKeys = Stream.concat(
          results.stream().map(NearbyKey::groupKey),
          // Include nearest welcome group. Ensures we always have at least one group.
          Stream.of(WelcomeGroup.nearestTo(this.origin)))
          .collect(toList());
    } else {
      groupKeys = results.stream().map(NearbyKey::groupKey).collect(toList());
    }

    List<Key<Group.Log>> logKeys = ranker.usesLogs()
        ? groupKeys.stream().map(Group.Log::keyFor).collect(toList())
        : null;
    Map<Key<User>, User> friends = friendKeys != null && ranker.usesFriends()
        ? ofy().load().keys(friendKeys)
        : Collections.emptyMap();
    Map<Key<Group>, Group> groups = ofy().load().group(ranker.loadGroups()).keys(groupKeys);
    Map<Key<Group.Log>, Group.Log> logs = ranker.usesLogs() ? ofy().load().keys(logKeys)
        : Collections.emptyMap();

    logger.info("Loaded {} groups and logs in {}.", groups.size(), restart(stopwatch));

    // Compute a ranking for each group.
    List<RankedGroup> ranked = results.stream().map(nk -> {
      Group group = groups.get(nk.groupKey);
      if (user != null) {
        if (group.lastSignificantComment != null && group.lastSignificantComment.isLoaded()) {
          if (!user.canSee(group.lastSignificantComment.get())) return null;
        }
        if (!user.canSee(group.owner.get())) {
          logger.debug("User can't see group.");
          return null;
        }
      }
      Group.Log log = logs.get(nk.logKey);
      GroupRanker.Input input = new GroupRanker.Input(
          user, friends.values(), group, log, nk.distance);
      return ranker.rank(input);
    }).filter(Objects::nonNull).collect(toList());

    logger.info("Ranked groups in {}.", restart(stopwatch));

    // Sort groups based on their ranking.
    List<RankedGroup> top = Ordering.natural().leastOf(ranked, this.limit * adminFactor);

    logger.info("Found top {} groups in {}.", top.size(), restart(stopwatch));

    // Don't return private groups to clients that don't support them.
    RequestHeader header = RequestHeaders.current();
    if (header != null) {
      Client client = Clients.getOrCreate(header.clientUuid);
      if (!client.supports(Feature.PRIVATE_GROUPS)) {
        top = top.stream()
            .filter(g -> g.group().preapprove == GroupMemberPreapproval.ANYONE)
            .collect(toList());
      }
    }

    // Pre-load group views. This avoids individual retrievals when we convert to GroupResponse.
    if (user != null) {
      GroupViews.viewsFor(user, Iterables.transform(top, RankedGroup::group));
    }

    return top;
  }

  private static String lastName(String name) {
    int start = name.lastIndexOf(' ');
    return start == -1 ? name : name.substring(start + 1);
  }

  /** Queries groups by location. */
  private static class ProximityQuery {

    private final S2LatLng origin;
    private final long radius;
    private final int limit;
    private final List<List<Entity>> groupLists = new ArrayList<>();

    /**
     * Queries nearby groups. Triggers 20 queries (5 cells * 4 weeks) in parallel.
     */
    public ProximityQuery(Iterable<String> spaces, Coordinates origin, long radius, int limit) {
      this.origin = origin.toS2LatLng();
      this.radius = radius;

      this.limit = limit;

      // TODO: Visualize these coverings so we can see how accurate they are.
      S2RegionCoverer coverer = new S2RegionCoverer();
      coverer.setMaxCells(5);
      S2CellUnion cells = coverer.getCovering(
          CircularRegion.create(origin.latitude, origin.longitude, radius));

      // Matches this month and the last (between one and two months).
      long thisMonth = Time.epochMonth();
      List<Long> months = ImmutableList.of(thisMonth, thisMonth - 1);

      /*
       * Use the low-level datastore API instead of Objectify. Projections in Objectify can
       * lead to confusion, this has none of the reflection overhead, and we can set the
       * prefetch size.
       */
      for (S2CellId cell : cells) {
        Filter filter = CompositeFilterOperator.and(
            new FilterPredicate(Fields.discoverable.getName(), EQUAL, true),
            new FilterPredicate(BasePresentEntity.Fields.deleted.getName(), EQUAL, false),

            new FilterPredicate(Group.Fields.spaceId.getName(), IN, spaces),
            // TODO: Uncomment this once we've added lastUpdateMonth to each Group.
            //new FilterPredicate(Fields.lastUpdateMonth.getName(), IN, months),

            new FilterPredicate(Fields.s2CellId.getName(), GREATER_THAN_OR_EQUAL, cell.rangeMin().id()),
            new FilterPredicate(Fields.s2CellId.getName(), LESS_THAN_OR_EQUAL, cell.rangeMax().id())
        );
        PreparedQuery query = datastore.prepare(new Query(Group.class.getSimpleName())
            .setFilter(filter)
            .addProjection(new PropertyProjection(Fields.s2CellId.getName(), Long.class)));
        FetchOptions fetchOptions = FetchOptions.Builder.withLimit(limit)
            .chunkSize(limit);
        groupLists.add(query.asList(fetchOptions));
      }
    }

    /**
     * Returns the aggregate list of results, bucketed by week, with the most recent first.
     * This is the result of a projection, so only the {@code id}, {@code s2CellId},
     * and {@code lastUpdateWeek} fields are set.
     */
    public List<NearbyKey> results() {
      Set<String> ids = new HashSet<>();
      return groupLists.stream()
          .flatMap(List::stream)
          // Remove duplicates. Can happen when we combine results of queries.
          .filter(e -> ids.add(e.getKey().getName()))
          .map(e -> new NearbyKey(origin, e))
          // Remove groups that are too far away. This happens when the S2 cells cover an area
          // larger than the requested distance.
          .filter(k -> k.distance < MAX_RADIUS)
          .collect(toList());
    }

    /**
     * True if the query matched more results than were returned (meaning we arbitrarily missed
     * results).
     */
    public boolean exceededLimit() {
      return size() >= limit;
    }

    /**
     * Returns the number of results.
     */
    public int size() {
      return groupLists.stream().mapToInt(List::size).sum();
    }
  }

  /** A Group key with a location. */
  private static class NearbyKey {

    private static Comparator<NearbyKey> BY_DISTANCE
        = Comparator.comparingDouble(NearbyKey::distance);

    private final Key<Group> groupKey;
    private final Key<Group.Log> logKey;
    private final long s2CellId;
    private final double distance;

    private NearbyKey(S2LatLng origin, Entity entity) {
      this.groupKey = Key.create(entity.getKey());
      this.logKey = Group.Log.keyFor(this.groupKey);
      this.s2CellId = (Long) entity.getProperty(Fields.s2CellId.getName());

      // Note: Can we just use the angle between two points?
      this.distance = new S2CellId(s2CellId).toLatLng().getEarthDistance(origin);
    }

    private Key<Group> groupKey() {
      return this.groupKey;
    }

    /** Returns the distance from the origin in m. */
    private double distance() {
      return this.distance;
    }
  }

  /** Visualizes S2 coverings. */
  public static void main(String[] args) {
    Coordinates location = Coordinates.SAN_FRANCISCO;
    S2RegionCoverer coverer = new S2RegionCoverer();
    coverer.setMaxCells(12);
    List<S2CellUnion> unions = new ArrayList<>();
    unions.add(coverer.getCovering(CircularRegion.create(location.latitude, location.longitude, 256000)));
    unions.add(coverer.getCovering(CircularRegion.create(location.latitude, location.longitude, 64000)));
    unions.add(coverer.getCovering(CircularRegion.create(location.latitude, location.longitude, 16000)));
    unions.add(coverer.getCovering(CircularRegion.create(location.latitude, location.longitude, 4000)));
    unions.add(coverer.getCovering(CircularRegion.create(location.latitude, location.longitude, 1000)));
    S2Visualizer.visualize(unions);
  }
}
