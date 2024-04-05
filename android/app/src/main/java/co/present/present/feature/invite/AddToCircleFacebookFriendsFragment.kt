package co.present.present.feature.invite

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import co.present.present.R
import co.present.present.model.Circle
import co.present.present.model.CurrentUser
import com.xwray.groupie.Group
import io.reactivex.Flowable

class AddToCircleFacebookFriendsFragment: AddToCircleFragment(), SearchFriendsEmptyView.InviteButtonClickListener {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_friends_to_circle, container, false)
    }

    override fun getItemsAndInfo(circleId: String): Flowable<Triple<List<Group>, CurrentUser, String>> {
        return viewModel.getFacebookFriendsItemsAndInfo(circleId)
    }

    companion object {
        fun newInstance(circleId: String): AddToCircleFacebookFriendsFragment {
            val fragment = AddToCircleFacebookFriendsFragment()
            val bundle = Bundle().apply { putString(Circle.ARG_CIRCLE, circleId) }
            fragment.arguments = bundle
            return fragment
        }
    }
}