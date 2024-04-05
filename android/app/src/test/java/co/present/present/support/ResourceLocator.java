package co.present.present.support;

import android.graphics.drawable.Drawable;

import org.robolectric.RuntimeEnvironment;

public class ResourceLocator
{
    public static String getString(int stringId)
    {
        return RuntimeEnvironment.application.getString(stringId);
    }

    public static Drawable getDrawable(int drawableId)
    {
        return RuntimeEnvironment.application.getDrawable(drawableId);
    }
}