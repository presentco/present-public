package co.present.present.feature.onboarding.listeners;

import present.proto.AuthorizationResponse;

public interface UserLoginListener {
    void onFacebookLinkSuccess(AuthorizationResponse authorizationResponse);
    void onLoginSuccess(AuthorizationResponse authorizationResponse);
}
