package co.present.present.feature.common

import android.net.Uri
import android.util.Log
import co.present.present.config.FeatureDataProvider
import co.present.present.db.CircleDao
import co.present.present.db.UserDao
import co.present.present.extensions.getCategoryName
import co.present.present.extensions.isCategoryLink
import co.present.present.model.Category
import co.present.present.model.Circle
import co.present.present.model.User
import io.reactivex.Single
import present.proto.ResolveUrlResponse
import present.proto.UrlResolverService
import resolve
import javax.inject.Inject

interface ResolveUrl<T> {
    fun resolve(url: String): Single<T>
}

abstract class BaseResolveUrl<T>(val urlResolverService: UrlResolverService,
                                 val circleDao: CircleDao,
                                 val userDao: UserDao)
    : ResolveUrl<T> {

    override fun resolve(url: String): Single<T> {
        return urlResolverService.resolve(url)
                .map { resolveUrlResponse -> responseToObject(resolveUrlResponse) }
                .flatMap { any -> saveToDatabase(any) }
    }

    protected abstract fun responseToObject(resolveUrlResponse: ResolveUrlResponse): T

    protected abstract fun saveToDatabase(any: T): Single<T>
}

class ResolveCircleUrl @Inject constructor(urlResolverService: UrlResolverService,
                                           circleDao: CircleDao,
                                           userDao: UserDao)
    : BaseResolveUrl<Circle>(urlResolverService, circleDao, userDao) {

    val TAG = javaClass.simpleName

    override fun resolve(url: String): Single<Circle> {
        return circleDao.getCircleByUrl(url)
                .doOnSuccess { Log.d(TAG, "Found circle with short link url in database, skipping resolution on server") }
                .doOnError { Log.d(TAG, "Didn't find circle with matching short link url in database") }
                .onErrorResumeNext { super.resolve(url) }
    }

    override fun responseToObject(resolveUrlResponse: ResolveUrlResponse): Circle {
        resolveUrlResponse.group?.let {
            return Circle(it)
        }
        error("Url couldn't be resolved to a circle")
    }

    override fun saveToDatabase(any: Circle): Single<Circle> {
        return Single.fromCallable {
            circleDao.insert(any); any
        }
    }
}

class ResolveUserUrl @Inject constructor(urlResolverService: UrlResolverService,
                                         circleDao: CircleDao,
                                         userDao: UserDao)
    : BaseResolveUrl<User>(urlResolverService, circleDao, userDao) {

    override fun responseToObject(resolveUrlResponse: ResolveUrlResponse): User {
        resolveUrlResponse.user?.let {
            return User(it)
        }
        error("Url couldn't be resolved to a user")
    }

    override fun saveToDatabase(any: User): Single<User> {
        return Single.fromCallable {
            userDao.insertOrPartialUpdate(user = any); any
        }
    }
}

class ResolveCategoryUrl @Inject constructor(urlResolverService: UrlResolverService,
                                             val featureDataProvider: FeatureDataProvider,
                                             circleDao: CircleDao,
                                             userDao: UserDao)
    : BaseResolveUrl<Category>(urlResolverService, circleDao, userDao) {

    override fun resolve(url: String): Single<Category> {
        return Single.fromCallable {
            val uri = Uri.parse(url)
            if (!uri.isCategoryLink()) error("Url is not a valid category short link: $url")
            Category(uri.getCategoryName())
        }
    }

    override fun responseToObject(resolveUrlResponse: ResolveUrlResponse): Category {
        throw NotImplementedError()
    }

    override fun saveToDatabase(any: Category): Single<Category> {
        throw NotImplementedError()
    }
}
