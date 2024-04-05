package co.present.present.feature.detail.info

import android.app.Application
import android.content.Context
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import co.present.present.R
import co.present.present.config.FeatureDataProvider
import co.present.present.db.CircleDao
import co.present.present.db.SpacesDao
import co.present.present.extensions.*
import co.present.present.feature.common.GetCircle
import co.present.present.feature.common.GetMembers
import co.present.present.feature.common.item.*
import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.feature.create.SettingsGroup
import co.present.present.feature.create.SwitchItem
import co.present.present.feature.detail.GetMemberRequests
import co.present.present.location.LocationDataProvider
import co.present.present.model.*
import co.present.present.service.rpc.*
import co.present.present.view.OnLinkClickedListener
import com.xwray.groupie.Group
import com.xwray.groupie.Item
import com.xwray.groupie.Section
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.rxkotlin.combineLatest
import io.reactivex.schedulers.Schedulers
import present.proto.FlagReason
import present.proto.GroupMemberPreapproval
import present.proto.GroupService
import javax.inject.Inject


class CircleViewModel @Inject constructor(private val circleService: GroupService,
                                          private val circleDao: CircleDao,
                                          private val spacesDao: SpacesDao,
                                          private val locationDataProvider: LocationDataProvider,
                                          private val featureDataProvider: FeatureDataProvider,
                                          private val joinCircle: JoinCircle,
                                          application: Application,
                                          private val getCurrentUser: GetCurrentUser,
                                          private val getCircle: GetCircle,
                                          getMembers: GetMembers,
                                          getMembershipRequests: GetMemberRequests)
    : AndroidViewModel(application), JoinCircle by joinCircle, GetCurrentUser by getCurrentUser, GetCircle by getCircle,
        GetMembers by getMembers, GetMemberRequests by getMembershipRequests {

    private val TAG = javaClass.simpleName

    private fun getItems(info: Quint<Optional<CurrentUser>, Circle, Location, List<User>, List<MemberRequest>>,
                         onLinkClickedListener: OnLinkClickedListener,
                         onSwitchChangedListener: SwitchItem.OnSwitchChangedListener)
            : List<Group> {
        info.let { (currentUserOptional, circle, location, members, memberRequests) ->
            return mutableListOf<Group>().apply {
                val currentUser = currentUserOptional.value

                add(Section(GrayHeaderItem(R.string.description)).apply {
                    setHideWhenEmpty(true)
                    if (circle.description.isEmpty()) {
                        add(TextItem(string(R.string.circle_no_description), onLinkClickedListener))
                    } else {
                        add(TextItem(circle.description, onLinkClickedListener))
                    }
                })

                add(Section(GrayHeaderItem(R.string.location)).apply {
                    setHideWhenEmpty(true)
                    circle.let { add(LocationItem(it)) }
                })

                add(Section(MembersHeader(context, circle)).apply {
                    circle.let {
                        val carouselContent = mutableListOf<Group>()
                        if (memberRequests.isNotEmpty()) {
                            carouselContent += MemberRequestsItem(memberRequests.size)
                        }
                        carouselContent += members.map { user -> MemberItem(user, it) }
                        add(CarouselItem(carouselContent))
                    }
                })


                add(SettingsGroup(currentUser, circle, featureDataProvider, onSwitchChangedListener))

                add(Section(GrayHeaderItem(R.string.category)).apply {
                    setHideWhenEmpty(true)
                    circle.let {
                        if (it.categories.isEmpty()) {
                            add(TextItem("None", onLinkClickedListener))
                        } else {
                            add(CategoriesTagCloudItem(it.categories))
                        }
                    }
                })

                add(ReportItem())
                if (circle.joined) add(LeaveItem(circle, currentUser))
                if (currentUser.canDelete(circle)) add(DeleteItem())
            }
        }
    }

    fun getItems(circleId: String,
                 onLinkClickedListener: OnLinkClickedListener,
                 onSwitchChangedListener: SwitchItem.OnSwitchChangedListener)
            : Flowable<List<Group>> {

        return  currentUserOptional.combineLatest(
                getCircle(circleId),
                locationDataProvider.getLocation(context).toFlowable(),
                getMembers(circleId),
                getMemberRequests(circleId)
        ).map {
            getItems(it, onLinkClickedListener, onSwitchChangedListener)
        }
    }

    fun getCircleInfo(circleId: String): Flowable<Triple<Circle, Optional<CurrentUser>, Location>> {
        return getCircle(circleId).combineLatest(
                currentUserOptional,
                locationDataProvider.getLocation(context).toFlowable())
    }

    fun toggleCircleNotifications(circleId: String): Single<Circle> {
        return getCircle(circleId).firstOrError().flatMap {
            toggleCircleNotificationsLocally(it).toSingleDefault(it)
        }.flatMap {  circle ->
            toggleCircleNotificationsOnServer(circle)
                    .onErrorResumeNext { resetCircleLocallyAndThrow(circle, it).subscribeOn(Schedulers.io()) }
                    .toSingleDefault(circle.copy(muted = !circle.muted))
        }
    }

    private fun toggleCircleNotificationsOnServer(circle: Circle): Completable {
        return if (circle.muted) {
            circleService.unmute(circle.id)
        } else {
            circleService.mute(circle.id)
        }
    }

    private fun toggleCircleNotificationsLocally(circle: Circle): Completable {
        return Completable.fromCallable {
            val updatedCircle = circle.copy(muted = !circle.muted)
            circleDao.update(updatedCircle)
        }
    }

    fun deleteCircle(circleId: String): Completable {
        return circleService.deleteCircle(circleId).toSingleDefault(circleId).map {
            circleDao.delete(circleId)
        }.toCompletable()
    }

    fun reportCircle(circleId: String, flagReason: FlagReason): Completable {
        return circleService.reportCircle(circleId, flagReason)
    }

    fun changeDiscoverability(value: Boolean, circleId: String): Completable {
        return updateCircle(circleId) { it.copy(discoverable = value) }
    }

    fun setPreapproval(newPreapproval: GroupMemberPreapproval, circleId: String): Completable {
        return updateCircle(circleId) { it.copy(preapproval = newPreapproval.value) }
    }

    /**
     * Perform an optimistic database update of the circle, followed by a server update.
     * If the server update fails, perform a database rollback and return an error,
     * otherwise return complete.
     */
    private fun updateCircle(circleId: String, update: (Circle) -> Circle): Completable {
        return getCircle(circleId).firstOrError().flatMapCompletable { circle ->
            val newCircle = update(circle)
            Completable.concatArray(
                    updateCircleLocally(newCircle),
                    updateCircleOnServer(context, newCircle)
                            .onErrorResumeNext { resetCircleLocallyAndThrow(circle, it) }
            )
        }
    }

    private fun updateCircleLocally(newCircle: Circle): Completable {
        return Completable.fromCallable {
            circleDao.update(newCircle)
        }
    }

    private fun updateCircleOnServer(context: Context, newCircle: Circle): Completable {
        return locationDataProvider.getLocation(context).flatMapCompletable { location ->
            circleService.putCircle(newCircle, location).subscribeOn(Schedulers.io())
        }
    }

    private fun resetCircleLocallyAndThrow(circle: Circle, throwable: Throwable): Completable {
        return Completable.fromCallable {
            circleDao.update(circle)
            throw throwable
        }
    }

    fun getEditEnabled(circleId: String): Flowable<Boolean> {
        return getCircle(circleId).combineLatest(currentUser).map { (circle, currentUser) ->
            currentUser.canEdit(circle)
        }.startWith(false)
    }

}

class MembersHeader(context: Context, val circle: Circle?) : ViewAllHeaderItem(
        stringRes = if (circle == null) R.string.members else R.string.empty,
        string = if (circle != null) context.resources.getQuantityString(R.plurals.members_w_num_template, circle.participantCount, circle.participantCount) else "") {

    override fun isSameAs(other: Item<*>?): Boolean {
        return other is MembersHeader
    }

    override fun bind(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        bind(holder, position)
    }
}

class LeaveItem(val circle: Circle, val currentUser: CurrentUser?): ActionItem(R.string.leave)
class ReportItem: ActionItem(R.string.report)
class DeleteItem: ActionItem(R.string.delete)

