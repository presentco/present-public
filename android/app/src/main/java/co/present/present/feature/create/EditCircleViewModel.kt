package co.present.present.feature.create

import android.app.Application
import android.view.View
import androidx.lifecycle.AndroidViewModel
import co.present.present.analytics.AmplitudeEvents
import co.present.present.analytics.Analytics
import co.present.present.config.FeatureDataProvider
import co.present.present.db.CircleDao
import co.present.present.db.SpacesDao
import co.present.present.extensions.Optional
import co.present.present.extensions.combineLatest
import co.present.present.extensions.context
import co.present.present.feature.common.item.CategoriesGroup
import co.present.present.feature.common.item.ChipCategoryItem
import co.present.present.feature.common.item.OnTextChangedListener
import co.present.present.feature.common.item.SpaceRadioButtonItem
import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.feature.common.viewmodel.UploadPhoto
import co.present.present.location.LocationDataProvider
import co.present.present.location.toCoordinates
import co.present.present.model.Circle
import co.present.present.model.CurrentUser
import co.present.present.model.Space
import co.present.present.model.filterInterestsOnly
import co.present.present.service.Filesystem
import co.present.present.service.rpc.putCircle
import com.google.android.gms.location.places.Place
import com.xwray.groupie.Group
import com.xwray.groupie.Item
import com.xwray.groupie.OnItemClickListener
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.rxkotlin.combineLatest
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import present.proto.GroupMemberPreapproval
import present.proto.GroupService
import java.util.*
import javax.inject.Inject


