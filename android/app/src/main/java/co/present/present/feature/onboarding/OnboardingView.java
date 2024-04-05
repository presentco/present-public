package co.present.present.feature.onboarding;

public interface OnboardingView {
    void showStep(int position);

    void showSettings();

    void userLoggedIn();

    void userWaitlisted();
}
