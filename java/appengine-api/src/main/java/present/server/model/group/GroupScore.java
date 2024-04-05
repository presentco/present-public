package present.server.model.group;

/**
 * A group's score. Lower is better. Some ranking algorithms score a group and then multiply
 * the score by the group's distance to determine the final ranking. This classes exposes the
 * individual components of the score to facilitate debugging.
 *
 * @author Bob Lee (bob@present.co)
 */
public class GroupScore {

  public final double logFactor;
  public final double creationFactor;
  public final double memberFactor;
  public final double commentFactor;
  public final double lastCommentFactor;
  public final double combined;

  private GroupScore(Builder builder) {
    logFactor = builder.logFactor;
    creationFactor = builder.creationFactor;
    memberFactor = builder.memberFactor;
    commentFactor = builder.commentFactor;
    lastCommentFactor = builder.lastCommentFactor;
    combined = logFactor * creationFactor * memberFactor * commentFactor * lastCommentFactor;
  }

  public Builder builder() {
    return new Builder()
        .logFactor(logFactor)
        .creationFactor(creationFactor)
        .memberFactor(memberFactor)
        .commentFactor(commentFactor)
        .lastCommentFactor(lastCommentFactor);
  }

  public static final class Builder {
    private double logFactor = 1;
    private double creationFactor = 1;
    private double memberFactor = 1;
    private double commentFactor = 1;
    private double lastCommentFactor = 1;

    public Builder() {}

    public Builder logFactor(double logFactor) {
      this.logFactor = logFactor;
      return this;
    }

    public Builder creationFactor(double creationFactor) {
      this.creationFactor = creationFactor;
      return this;
    }

    public Builder memberFactor(double memberFactor) {
      this.memberFactor = memberFactor;
      return this;
    }

    public Builder commentFactor(double commentFactor) {
      this.commentFactor = commentFactor;
      return this;
    }

    public Builder lastCommentFactor(double lastCommentFactor) {
      this.lastCommentFactor = lastCommentFactor;
      return this;
    }

    public GroupScore build() {
      return new GroupScore(this);
    }
  }
}
