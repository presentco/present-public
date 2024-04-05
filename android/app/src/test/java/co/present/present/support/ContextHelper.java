package co.present.present.support;

import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;
import android.view.ViewGroup;

import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

public class ContextHelper
{
    private static Context genericContext()
    {
        return RuntimeEnvironment.application.getApplicationContext();
    }

    public static AppCompatActivity genericActivity ()
    {
        return Robolectric.buildActivity( AppCompatActivity.class )
                          .create()
                          .start()
                          .resume()
                          .get();
    }

    public static ViewGroup genericViewGroup ()
    {
        return new ViewGroup( genericContext() )
        {
            @Override
            protected void onLayout (boolean changed,
                                     int l,
                                     int t,
                                     int r,
                                     int b)
            {

            }
        };
    }
}