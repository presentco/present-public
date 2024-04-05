package co.present.present.feature.profile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import co.present.present.R
import co.present.present.db.Database
import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.feature.common.viewmodel.UploadPhoto
import co.present.present.model.CurrentUser
import co.present.present.service.BitmapDownloader
import co.present.present.service.Filesystem
import co.present.present.service.rpc.putUserProfile
import co.present.present.user.UserDataProvider
import io.reactivex.*
import io.reactivex.subjects.BehaviorSubject
import present.proto.UserService
import javax.inject.Inject

/**
 * TODO: Switch this to use Rx like circle edit view model
 * Using the user prefs to persist temporary name, photo etc made sense with our original
 * onboarding flow (which could be restored between app launches if not completed) but doesn't
 * make sense anymore.
 *
 */
class EditProfileViewModel @Inject constructor(
        application: Application,
        private var userPrefs: UserDataProvider,
        val database: Database,
        private val userService: UserService,
        private val getCurrentUser: GetCurrentUser,
        private val uploadPhoto: ProfileUploadPhotoImpl,
        private val filesystem: Filesystem,
        private val bitmapDownloader: BitmapDownloader)
    : AndroidViewModel(application),
        GetCurrentUser by getCurrentUser,
        UploadPhoto by uploadPhoto {

    fun getOutputUri(): Uri = temporaryPhotoUri

    private val doneEnabled = BehaviorSubject.createDefault(true)

    /**
     * Get either a copy of the user's current profile picture (from server) or the locally stored
     * temporary file, if they are in the middle of choosing one in the onboarding flow.  This way,
     * the activity is totally source-agnostic
     */
    fun getTemporaryPhotoUri(): Maybe<Uri> {
        return Maybe.create<Uri> { emitter ->
            if (temporaryPhotoFile.exists()) {
                emitter.onSuccess(temporaryPhotoUri)
            } else {
                emitter.onComplete()
            }
        }.switchIfEmpty(currentUser.firstElement().flatMap { currentUser ->
            loadProfilePictureToLocalStorage(currentUser)
        })
    }

    fun getTemporaryBio(): Single<String> {
        return currentUser.firstOrError().map {
            if (userPrefs.bio == null) {
                userPrefs.bio = it.bio
            }
            userPrefs.bio
        }
    }

    fun showFacebookButton(): Flowable<Boolean> {
        return currentUser.map {  !it.facebookLinked }
    }

    fun bioChanged(bio: String) {
        userPrefs.bio = bio
    }

    fun clearTemporaryBio() {
        userPrefs.bio = null
    }

    fun postProfile(): Completable {
        return currentUser.firstOrError().flatMap { user ->
            userService.putUserProfile(
                bio = userPrefs.bio,
                interests = user.interests,
                name = Pair(userPrefs.firstName!!, userPrefs.lastName!!),
                photoUuid = userPrefs.photoUuid
            )
        }
                .flatMapCompletable { userProfileResponse ->
                    clearTemporaryBio()
                    database.persistUserProfile(userProfileResponse)
                }
    }

    fun postPhotoAndName(): Completable {
        return currentUser.firstOrError().flatMap { user ->
            userService.putUserProfile(name = Pair(userPrefs.firstName!!, userPrefs.lastName!!),
                    bio = user.bio, interests = user.interests, photoUuid = userPrefs.photoUuid) }
                .flatMapCompletable {
                    clearTemporaryPhoto()
                    database.persistUserProfile(it)
                }
    }

    fun clearTemporaryPhoto() {
        temporaryPhotoFile.delete()
    }

    fun getTemporaryName(): Single<Pair<String, String>> {
        return currentUser.firstOrError().map { profile ->
            if (userPrefs.firstName.isNullOrEmpty() || userPrefs.lastName.isNullOrEmpty()) {
                userPrefs.firstName = profile.name.first
                userPrefs.lastName = profile.name.last
            }

            // We guarantee in the previous statement that these values are never null (but not
            // sure how to express that to the compiler ...)
            Pair(userPrefs.firstName!!, userPrefs.lastName!!)
        }
    }

    fun nameChanged(firstName: String, lastName: String) {
        userPrefs.firstName = firstName
        userPrefs.lastName = lastName
        doneEnabled.onNext(canSubmit(firstName, lastName))
    }

    private fun canSubmit(firstName: String, lastName: String) =
            firstName.isNotEmpty() && lastName.isNotEmpty()

    fun submitEnabled() = doneEnabled.toFlowable(BackpressureStrategy.LATEST)

    fun clearTemporaryName() {
        userPrefs.firstName = null
        userPrefs.lastName = null
    }

    private fun loadProfilePictureToLocalStorage(profile: CurrentUser): Maybe<Uri> {
        return Maybe.create { emitter ->
            // Just return empty uri if no profile photo
            if (profile.photo.isEmpty()) {
                emitter.onComplete()
                return@create
            }

            // Load user's profile picture to local file storage
            val bitmap = bitmapDownloader.download(getApplication(), profile.photo, R.dimen.avatar_large_dimen)
            filesystem.writeToFile(bitmap, temporaryPhotoFile)
            emitter.onSuccess(temporaryPhotoUri)
        }
    }
}