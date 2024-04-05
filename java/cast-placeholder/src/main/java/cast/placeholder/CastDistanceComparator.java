package cast.placeholder;

import present.s2.AbstractDistanceComparator;
import com.google.common.geometry.S2LatLng;

/**
 * @author Bob Lee (bob@present.co)
 */
public class CastDistanceComparator extends AbstractDistanceComparator<Cast> {

  public CastDistanceComparator(S2LatLng origin) {
    super(origin);
  }

  @Override protected S2LatLng locationOf(Cast cast) {
    return S2LatLng.fromDegrees(cast.latitude, cast.longitude);
  }
}
