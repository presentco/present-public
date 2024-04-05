package present.server.model.util;

import com.google.common.geometry.S2CellId;
import com.google.common.geometry.S2LatLng;
import com.googlecode.objectify.annotation.Index;

/**
 * A location associated with a bubble.
 *
 * @author Bob Lee (bob@present.co)
 */
public class SuggestedLocation {

  /** Source of the location. */
  public present.proto.SuggestedLocation.Source source;

  /** S2 leaf cell ID for location. See S2CellId. */
  @Index public long s2CellId;

  /** Location suggested by search. */
  public Coordinates location;

  /** Radius suggested by search. */
  public double radius;

  public SuggestedLocation(present.proto.SuggestedLocation proto) {
    this.source = proto.source;
    S2CellId cellId = S2CellId.fromLatLng(
        S2LatLng.fromDegrees(proto.location.latitude, proto.location.longitude));
    this.s2CellId = cellId.id();
    this.location = new Coordinates(proto.location);
    this.radius = proto.radius;
  }

  public SuggestedLocation() {}
}
