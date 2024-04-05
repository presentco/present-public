package co.present.present.feature.profile.joined

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import co.present.present.analytics.AmplitudeEvents
import co.present.present.di.ActivityScope
import co.present.present.feature.common.item.OnCircleJoinClickListener
import co.present.present.feature.discovery.CircleListFragment
import co.present.present.feature.discovery.JoinedCirclesViewModel
import co.present.present.model.User
import com.xwray.groupie.Group
import io.reactivex.Flowable
import kotlinx.android.synthetic.main.fragment_display_circles.*

/**
 * Joined Circles
 */
@ActivityScope
class JoinedCirclesFragment : CircleListFragment() {

    private val userId: String by lazy { arguments!!.getString(User.USER) }

    private lateinit var joinedCirclesViewModel: JoinedCirclesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        joinedCirclesViewModel = ViewModelProviders.of(baseActivity, viewModelFactory).get(JoinedCirclesViewModel::class.java)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        emptyView.init(joinedCirclesViewModel, baseActivity, AmplitudeEvents.PROFILE_CIRCLES_TAP_CREATE_CIRCLE, analytics)
    }

    override fun getItems(onCircleJoinClickListener: OnCircleJoinClickListener): Flowable<List<Group>> {
        return joinedCirclesViewModel.getItems(userId)
    }

    override fun performInjection() {
        activityComponent.inject(this)
    }

    companion object {

        fun newInstance(userId: String): Fragment {
            val bundle = Bundle().apply { putString(User.USER, userId) }
            return JoinedCirclesFragment().apply { arguments = bundle }
        }
    }
}