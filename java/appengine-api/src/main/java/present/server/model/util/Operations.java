package present.server.model.util;

import javax.annotation.Nullable;

/**
 * @author Pat Niemeyer (pat@pat.net)
 */
public class Operations {

  // Take the first argument if non-null, else return the second
  @Nullable public static <T> T firstIfNotNull(@Nullable T first, @Nullable T second) {
    return first != null?first:second;
  }
  // Return first if not null, else second if not null, else third.
  @Nullable public static <T> T firstIfNotNull(@Nullable T first, @Nullable T second, @Nullable T third) {
    return firstIfNotNull(firstIfNotNull(first, second), third);
  }
  // Return first if not null, else second if not null, else third.
  @Nullable public static Boolean firstIfNotNull(@Nullable Boolean first, @Nullable Boolean second, @Nullable Boolean third) {
    return firstIfNotNull(firstIfNotNull(first, second), third);
  }
  // Return first if not null, else second if not null, else third if not null, else the fourth.
  @Nullable public static <T> T firstIfNotNull(
      @Nullable T first, @Nullable T second, @Nullable T third, @Nullable T fourth) {
    return firstIfNotNull(firstIfNotNull(firstIfNotNull(first, second), third), fourth);
  }
  // Return first if not null, else second if not null, else third if not null, else the fourth.
  @Nullable public static Boolean firstIfNotNull(
      @Nullable Boolean first, @Nullable Boolean second, @Nullable Boolean third, @Nullable Boolean fourth) {
    return firstIfNotNull(firstIfNotNull(firstIfNotNull(first, second), third), fourth);
  }

}
