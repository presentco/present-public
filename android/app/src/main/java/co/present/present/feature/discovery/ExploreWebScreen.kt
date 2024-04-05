package co.present.present.feature.discovery

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import co.present.present.BaseActivity
import co.present.present.ViewModelFactory
import co.present.present.analytics.AmplitudeEvents
import co.present.present.extensions.*
import co.present.present.feature.WebScreen
import co.present.present.model.CurrentUser
import co.present.present.service.rpc.getExplore
import io.reactivex.Single
import present.proto.GroupService
import javax.inject.Inject

class ExploreWebScreen(context: Context, attributeSet: AttributeSet) : WebScreen(context, attributeSet) {
    private val TAG = javaClass.simpleName

    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var groupService: GroupService

    override fun initiate(activity: BaseActivity) {
        super.initiate(activity)
        activity.activityComponent.inject(this)
    }

    override fun getHtml(): Single<String> {
        return if (featureDataProvider.overrideHomeUrl) {
            getHtml(featureDataProvider.homeUrl)
        } else {
            groupService.getExplore()
        }
    }

    override fun onUrlClicked(url: String, currentUser: CurrentUser?) {
        val uri = Uri.parse(url)
        val event = if (uri.isCircleLink()) {
            AmplitudeEvents.HOME_EXPLORE_TAP_CIRCLE
        } else if (uri.isCreateCircleLink()) {
            AmplitudeEvents.HOME_EXPLORE_TAP_CREATE_CIRCLE
        } else if (uri.isChooseLocationLink()) {
            AmplitudeEvents.HOME_EXPLORE_TAP_CHANGE_LOCATION
        } else if (uri.isCategoryLink()) {
            AmplitudeEvents.HOME_EXPLORE_TAP_VIEW_ALL_CATEGORY
        } else null

        event?.let { analytics.log(it) }
        super.onUrlClicked(url, currentUser)
    }
}