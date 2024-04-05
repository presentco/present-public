package co.present.present

import androidx.lifecycle.ViewModel
import android.net.Uri
import co.present.present.feature.common.ResolveUrl
import co.present.present.model.Category
import co.present.present.model.Circle
import co.present.present.model.User
import io.reactivex.Single
import javax.inject.Inject

class UrlResolverViewModel @Inject constructor(private val circleResolver: ResolveUrl<Circle>,
                                               private val categoryResolver: ResolveUrl<Category>,
                                               private val userResolver: ResolveUrl<User>) : ViewModel() {

    /**
     * Returns the Android model classes (Circle, User etc)
     */
    fun resolve(uri: Uri): Single<out Any> {

        val resolver = when (uri.pathSegments[0]) {
            Category.shortLinkPath -> categoryResolver
            Circle.shortLinkPath -> circleResolver
            User.shortLinkPath -> userResolver
            else -> error("We don't know how to handle this type of deep link yet")
        }

        return resolver.resolve(uri.toString())
    }
}