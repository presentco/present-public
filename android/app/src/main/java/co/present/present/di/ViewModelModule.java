package co.present.present.di;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import co.present.present.DebugViewModel;
import co.present.present.UrlResolverViewModel;
import co.present.present.ViewModelFactory;
import co.present.present.feature.BottomNavViewModel;
import co.present.present.feature.CitiesViewModel;
import co.present.present.feature.activity.ActivityViewModel;
import co.present.present.feature.create.ChooseCategoriesViewModel;
import co.present.present.feature.create.CircleShareViewModel;
import co.present.present.feature.create.EditCircleViewModel;
import co.present.present.feature.detail.chat.ChatViewModel;
import co.present.present.feature.detail.info.CircleViewModel;
import co.present.present.feature.detail.info.MembersViewModel;
import co.present.present.feature.discovery.CategoryViewModel;
import co.present.present.feature.discovery.CirclesViewModel;
import co.present.present.feature.discovery.FeedViewModel;
import co.present.present.feature.discovery.JoinedCirclesViewModel;
import co.present.present.feature.image.ImageGalleryViewModel;
import co.present.present.feature.invite.AddFriendsViewModel;
import co.present.present.feature.invite.AddToCircleViewModel;
import co.present.present.feature.onboarding.PhoneVerificationViewModel;
import co.present.present.feature.onboarding.PhoneViewModel;
import co.present.present.feature.profile.EditProfileViewModel;
import co.present.present.feature.profile.FriendsViewModel;
import co.present.present.feature.profile.SettingsViewModel;
import co.present.present.feature.profile.info.UserProfileViewModel;
import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;

@Module
public abstract class ViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(AddFriendsViewModel.class)
    public abstract ViewModel bindAddFriendsViewModel(AddFriendsViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(AddToCircleViewModel.class)
    public abstract ViewModel bindAddToCircleViewModel(AddToCircleViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(DebugViewModel.class)
    public abstract ViewModel bindDebugViewModel(DebugViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(PhoneVerificationViewModel.class)
    public abstract ViewModel bindPhoneVerificationViewModel(PhoneVerificationViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(PhoneViewModel.class)
    public abstract ViewModel bindPhoneViewModel(PhoneViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(FeedViewModel.class)
    public abstract ViewModel bindFeedViewModel(FeedViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(MembersViewModel.class)
    public abstract ViewModel bindMembersViewModel(MembersViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(CircleShareViewModel.class)
    public abstract ViewModel bindCircleShareViewModel(CircleShareViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(UserProfileViewModel.class)
    public abstract ViewModel bindUserViewModel(UserProfileViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(EditProfileViewModel.class)
    public abstract ViewModel bindEditProfileViewModel(EditProfileViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(CirclesViewModel.class)
    public abstract ViewModel bindCirclesViewModel(CirclesViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(CategoryViewModel.class)
    public abstract ViewModel bindNearbyCirclesViewModel(CategoryViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(JoinedCirclesViewModel.class)
    public abstract ViewModel bindJoinedCirclesViewModel(JoinedCirclesViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(CircleViewModel.class)
    public abstract ViewModel bindCircleViewModel(CircleViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(ChatViewModel.class)
    public abstract ViewModel bindChatViewModel(ChatViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(UrlResolverViewModel.class)
    public abstract ViewModel bindUrlResolverViewModel(UrlResolverViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(CitiesViewModel.class)
    public abstract ViewModel bindCitiesViewModelViewModel(CitiesViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(EditCircleViewModel.class)
    public abstract ViewModel bindEditCircleViewModel(EditCircleViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(ChooseCategoriesViewModel.class)
    public abstract ViewModel bindChooseCategoriesViewModel(ChooseCategoriesViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(ActivityViewModel.class)
    public abstract ViewModel bindActivityViewModel(ActivityViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(BottomNavViewModel.class)
    public abstract ViewModel bindBottomNavViewModel(BottomNavViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(ImageGalleryViewModel.class)
    public abstract ViewModel bindImageGalleryViewModel(ImageGalleryViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(FriendsViewModel.class)
    public abstract ViewModel bindFriendsViewModel(FriendsViewModel viewModel);

    @Binds
    public abstract ViewModelProvider.Factory bindViewModelFactory(ViewModelFactory factory);

    @Binds
    @IntoMap
    @ViewModelKey(SettingsViewModel.class)
    public abstract ViewModel bindSettingsViewModel(SettingsViewModel viewModel);
}
