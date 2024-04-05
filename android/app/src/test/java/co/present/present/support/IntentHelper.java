package co.present.present.support;

import android.app.Activity;
import android.content.Intent;

import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowIntent;

import static org.robolectric.Shadows.shadowOf;

public class IntentHelper {
    public static ShadowIntent getLastStartedIntent(Activity activity) {
        ShadowActivity shadowActivity = shadowOf(activity);
        Intent intent = shadowActivity.getNextStartedActivity();
        return shadowOf(intent);
    }
}
