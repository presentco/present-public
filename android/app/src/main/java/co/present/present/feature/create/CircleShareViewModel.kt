package co.present.present.feature.create

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import co.present.present.R
import co.present.present.feature.common.GetCircle
import co.present.present.feature.common.GetFriends
import co.present.present.feature.common.item.ActionableHeaderItem
import co.present.present.feature.common.item.CarouselItem
import co.present.present.feature.common.item.CircleItemPreview
import co.present.present.feature.common.item.FriendItem
import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.model.Circle
import co.present.present.model.User
import com.xwray.groupie.Group
import com.xwray.groupie.Section
import io.reactivex.Flowable
import io.reactivex.rxkotlin.combineLatest
import javax.inject.Inject


class CircleShareViewModel @Inject constructor(getCircle: GetCircle, getCurrentUser: GetCurrentUser,
                                               getFriends: GetFriends, application: Application)
    : AndroidViewModel(application), GetCircle by getCircle, GetCurrentUser by getCurrentUser,
        GetFriends by getFriends {


    fun getItems(circleId: String,
                 shareButtonClickListener: CircleShareButtonsItem.OnShareButtonClickListener)
            : Flowable<List<Group>> {
        return currentUser.combineLatest(getCircle(circleId), currentUser.flatMap { getFriends(it.id) })
                .map { (currentUser, circle, followers) ->
                    mutableListOf<Group>().apply {
                        add(CircleItemPreview(currentUser, circle))
                        add(CircleShareButtonsItem(circle, shareButtonClickListener))

                        add(Section(AddYourFriendsHeader(circle)).apply {
                            setHideWhenEmpty(true)
                            add(CarouselItem(followers.map { FriendCircleItem(circle, it) }))
                        })
                    }
                }
    }
}

class AddYourFriendsHeader(val circle: Circle) : ActionableHeaderItem(R.string.add_your_friends)
class FriendCircleItem(val circle: Circle, user: User): FriendItem(user)
