package co.present.present.feature.profile

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import co.present.present.db.Database
import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.model.CurrentUser
import co.present.present.service.BitmapDownloader
import co.present.present.service.Filesystem
import co.present.present.support.DataHelper
import co.present.present.user.UserDataProvider
import com.nhaarman.mockito_kotlin.*
import io.reactivex.Flowable
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import present.proto.UserProfile
import present.proto.UserService
import java.io.File

class EditProfileViewModelTest {

    lateinit var testObject: EditProfileViewModel
    @Mock lateinit var application: Application
    @Mock lateinit var database: Database
    @Mock lateinit var getCurrentUser: GetCurrentUser
    @Mock lateinit var userService: UserService
    @Mock lateinit var uploadPhoto: ProfileUploadPhotoImpl
    @Mock lateinit var userPrefs: UserDataProvider
    @Mock lateinit var filesystem: Filesystem
    @Mock lateinit var tempPhotoFile: File
    @Mock lateinit var tempPhotoUri: Uri
    @Mock lateinit var bitmapDownloader: BitmapDownloader
    @Mock lateinit var bitmap: Bitmap
    var currentUser: CurrentUser = DataHelper.getCurrentUser()
    @Mock lateinit var userProfile: UserProfile


    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        testObject = EditProfileViewModel(application, userPrefs, database, userService, getCurrentUser, uploadPhoto, filesystem, bitmapDownloader)
        whenever(getCurrentUser.currentUser) doReturn Flowable.just(currentUser)
        whenever(filesystem.uriFromFile(tempPhotoFile)) doReturn tempPhotoUri
        whenever(filesystem.fromFilesDir(any())) doReturn tempPhotoFile
        whenever(bitmapDownloader.download(any(), any(), any())) doReturn bitmap
        whenever(uploadPhoto.temporaryPhotoUri) doReturn tempPhotoUri
        whenever(uploadPhoto.temporaryPhotoFile) doReturn tempPhotoFile
    }

    @Test
    fun whenThereIsAlreadyATemporaryPhotoOnDisk_thenJustReturnUri() {
        whenever(tempPhotoFile.exists()) doReturn true
        currentUser.photo = "https://photo.url"

        testObject.getTemporaryPhotoUri().test().assertResult(tempPhotoUri)

        // Assert that we don't try to download the profile picture or save it to the file system
        verifyZeroInteractions(bitmapDownloader)
        verify(filesystem, times(0)).writeToFile(any(), any())
    }

    @Test
    fun whenThereIsNoTemporaryPhotoOnDisk_andNoFacebookPhotoUrl_thenReturnOnComplete() {
        whenever(tempPhotoFile.exists()) doReturn false
        currentUser.photo = ""

        testObject.getTemporaryPhotoUri().test().assertResult()
    }

    @Test
    fun whenThereIsNoTemporaryPhotoOnDisk_andThereIsAFacebookPhotoUrl_thenSaveFromNetworkToFilesystemAndReturnUri() {
        whenever(tempPhotoFile.exists()) doReturn false
        currentUser.photo = "https://photo.url"

        testObject.getTemporaryPhotoUri().test().assertResult(tempPhotoUri)

        verify(bitmapDownloader).download(any(), any(), any())
        verify(filesystem).writeToFile(any(), any())
    }

}