package present.server.model.console.users;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import present.proto.Platform;
import present.server.model.activity.GroupReferrals;
import present.server.model.user.Client;
import present.server.model.user.User;
import present.server.model.user.UserState;

/**
 * @author pat@pat.net
 * Date: 9/7/17
 */
class UserJson {
  public String uuid; // The uuid of the whitelist (or user) entity
  public String photo;
  public String firstName;
  public String lastName;
  public String phone;
  public String email;
  public String facebookLink; // Profile URL
  public long signupTime;
  public Boolean notificationsEnabled;
  public long lastActivityTime;
  public String signupCity;
  public String signupState;
  public double signupLat, signupLong, signupLocationAccuracy;
  public String presentLink; // Profile URL
  public String userState;
  public String review;
  public String[] clients;
  public String[] availableActions;
  public String debugString;
  public Long referrals;

  public UserJson(Collection<Client> clients, User user, Long referrals) {
    // todo:
    if (user.state == null) {
      user.state = UserState.SIGNING_UP;
    }

    uuid = user.uuid;
    this.photo = user.profilePhotoUrl();
    this.firstName = user.firstName;
    this.lastName = user.lastName;
    this.signupTime = user.signupTime;
    this.facebookLink = user.facebookLink();

    if (this.email == null) this.email = user.email();
    if (this.phone == null) this.phone = user.phoneNumber;

    // Compute last updated time.
    this.lastActivityTime = Long.MIN_VALUE;
    clients.forEach(c -> {
      if (c.deviceTokenUpdateTime > this.lastActivityTime) {
        this.lastActivityTime = c.deviceTokenUpdateTime;
      }
    });
    if (user.updatedTime > lastActivityTime) {
      this.lastActivityTime = user.updatedTime;
    }

    this.notificationsEnabled = clients.stream()
        .map(client -> client.notificationsEnabled)
        .filter(Predicates.notNull())
        .reduce(Boolean::logicalOr)
        .orElse(null);

    this.presentLink = user.shortLink();
    this.userState = user.state.toString();
    this.debugString = user.debugString();

    List<String> adminActions = new ArrayList<>();
    for (UserState state : user.state.validAdminTransitions()) {
      //String adminAction = String.format("<a href=\"%s\">%s</a>", state.adminVerb(), state.adminVerb() );
      adminActions.add(state.adminVerb());
    }
    this.availableActions = adminActions.toArray(new String[0]);

    this.review = user.review == null ? null : user.review.description;

    if (user.signupLocation != null) {
      this.signupLat = user.signupLocation.latitude;
      this.signupLong = user.signupLocation.longitude;
      this.signupLocationAccuracy = user.signupLocation.accuracy;
    }
    if (user.signupAddress != null) {
      this.signupCity = user.signupAddress.city;
      this.signupState = user.signupAddress.state;
    }

    this.clients = Lists.newArrayList(clients.stream().filter(c -> c.platform != 0)
        .map(UserJson::platformToString)
        .collect(Collectors.toSet()))
        .toArray(new String[0]);

    this.referrals = referrals;
  }

  private static String platformToString(Client client) {
    String s = client.platform().name();
    if (client.clientVersion != null) s = s + " " + client.clientVersion;
    return s;
  }
}
