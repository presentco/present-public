package present.server.model.group;

import com.google.common.primitives.Doubles;
import javax.annotation.Nullable;

/**
 * A group ranked relative to other groups. The ranking is some combination of a group's score
 * and distance (determined by {@link GroupRanker}).
 *
 * @author Bob Lee (bob@present.co)
 */
public class RankedGroup implements Comparable<RankedGroup> {

  final GroupRanker.Input input;

  /** Factor multiplied by the distance to determine the final ranking. Lower is better. */
  @Nullable final GroupScore score;

  /** Used to rank the group. */
  final double ranking;

  public RankedGroup(GroupRanker.Input input, GroupScore score, double ranking) {
    this.input = input;
    this.score = score;
    this.ranking = ranking;
  }

  /** Distance combined with score. */
  public double ranking() {
    return this.ranking;
  }

  /** Distance to the group in m. */
  public double distance() {
    return this.input.distance;
  }

  public Group group() {
    return input.group;
  }

  @Override public int compareTo(RankedGroup other) {
    return Doubles.compare(ranking(), other.ranking());
  }
}
