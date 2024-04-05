package present.server.tool;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.util.concurrent.Futures;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.wire.WireTypeAdapterFactory;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;
import present.proto.HomeModel;
import present.server.GroupsHtml;
import present.server.model.Space;
import present.server.model.group.Group;
import present.server.model.group.GroupRanker;
import present.server.model.group.GroupSearch;
import present.server.model.group.RankedGroup;
import present.server.model.user.User;
import present.server.model.user.Users;
import present.server.model.util.Address;
import present.server.model.util.Coordinates;

import static java.util.stream.Collectors.toList;

/**
 * @author Pat Niemeyer (pat@pat.net)
 * Date: 1/25/18
 */
public class GenerateGroupsJson extends RemoteTool {

  public static void main(String[] args) throws IOException {
    against(PRODUCTION_SERVER, () -> {
      User bob = Users.bob();
      bob.run(() -> {
        try {
          Address address = new Address();
          address.city = "San Francisco";
          Future<Address> futureAdress = Futures.immediateFuture(address);
          HomeModel homeModel = GroupsHtml.getHomeModel(bob,
              Coordinates.SAN_FRANCISCO, futureAdress, getGroups(GroupRanker.EXPLORE));
          for (HomeModel.Section section : homeModel.sections) {
            System.out.println(section.name);
          }
          writeJson("explore", homeModel);
          writeJson("nearby", GroupsHtml.getFeedModel(bob, Coordinates.SAN_FRANCISCO,
              futureAdress, getGroups(GroupRanker.NEARBY_FEED_BY_TIME)));
          System.out.println("Done.");
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    });
  }

  private static List<Group> getGroups(GroupRanker ranker) {
    return GroupSearch
        .near(Coordinates.SAN_FRANCISCO)
        .space(null)
        .using(ranker)
        .limit(100)
        .run()
        .stream()
        .map(RankedGroup::group)
        .collect(toList());
  }

  private static final Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(new WireTypeAdapterFactory())
      .setPrettyPrinting()
      .disableHtmlEscaping()
      .create();

  private static void writeJson(String name, Object o) throws IOException {
    Files.asCharSink(new File("java/appengine-api/src/main/resources/" + name + ".json"),
        Charsets.UTF_8).write(gson.toJson(o));
  }
}
