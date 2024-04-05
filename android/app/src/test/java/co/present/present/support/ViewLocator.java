package co.present.present.support;

import android.app.Activity;
import androidx.annotation.IdRes;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.facebook.login.widget.LoginButton;

public class ViewLocator {

    public static <T extends View> T getView(Activity activity, @IdRes int viewId) {
        return (T) activity.findViewById(viewId);
    }

    public static <T extends View> T getView(Fragment fragment, @IdRes int viewId) {
        return (T) fragment.getView().findViewById(viewId);
    }

    @Deprecated
    public static TextView getTextView(Activity activity, int viewId) {
        return (TextView) activity.findViewById(viewId);
    }

    @Deprecated
    public static TextView getTextView(Fragment fragment, int viewId) {
        return (TextView) getFragmentView(fragment).findViewById(viewId);
    }

    @Deprecated
    public static ImageView getImageView(Activity activity, int viewId) {
        return (ImageView) activity.findViewById(viewId);
    }

    @Deprecated
    public static ImageView getImageView(Fragment fragment, int viewId) {
        return (ImageView) getFragmentView(fragment).findViewById(viewId);
    }

    @Deprecated
    public static ImageButton getImageButton(Fragment fragment, int viewId) {
        return (ImageButton) getFragmentView(fragment).findViewById(viewId);
    }

    @Deprecated
    public static ImageButton getImageButton(Activity activity, int viewId) {
        return (ImageButton) activity.findViewById(viewId);
    }

    @Deprecated
    public static Button getButton(Activity activity, int viewId) {
        return (Button) activity.findViewById(viewId);
    }

    @Deprecated
    public static Button getButton(Fragment fragment, int viewId) {
        return (Button) getFragmentView(fragment).findViewById(viewId);
    }

    @Deprecated
    public static Toolbar getToolbar(Activity activity, int viewId) {
        return (Toolbar) activity.findViewById(viewId);
    }

    @Deprecated
    public static ViewPager getViewPager(Activity activity, int viewId) {
        return (ViewPager) activity.findViewById(viewId);
    }

    @Deprecated
    public static TabLayout getTabLayout(Activity activity, int viewId) {
        return (TabLayout) activity.findViewById(viewId);
    }

    @Deprecated
    public static FloatingActionButton getFAB(Activity activity, int viewId) {
        return (FloatingActionButton) activity.findViewById(viewId);
    }

    @Deprecated
    public static RecyclerView getRecyclerView(Activity activity, int viewId) {
        return (RecyclerView) activity.findViewById(viewId);
    }

    @Deprecated
    public static RecyclerView getRecyclerView(Fragment fragment, int viewId) {
        return (RecyclerView) getFragmentView(fragment).findViewById(viewId);
    }

    @Deprecated
    public static LoginButton getLoginButton(Activity activity, int viewId) {
        return (LoginButton) activity.findViewById(viewId);
    }

    @Deprecated
    public static ToggleButton getToggleButton(Fragment fragment, int viewId) {
        return (ToggleButton) getFragmentView(fragment).findViewById(viewId);
    }

    private static View getFragmentView(Fragment fragment) {
        return fragment.getView();
    }
}