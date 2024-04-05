package present.server.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.RequestHeader;
import present.proto.SpaceResponse;
import present.server.RequestHeaders;
import present.server.model.user.User;
import present.server.model.user.Users;
import present.wire.rpc.core.ClientException;

/**
 * Spaces are collections of circles.
 *
 * @author Bob Lee (bob@present.co)
 */
public class Space {

  private static final Logger logger = LoggerFactory.getLogger(Space.class);

  // Note: Add new spaces to get() below!
  public static final Space EVERYONE = new Space("everyone", "Everyone");
  public static final Space WOMEN_ONLY = new Space("women-only", "Women Only");
  public static final Space PRESENT_INSIDERS = new Space("present-insiders", "Present Insiders");
  public static final Space POWER_RISING = new Space("power-rising", "Power Rising");

  public final String id;
  public final String name;

  private final SpaceResponse response;

  private Space(String id, String name) {
    this.id = id;
    this.name = name;
    this.response = new SpaceResponse(id, name);
  }

  public SpaceResponse toResponse() {
    return this.response;
  }

  public static Space get(String id) {
    if (id == null) return null;
    if (id.equals(WOMEN_ONLY.id)) return WOMEN_ONLY;
    if (id.equals(EVERYONE.id)) return EVERYONE;
    if (id.equals(PRESENT_INSIDERS.id)) return PRESENT_INSIDERS;
    if (id.equals(POWER_RISING.id)) return POWER_RISING;
    // TODO: This should sometimes be a server exception!
    throw new ClientException("Invalid space");
  }

  /**
   * Determines the selected space in priority order:
   *
   * 1. Space specified in RPC request
   * 2. Space specified in request headers
   *
   * @param requestedSpace space ID specified in RPC request, null if none provided
   *
   * @return the selected space, or null if none was specified
   */
  public static Space selected(String requestedSpace) {
    Space space;
    if (requestedSpace != null) {
      space = get(requestedSpace);
    } else {
      RequestHeader header = RequestHeaders.current();
      space = header == null ? null : Space.get(header.spaceId);
    }

    // Check access to this space.
    if (space != null && space != Space.EVERYONE) {
      // If it's not the everyone space, the user must be logged in.
      User user = Users.current();
      if (!user.canAccess(space)) {
        throw new ClientException("Unauthorized");
      }
    }
    return space;
  }

  public boolean isAccessibleBy(User user) {
    if (this == Space.EVERYONE) return true;

    if (user == null) return false;

    if (user.isAdmin()) return true;

    if (this == Space.WOMEN_ONLY) {
      // Only women can see women-only content.
      return user.isWoman();
    }

    return user.spaceIds != null && user.spaceIds.contains(id);
  }
}
