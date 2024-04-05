package present.server.model.group;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Ordering;
import com.googlecode.objectify.Key;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.server.model.Space;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static java.util.stream.Collectors.toList;
import static present.server.tool.RemoteTool.PRODUCTION_SERVER;
import static present.server.tool.RemoteTool.against;

/**
 * Finds groups that are trending.
 *
 * @author Bob Lee (bob@present.co)
 */
public class TrendingGroups {

  private static final Logger logger = LoggerFactory.getLogger(TrendingGroups.class);

  public static List<RankedGroup> trendingIn(Space space) {
    // TODO: We can page these instead of loading them all into memory at once.
    Stopwatch sw = Stopwatch.createStarted();
    List<Group> all = Groups.active().list().stream()
        .filter(g -> g.space() == space)
        .collect(toList());
    List<Key<Group.Log>> logKeys = all.stream()
        .map(g -> Group.Log.keyFor(g.getKey()))
        .collect(toList());
    Map<Key<Group.Log>, Group.Log> logs = ofy().load().keys(logKeys);
    GroupRanker ranker = GroupRanker.EXPLORE;
    List<RankedGroup> rankedGroups = new ArrayList<>();
    for (Group group : all) {
      // Filter out welcome circles.
      if (group.title.toLowerCase().startsWith("welcome to ")) continue;

      Group.Log log = logs.get(Group.Log.keyFor(group.getKey()));
      RankedGroup ranked = ranker.rank(new GroupRanker.Input(
         null, Collections.emptyList(), group, log, 0
      ));
      rankedGroups.add(ranked);
    }
    Comparator<RankedGroup> comparator = (a, b) -> Double.compare(a.score.combined, b.score.combined);
    List<RankedGroup> top = Ordering.from(comparator).leastOf(rankedGroups, 50);
    logger.info("Found trending groups in {}.", sw);
    return top;
  }

  public static void main(String[] args) {
    against(PRODUCTION_SERVER, () -> {
      List<RankedGroup> top = trendingIn(Space.EVERYONE);
      for (RankedGroup ranked : top) {
        Group group = ranked.group();
        System.out.print(group.title);
        System.out.print('\t');
        System.out.print(ranked.score.combined);
        System.out.print('\t');
        System.out.print(group.locationName);
        System.out.print('\t');
        System.out.println(group.shortLink());
      }
    });
  }
}
