package co.present.present.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import co.present.present.BuildConfig
import co.present.present.extensions.newLocation
import present.proto.GroupMemberPreapproval
import present.proto.GroupMembershipState
import present.proto.GroupResponse

@Entity
data class Circle(@PrimaryKey var id: String,
                  var ownerId: String,
                  var ownerPhoto: String?,
                  var creationTime: Long,
                  var title: String,
                  var participantCount: Int,
                  var commentCount: Int,
                  var lastCommentTime: Long,
                  var locationName: String,
                  var coverPhoto: String?,
                  var coverPhotoId: String?,
                  var categories: List<@JvmSuppressWildcards String>,
                  var description: String,
                  var url: String,
                  var latitude: Double,
                  var longitude: Double,
                  var accuracy: Double,
                  var joined: Boolean,
                  val joinRequests: Int,
                  var unread: Boolean,
                  val unreadCount: Int,
                  var muted: Boolean,
                  val startTime: Long?,
                  val endTime: Long?,
                  val spaceId: String,
                  val discoverable: Boolean,
                  val membershipState: Int,
                  val preapproval: Int) {

    @Ignore
    constructor(groupResponse: GroupResponse) : this(
            id = groupResponse.uuid,
            ownerId = groupResponse.owner.id,
            ownerPhoto = groupResponse.owner.photo,
            creationTime = groupResponse.creationTime ?: 0,
            title = groupResponse.title ?: "",
            participantCount = groupResponse.memberCount ?: 0,
            commentCount = groupResponse.commentCount ?: 0,
            lastCommentTime = groupResponse.lastCommentTime ?: 0,
            locationName = groupResponse.locationName ?: "",
            coverPhoto = groupResponse.cover?.content,
            coverPhotoId = groupResponse.cover?.uuid,
            categories = groupResponse.categories,
            description = groupResponse.description ?: "",
            url = groupResponse.url,
            latitude = groupResponse.location.latitude,
            longitude = groupResponse.location.longitude,
            accuracy = groupResponse.location.accuracy,
            joined = groupResponse.joined ?: false,
            joinRequests = groupResponse.joinRequests ?: 0,
            unread = groupResponse.unread ?: false,
            unreadCount = groupResponse.unreadCount ?: 0,
            muted = groupResponse.muted ?: false,
            startTime = groupResponse.schedule?.startTime,
            endTime = groupResponse.schedule?.endTime,
            spaceId = groupResponse.space.id,
            discoverable = groupResponse.discoverable,
            membershipState = groupResponse.membershipState?.value ?: GroupMembershipState.NONE.value,
            preapproval = groupResponse.preapprove.value
    )

    val location get() = newLocation().apply {
        latitude = this@Circle.latitude
        longitude = this@Circle.longitude
        accuracy = this@Circle.accuracy.toFloat()
    }

    fun hasComments() = commentCount > 0

    // This shouldn't ever be false in real life, but it could happen in staging.
    fun hasMembers() = participantCount > 0

    fun getGroupMembershipState(): GroupMembershipState {
        return GroupMembershipState.fromValue(membershipState)
    }

    fun isJoinedOrRequested(): Boolean {
        return listOf(GroupMembershipState.ACTIVE, GroupMembershipState.REQUESTED, GroupMembershipState.REJECTED)
                .contains(getGroupMembershipState())
    }

    fun getSpace(): Space {
        return if (spaceId == Space.womenOnlyId) return Space.WomenOnly else Space.Everyone
    }

    fun getGroupMemberPreapproval(): GroupMemberPreapproval {
        return GroupMemberPreapproval.fromValue(preapproval)
    }

    fun isNotJoinedOrRequested(): Boolean = !isJoinedOrRequested()

    fun isWomenOnly(): Boolean = spaceId == Space.womenOnlyId

    companion object {
         @JvmStatic val ARG_CIRCLE = "${BuildConfig.APPLICATION_ID}.Circle"
        val shortLinkPath = "g"
    }
}
