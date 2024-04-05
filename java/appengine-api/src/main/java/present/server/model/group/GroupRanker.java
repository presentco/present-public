package present.server.model.group;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.util.Set;
import present.proto.GroupLog;
import present.server.Time;
import present.server.model.comment.Comment;
import present.server.model.user.User;

/**
 * Algorithms for ranking groups.
 *
 * @author Bob Lee (bob@present.co)
 */
public enum GroupRanker {

  /** Ranks nearby feed by time. Simple, but easy to spam. */
  NEARBY_FEED_BY_TIME {
    @Override RankedGroup rank(Input input) {
      // The last comment time or the group creation time if there are no comments.
      double ranking = -input.group.lastCommentTime;
      return new RankedGroup(input, null, ranking);
    }

    @Override boolean usesLogs() {
      return false;
    }

    @Override boolean usesFriends() {
      return false;
    }

    @Override Class<?>[] loadGroups() {
      return new Class<?>[] { Group.LastComment.class };
    }

    @Override int retrieve(int limit) {
      return limit;
    }
  },

  /** Starts with the baseline scoring and adds extra emphasis to the last comment time. */
  NEARBY_FEED {
    @Override RankedGroup rank(Input input) {
      Comment lastComment = input.group.lastSignificantComment.get();
      double delta = timeFactor(lastComment.creationTime) * 0.2;
      if (input.friendIds.contains(lastComment.getAuthor().shortId)) {
        delta *= FRIEND_FACTOR;
        input.group.involvesFriends = true;
      }
      GroupScore score = score(input).builder().lastCommentFactor(1 - delta).build();
      return new RankedGroup(input, score, score.combined * input.distance);
    }

    @Override Class<?>[] loadGroups() {
      return new Class<?>[] { Group.LastComment.class };
    }

    @Override boolean usesLogs() {
      return true;
    }

    @Override boolean usesFriends() {
      return true;
    }

    @Override int retrieve(int limit) {
      return limit * 3 / 2;
    }
  },

  /** Ranks groups based on our default scoring method. */
  EXPLORE {
    @Override RankedGroup rank(Input input) {
      GroupScore score = score(input);
      return new RankedGroup(input, score, score.combined * input.distance);
    }

    @Override boolean usesLogs() {
      return true;
    }

    @Override boolean usesFriends() {
      return true;
    }

    @Override int retrieve(int limit) {
      return limit * 3 / 2;
    }
  };

  /** If it's a friend, boost the effect 2X. */
  private static final int FRIEND_FACTOR = 2;

  /** Ranks the given group. Lower rankings are better. */
  abstract RankedGroup rank(Input input);

  /** Returns true if this ranking uses group logs. */
  abstract boolean usesLogs();

  /** Returns true if this ranking uses friends. */
  abstract boolean usesFriends();

  Class<?>[] loadGroups() {
    return new Class<?>[0];
  }

  /** Specifies the number of entities to retrieve from the datastore. */
  abstract int retrieve(int limit);

  /** Scores the given group. */
  private static GroupScore score(Input input) {
    // TODO: Look for friends as group members. Maybe use a bitset and/or bloom filter?
    double logFactor = input.log == null ? 1.0 : logFactor(input);
    double creationFactor = creationFactor(input);
    // Reduce distance by up to 50% each depending on member and comment counts.
    double memberFactor = demote(input.group.memberCount, 100, 0.5);
    // We already boost the ranking based on up to 5 comments from the log. This gives
    // us a little extra boost when more comments are present (without accounting for time).
    double commentFactor = demote(input.group.activeComments, 100, 0.5);
    return new GroupScore.Builder()
        .logFactor(logFactor)
        .creationFactor(creationFactor)
        .memberFactor(memberFactor)
        .commentFactor(commentFactor)
        .build();
  }

  /** Creates a factor that starts at 1 and converges on 0.5 at scale. */
  private static double demote(int value, int scale, double maximumImpact) {
    return Curves.exponential(((double) value) / scale) * maximumImpact + (1 - maximumImpact);
  }

  /**
   * Returns the product of the scoring factors for the events in the given log. Lower is better.
   */
  private static double logFactor(Input input) {
    // The log contains up to 20 events, up to 5 of each type.
    // The effects of events compound. Is this what we want?
    // We could add them together instead and set a maximum effect for the entire log.
    return input.log.log.entries.stream()
        .mapToDouble(e -> logFactor(input, e))
        .reduce(1, (a, b) -> a * b);
  }

  /**
   * Returns the scoring factor for the given event. Lower is better.
   */
  private static double logFactor(Input input, GroupLog.Entry entry) {
    double delta = deltaFor(entry.type);

    // If this is a friend, increase the effect.
    if (input.friendIds.contains(entry.userId)) {
      delta *= FRIEND_FACTOR;

      // Set a flag so we can add a "Friends" category later.
      switch (entry.type) {
        case JOIN:
        case COMMENT:
        case INVITE:
          input.group.involvesFriends = true;
      }
    }

    // Scale down with distance. Events > 256km away from the group have no effect.
    if (entry.distance != null) {
      delta *= Curves.sigmoidal(entry.distance / 256_000);
    }

    // Scale down over time. Events longer than a month ago have no effect.
    delta *= timeFactor(entry.timestamp);

    return 1 - Math.min(delta, 1);
  }

  /**
   * Returns the baseline delta for the given event type. This will be subtracted from 1 to
   * determine the scoring factor. Higher is better.
   */
  private static double deltaFor(GroupLog.Entry.Type type) {
    switch (type) {
      case OPEN: return 0.1;
      case COMMENT: return 0.1;
      case DELETE_COMMENT: return -0.1;
      case JOIN: return 0.1;
      case LEAVE: return -0.1;
      case INVITE: return 0.1;
      default: throw new AssertionError();
    }
  }

  /** Computes a scoring factor based on the group's creation. */
  private static double creationFactor(Input input) {
    Group group = input.group;
    // If it was just created, it will appear 25% closer, 50% closer if a friend created it.
    double delta = timeFactor(group.createdTime) * 0.25;
    if (input.friendIds.contains(group.owner.get().shortId)) {
      delta *= FRIEND_FACTOR;
      input.group.involvesFriends = true;
    }
    return 1 - Math.min(delta, 1);
  }

  /** Returns a value between 0 and 1 depending on how long ago timestamp occurred. Now = 1. */
  static double timeFactor(long timestamp) {
    long age = Math.max(System.currentTimeMillis() - timestamp, 0);
    return Curves.sigmoidal(((double) age) / Time.MONTH_IN_MILLIS);
  }

  static class Input {

    final User user;
    final Set<Long> friendIds;
    final Group group;
    final Group.Log log;
    final double distance;

    Input(User user, Iterable<User> friends, Group group, Group.Log log,
        double distance) {
      this.user = user;
      this.friendIds = Sets.newHashSet(Iterables.transform(friends, f -> f.shortId));
      this.group = group;
      this.log = log;
      this.distance = distance;
    }
  }
}
