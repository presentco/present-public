package cast.placeholder;

import present.s2.CircularRegion;
import present.wire.rpc.core.ClientException;
import present.wire.rpc.core.ServerException;
import cast.placeholder.proto.CastLifetimeResponse;
import cast.placeholder.proto.CastResponse;
import cast.placeholder.proto.CastService;
import cast.placeholder.proto.DeleteCastRequest;
import cast.placeholder.proto.Empty;
import cast.placeholder.proto.FlagCastRequest;
import cast.placeholder.proto.NearbyCastsRequest;
import cast.placeholder.proto.NearbyCastsResponse;
import cast.placeholder.proto.PutCastRequest;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.geometry.S2CellId;
import com.google.common.geometry.S2CellUnion;
import com.google.common.geometry.S2LatLng;
import com.google.common.geometry.S2RegionCoverer;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.Work;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * @author Bob Lee (bob@present.co)
 */
public class AppEngineCastService implements CastService {

  private static final Logger logger = LoggerFactory.getLogger(AppEngineCastService.class);

  private static final int MAX_CASTS = 100;

  @Override public NearbyCastsResponse nearbyCasts(NearbyCastsRequest request)
      throws IOException {
    Stopwatch stopwatch = Stopwatch.createStarted();

    final Client client = Clients.current();
    double latitude = request.location.latitude;
    double longitude = request.location.longitude;

    // App Engine limits us to 10 parallel queries. We perform 10 queries per region
    // ranging from a 100 km radius up to 3/4 of the United States.
    long radius = 100_000; // 100km
    List<CastQuery> queries = new ArrayList<>();
    // Set a high limit for the smallest radius so we don't miss casts in dense areas.
    // Radii: 100, 200, 400, 800, 1600, 3200km
    queries.add(new CastQuery(latitude, longitude, radius, 500 /* up to 5000 casts */));
    for (int i = 0; i < 5; i++) {
      radius *= 2;
      queries.add(new CastQuery(latitude, longitude, radius, 100 /* up to 1000 casts */));
    }

    // Find the query returning the most results without going over the limit.
    Iterator<CastQuery> iterator = queries.iterator();
    CastQuery best = iterator.next();
    if (best.exceededLimit()) {
      // If this happens, we should lower the initial radius.
      logger.warn("Exceeded query limit using a {}km radius @ {}, {}.",
          best.radius / 1000, latitude, longitude);
    } else {
      while (iterator.hasNext()) {
        CastQuery current = iterator.next();
        if (current.exceededLimit()) {
          logger.info("Exceeded limit with {}km radius @ {}, {}.",
              best.radius / 1000, latitude, longitude);
          break;
        }
        best = current;
      }
      logger.info("Found {} casts using a {}km radius @ {}, {}.",
          best.size(), best.radius / 1000, latitude, longitude);
    }

    // Retrieve and filter the entities.
    Iterable<Cast> casts = ofy().load().keys(best).values();
    Iterable<Cast> filtered = Iterables.filter(casts, new CastFilter());
    S2LatLng origin = S2LatLng.fromDegrees(latitude, longitude);
    List<Cast> closest = Ordering.from(new CastDistanceComparator(origin))
        .leastOf(filtered, MAX_CASTS);
    List<CastResponse> responses = Lists.transform(closest,
        new Function<Cast, CastResponse>() {
      @Override public CastResponse apply(Cast cast) {
        Client creator = cast.creator.get();
        boolean owner = creator.privateId.equals(client.privateId);
        String image = cast.imageUrl();
        return new CastResponse(cast.id, cast.creationTime, owner, image, creator.publicId);
      }
    });

    logger.info("Found nearby casts in {}.", stopwatch);

    return new NearbyCastsResponse(responses);
  }

  /** Asynchronously queries nearby casts using the given search radius. */
  static class CastQuery implements Iterable<Key<Cast>> {

    private final List<List<Key<Cast>>> keyLists = new ArrayList<>();

    private final long radius;
    private final int cellLimit;

    public CastQuery(double latitude, double longitude, long radius, int cellLimit) {
      this.radius = radius;
      this.cellLimit = cellLimit;
      // Break the region down into 5 cells and perform 2 queries per cell (one for today and
      // one for yesterday). This results in 10 queries.
      S2RegionCoverer coverer = new S2RegionCoverer();
      coverer.setMaxCells(5);
      S2CellUnion cells = coverer.getCovering(
          CircularRegion.create(latitude, longitude, radius));
      long today = Calendar.today();
      for (S2CellId cell : cells) {
        keyLists.add(queryCasts(cell, today - 1));
        keyLists.add(queryCasts(cell, today));
      }
    }

