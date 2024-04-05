package present.server.tool;

import com.google.common.collect.ImmutableList;
import com.googlecode.objectify.Key;
import java.util.List;
import present.server.model.user.User;
import present.server.model.user.Users;

public class StagingUsers {

  public static User bobbie() {
    return Users.findByEmail("bobbie_mhwbkat_lee@tfbnw.net");
  }

  public static User janete() {
    return User.get("51d730db-c8d0-4097-9f15-fa542526e2d6");
  }

  public static User bob() { return User.get("4b7e8518-a0cb-4a25-ac0b-aab5f7e916b8"); }

  public static User gabrielle() { return User.get("74c33d8f-d1c4-478d-b3e9-40fa5be36e8c"); }

  public static User kassia() { return User.get("851462a3-ce9e-44eb-8d85-2a18a2cb8792"); }

  public static User kristina() { return User.get("b50fb220-b83e-4061-9394-9a2f165f6187"); }

  public static User pegah() { return User.get("74c89bc5-419b-4004-89ce-6ee1eaa87044"); }

  public static User lisa() { return Users.findByPhone("16037708302"); }

  public static User pat() { return User.get("f3fde0f3-ccd1-4736-8e97-1fa42391b971"); }

  public static List<Key<User>> adminKeys() {
    return ImmutableList.of(
        janete().getKey(),
        bob().getKey(),
        gabrielle().getKey(),
        kassia().getKey(),
        kristina().getKey(),
        pegah().getKey(),
        lisa().getKey(),
        pat().getKey());
  }
}
