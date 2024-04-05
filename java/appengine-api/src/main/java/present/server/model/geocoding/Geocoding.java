package present.server.model.geocoding;

import co.present.unblock.Unblock;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.SettableFuture;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.GeocodingApiRequest;
import com.google.maps.PendingResult;
import com.google.maps.model.AddressComponent;
import com.google.maps.model.AddressComponentType;
import com.google.maps.model.ComponentFilter;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.server.model.user.User;
import present.server.model.util.Address;
import present.server.model.util.Coordinates;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * @author Pat Niemeyer (pat@present.co)
 * Date: 11/9/17
 */
public class Geocoding {
  private static final Logger logger = LoggerFactory.getLogger(Geocoding.class);

  public static String googleMapsInternalMapsKey = "xxx";
  public static GeoApiContext geoApiContext = new GeoApiContext.Builder().apiKey(googleMapsInternalMapsKey).build();

  private static final Coordinates NOT_FOUND = new Coordinates(0, 0);

  private static LoadingCache<String, Coordinates> zipCodeLocations = CacheBuilder.newBuilder()
      .build(new CacheLoader<String, Coordinates>() {
        @Override public Coordinates load(String zipCode) throws Exception {
          GeocodingApiRequest request = new GeocodingApiRequest(geoApiContext);
          request.components(ComponentFilter.postalCode(zipCode),
              ComponentFilter.country("United States"));
          GeocodingResult[] results = request.await();
          if (results.length == 0) {
            logger.info("No results for {}.", zipCode);
            return NOT_FOUND;
          }
          GeocodingResult result = results[0];
          if (result.geometry == null) {
            logger.info("No geometry for {}.", zipCode);
            return NOT_FOUND;
          }
          LatLng location = result.geometry.location;
          if (location == null) {
            logger.info("No location for {}.", zipCode);
            return NOT_FOUND;
          }
          return new Coordinates(location.lat, location.lng);
        }
      });

  public static Coordinates geocodeZipCode(String zipCode) {
    try {
      Coordinates location = zipCodeLocations.get(zipCode);
      return location == NOT_FOUND ? null : location;
    } catch (ExecutionException e) {
      logger.warn("Error geocoding zip code.", e);
      return null;
    }
  }

  public static void geocodeSignupLocation(User user) {
    if (user.signupLocation == null) {
      logger.error("User doesn't have signup location: {}", user);
      return;
    }
    Address signupAddress = Geocoding.reverseGeocodeNow(user.signupLocation);
    ofy().transact(() -> {
      User latest = user.reload();
      latest.signupAddress = signupAddress;
      latest.save();
    });
    logger.debug("geotagged user: {}, address = {}", user, user.signupAddress);
  }

  /**
   * Make a synchronous call to the Google geocoding API to reverse geocode the location.
   *
   * @return an address structure or null on error or timeout.
   */
  public static Address reverseGeocodeNow(Coordinates location) {
    try {
      return reverseGeocode(location).get(5, TimeUnit.SECONDS);
    } catch (Exception e) {
      logger.error("Reverse geocoding error", e);
      return null;
    }
  }

  /**
   * Make an asynchronous call to the Google geocoding API to reverse geocode the location.
   */
  public static Future<Address> reverseGeocode(Coordinates location) {
    SettableFuture<Address> future = SettableFuture.create();
    GeocodingApiRequest request = GeocodingApi.reverseGeocode(geoApiContext,
        new LatLng(location.latitude, location.longitude));
    request.setCallback(new PendingResult.Callback<GeocodingResult[]>() {
      @Override public void onResult(GeocodingResult[] geocodingResults) {
        if (geocodingResults.length == 0) {
          future.set(null);
          return;
        }
        GeocodingResult result = geocodingResults[0];
        Address address = new Address();
        for (AddressComponent addressComponent : result.addressComponents) {
          for (AddressComponentType type : addressComponent.types) {
            switch (type) {
              case LOCALITY:
                address.city = addressComponent.longName;
                break;
              case ADMINISTRATIVE_AREA_LEVEL_1:
                address.state = addressComponent.shortName;
                break;
              case COUNTRY:
                address.country = addressComponent.longName;
                break;
            }
            if (address.isComplete()) break;
          }
        }
        future.set(address);
      }

      @Override public void onFailure(Throwable throwable) {
        future.setException(throwable);
      }
    });

    // TODO: Modify Unblock so we can monitor this future.
    return future;
  }

  public static void main(String[] args) {
    System.out.println(geocodeZipCode("94107"));
  }
}

