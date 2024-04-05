package present.server.tool;

import present.server.ShortLinks;
import present.server.model.user.User;
import present.server.model.user.Users;

public class ProductionUsers {

  public static User bob() {
    return User.get("a75182f6-e86f-4985-8075-f618ce7f3d45");
  }

  public static User janete() {
    return User.get("49d32839-25a3-48a2-94c7-cbb7ae4c3c63");
  }

  public static User apple() {
    return User.get("444a21d9-7751-4e77-ae80-fea7b86280d2");
  }

  public static User chauntie() {
    return User.get("a078ff7d-f088-4894-b2fc-01f630c8ff2e");
  }

  public static User kristina() {
    return User.get("c9d59df4-21e2-4dcb-87cb-85877a969b4d");
  }

  public static User gabrielle() { return User.get("b9b562fa-bbd2-4726-801f-d90f0955087b"); }

  public static User kassia() { return User.get("e3d4a3aa-9fcf-4680-b9b6-7553309d1ed0"); }

  public static User pegah() { return User.get("21c77f7e-7fc3-4c17-ab25-1b910e05a927"); }

  public static User lisa() { return User.get("60d39587-2149-4abb-88b9-02ce1013aa95"); }

  public static User kayla() { return User.get("4663b077-cd2c-4ca8-9171-75fe39151106"); }

  public static User emma() { return User.get("e301217d-0d46-4efa-9366-e9aa32122b0b"); }

  public static User lydiaFromFastCompany() {
    return User.get("5c2abc3d-2631-4def-b775-d1fd366049a7");
  }

  public static User erinFromCnet() {
    return User.get("8a155eab-1f84-465a-8d11-50187eeef2a7");
  }

  public static User catFromWsj() {
    return User.get("d465e8ea-9a04-41f0-bb51-34f4ee6eec78");
  }


  public static void main(String[] args) {
    RemoteTool.against(RemoteTool.PRODUCTION_SERVER, () -> {
      bob().run(() -> {
        System.out.println(ShortLinks.toApp());
      });
    });
  }

  public static User pat() {
    return User.get("acce573d-f16a-47f3-84b6-61b34beb9409");
  }
}
