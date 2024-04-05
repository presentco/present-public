package co.present.present.feature.profile

import co.present.present.db.BlockedDao
import co.present.present.model.Blocked
import co.present.present.service.rpc.blockUser
import co.present.present.service.rpc.getBlocked
import co.present.present.service.rpc.unblockUser
import io.reactivex.Completable
import io.reactivex.Flowable
import present.proto.UserService

/**
 * Created by lisa on 4/10/18.
 */
interface GetBlocked {

    fun isBlocked(userId: String): Flowable<Boolean>

    fun getBlocked(): Flowable<List<Blocked>>

    fun toggleUserBlock(userId: String, isBlocked: Boolean): Completable
}

class GetBlockedImpl(val blockedDao: BlockedDao, val userService: UserService): GetBlocked {
    override fun isBlocked(userId: String): Flowable<Boolean> {
        return getBlocked().map { it.any { it.userId == userId } }
    }

    private var blocked: Flowable<List<Blocked>>? = null

    override fun getBlocked(): Flowable<List<Blocked>> {
        if (blocked == null) {
            blocked = blockedDao.getBlocked().replay(1).autoConnect()
            refreshBlockedAsync()
        }
        return blocked!!
    }

    fun refreshBlockedAsync() {
        userService.getBlocked().map { blockedUsers ->
            blockedDao.clear()
            blockedDao.insertAll(blockedUsers.map { Blocked(userId = it.id) })
        }
    }

    override fun toggleUserBlock(userId: String, isBlocked: Boolean): Completable {
        return if (isBlocked) {
            userService.unblockUser(userId)
        } else {
            userService.blockUser(userId)
        }.toSingleDefault(isBlocked).flatMapCompletable { wasBlocked ->
            Completable.fromCallable {
                if (wasBlocked) {
                    blockedDao.delete(userId)
                } else {
                    blockedDao.insert(Blocked(userId = userId))
                }
            }
        }
    }


}