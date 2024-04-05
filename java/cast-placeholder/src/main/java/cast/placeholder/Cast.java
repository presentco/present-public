package cast.placeholder;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;

/**
 * @author Bob Lee (bob@present.co)
 */
@Entity @Cache public class Cast extends BaseEntity {

  @Id public String id;

  @Index @Load public Ref<Client> creator;

  public long creationTime;

  /** Days since the Unix epoch. */
  @Index public long day;

  /** S2 leaf cell ID for location. See S2CellId. */
  @Index public long s2CellId;

  public double latitude;
  public double longitude;

  public double accuracy;

  /** Deleted by user. */
  public boolean deleted;

  public boolean censored;

  public boolean visible() {
    return !deleted && !censored && !creator.get().censored;
  }

  public String imageUrl() {
    return GoogleCloudStorage.urlFor(GoogleCloudStorage.fileForImage(id));
  }

  public String mapImageUrl(int width, int height) {
    return "https://maps.googleapis.com/maps/api/staticmap"
        + "?size=" + width + "x" + height
        + "&maptype=roadmap"
        + "&markers=color:red%7Clabel:S%7C" + latitude + "," + longitude
        + "&key=AIzaSyD9aobC_4paJkUyGhNjwVcM0IBKslU0aKE";
  }
}
