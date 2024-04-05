package present.server.tool;

import com.googlecode.objectify.Key;
import java.util.List;
import java.util.Map;
import present.server.model.comment.GroupView;
import present.server.model.comment.GroupViews;
import present.server.model.group.Group;
import present.server.model.group.WelcomeGroup;
import present.server.model.user.User;

import static present.server.tool.RemoteTool.PRODUCTION_SERVER;
import static present.server.tool.RemoteTool.against;

public class WelcomeGroupEngagement {
  public static void main(String[] args) {
    against(PRODUCTION_SERVER, () -> {
      for (WelcomeGroup welcomeGroup : WelcomeGroup.ALL) {
        Group group = welcomeGroup.group();
        List<Key<User>> members = group.memberKeys();
        Map<Key<User>, GroupView> views = GroupViews.viewsFor(group, members);
        int read = 0;
        for (GroupView view : views.values()) {
          if (view == null || view.lastRead == null) continue;
          // -1 because I just posted a comment
          if (view.lastRead >= group.lastCommentIndex() - 1) read++;
        }
        System.out.println(welcomeGroup.city + "\t" + (read * 100 / members.size()) + "% (" + members.size() + ")");
      }
    });
  }
}