    private List<Key<Cast>> queryCasts(S2CellId cellId, long day) {
      return ofy().load()
          .type(Cast.class)
          .limit(cellLimit)
          .filter("day", day)
          .filter("s2CellId >=", cellId.rangeMin().id())
          .filter("s2CellId <=", cellId.rangeMax().id())
          .keys()
          .list();
    }

    public boolean exceededLimit() {
      for (List<Key<Cast>> keys : keyLists) if (keys.size() >= cellLimit) return true;
      return false;
    }

    public int size() {
      int size = 0;
      for (List<Key<Cast>> keyList : keyLists) {
        size += keyList.size();
      }
      return size;
    }

    @Override public Iterator<Key<Cast>> iterator() {
      return Iterables.concat(keyLists).iterator();
    }
  }

  /** Picks casts that are visible and that were created in the past 24 hours. */
  private static class CastFilter implements Predicate<Cast> {
    final long now = System.currentTimeMillis();
    @Override public boolean apply(Cast cast) {
      return cast.visible() && now - cast.creationTime < Calendar.DAY_MS;
    }
  }

  @Override public Empty putCast(final PutCastRequest request) throws IOException {
    logger.debug("Put cast: "+request);
    final Client client = Clients.current();
    if (!Objects.equals(request.deviceName, client.deviceName)) {
      client.deviceName = request.deviceName;
      ofy().save().entity(client);
    }
    ofy().transact(new Work<Client>() {
      @Override public Client run() {
        Key<Cast> key = Key.create(Cast.class, request.id);
        Cast cast = ofy().load().key(key).now();
        if (cast == null) {
          cast = new Cast();
          cast.id = request.id;
          cast.creator = Ref.create(client);
          cast.creationTime = System.currentTimeMillis();
          cast.day = Calendar.today();
          double latitude = request.location.latitude;
          double longitude = request.location.longitude;
          cast.s2CellId = S2CellId.fromLatLng(
              S2LatLng.fromDegrees(latitude, longitude)).id();
          cast.latitude = latitude;
          cast.longitude = longitude;
          cast.accuracy = request.location.accuracy;
          try {
            GoogleCloudStorage.upload(request.image, cast.id);
          } catch (IOException e) {
            throw new ServerException(e);
          }
          ofy().save().entity(cast);
          if (!Environment.inDevelopment() && !Environment.inTest()) {
            QueueFactory.getDefaultQueue().add(CastToSlack.optionsFor(cast));
          }
          logger.debug("Url for cast: "+cast.imageUrl());
        }
        return client;
      }
    });
    return new Empty();
  }

  @Override public Empty flagCast(FlagCastRequest request) throws IOException {
    Client client = Clients.current();

    Cast cast = ofy().load().type(Cast.class).id(request.castId).now();

    // Email regarding flag.
    if (!Environment.inTest()) {
      FlagEmail flagEmail = new FlagEmail(client, cast);
      flagEmail.send();
      if (Environment.inDevelopment()) logger.info(flagEmail.html());
    }

    // Record flag in data store.
    Flag flag = new Flag();
    // Use the cast ID so we only store one flag per user/cast.
    flag.id = request.castId;
    flag.client = Ref.create(client);
    flag.cast = Ref.create(cast);
    ofy().save().entity(flag);

    return new Empty();
  }

  @Override public Empty deleteCast(final DeleteCastRequest request) throws IOException {
    final Client client = Clients.current();
    ofy().transact(new Runnable() {
      @Override public void run() {
        Key<Cast> key = Key.create(Cast.class, request.castId);
        Cast cast = ofy().load().key(key).now();
        if (cast == null) throw new ClientException("Cast not found: " + request.castId);
        if (!cast.creator.get().privateId.equals(client.privateId)) {
          throw new ClientException("Cast " + cast.id + " not owned by Client "
              + client.privateId + ".");
        }
        cast.deleted = true;
        ofy().save().entity(cast);
      }
    });
    return new Empty();
  }

  @Override public CastLifetimeResponse castLifetime(Empty empty) throws IOException {
    return new CastLifetimeResponse(Calendar.DAY_MS, "24 hours");
  }
}
