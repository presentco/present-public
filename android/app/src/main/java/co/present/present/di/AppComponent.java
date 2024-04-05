package co.present.present.di;

import android.app.Application;

import javax.inject.Singleton;

import co.present.present.feature.profile.EditProfileViewModel;
import co.present.present.feature.profile.info.UserProfileViewModel;
import co.present.present.notifications.FirebaseIdService;
import co.present.present.notifications.FirebaseMessagingService;
import co.present.present.service.RpcManager;
import dagger.Component;

@Singleton
@Component(
        modules = {AppModule.class, ServiceModule.class}
)
public interface AppComponent {

    void inject(FirebaseIdService service);

    void inject(FirebaseMessagingService service);

    void inject(UserProfileViewModel userProfileViewModel);

    void inject(EditProfileViewModel userProfileViewModel);

    ActivityComponent plus(ActivityModule module);

    Application getApplication();

    RpcManager getRpcManager();

}
