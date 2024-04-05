package co.present.present;

import co.present.present.di.AppComponent;
import co.present.present.di.AppModule;
import co.present.present.di.DaggerAppComponent;
import co.present.present.di.TestAppModule;
import co.present.present.di.TestServiceModule;

public class TestPresentApplication extends PresentApplication {

    @Override
    protected AppComponent createAppComponent() {
        return DaggerAppComponent.builder()
                                 .appModule(new TestAppModule(this))
                                 .serviceModule(new TestServiceModule())
                                 .build();
    }

    @Override
    public AppModule getAppModule() {
        return new TestAppModule(this);
    }
}
