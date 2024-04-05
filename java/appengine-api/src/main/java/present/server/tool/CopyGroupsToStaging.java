package present.server.tool;

import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import present.server.model.content.Content;
import present.server.model.group.Group;
import present.server.model.group.Groups;
import present.server.model.user.User;

import static com.googlecode.objectify.ObjectifyService.ofy;

public class CopyGroupsToStaging {

  public static void main(String[] args) {
    List<Group> groups = new ArrayList<>();

    RemoteTool.against(RemoteTool.PRODUCTION_SERVER, () -> {
      groups.addAll(Lists.newArrayList(Groups.all()));
    });

    RemoteTool.against(RemoteTool.STAGING_SERVER, () -> {
      User bob = User.get("4b7e8518-a0cb-4a25-ac0b-aab5f7e916b8");
      groups.stream().forEach(g -> {
        g.id = g.uuid(); // Strip off location.
        g.owner = bob.getRef();
        g.memberCount = 1;
        if (g.coverContent != null) {
          g.coverContent = Ref.create(Key.create(Content.class,
              g.coverContent.get().uuid));
        }
      });
      List<Content> contents = groups.stream().filter(Group::hasCoverContent)
          .map(g -> g.coverContent.get()).filter(Objects::nonNull).collect(Collectors.toList());
      ofy().save().entities(contents);
      ofy().save().entities(groups);
    });
  }
}
