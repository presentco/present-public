package present.server.model.group;

/**
 * Declining functions that are scaled such that the results start at 1 when x = 0 and
 * asymptotically approach 0 as x approaches 1. The idea here is that you can replace them with
 * one another if you want a different dropoff style. Graphs of these functions can be found here:
 * https://goo.gl/AJUhDs
 *
 * @author Bob Lee (bob@present.co)
 */
public class Curves {

  private Curves() {}

  /**
   * A gradually decreasing function that converges with 0 around x = 1, with an inflection
   * point around 0.5.
   *
   * f(0)    ≈ 1
   * f(0.25) ≈ 0.92
   * f(0.5)  ≈ 0.5
   * f(0.75) ≈ 0.08
   * f(1)    ≈ 0
   */
  public static double sigmoidal(double x) {
    return 1 / (1 + Math.exp(x * 10 - 5));
  }

  /**
   * A steeply decreasing function that converges with 0 around x = 1.
   *
   * f(0)    ≈ 1
   * f(0.25) ≈ 0.29
   * f(0.5)  ≈ 0.08
   * f(0.75) ≈ 0.02
   * f(1)    ≈ 0.01
   */
  public static double exponential(double x) {
    return Math.exp(-x * 5);
  }
}
