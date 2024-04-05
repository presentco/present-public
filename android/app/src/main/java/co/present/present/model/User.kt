package co.present.present.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import co.present.present.BuildConfig
import present.proto.UserResponse

@Entity
data class User(@PrimaryKey var id: String,
                var bio: String,
                var firstName: String,
                var name: String,
                var photo: String,
                var interests: List<@JvmSuppressWildcards Interest>,
                var link: String,
                val member: Boolean) {

    @Ignore
    constructor(userResponse: UserResponse) : this(
            id = userResponse.id,
            bio = userResponse.bio ?: "",
            firstName = userResponse.firstName ?: "",
            name = userResponse.name ?: "",
            photo = userResponse.photo ?: "",
            interests = userResponse.interests.mapNotNull { it.toInterest() },
            link = userResponse.link,
            member = userResponse.member)

    @Ignore
    constructor(currentUser: CurrentUser) : this(
            id = currentUser.id,
            bio = currentUser.bio,
            firstName = currentUser.name.first,
            name = currentUser.fullName,
            photo = currentUser.photo,
            interests = currentUser.interests,
            link = currentUser.link,
            member = true
    )

    companion object {
        @JvmStatic val USER = "${BuildConfig.APPLICATION_ID}.User"
        val shortLinkPath = "u"
    }
}

fun User.isOwner(circle: Circle) = id == circle.ownerId

fun User.isAlso(currentUser: CurrentUser?) = id == currentUser?.id

fun User.isNot(currentUser: CurrentUser) = !isAlso(currentUser)


