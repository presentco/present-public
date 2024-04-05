package co.present.present.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import present.proto.Gender
import present.proto.UserNotificationSettings
import present.proto.UserProfile

@Entity
data class CurrentUser(@PrimaryKey var id: String,
                       var bio: String,
                       @Embedded(prefix = "name_") var name: Name,
                       var photo: String,
                       var interests: List<@JvmSuppressWildcards Interest>,
                       var isAdmin: Boolean,
                       var appShareUrl: String,
                       val facebookLinked: Boolean,
                       val phoneVerified: Boolean,
                       @Embedded var notificationSettings: NotificationSettings,
                       var link: String,
                       var gender: Int?) {

    @Ignore
    constructor(userProfile: UserProfile) : this(
            id = userProfile.id,
            bio = userProfile.bio ?: "",
            name = Name(userProfile.name),
            photo = userProfile.photo ?: "",
            interests = (userProfile.interests ?: listOf()).mapNotNull { it.toInterest() },
            isAdmin = userProfile.isAdmin,
            appShareUrl = userProfile.appShareLink,
            facebookLinked = userProfile.facebookLinked,
            phoneVerified = userProfile.phoneVerified,
            notificationSettings = NotificationSettings(userProfile.notificationSettings),
            link = userProfile.link,
            gender = userProfile.gender?.value)


    val fullName: String
        get() = "${name.first} ${name.last}"

    val isMan get() = gender?.equals(Gender.MAN.value) == true

}

data class NotificationSettings(
        var userCommentsOnJoinedGroup: Boolean,
        var userJoinsOwnedGroup: Boolean) {

    @Ignore
    constructor(settings: UserNotificationSettings) : this(
            userCommentsOnJoinedGroup = settings.userCommentsOnJoinedGroup ?: false,
            userJoinsOwnedGroup = settings.userJoinsOwnedGroup ?: false)
}

fun CurrentUser.isOwner(circle: Circle): Boolean = id == circle.ownerId

fun CurrentUser?.canDelete(circle: Circle) = this != null && (isAdmin || isOwner(circle))

fun CurrentUser.canEdit(circle: Circle) = isAdmin || isOwner(circle)


