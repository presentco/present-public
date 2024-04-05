package present.server.model.group;

import java.util.List;
import java.util.concurrent.TimeUnit;
import present.server.Time;
import present.server.model.Space;
import present.server.model.util.Coordinates;
import present.server.tool.ProductionUsers;

import static present.server.tool.RemoteTool.PRODUCTION_SERVER;
import static present.server.tool.RemoteTool.against;

/**
 * Prints rankings.
 *
 * @author Bob Lee (bob@present.co)
 */
public class PrintRankings {
  public static void main(String[] args) {
    against(PRODUCTION_SERVER, () -> {
      ProductionUsers.janete().run(() -> {
        List<RankedGroup> rankedGroups = GroupSearch
            .near(Coordinates.PRESENT_COMPANY)
            .space(Space.EVERYONE)
            .using(GroupRanker.NEARBY_FEED)
            .limit(200)
            .run();
        for (RankedGroup scored : rankedGroups) {
          Group group = scored.group();
          System.out.println(group.title);
          System.out.println("Log factor:          " + scored.score.logFactor
              + " for " + scored.input.log.log.entries.size() + " entries");
          long age = System.currentTimeMillis() - group.createdTime;
          System.out.println("Creation factor:     " + scored.score.creationFactor
              + " created " + Time.describeDuration(age, TimeUnit.MILLISECONDS) + " ago");
          System.out.println("Member factor:       " + scored.score.memberFactor
              + " for " + group.memberCount + " members");
          System.out.println("Comment factor:      " + scored.score.commentFactor
              + " for " + group.activeComments + " comments");
          if (group.lastSignificantComment != null && group.lastSignificantComment.isLoaded()) {
            age = System.currentTimeMillis() - group.lastSignificantComment.get().createdTime;
            System.out.println("Last comment factor: " + scored.score.lastCommentFactor
                + " created " + Time.describeDuration(age, TimeUnit.MILLISECONDS) + " ago");
          }
          System.out.println("Distance:        " + scored.distance());
          System.out.println("Combined:        " + scored.ranking());
          System.out.println();
        }
      });
    });
  }
}
