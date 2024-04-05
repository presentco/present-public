package present.server.model.group;

import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static present.server.model.group.Category.Dynamic.FRIENDS;
import static present.server.model.group.Category.Dynamic.NEW;
import static present.server.model.group.Category.Dynamic.WOMEN_ONLY;

/**
 * Specifies valid values for Categories and returns string constants for each.
 */
public enum Category {

  ATTEND("Attend", LegacyLabel.EVENTS),
  ORGANIZE("Organize", LegacyLabel.COMMUNITIES),
  EAT_DRINK("Eat & Drink", LegacyLabel.FOOD_DRINK),
  EXERCISE("Exercise", LegacyLabel.FITNESS),
  LIVE("Live", LegacyLabel.HEALTH),
  LEARN("Learn", null),
  SHOP("Shop", LegacyLabel.STYLE),
  VOLUNTEER("Volunteer", LegacyLabel.SOCIAL_GOOD),
  WORK("Work", LegacyLabel.CAREER),
  ;

  private final String label;
  private final String legacyLabel;

  private static Map<String, Category> byLabel = Maps.uniqueIndex(Arrays.asList(values()), c -> c.label);

  Category(String label, String legacyLabel) {
    this.label = label;
    this.legacyLabel = legacyLabel;
  }

  public String label() {
    return this.label;
  }

  public static List<Category> asList() {
    return Arrays.asList(values());
  }

  public static List<String> names() {
    return Arrays.stream(values()).map(Category::label).collect(Collectors.toList());
  }

  public static boolean isValid(String name) {
    return byLabel.containsKey(name);
  }

  /**
   * Maps an input label to a current label.
   */
  public static String map(String input) {
    input = input.trim();

    switch (input) {
      case LegacyLabel.CAREER: return WORK.label;
      case LegacyLabel.COMMUNITIES: return ORGANIZE.label;
      case LegacyLabel.EVENTS: return ATTEND.label;
      case LegacyLabel.FAMILY: return null;
      case LegacyLabel.FITNESS: return EXERCISE.label;
      case LegacyLabel.FOOD_DRINK: return EAT_DRINK.label;
      case LegacyLabel.HEALTH: return LIVE.label;
      case LegacyLabel.SOCIAL_GOOD: return VOLUNTEER.label;
      case LegacyLabel.STYLE: return SHOP.label;
      case LegacyLabel.WOMAN_OWNED: return SHOP.label;

      case "Connect": return ORGANIZE.label;
      case "Style & Beauty": return SHOP.label;
      case "Lifestyle": return ORGANIZE.label;
      case "News": return ATTEND.label;

      default: return input;
    }
  }

  private static class LegacyLabel {
    private static final String CAREER = "Career";
    private static final String COMMUNITIES = "Communities";
    private static final String EVENTS = "Events";
    private static final String FAMILY = "Family";
    private static final String FITNESS = "Fitness";
    private static final String FOOD_DRINK = "Food & Drink";
    private static final String HEALTH = "Health";
    private static final String SOCIAL_GOOD = "Social Good";
    private static final String STYLE  = "Style";
    private static final String WOMAN_OWNED = "Woman Owned";
  }

  /** Returns true if the given category is dynamic. */
  public static boolean isDynamic(String category) {
    switch (category) {
      case NEW:
      case WOMEN_ONLY:
      case FRIENDS:
        return true;
    }
    return false;
  }

  /** Categories that are applied on the fly, not stored in the database. */
  public static class Dynamic {

    /** Category for circles created since the user's last session. */
    public static final String NEW = "New";

    /** Category for circles in the women-only space. */
    public static final String WOMEN_ONLY = "Women-Only";

    /** Category for friends' circles. */
    public static final String FRIENDS = "Friends";
  }
}
