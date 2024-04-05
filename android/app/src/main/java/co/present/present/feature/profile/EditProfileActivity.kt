package co.present.present.feature.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.lifecycle.ViewModelProviders
import co.present.present.BaseActivity
import co.present.present.R
import co.present.present.analytics.AmplitudeEvents
import co.present.present.extensions.*
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_edit_profile.*
import kotlinx.android.synthetic.main.toolbar.*

open class EditProfileActivity : BaseActivity() {
    private val TAG: String = javaClass.simpleName

    override fun performInjection() {
        activityComponent.inject(this)
    }

    lateinit var viewModel: EditProfileViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)
        analytics.log(AmplitudeEvents.PROFILE_VIEW_EDIT_BIO)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(EditProfileViewModel::class.java)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setTitle(R.string.edit_profile)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_close)
        }

        viewModel.getTemporaryBio()
                .compose(applySingleSchedulers())
                .subscribeBy(
                        onError = { e ->
                            Log.e(TAG, "Failed to get current user, this should never happen!", e)
                        },
                        onSuccess = { temporaryBio ->
                            bio.setText(temporaryBio)
                            bio.setSelection(temporaryBio.length)
                            bio.post { bio.showKeyboard() }
                        }
                ).addTo(disposable)

        viewModel.getTemporaryPhotoUri().compose(applyMaybeSchedulers()).subscribeBy(
                onSuccess = {
                    loadProfilePhoto(it)
                },
                onComplete = {
                    Log.d(TAG, "No temporary photo uri, bc user has no FB profile photo")
                },
                onError = { e ->
                    Log.e(TAG, "Error getting temp photo URI -- prob bc of a network failure downloading FB profile photo", e)
                }
        ).addTo(disposable)



        viewModel.getTemporaryName()
                .compose(applySingleSchedulers())
                .subscribeBy(
                        onSuccess = {
                            firstName.setText(it.first, TextView.BufferType.EDITABLE)
                            lastName.setText(it.second, TextView.BufferType.EDITABLE)
                            firstName.showKeyboard()
                        }
                )

        bio.afterTextChanged { viewModel.bioChanged(bio.text.toString()) }
        firstName.afterTextChanged { afterTextChanged() }
        lastName.afterTextChanged { afterTextChanged() }
        editButton.setOnClickListener { editProfileImage() }
    }

    private fun afterTextChanged() {
        val firstNameString = firstName.text.toString()
        val lastNameString = lastName.text.toString()
        viewModel.nameChanged(firstNameString, lastNameString)
    }

    private fun postBio(item: MenuItem) {
        viewModel.postProfile()
                .compose(applyCompletableSchedulers())
                .doOnSubscribe { item.isEnabled = false }
                .subscribeBy(
                        onError = { e ->
                            Log.e(TAG, "Failed to post current user profile to server", e)
                            item.isEnabled = true
                            snackbar(R.string.network_error)
                        },
                        onComplete = {
                            analytics.log(AmplitudeEvents.PROFILE_EDIT_BIO)
                            finish()
                        }
                ).addTo(disposable)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.edit_profile, menu)
        viewModel.submitEnabled().subscribeBy(
                onError = {},
                onNext = {
                    menu.findItem(R.id.save).isEnabled = it
                }
        )
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.save -> postBio(item)
        }

        return true
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
                .start(this)
    }

    private fun loadProfilePhoto(imageUri: Uri) {
        // Don't allow Glide to cache the image, otherwise it won't load a new one from
        // the same URI if the user tries a second time
        photo.loadCircularImageFromUri(imageUri, skipMemoryCache = true, diskCacheStrategy = DiskCacheStrategy.NONE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                clearPhoto()
                spinner.show()
                savePhoto()
            }
        } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
            snackbar(R.string.generic_error)
        }
    }

    private fun clearPhoto() {
        photo.setImageResource(R.drawable.circle_purple)
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

    override fun onBackPressed() {
        super.onBackPressed()
        viewModel.clearTemporaryBio()
        viewModel.clearTemporaryName()
        viewModel.clearTemporaryPhoto()
    }
}