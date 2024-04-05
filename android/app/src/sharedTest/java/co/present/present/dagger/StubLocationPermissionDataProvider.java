package co.present.present.dagger;

import android.app.Activity;
import android.content.Context;

import co.present.present.location.LocationPermissions;

/**
 * Stub location permission to skip Android OS for runtime permissions.
 */
public class StubLocationPermissionDataProvider extends LocationPermissions {
    @Override
    public boolean isGranted(Context context) {
        return true;
    }

    @Override
    public boolean shouldShowRationale(Activity activity) {
        return false;
    }

    @Override
    public void requestPermission(Activity activity, int requestCode) {
        // no-op
    }
}
