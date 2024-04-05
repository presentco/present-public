package present.server.model.group;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CurvesTest {

  @Test public void testSimoidal() {
    assertEquals(1, Curves.sigmoidal(0), 0.01);
    assertEquals(0.92, Curves.sigmoidal(0.25), 0.01);
    assertEquals(0.5, Curves.sigmoidal(0.5), 0.01);
    assertEquals(0.08, Curves.sigmoidal(0.75), 0.01);
    assertEquals(0, Curves.sigmoidal(1), 0.01);
  }

  @Test public void testExponential() {
    assertEquals(1, Curves.exponential(0), 0.01);
    assertEquals(0.29, Curves.exponential(0.25), 0.01);
    assertEquals(0.08, Curves.exponential(0.5), 0.01);
    assertEquals(0.02, Curves.exponential(0.75), 0.01);
    assertEquals(0.01, Curves.exponential(1), 0.01);
  }
}
