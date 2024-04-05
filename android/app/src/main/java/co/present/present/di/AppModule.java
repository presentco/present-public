package co.present.present.di;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;

import com.facebook.CallbackManager;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import org.threeten.bp.Instant;

import javax.inject.Singleton;

import co.present.present.analytics.Analytics;
import co.present.present.config.FeatureDataProvider;
import co.present.present.feature.onboarding.OnboardingDataProvider;
import co.present.present.location.LocationDataProvider;
import co.present.present.service.BitmapDownloader;
import co.present.present.service.Filesystem;
import co.present.present.service.RpcManager;
import co.present.present.user.UserDataProvider;
import dagger.Module;
import dagger.Provides;

import static android.content.Context.MODE_PRIVATE;

@Module(includes = ViewModelModule.class)
public class AppModule {
    protected final Application application;

    public AppModule(Application application) {
        this.application = application;
    }

    @Provides @Singleton
    Context provideContext() {
        return this.application;
    }

    @Provides @Singleton
    Application provideApplication() {
        return application;
    }

    @Provides @Singleton
    SharedPreferences providesSharedPreferences(Application application) {
        return application.getApplicationContext()
                .getSharedPreferences("co.present.PREFERENCE_FILE_KEY", MODE_PRIVATE);
    }

    @Provides @Singleton
    Bus provideBus() {
        return new Bus(ThreadEnforcer.ANY);
    }

    @Provides @Singleton
    CallbackManager provideCallbackManager() {
        return CallbackManager.Factory.create();
    }

    @Provides @Singleton
    LoginManager provideLoginManager() {
        return LoginManager.getInstance();
    }

    @Provides @Nullable
    String provideFacebookId() {
        Profile profile = Profile.getCurrentProfile();
        return profile == null ? null : profile.getId();
    }

    @Provides
    Instant provideCurrentTime() {
        return Instant.now();
    }

    @Provides @Singleton
    OnboardingDataProvider provideOnboardingDataProvider(LocationDataProvider locationDataProvider,
                                                         Application application,
                                                         RpcManager rpcManager, UserDataProvider userDataProvider, Bus bus, SharedPreferences preferences) {
        return new OnboardingDataProvider(rpcManager, locationDataProvider, userDataProvider, bus, preferences, application);
    }

    @Provides @Singleton
    Analytics provideAnalytics(FeatureDataProvider featureDataProvider, Application application) {
        return new Analytics(featureDataProvider, application);
    }

    @Provides
    Filesystem provideFilesystem() {
        return new Filesystem();
    }

    @Provides
    BitmapDownloader provideBitmapDownloader() {
        return new BitmapDownloader();
    }
}
