package co.present.present.feature.onboarding.listeners;

import co.present.present.model.CurrentUser;

public interface DbFetchListener {
    void onUserFetchSuccess(CurrentUser profile);
}
