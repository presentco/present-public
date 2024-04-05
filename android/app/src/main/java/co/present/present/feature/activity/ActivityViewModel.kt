package co.present.present.feature.activity

import androidx.lifecycle.ViewModel
import com.xwray.groupie.Item
import getPastActivity
import io.reactivex.Single
import present.proto.ActivityService
import present.proto.ActivityType
import javax.inject.Inject


class ActivityViewModel @Inject constructor(val activityService: ActivityService) : ViewModel() {

    fun getNotifications(): Single<List<Item<*>>> {
        return activityService.getPastActivity()
                .map { pastActivityResponse ->
                    pastActivityResponse.events
                            .filter {
                                // For future proofing, drop all unknown types of events
                                it.type != ActivityType.UNKNOWN
                            }.map { eventResponse -> ActivityFeedItem(eventResponse)
                    }
                }
    }

}
