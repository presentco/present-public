package present.server.tool;

import com.google.common.base.Stopwatch;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import present.proto.GroupLog;
import present.server.model.group.Group;
import present.server.model.user.User;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Batch loads Group.Logs so they can be quickly searched.
 *
 * @author Bob Lee (bob@present.co)
 */
public class SearchGroupLogs {
  public static void main(String[] args) {
    RemoteTool.against(RemoteTool.PRODUCTION_SERVER, () -> {
      Stopwatch sw = Stopwatch.createStarted();
      User lydia = ProductionUsers.lydiaFromFastCompany();
      forEach(group -> {
        for (GroupLog.Entry entry : group.getLog().log.entries) {
          if (entry.userId == lydia.shortId && entry.type == GroupLog.Entry.Type.OPEN) {
            System.out.println(group.title);
          }
        }
      });
      System.out.println(sw);
    });
  }

  /**
   * Batch loads Group.Logs and invokes consumer for each group with a log.
   */
  private static void forEach(Consumer<Group> consumer) {
    Map<String, Group.Log> logs = ofy().load()
        .type(Group.Log.class)
        .list()
        .stream()
        .collect(Collectors.toMap(log -> log.id, log -> log));
    List<Group> groups = ofy().load().type(Group.class).list();
    for (Group group : groups) {
      Group.Log log = logs.get(group.uuid());
      if (log != null) consumer.accept(group);
    };
  }
}
