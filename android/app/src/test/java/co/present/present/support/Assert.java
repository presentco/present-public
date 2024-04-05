package co.present.present.support;


import android.view.View;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

public class Assert
{
    public static void assertViewIsVisible(View view)
    {
        assertNotNull(view);
        assertThat(view.getVisibility(), equalTo(View.VISIBLE));
    }

    public static void assertViewIsInvisible(View view)
    {
        assertNotNull(view);
        assertThat(view.getVisibility(), equalTo(View.INVISIBLE));
    }

    public static void assertViewIsGone(View view)
    {
        assertNotNull(view);
        assertThat(view.getVisibility(), equalTo(View.GONE));
    }
}