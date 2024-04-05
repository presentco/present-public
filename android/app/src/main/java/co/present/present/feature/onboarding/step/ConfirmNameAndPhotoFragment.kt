package co.present.present.feature.onboarding.step


import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import co.present.present.BaseFragment
import co.present.present.R
import co.present.present.analytics.AmplitudeEvents
import co.present.present.extensions.*
import co.present.present.feature.onboarding.events.AdvanceOnboardingEvent
import co.present.present.feature.profile.EditProfileViewModel
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.squareup.otto.Bus
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_onboarding_photo.*
import javax.inject.Inject


open class ConfirmNameAndPhotoFragment : BaseFragment() {
    private val TAG = javaClass.simpleName
    @Inject lateinit var bus: Bus
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    lateinit var viewModel: EditProfileViewModel

    @get:LayoutRes
    open val layoutResId: Int = R.layout.fragment_onboarding_photo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        performInjection()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        logView()
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(EditProfileViewModel::class.java)
//        viewModel.getTemporaryPhotoUri().compose(applyMaybeSchedulers()).subscribeBy(
//                onSuccess = {
//                    loadProfilePhoto(it)
//                },
//                onComplete = {
//                    Log.d(TAG, "No temporary photo uri, bc user has no FB profile photo")
//                },
//                onError = { e ->
//                    Log.e(TAG, "Error getting temp photo URI -- prob bc of a network failure downloading FB profile photo", e)
//                }
//        ).addTo(disposable)
    }

    open fun logView() {
        analytics.log(AmplitudeEvents.SIGNUP_VIEW)
    }

    override fun onResume() {
        super.onResume()

        viewModel.getTemporaryName()
                .compose(applySingleSchedulers())
                .subscribeBy(
                        onSuccess = {
                            firstName.setText(it.first, TextView.BufferType.EDITABLE)
                            lastName.setText(it.second, TextView.BufferType.EDITABLE)
                        }
                ).addTo(disposable)

        viewModel.showFacebookButton().compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = {},
                        onNext = { show ->
                            listOf(facebookButton, divider, or).forEach {
                                it.setVisible(show)
                            }
                        }
                ).addTo(disposable)

        viewModel.getTemporaryPhotoUri().compose(applyMaybeSchedulers()).subscribeBy(
                onSuccess = {
                    loadProfilePhoto(it)
                },
                onComplete = {
                    Log.e(TAG, "No temporary photo uri, bc user has no FB profile photo")
                },
                onError = { e ->
                    Log.e(TAG, "Error getting temp photo URI -- prob bc of a network failure downloading FB profile photo", e)
                }
        ).addTo(disposable)

        firstName.afterTextChanged { afterTextChanged() }
        lastName.afterTextChanged { afterTextChanged() }
        facebookButton.setOnClickListener {
            startActivityForResult(Intent(requireContext(), FacebookLinkActivity::class.java), REQUEST_FACEBOOK)
        }
    }

    private fun afterTextChanged() {
        val firstNameString = firstName.text.toString()
        val lastNameString = lastName.text.toString()
        viewModel.nameChanged(firstNameString, lastNameString)
        nextButton.isEnabled = firstNameString.isNotBlank() && lastNameString.isNotBlank()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(layoutResId, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        editButton.setOnClickListener { editProfileImage() }
        nextButton.setOnClickListener { onNextButtonClicked() }
    }

    fun onNextButtonClicked() {
        viewModel.postPhotoAndName()
                .compose(applyCompletableSchedulers())
                .doOnSubscribe {
                    spinner.show()
                }
                .subscribeBy(
                        onError = { e ->
                            spinner.hide()
                            Log.e(TAG, "Error updating photo and name", e)
                        },
                        onComplete = {
                            Log.d(TAG, "User profile posted successfully")
                            onPostNameAndPhotoSuccess()
                        })
    }

    open fun onPostNameAndPhotoSuccess() {
        advance()
        logSubmit()
    }

    open fun logSubmit() {
        analytics.log(AmplitudeEvents.SIGNUP_SUBMIT)
    }

    private fun clearPhoto() {
        photo.setImageResource(R.drawable.circle_purple)
    }

    private fun loadProfilePhoto(imageUri: Uri) {
        // Don't allow Glide to cache the image, otherwise it won't load a new one from
        // the same URI if the user tries a second time
        photo.loadCircularImageFromUri(imageUri, skipMemoryCache = true, diskCacheStrategy = DiskCacheStrategy.NONE)
    }

    private fun editProfileImage() {
        CropImage.activity()
                .setCropShape(CropImageView.CropShape.RECTANGLE)
                .setAspectRatio(1, 1)
                .setCropMenuCropButtonTitle("Done")
                .setAllowFlipping(false)
                .setAllowRotation(false)
                .setAutoZoomEnabled(false)
                .setRequestedSize(1080, 1080)
                .setOutputUri(viewModel.getOutputUri())
                .start(context!!, this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                clearPhoto()
                spinner.show()
                savePhoto()
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                snackbar(R.string.generic_error)
            }
        }
    }

    private fun savePhoto() {
        viewModel.uploadPhoto()
                .compose(applySingleSchedulers())
                .subscribeBy(
                        onSuccess = { imageUri ->
                            Log.d(TAG, "Successfully uploaded new profile photo")
                            spinner.hide()
                            loadProfilePhoto(imageUri)
                        },
                        onError = { e ->
                            Log.e(TAG, "Error uploading profile photo", e)
                            snackbar(R.string.photo_upload_error)
                            spinner.hide()
                        }).addTo(disposable)
    }

    protected open fun performInjection() {
        activityComponent.inject(this)
    }

    private fun advance() {
        bus.post(AdvanceOnboardingEvent())
    }

    companion object {
        const val REQUEST_FACEBOOK = 1234
    }
}