class EditCircleViewModel @Inject constructor(private val circleService: GroupService,
                                              private val photoUploader: CircleUploadPhotoImpl,
                                              val filesystem: Filesystem,
                                              val circleDao: CircleDao,
                                              val getCurrentUser: GetCurrentUser,
                                              val featureDataProvider: FeatureDataProvider,
                                              val spacesDao: SpacesDao,
                                              private val locationDataProvider: LocationDataProvider,
                                              val analytics: Analytics,
                                              application: Application)
    : AndroidViewModel(application), UploadPhoto by photoUploader, GetCurrentUser by getCurrentUser,
        OnItemClickListener, SwitchItem.OnSwitchChangedListener {

    val TAG = javaClass.simpleName

    // User changed the title manually
    private var title = BehaviorSubject.create<String>()

    private fun getInitialTitle(circleId: String?, prefillTitle: String?): Flowable<String> {
        return getCircle(circleId).map { it.value?.title ?: prefillTitle ?: "" }.toFlowable()
    }

    private fun getTitle(circleId: String?, prefillTitle: String?): Flowable<String> {
        return getInitialTitle(circleId, prefillTitle)
                .mergeWith(title.toFlowable(BackpressureStrategy.LATEST))
    }

    // User changed the space manually
    private val space = PublishSubject.create<Space>()

    /**
     * The space that should be shown in the editing UI, starting with the space
     * either of the circle we're editing, or the default (current) space we're in,
     * and then followed by any changes the user selects themself
     */
    private fun getSpace(circleId: String?): Flowable<Space> {
        return  getCircle(circleId).zipWith(spacesDao.getSpaces().firstOrError())
                .map { (optionalCircle: Optional<Circle>, spaces: List<Space>) ->
                    val circle = optionalCircle.value
                    val selectedSpaceId = circle?.spaceId ?: Space.everyoneId
                    spaces.first { it.id == selectedSpaceId }
                }.toFlowable().mergeWith(space.toFlowable(BackpressureStrategy.LATEST))
    }

    private fun getDiscoverability(circleId: String?): Flowable<Boolean> {
        return getCircle(circleId)
                .map { it.value?.discoverable ?: true }.toFlowable()
                .mergeWith(discoverable.toFlowable(BackpressureStrategy.LATEST))
    }

    // User changed the discoverability
    private val discoverable = BehaviorSubject.create<Boolean>()

    //User changed the preapproval
    private val preapproval = BehaviorSubject.create<GroupMemberPreapproval>()

    private fun getPreapproval(circleId: String?): Flowable<GroupMemberPreapproval> {
        return getCircle(circleId)
                .map { GroupMemberPreapproval.fromValue(it.value?.preapproval ?: GroupMemberPreapproval.ANYONE.value) }.toFlowable()
                .mergeWith(preapproval.toFlowable(BackpressureStrategy.LATEST))
    }

    fun setPreapproval(preapproval: GroupMemberPreapproval) {
        this.preapproval.onNext(preapproval)
    }

    private var coverImageUrl: String? = null

    private var coverImageId: String? = null


    // A new place for the circle, picked by the user. Place is a class from the Google Maps API
    var place = BehaviorSubject.create<Optional<Place>>()

    /**
     * The place that should be shown in the editing UI. It might be missing initially.
     */
    private fun getPlace(circleId: String?): Flowable<Optional<out Place>> {
        return getCircle(circleId)
                .flatMap {
                    if (it.value != null) {
                        Single.just(Optional(ExistingPlace(it.value)))
                    } else {
                        locationDataProvider.getCurrentPlace(context)
                    }
                }.toFlowable()
                .mergeWith(place.toFlowable(BackpressureStrategy.LATEST))
    }

    // New categories chosen by the user
    private var categories = BehaviorSubject.create<List<String>>()

    private fun getCategories(circleId: String?, prefilledCategories: List<String>): Flowable<List<String>> {
        return getCircle(circleId).map { it.value?.categories ?: prefilledCategories }.toFlowable()
                .mergeWith(categories.toFlowable(BackpressureStrategy.LATEST))
    }

    // New description from user input
    private var description = BehaviorSubject.create<String>()

    private fun getDescription(circleId: String?, prefillDescription: String?): Flowable<String> {
        return getCircle(circleId).map {
            it.value?.description ?: prefillDescription ?: ""
        }.toFlowable()
                .mergeWith(description.toFlowable(BackpressureStrategy.LATEST))
    }

    data class EditCircleInfo(val space: Space,
                              val categories: List<String>,
                              val placeOptional: Optional<out Place>,
                              val title: String,
                              val description: String,
                              val preapproval: GroupMemberPreapproval,
                              val discoverable: Boolean,
                              val currentUser: CurrentUser)

    private fun getEditCircleInfo(circleId: String?, prefillInfo: EditCircleActivity.PrefillInfo): Flowable<EditCircleInfo> {
        return getSpace(circleId).combineLatest(
                getCategories(circleId, prefillInfo.categories),
                getPlace(circleId),
                getTitle(circleId, prefillInfo.title),
                getDescription(circleId, prefillInfo.description),
                getPreapproval(circleId),
                getDiscoverability(circleId),
                currentUser).map { (
                                           space: Space,
                                           categories: List<String>,
                                           placeOptional: Optional<out Place>,
                                           title: String,
                                           description: String,
                                           preapproval: GroupMemberPreapproval,
                                           discoverable: Boolean,
                                           currentUser: CurrentUser
                                   ) ->
            EditCircleInfo(space, categories, placeOptional, title, description, preapproval, discoverable, currentUser)
        }
    }

    fun getItems(circleId: String? = null, prefillInfo: EditCircleActivity.PrefillInfo,
                 onEditImageListener: EditCircleImageNameLocationItem.EditImageListener,
                 onEditLocationListener: EditCircleImageNameLocationItem.EditLocationListener): Flowable<out List<Group>> {
        return getCircle(circleId).toFlowable().combineLatest(getEditCircleInfo(circleId, prefillInfo)).map { (circleOptional, editCircleInfo) ->
            editCircleInfo.let { (
                                         space: Space,
                                         categories: List<String>,
                                         placeOptional: Optional<out Place>,
                                         title: String,
                                         description: String,
                                         preapproval: GroupMemberPreapproval,
                                         discoverable: Boolean,
                                         currentUser: CurrentUser
                                 ) ->

                val circle = circleOptional.value
                coverImageUrl = circle?.coverPhoto
                coverImageId = circle?.coverPhotoId

                val onTextChangedListener = OnCircleTextChangedListener(circle)
                val onCategoryToggleListener = OnCircleCategoryToggleListener(circle, categories)

                mutableListOf<Group>().apply {
                    add(EditCircleImageNameLocationItem(
                            coverImageUrl,
                            name = title,
                            place = placeOptional.value,
                            editImageListener = onEditImageListener,
                            editLocationListener = onEditLocationListener,
                            onNameChangedListener = onTextChangedListener).apply {
                        if (photoUploader.uuid != null) {
                            coverImageUri = photoUploader.temporaryPhotoUri
                        }
                    })

                    add(DescriptionEditableTextItem(onTextChangedListener, description))
                    add(SettingsGroup(currentUser, circle, space, discoverable, preapproval, featureDataProvider = featureDataProvider,
                            onSwitchChangedListener = this@EditCircleViewModel, onSpaceChangedListener = this@EditCircleViewModel))

                    add(CategoriesGroup(categories, context, onCategoryToggleListener))
                }
            }
        }
    }

    override fun onSwitchChanged(item: SwitchItem, value: Boolean) {
        when (item) {
            is DiscoverabilitySwitchItem -> discoverable.onNext(value)
            is WomenOnlySwitchItem -> space.onNext(if (value) Space.WomenOnly else Space.Everyone)
        }
    }

    inner class OnCircleTextChangedListener(val circle: Circle?) : OnTextChangedListener {
        override fun onTextChanged(item: Item<*>, text: String) {
            when (item) {
                is EditCircleImageNameLocationItem -> {
                    this@EditCircleViewModel.title.onNext(text)
                }
                is DescriptionEditableTextItem -> {
                    analytics.log(if (circle == null) AmplitudeEvents.CIRCLE_CREATE_SET_DESCRIPTION else AmplitudeEvents.CIRCLE_EDIT_SET_DESCRIPTION)
                    this@EditCircleViewModel.description.onNext(text)
                }
            }
        }
    }

    inner class OnCircleCategoryToggleListener(val circle: Circle?, val categories: List<String>) : ChipCategoryItem.OnCategoryToggleListener {
        override fun onCategoryToggled(item: Item<*>, category: String) {
            analytics.log(if (circle == null) AmplitudeEvents.CIRCLE_CREATE_SET_CATEGORIES else AmplitudeEvents.CIRCLE_EDIT_SET_CATEGORIES)
            toggleCategory(category, categories)
        }
    }

    override fun onItemClick(item: Item<*>, view: View) {
        when (item) {
            is SpaceRadioButtonItem -> changeSpace(item.space)
        }
    }

    fun submitEnabled(circleId: String?, prefill: EditCircleActivity.PrefillInfo): Flowable<Pair<Boolean, EditCircleInfo>> {
        return getEditCircleInfo(circleId, prefill)
                .map {

                    val place = it.placeOptional.value
                    val isEnabled = it.title.isNotBlank() && it.title.isNotEmpty() && place != null

                    Pair(isEnabled, it)
                }
    }

    private fun changeSpace(space: Space) {
        this.space.onNext(space)
    }

    private fun toggleCategory(category: String, categories: List<String>) {
        if (categories.contains(category)) {
            this.categories.onNext(categories - category)
        } else if (categories.filterInterestsOnly().size < 3) {
            this.categories.onNext(categories + category)
        }
    }

    private fun getCircle(circleId: String? = null): Single<Optional<Circle>> {
        return if (circleId == null) Single.just(Optional<Circle>(null))
        else circleDao.getCircle(circleId).firstOrError().map { Optional(it) }
    }

    fun updateCircle(circleId: String?, info: EditCircleInfo): Single<Circle> {
        return locationDataProvider.getLocation(context).flatMap { location ->
            val place = info.placeOptional.value!!
            circleService.putCircle(
                    uuid = circleId ?: UUID.randomUUID().toString(),
                    title = info.title,
                    description = info.description,
                    locationName = place.getUserVisibleName(),
                    latitude = place.latLng.latitude,
                    longitude = place.latLng.longitude,
                    categories = info.categories,
                    createdFrom = location.toCoordinates(),
                    photoUuid = photoUploader.uuid ?: coverImageId,
                    space = info.space,
                    preapproval = info.preapproval,
                    discoverable = info.discoverable)
                    .subscribeOn(Schedulers.io())
                    .map { circleDao.insert(it); it }
        }
    }

    fun setPlace(place: Place) {
        this.place.onNext(Optional(place))
    }

}

