package co.present.present.service.rpc

import android.net.Uri
import co.present.present.model.FriendshipState
import co.present.present.model.Interest
import co.present.present.model.User
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import present.proto.*


fun UserService.verify(code: String): Single<AuthorizationResponse> {
    return Single.fromCallable {
        verify(VerifyRequest(null, code))
    }
}
fun UserService.requestVerification(phoneNumber: String): Single<Int> {
    return Single.fromCallable {
        requestVerification(RequestVerificationRequest(phoneNumber))
    }.map { it.codeLength }
}

fun UserService.putUserProfile(name: Pair<String, String>? = null, photoUuid: String? = null, bio: String? = null, interests: Collection<Interest>? = null): Single<UserProfile> {
    return Single.fromCallable {
        var photoRequest: ContentReferenceRequest? = null
        photoUuid?.let {
            photoRequest = ContentReferenceRequest(it, ContentType.JPEG)
        }

        putUserProfile(UserProfileRequest(
                if (name != null) UserName(name.first, name.second) else null,
                photoRequest,
                bio,
                interests?.map { it.canonicalString }, null, null
        ))
    }
}

fun UserService.verifyLink(uri: Uri): Single<AuthorizationResponse> {
    return Single.fromCallable {
        verify(VerifyRequest(uri.toString(), null))
    }
}

fun UserService.getUserProfile(): Single<UserProfile> {
    return Single.fromCallable {
        getUserProfile(Empty())
    }
}

fun UserService.completeSignup(): Single<AuthorizationResponse> {
    return Single.fromCallable {
        completeSignup(Empty())
    }
}

fun UserService.synchronize(notificationsEnabled: Boolean): Single<SynchronizeResponse> {
    return Single.fromCallable {
        synchronize(SynchronizeRequest(notificationsEnabled))
    }
}

fun UserService.linkFacebook(token: String, uuid: String): Observable<AuthorizationResponse> {
    return Observable.fromCallable { linkFacebook(LinkFacebookRequest(token, uuid)) }
}

fun UserService.putToken(token: String?): Completable {
    return Completable.fromCallable {
        putDeviceToken(PutDeviceTokenRequest(token, PutDeviceTokenRequest.ApnsEnvironment.PRODUCTION))
    }
}

fun UserService.getUser(userId: String): Single<UserResponse> {
    return Single.fromCallable {
        getUser(UserRequest.Builder().userId(userId).build())
    }
}

fun UserService.blockUser(userId: String): Completable {
    return Completable.fromCallable {
        blockUser(UserRequest.Builder().userId(userId).build())
    }
}

fun UserService.unblockUser(userId: String): Completable {
    return Completable.fromCallable {
        unblockUser(UserRequest.Builder().userId(userId).build())
    }
}

fun UserService.getBlocked(): Single<List<User>> {
    return Single.fromCallable {
        getBlockedUsers(Empty())
    }.map { it.users.map { User(it) } }
}

fun UserService.getIncomingFriendRequests(): Single<List<User>> {
    return Single.fromCallable {
        getIncomingFriendRequests(Empty()).users.map { User(it) }
    }
}

fun UserService.getOutgoingFriendRequests(): Single<List<User>> {
    return Single.fromCallable {
        getOutgoingFriendRequests(Empty()).users.map { User(it) }
    }
}

fun UserService.getFriends(userId: String): Single<List<User>> {
    return Single.fromCallable {
        getFriends(UserRequest.Builder().userId(userId).build()).users.map { User(it) }
    }
}

fun UserService.removeFriend(userId: String): Completable {
    return Completable.fromCallable { removeFriend(UserRequest.Builder().userId(userId).build()) }
}

fun UserService.addFriend(userId: String? = null, phoneNumber: String? = null): Single<FriendshipState> {
    return Single.fromCallable {
        val request = UserRequest.Builder().apply {
            userId?.let { userId(it) }
            phoneNumber?.let { phoneNumber(it) }
        }.build()
        addFriend(request).result
    }.map { co.present.present.model.FriendshipState.fromFriendshipState(it) }
}