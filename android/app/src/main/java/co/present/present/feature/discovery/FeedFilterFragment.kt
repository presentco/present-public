package co.present.present.feature.discovery

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProviders
import co.present.present.analytics.AmplitudeEvents
import co.present.present.di.ActivityScope
import co.present.present.feature.common.item.OnCircleJoinClickListener
import co.present.present.feature.common.item.SmallCircleItem
import com.xwray.groupie.Group
import com.xwray.groupie.Item
import io.reactivex.Flowable
import kotlinx.android.synthetic.main.fragment_display_circles.*


@ActivityScope
open class FeedFilterFragment : CircleListFragment() {

    protected lateinit var feedViewModel: FeedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        feedViewModel = ViewModelProviders.of(parentFragment!!, viewModelFactory).get(FeedViewModel::class.java)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        emptyView.init(feedViewModel, baseActivity, AmplitudeEvents.HOME_SEARCH_TAP_CREATE_CIRCLE, analytics)
    }

    override fun getItems(onCircleJoinClickListener: OnCircleJoinClickListener): Flowable<List<Group>> {
        return feedViewModel.getItems(onCircleJoinClickListener)
    }

    override fun performInjection() {
        activityComponent.inject(this)
    }

    override fun onItemClick(item: Item<*>, view: View) {
        if (item is SmallCircleItem) analytics.log(AmplitudeEvents.HOME_SEARCH_TAP_CIRCLE)
        super.onItemClick(item, view)
    }

}