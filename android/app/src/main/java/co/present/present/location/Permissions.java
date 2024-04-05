package co.present.present.location;

import android.app.Activity;
import android.content.Context;

/**
 * Extracted interface for runtime Android permissions for testing.
 */
public interface Permissions {
    boolean isGranted(Context context);

    boolean shouldShowRationale(Activity activity);

    void requestPermission(Activity activity, int requestCode);
}


