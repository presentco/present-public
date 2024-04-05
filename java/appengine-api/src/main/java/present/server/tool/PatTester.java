package present.server.tool;

import present.proto.FriendshipState;
import present.proto.GroupMembershipState;
import present.server.model.console.whitelist.WhitelistedUser;
import present.server.model.group.Group;
import present.server.model.group.GroupMembership;
import present.server.model.group.Groups;
import present.server.model.user.Friendship;
import present.server.model.user.User;
import present.server.model.user.Users;

import static com.googlecode.objectify.ObjectifyService.ofy;

public class PatTester {
  static String firstName = "Lisa";
  static String lastName = "Zeitouni";
  static String email = "lisawray@gmail.com";
  static String phone = "16037708302";

//  static String firstName = "Gabrielle";
//  static String lastName = "Taylor";
//  static String email = "gabrielle.taylor121@gmail.com";

//  static String email = "pat_etdivui_tester@tfbnw.net";
//  static String firstName = "Pat";
//  static String lastName = "Tester";
  //static String email = "pat@pat.net";

  static String server = RemoteTool.STAGING_SERVER;
  //static String server = RemoteTool.DEV_SERVER;

  public static class Reset {
    public static void main(String[] args) {
      RemoteTool.against(server, () -> {
        //User user = Users.findByEmail(email);
        User user = Users.findByPhone(phone);
        //User user = Users.findOneByNameIncluding(firstName + " " + lastName);
        if (user == null) return;
        System.out.println("Removing user and data.");
        Users.cascadingDelete(user.getKey());
      });
      RemoveFromWhitelist.main(args);
    }
  }

  public static class Whitelist {
    public static void main(String[] args) {
      RemoteTool.against(server, () -> {
        WhitelistedUser wu = WhitelistedUser.findByEmail(email);
        if (wu != null) { return; }
        System.out.println("Adding to whitelist");
        wu = new WhitelistedUser("phone", email, firstName, lastName, true);
        ofy().save().entity(wu).now();
      });
    }
  }

  public static class RemoveFromWhitelist {
    public static void main(String[] args) {
      RemoteTool.against(server, () -> {
        WhitelistedUser wu = WhitelistedUser.findByEmail(email);
        if (wu == null) { return; }
        System.out.println("Removing from whitelist");
        wu.deleteHard().now();
      });
    }
  }

  public static class MakeAdmin {
    public static void main(String[] args) {
      RemoteTool.against(server, () -> {
        User user = Users.findByEmail(email);
        if(user == null) { return; }
        System.out.println("Making admin.");
        user.privileges.isAdmin = true;
        user.save().now();
      });
    }
  }
  public static class ResetAdmin {
    public static void main(String[] args) {
      RemoteTool.against(server, () -> {
        User user = Users.findByEmail(email);
        if(user == null) { return; }
        System.out.println("Reset admin.");
        user.privileges.isAdmin = false;
        user.save().now();
      });
    }
  }

  public static class RequestGroupMembership {
    public static void main(String[] args) {
      RemoteTool.against(server, () -> {
        User user = Users.search("Lisatest4", 1).get(0);
        Group group = Groups.findOneByTitle("Lisa Members Test");
        GroupMembershipState requestedState = GroupMembershipState.REQUESTED;
        
        GroupMembership groupMembership = GroupMembership.getOrCreate(user, group);
        groupMembership.changeState(requestedState);
        System.out.println("Requested membership state " + requestedState + " for user " + user.name() + " in group " + group.title);
      });
    }
  }

  public static class RequestFriendship {
    public static void main(String[] args) {
       RemoteTool.against(server, () -> {
         User fromUser = Users.search("Lisatest4", 1).get(0);
         User toUser = Users.search("Zeitouni", 1).get(0);
         FriendshipState friendshipState = Friendship.addFriend(fromUser, toUser);
         System.out.println("Friendship state is now " + friendshipState + " for user " + fromUser.name() + " to user " + toUser.name());
       });
    }
  }

}
