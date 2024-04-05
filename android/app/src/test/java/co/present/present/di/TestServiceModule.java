package co.present.present.di;

import android.content.SharedPreferences;

import java.util.UUID;

import javax.inject.Singleton;

import co.present.present.config.FeatureDataProvider;
import co.present.present.dagger.StubLocationPermissionDataProvider;
import co.present.present.dagger.TestRpcManager;
import co.present.present.location.LocationDataProvider;
import co.present.present.location.LocationPermissions;
import co.present.present.service.RpcManager;
import dagger.Module;
import dagger.Provides;

@Module
public class TestServiceModule extends ServiceModule {

    final private TestRpcManager rpcManager;

    public TestServiceModule() {
        rpcManager = new TestRpcManager(UUID.randomUUID());
    }

    @Provides
    @Singleton
    @Override
    protected RpcManager provideRpcManager(SharedPreferences preferences,
                                           LocationDataProvider locationDataProvider,
                                           FeatureDataProvider featureDataProvider) {
        return rpcManager;
    }

    @Provides
    @Override
    protected FeatureDataProvider provideFeatureDataProvider(SharedPreferences preferences) {
        return new FeatureDataProvider(preferences);
    }

    @Provides @Singleton @Override
    protected LocationDataProvider provideLocationDataProvider(LocationPermissions locationPermissions) {
        return new LocationDataProvider(locationPermissions);
    }

    @Provides @Singleton @Override
    LocationPermissions provideLocationPermissions() {
        return new StubLocationPermissionDataProvider();
    }
}
