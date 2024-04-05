package co.present.present.feature.profile

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import co.present.present.BuildConfig
import co.present.present.R
import co.present.present.feature.common.item.ActionItem
import co.present.present.feature.common.item.DividerItem
import co.present.present.feature.common.item.HeaderItem
import co.present.present.feature.common.item.TextItem
import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.feature.create.SwitchItem
import com.xwray.groupie.Group
import io.reactivex.Completable
import io.reactivex.Flowable
import present.proto.UserRequest
import present.proto.UserService
import javax.inject.Inject

class SettingsViewModel @Inject constructor(
        val userService: UserService, val getCurrentUser: GetCurrentUser)
    : ViewModel(), GetCurrentUser by getCurrentUser {
    fun getItems(): Flowable<List<Group>> {
        return Flowable.just(
                mutableListOf<Group>().apply {

                    add(HeaderItem(R.string.settings))
                    //add(PushNotificationsItem())
                    add(LogOutActionItem())
                    add(DeleteAccountActionItem())
                    add(DividerItem())

                    add(HeaderItem(R.string.app_name))
                    add(WebsiteActionItem())
                    add(BlogActionItem())
                    add(PrivacyPolicyActionItem())
                    add(TermsOfServiceActionItem())
                    add(SupportActionItem())

                    add(DividerItem())
                    add(TextItem("App Version: ${BuildConfig.VERSION_NAME}"))
                }
        )

    }

    fun deleteAccount(): Completable {
        return currentUser.firstOrError().map {
            userService.deleteAccount(UserRequest.Builder().userId(it.id).build())
        }.toCompletable()
    }
}

class PushNotificationsItem: SwitchItem(R.string.push_notifications, switchValue = true)

open class LinkActionItem(@StringRes stringRes: Int, @StringRes val link: Int) : ActionItem(stringRes)

class LogOutActionItem: ActionItem(R.string.log_out)
class DeleteAccountActionItem: ActionItem(R.string.delete_account)
class WebsiteActionItem: LinkActionItem(R.string.website, R.string.present_home)
class BlogActionItem: LinkActionItem(R.string.blog, R.string.present_blog)
class PrivacyPolicyActionItem: LinkActionItem(R.string.privacy_policy, R.string.privacy_link)
class TermsOfServiceActionItem: LinkActionItem(R.string.terms_of_service, R.string.tos_link)
class SupportActionItem: LinkActionItem(R.string.contact_support, R.string.support_link)
