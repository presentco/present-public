package present.server.tool;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.Gender;
import present.server.facebook.Facebook;
import present.server.facebook.FacebookUserData;
import present.server.model.user.User;
import present.server.model.user.Users;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static present.server.tool.RemoteTool.PRODUCTION_SERVER;
import static present.server.tool.RemoteTool.against;

public class PrintGenders {

  private static final Logger logger = LoggerFactory.getLogger(PrintGenders.class);

  public static void main(String[] args) {
    against(PRODUCTION_SERVER, () -> {
      List<User> users = Users.query().filter(User.Fields.facebookId + " !=", null).list();
      ImmutableMap<String, FacebookUserData> facebookData = Maps.uniqueIndex(
          ofy().load().type(FacebookUserData.class).list(), data -> data.id);
      Multiset<Gender> genders = HashMultiset.create();
      Multiset<String> reasons = HashMultiset.create();
      for (User user : users) {
        FacebookUserData facebook = facebookData.get(user.facebookId);
        Gender gender = Facebook.genderOf(facebook);
        genders.add(gender);
        if (gender == Gender.OTHER) {
          if (facebook == null) {
            reasons.add("missing facebook");
            continue;
          }
          if (facebook.gender == null) {
            reasons.add("missing gender");
            continue;
          }
          reasons.add("unrecognized gender");
          System.out.println(facebook.gender);
        }
      }
      System.out.println(genders);
      System.out.println(reasons);
    });
  }
}
