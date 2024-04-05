package co.present.present.di;

import android.app.Application;

import co.present.present.di.AppModule;
import dagger.Module;

@Module
public class TestAppModule extends AppModule {
    public TestAppModule(Application application) {
        super(application);
    }
}
