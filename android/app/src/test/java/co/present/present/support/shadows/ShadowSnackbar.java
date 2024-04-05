package co.present.present.support.shadows;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import com.google.android.material.snackbar.Snackbar;
import android.view.View;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;

/**
 * Shadow of the snackbar class, no-op
 */
@Implements(Snackbar.class)
public class ShadowSnackbar {

    @Implementation
    public static Snackbar make(@NonNull View view, @StringRes int resId, int duration) {
        return Shadow.newInstanceOf(Snackbar.class);
    }
}
