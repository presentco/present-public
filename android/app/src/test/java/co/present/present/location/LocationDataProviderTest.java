package co.present.present.location;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link LocationDataProvider}
 */

@Ignore
public class LocationDataProviderTest {
    private LocationDataProvider underTest;
    @Mock LocationPermissions locationPermissions;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        underTest = new LocationDataProvider(locationPermissions);
    }

    @Test
    public void formatDistance_Here() throws Exception {
        assertEquals("Here", LocationDataProviderKt.formatDistance(145f)); // 145m is 0.09mi
    }

    @Test
    public void formatDistance_oneTenthMile() throws Exception {
        assertEquals("0.1 mi", LocationDataProviderKt.formatDistance(161f)); // 161m is 0.1mi
    }

    @Test
    public void formatDistance_lessThanOneMile() throws Exception {
        assertEquals("0.6 mi", LocationDataProviderKt.formatDistance(1000f));
    }

    @Test
    public void formatDistance_manyMiles() throws Exception {
        assertEquals("17 mi", LocationDataProviderKt.formatDistance(26560));
    }
}