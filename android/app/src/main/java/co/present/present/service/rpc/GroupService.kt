package co.present.present.service.rpc

import android.location.Location
import co.present.present.location.toCoordinates
import co.present.present.model.Circle
import co.present.present.model.MemberRequest
import co.present.present.model.Space
import io.reactivex.Completable
import io.reactivex.Single
import present.proto.*

typealias CircleService = GroupService


fun CircleService.markRead(circleId: String, lastReadIndex: Int): Completable {
    return Completable.fromCallable {
        markRead(MarkReadRequest.Builder().groupId(circleId).lastRead(lastReadIndex).build())
    }
}

fun CircleService.approveMember(userId: String, circleId: String): Completable {
    return Completable.fromCallable {
        addMembers(MembersRequest.Builder()
                .groupId(circleId)
                .userIds(listOf(userId))
                .build()
        )
    }
}

fun CircleService.removeMember(userId: String, circleId: String): Completable {
    return Completable.fromCallable {
        removeMembers(MembersRequest.Builder()
                .groupId(circleId)
                .userIds(listOf(userId))
                .build())
    }
}

fun CircleService.getExplore(): Single<String> {
    return Single.fromCallable { getExploreHtml(ExploreHtmlRequest(null)) }.map { it.html }
}

fun CircleService.getMemberRequests(circleId: String): Single<List<MemberRequest>> {
    return Single.fromCallable {
        getMembershipRequests(MembershipRequestsRequest(circleId))
    }.map { it.requests.map { MemberRequest(it) } }
}

fun CircleService.getCircle(uuid: String): Single<Circle> {
    return Single.fromCallable {
        getGroup(GroupRequest.Builder().groupId(uuid).build())
    }.map { Circle(it) }
}

fun CircleService.putCircle(uuid: String,
                            title: String,
                            description: String,
                            locationName: String,
                            latitude: Double,
                            longitude: Double,
                            categories: List<String>,
                            createdFrom: Coordinates,
                            space: Space,
                            preapproval: GroupMemberPreapproval,
                            discoverable: Boolean,
                            photoUuid: String? = null): Single<Circle> {
    return Single.fromCallable {
        var photoRequest: ContentReferenceRequest? = null
        photoUuid?.let {
            photoRequest = ContentReferenceRequest(it, ContentType.JPEG)
        }
        val putGroupRequest = PutGroupRequest.Builder().uuid(uuid)
                .location(Coordinates(latitude, longitude, 0.0))
                .title(title)
                .discoverable(discoverable)
                .categories(categories)
                .description(description)
                .createdFrom(createdFrom)
                .locationName(locationName)
                .suggestedLocation(null)
                .preapprove(preapproval)
                .spaceId(space.id)
                .cover(photoRequest)
                .build()
        putGroup(putGroupRequest)
    }.map { Circle(it.group) }
}

fun CircleService.putCircle(circle: Circle, location: Location): Completable {
    return Completable.fromCallable {
        var photoRequest: ContentReferenceRequest? = null
        circle.coverPhotoId?.let {
            photoRequest = ContentReferenceRequest(it, ContentType.JPEG)
        }
        val putGroupRequest = PutGroupRequest.Builder().uuid(circle.id)
                .location(Coordinates(circle.latitude, circle.longitude, 0.0))
                .title(circle.title)
                .discoverable(circle.discoverable)
                .categories(circle.categories)
                .description(circle.description)
                .locationName(circle.locationName)
                .createdFrom(location.toCoordinates())
                .preapprove(circle.getGroupMemberPreapproval())
                .spaceId(circle.spaceId)
                .apply {
                    if (circle.startTime != null && circle.startTime > 0) {
                        schedule(Schedule.Builder()
                                .startTime(circle.startTime)
                                .apply {
                                    if (circle.endTime != null && circle.endTime > 0) endTime(circle.endTime)
                                }
                                .build())
                    }
                }
                .cover(photoRequest)
                .build()
        putGroup(putGroupRequest)
    }
}

fun CircleService.getNearbyCircles(spaceId: String, location: Location): Single<List<Circle>> {
    return Single.fromCallable {
        getNearbyGroups(NearbyGroupsRequest.Builder()
                .location(location.toCoordinates())
                .spaceId(spaceId)
                .build())
    }.map { it.nearbyGroups.map { Circle(it) } }
}

fun CircleService.getJoinedCircles(userId: String? = null): Single<List<Circle>> {
    return Single.fromCallable {
        getJoinedGroups(JoinedGroupsRequest.Builder().userId(userId).build())
    }.map { it.groups.map { Circle(it) } }
}

fun CircleService.joinCircle(circleId: String): Single<GroupMembershipState> {
    return Single.fromCallable {
        joinGroup(JoinGroupRequest.Builder().groupId(circleId).build()).result
    }
}

fun CircleService.leaveCircle(circleId: String): Completable {
    return Completable.fromCallable {
        leaveGroup(LeaveGroupRequest(circleId))
    }
}

fun CircleService.getMembers(circleId: String): Single<GroupMembersResponse> {
    return Single.fromCallable {
        getGroupMembers(GroupMembersRequest(circleId))
    }
}

fun CircleService.getPastComments(circleId: String): Single<PastCommentsResponse> {
    return Single.fromCallable {
        getPastComments(PastCommentsRequest.Builder().groupId(circleId).build())
    }
}

fun CircleService.getLiveServer(circleId: String): Single<FindLiveServerResponse> {
    return Single.fromCallable {
        findLiveServer(FindLiveServerRequest(circleId))
    }
}

fun CircleService.postComment(messageId: String, circleId: String, message: String, photoUuid: String? = null): Completable {
    return Completable.fromCallable {
        val contentRefReq = if (photoUuid != null) ContentReferenceRequest(photoUuid, ContentType.JPEG) else null
        putComment(PutCommentRequest(messageId, circleId, message, contentRefReq))
    }
}

fun CircleService.deleteComment(messageId: String): Completable {
    return Completable.fromCallable {
        deleteComment(DeleteCommentRequest(messageId))
    }
}

fun CircleService.getReferralCount(): Single<CountGroupReferralsResponse> {
    return Single.fromCallable {
        countGroupReferrals(Empty())
    }
}

fun CircleService.getReferrals(): Single<GroupReferralsResponse> {
    return Single.fromCallable {
        getGroupReferrals(Empty())
    }
}

fun CircleService.getCities(): Single<List<City>> {
    return Single.fromCallable {
        getCities(Empty())
    }.map { it.cities }
}

fun CircleService.mute(circleId: String): Completable {
    return Completable.fromCallable { muteGroup(MuteGroupRequest(circleId)) }
}

fun CircleService.unmute(circleId: String): Completable {
    return Completable.fromCallable { unMuteGroup(UnmuteGroupRequest(circleId)) }
}

fun CircleService.deleteCircle(circleId: String): Completable {
    return Completable.fromCallable {
        deleteGroup(DeleteGroupRequest(circleId))
    }
}

fun CircleService.reportCircle(circleId: String, reason: FlagReason): Completable {
    return Completable.fromCallable {
        flagGroup(FlagGroupRequest(circleId, reason, null))
    }
}

fun CircleService.inviteUsers(circleId: String, userIds: List<String>): Completable {
    return Completable.fromCallable {
        inviteFriends(InviteFriendsRequest.Builder()
                .groupId(circleId)
                .userIds(userIds).build())
    }
}
