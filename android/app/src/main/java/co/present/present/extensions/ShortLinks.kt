package co.present.present.extensions

import android.net.Uri
import co.present.present.model.Category
import co.present.present.model.Circle
import co.present.present.model.User

val shortLinkPaths = listOf(Category.shortLinkPath, Circle.shortLinkPath, User.shortLinkPath)

fun Uri.isValidShortLink(): Boolean {
    return pathSegments.size > 1 && shortLinkPaths.contains(pathSegments[0])
}

fun Uri.isCircleRequestsLink(): Boolean {
    return pathSegments.size > 1 && pathSegments[0] == Circle.shortLinkPath && fragment == "requests"
}

fun Uri.isAppLink(): Boolean {
    return pathSegments.size > 0 && pathSegments[0] == "app"
}

fun Uri.isCreateCircleLink(): Boolean {
    return pathSegments.size > 1 && pathSegments[0] == "app" && pathSegments[1] == "createCircle"
}

fun Uri.isTourLink(): Boolean {
    return pathSegments.size > 1 && pathSegments[0] == "app" && pathSegments[1] == "tour"
}

fun Uri.isChooseLocationLink(): Boolean {
    return pathSegments.size > 1 && pathSegments[0] == "app" && pathSegments[1] == "changeLocation"
}

fun Uri.isAddFriendsLink(): Boolean {
    return pathSegments.size > 1 && pathSegments[0] == "app" && pathSegments[1] == "addFriends"
}

fun Uri.isLinkFacebookLink(): Boolean {
    return pathSegments.size > 1 && pathSegments[0] == "app" && pathSegments[1] == "linkFacebook"
}

fun Uri.isLoginLink(): Boolean {
    return pathSegments.size > 1 && pathSegments[0] == "app" && pathSegments[1] == "login"
}

fun Uri.isVerifyLink(): Boolean {
    return pathSegments.size > 1 && pathSegments[0] == "v"
}

fun Uri.isCategoryLink(): Boolean {
    return pathSegments.size > 0 && pathSegments[0] == Category.shortLinkPath
}

fun Uri.isCircleLink(): Boolean {
    return pathSegments.size > 0 && pathSegments[0] == Circle.shortLinkPath
}

fun Uri.getCategoryName(): String {
    return Uri.decode(pathSegments[1])
}



