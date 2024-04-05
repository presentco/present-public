package co.present.present.di;

import org.jetbrains.annotations.NotNull;

import co.present.present.LaunchActivity;
import co.present.present.feature.BottomNav;
import co.present.present.feature.CategoryActivity;
import co.present.present.feature.CityPickerActivity;
import co.present.present.feature.MainActivity;
import co.present.present.feature.SignUpDialogActivity;
import co.present.present.feature.WebFragment;
import co.present.present.feature.WebScreen;
import co.present.present.feature.activity.ActivityFeedFragment;
import co.present.present.feature.create.CircleShareActivity;
import co.present.present.feature.create.EditCircleActivity;
import co.present.present.feature.create.PreApproveActivity;
import co.present.present.feature.detail.CircleActivity;
import co.present.present.feature.detail.CircleDetailActivity;
import co.present.present.feature.detail.chat.ChatFragment;
import co.present.present.feature.detail.info.CircleDetailFragment;
import co.present.present.feature.detail.info.CircleViewModel;
import co.present.present.feature.detail.info.MembersActivity;
import co.present.present.feature.discovery.CategoryFragment;
import co.present.present.feature.discovery.CirclesViewModel;
import co.present.present.feature.discovery.ExploreWebScreen;
import co.present.present.feature.discovery.FeedFilterFragment;
import co.present.present.feature.discovery.FeedFragment;
import co.present.present.feature.discovery.FeedTabFragment;
import co.present.present.feature.image.ImageGalleryActivity;
import co.present.present.feature.invite.AddFriendsActivity;
import co.present.present.feature.invite.AddFriendsFragment;
import co.present.present.feature.invite.AddToCircleActivity;
import co.present.present.feature.invite.AddToCircleContactsFragment;
import co.present.present.feature.invite.AddToCircleFacebookFriendsFragment;
import co.present.present.feature.invite.AddToCircleFragment;
import co.present.present.feature.invite.AddToCircleFriendsFragment;
import co.present.present.feature.invite.ContactsPermissionPlaceholderFragment;
import co.present.present.feature.onboarding.OnboardingActivity;
import co.present.present.feature.onboarding.OnboardingDataProvider;
import co.present.present.feature.onboarding.PhoneLoginActivity;
import co.present.present.feature.onboarding.PhoneVerificationActivity;
import co.present.present.feature.onboarding.WaitlistActivity;
import co.present.present.feature.onboarding.step.ConfirmNameAndPhotoFragment;
import co.present.present.feature.onboarding.step.FacebookLinkActivity;
import co.present.present.feature.onboarding.step.LinkVerificationActivity;
import co.present.present.feature.onboarding.step.LocationPermissionFragment;
import co.present.present.feature.profile.EditProfileActivity;
import co.present.present.feature.profile.FriendsActivity;
import co.present.present.feature.profile.FriendsFragment;
import co.present.present.feature.profile.SettingsActivity;
import co.present.present.feature.profile.UserProfileActivity;
import co.present.present.feature.profile.UserProfileFragment;
import co.present.present.feature.profile.joined.JoinedCirclesFragment;
import co.present.present.feature.welcome.PlaceholderFragment;
import co.present.present.location.BaseLocationPermissionFragment;
import co.present.present.location.LocationPermissionActivity;
import dagger.Subcomponent;

@ActivityScope
@Subcomponent(
        modules = ActivityModule.class
)
public interface ActivityComponent {
    void inject(LaunchActivity activity);

    void inject(FacebookLinkActivity activity);
    void inject(OnboardingActivity activity);
    void inject(WaitlistActivity activity);
    void inject(ConfirmNameAndPhotoFragment fragment);
    void inject(BaseLocationPermissionFragment fragment);
    void inject(LocationPermissionFragment fragment);
    void inject(OnboardingDataProvider dataProvider);
    void inject(EditProfileActivity activity);
    void inject(LocationPermissionActivity activity);

    void inject(CategoryFragment fragment);
    void inject(JoinedCirclesFragment fragment);
    void inject(CirclesViewModel dataProvider);

    void inject(UserProfileActivity activity);

    void inject(CircleActivity activity);
    void inject(ChatFragment fragment);
    void inject(CircleDetailFragment fragment);
    void inject(CircleViewModel dataProvider);

    void inject(EditCircleActivity activity);
    void inject(ExploreWebScreen exploreWebScreen);

    void inject(ActivityFeedFragment activityFeedFragment);
    void inject(BottomNav bottomNav);
    void inject(ImageGalleryActivity imageGalleryActivity);
    void inject(MainActivity mainActivity);
    void inject(WebFragment webFragment);
    void inject(FeedFragment feedFragment);
    void inject(UserProfileFragment userProfileFragment);
    void inject(CategoryActivity categoryActivity);
    void inject(LinkVerificationActivity linkVerificationActivity);
    void inject(PlaceholderFragment placeholderFragment);
    void inject(@NotNull FriendsActivity friendsActivity);
    void inject(@NotNull FriendsFragment friendsFragment);
    void inject(@NotNull CircleShareActivity circleShareActivity);
    void inject(@NotNull MembersActivity membersActivity);
    void inject(@NotNull FeedFilterFragment feedFilterFragment);
    void inject(@NotNull PhoneLoginActivity phoneLoginActivity);
    void inject(@NotNull PhoneVerificationActivity phoneVerificationActivity);
    void inject(@NotNull FeedTabFragment feedTabFragment);
    void inject(@NotNull CityPickerActivity cityPickerActivity);
    void inject(@NotNull SignUpDialogActivity signUpDialogActivity);
    void inject(@NotNull WebScreen webScreen);

    void inject(@NotNull PreApproveActivity preApproveActivity);

    void inject(@NotNull CircleDetailActivity circleDetailActivity);

    void inject(@NotNull AddToCircleActivity addToCircleActivity);
    void inject(ContactsPermissionPlaceholderFragment contactsPermissionPlaceholderFragment);

    void inject(@NotNull AddToCircleContactsFragment addToCircleContactsFragment);

    void inject(@NotNull AddToCircleFriendsFragment addToCircleFriendsFragment);

    void inject(@NotNull AddToCircleFacebookFriendsFragment addToCircleFacebookFriendsFragment);

    void inject(@NotNull AddToCircleFragment addToCircleFragment);

    void inject(@NotNull AddFriendsActivity addFriendsActivity);

    void inject(@NotNull AddFriendsFragment addFriendsFragment);

    void inject(@NotNull SettingsActivity settingsActivity);
}
