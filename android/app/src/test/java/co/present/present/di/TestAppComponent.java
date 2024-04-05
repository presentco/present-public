package co.present.present.di;

import android.app.Application;
import android.content.SharedPreferences;

import javax.inject.Singleton;

import co.present.present.feature.onboarding.OnboardingDataProvider;
import co.present.present.service.RpcManager;
import dagger.Component;

/**
 * Dagger component for testing the app.
 */
@Singleton
@Component(
        modules = {TestAppModule.class, TestServiceModule.class}
)
public interface TestAppComponent extends AppComponent {
    Application getApplication();

    OnboardingDataProvider getDataProvider();

    RpcManager getRpcManager();

    SharedPreferences getSharedPreferences();
}
