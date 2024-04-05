package co.present.present.feature.create

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.UiThread
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import co.present.present.BaseActivity
import co.present.present.BuildConfig
import co.present.present.R
import co.present.present.analytics.AmplitudeEvents
import co.present.present.analytics.AmplitudeKeys
import co.present.present.di.ActivityScope
import co.present.present.extensions.*
import co.present.present.feature.detail.CircleActivity
import co.present.present.model.Circle
import com.google.android.gms.location.places.Place
import com.google.android.gms.location.places.ui.PlacePicker
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.OnItemClickListener
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_edit_circle.*
import present.proto.GroupMemberPreapproval
import java.util.*

/**
 * Place.name is filled with the longitude and latitude if a place doesn't otherwise have a
 * title.  So this checks if the place is type "other" (there are "types" for bars, restaurants,
 * churches, public squares, and so on) and if it is, we conclude this is just a map point.
 *
 * In that case, we use the address (e.g. 244 Kearny St, San Francisco, CA)
 */
fun Place.getUserVisibleName(): String {
    return if (this !is ExistingPlace && placeTypes.contains(Place.TYPE_OTHER)) { address } else { name }.toString()
}

@ActivityScope
class EditCircleActivity : BaseActivity(), OnItemClickListener,
        EditCircleImageNameLocationItem.EditImageListener,
        EditCircleImageNameLocationItem.EditLocationListener {

    private val TAG = javaClass.simpleName
    private val startTag = "startTag"
    private val endTag = "endTag"

    lateinit var editCircleViewModel: EditCircleViewModel
    private val circleId: String? by lazy { intent.getStringExtra(Circle.ARG_CIRCLE) }
    private val adapter = GroupAdapter<ViewHolder>().apply {
        setOnItemClickListener(this@EditCircleActivity)
    }
    private val prefillInfo: EditCircleActivity.PrefillInfo by lazy {
        PrefillInfo(
                title = intent.getStringExtra(ARG_TITLE),
                description = intent.getStringExtra(ARG_DESCRIPTION),
                categories = intent.getStringArrayListExtra(ARG_CATEGORIES) ?: listOf()
        )
    }

    // This is a temporary solution to debounce clicks on the location chooser. I should come up
    // with a more long term solution for wiring clicks from groupie to rx
    private var canLaunchLocation = true

    private val editing get() = circleId != null
    private val creating get() = !editing

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_circle)
        performInjection()
        editCircleViewModel = ViewModelProviders.of(this, viewModelFactory).get(EditCircleViewModel::class.java)

        setSupportActionBar(editCircleToolbar as Toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_close)
            setTitle(if (creating) R.string.add_circle else R.string.edit_circle)
        }

        recyclerView.layoutManager = LinearLayoutManager(this@EditCircleActivity)
        recyclerView.adapter = adapter

        editCircleViewModel.getItems(
                circleId,
                prefillInfo,
                onEditImageListener = this,
                onEditLocationListener = this)
                .compose(applyFlowableSchedulers())
                .subscribeBy(
                        onNext = { items ->
                            adapter.update(items)
                        }
                )

        if (editing) {
            analytics.log(AmplitudeEvents.CIRCLE_EDIT_VIEW, AmplitudeKeys.CIRCLE_ID, circleId!!)
        } else {
            analytics.log(AmplitudeEvents.CIRCLE_CREATE_VIEW)
        }
    }

    override fun onItemClick(item: Item<*>, view: View) {
        if (item is ActionablePreapprovalItem) launchPreapprovalPicker(item.preapproval, item.womenOnly)
        super.onItemClick(item, view)
    }

    private fun launchPreapprovalPicker(preapproval: GroupMemberPreapproval, womenOnly: Boolean) {
        slideOverFromRight(PreApproveActivity.newIntent(this, preapproval, womenOnly), PREAPPROVAL_REQUEST)
    }

    override fun onEditImageClicked(item: com.xwray.groupie.kotlinandroidextensions.Item) {
        logTapCoverPhoto()
        launchImagePicker()
    }

    override fun onEditLocationClicked(item: com.xwray.groupie.kotlinandroidextensions.Item) {
        logTapLocation()
        launchLocationPicker()
    }

    private fun logTapLocation() {
        if (creating) {
            analytics.log(AmplitudeEvents.CIRCLE_CREATE_VIEW_LOCATION)
        } else {
            analytics.log(AmplitudeEvents.CIRCLE_EDIT_VIEW_LOCATION, AmplitudeKeys.CIRCLE_ID, circleId!!)
        }
    }

    private fun logTapCoverPhoto() {
        if (creating) {
            analytics.log(AmplitudeEvents.CIRCLE_CREATE_VIEW_COVER_PHOTO)
        } else {
            analytics.log(AmplitudeEvents.CIRCLE_EDIT_VIEW_COVER_PHOTO, AmplitudeKeys.CIRCLE_ID, circleId!!)
        }
    }

    private fun launchImagePicker() {
        CropImage.activity()
                .setCropShape(CropImageView.CropShape.RECTANGLE)
                .setAspectRatio(16, 9)
                .setCropMenuCropButtonTitle("Done")
                .setAllowFlipping(false)
                .setAllowRotation(false)
                .setAutoZoomEnabled(false)
                .setRequestedSize(1080, 1080)
                .setOutputUri(editCircleViewModel.temporaryPhotoUri)
                .start(this)
    }

    private fun launchLocationPicker() {
        if (canLaunchLocation) {
            canLaunchLocation = false
            startActivityForResult(PlacePicker.IntentBuilder().build(this), PLACE_PICKER_REQUEST)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_create_circle, menu)
        disposable += editCircleViewModel.submitEnabled(circleId, prefillInfo)
                .compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = { Log.d(TAG, "error", it) },
                        onNext = { (isEnabled, editInfo)  ->
                            val done = menu.findItem(R.id.done)
                            done.isEnabled = isEnabled
                            done.setOnMenuItemClickListener {
                                updateCircle(editInfo)
                                true
                            }
                        }
                )
        return true
    }

    private fun updateCircle(editInfo: EditCircleViewModel.EditCircleInfo) {
        editCircleViewModel.updateCircle(circleId, editInfo)
                .compose(applySingleSchedulers())
                .doOnSubscribe { showLoading(true) }
                .subscribeBy(
                        onError = { e ->
                            Log.e(TAG, "Error making group", e)
                            showLoading(false)
                            snackbar(R.string.network_error)
                        },
                        onSuccess = { circle ->
                            analytics.log(if (creating) AmplitudeEvents.CIRCLE_CREATE_COMMIT else AmplitudeEvents.CIRCLE_EDIT_COMMIT)
                            finish()

                            if (creating) {
                                start(CircleActivity.newIntent(this, circle.id))
                                start(CircleShareActivity.newIntent(this, circle.id))
                            }
                        }
                )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finishAndSlideBackOverToBottom()
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        finishAndSlideBackOverToBottom()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            PLACE_PICKER_REQUEST -> {

                // Reset boolean so user can re-pick location if they want
                canLaunchLocation = true

                if (resultCode == RESULT_OK) {
                    val place = PlacePicker.getPlace(this, data)
                    editCircleViewModel.setPlace(place)

                    if (creating) {
                        analytics.log(AmplitudeEvents.CIRCLE_CREATE_SET_LOCATION)
                    } else {
                        analytics.log(AmplitudeEvents.CIRCLE_EDIT_SET_LOCATION, AmplitudeKeys.CIRCLE_ID, circleId!!)
                    }
                }
            }

            PREAPPROVAL_REQUEST -> {
                if (resultCode == Activity.RESULT_OK) {
                    val newPreapproval = PreApproveActivity.fromIntent(data)
                    editCircleViewModel.setPreapproval(newPreapproval)
                }
            }

            CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    editCircleViewModel.uploadPhoto().compose(applySingleSchedulers())
                            .doOnSubscribe { showLoading(true) }
                            .subscribeBy(
                                    onSuccess = { uri ->
                                        Log.d(TAG, "Successfully uploaded new profile photo")
                                        showLoading(false)
                                        val coverPhotoItem = adapter.getItem(0) as EditCircleImageNameLocationItem
                                        coverPhotoItem.coverImageUri = uri

                                        if (creating) {
                                            analytics.log(AmplitudeEvents.CIRCLE_CREATE_SET_COVER_PHOTO)
                                        } else {
                                            analytics.log(AmplitudeEvents.CIRCLE_EDIT_SET_COVER_PHOTO, AmplitudeKeys.CIRCLE_ID, circleId!!)
                                        }
                                    },
                                    onError = { e ->
                                        Log.e(TAG, "Error uploading profile photo", e)
                                        snackbar(R.string.photo_upload_error)
                                        showLoading(false)
                                    }
                            )
                } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    snackbar(R.string.generic_error)
                }
            }
        }
    }

    @UiThread
    private fun showLoading(loading: Boolean) {
        if (loading) {
            spinner.visibility = View.VISIBLE
        } else {
            spinner.visibility = View.GONE
        }
    }

    override fun performInjection() {
        activityComponent.inject(this)
    }

    companion object {
        private var PLACE_PICKER_REQUEST = 1
        private const val PREAPPROVAL_REQUEST = 2

        private const val ARG_TITLE = "${BuildConfig.APPLICATION_ID}.title"
        private const val ARG_DESCRIPTION = "${BuildConfig.APPLICATION_ID}.description"
        private const val ARG_CATEGORIES = "${BuildConfig.APPLICATION_ID}.categories"


        fun newIntent(context: Context, circleId: String?): Intent {
            val intent = Intent(context, EditCircleActivity::class.java)
            intent.putExtra(Circle.ARG_CIRCLE, circleId)
            return intent
        }

        fun newPrefillIntent(context: Context, prefillInfo: PrefillInfo): Intent {
            val intent = Intent(context, EditCircleActivity::class.java)
            intent.putExtra(ARG_TITLE, prefillInfo.title)
            intent.putExtra(ARG_DESCRIPTION, prefillInfo.description)
            intent.putStringArrayListExtra(ARG_CATEGORIES, ArrayList(prefillInfo.categories))
            return intent
        }
    }

    data class PrefillInfo(val title: String? = "",
                           val categories: List<String> = listOf(),
                           val description: String? = "") {

        constructor(uri: Uri): this(
                title = uri.getQueryParameter("title"),
                categories = uri.getQueryParameters("category"),
                description = uri.getQueryParameter("description"))
    }
}
