import io.reactivex.Single
import present.proto.ActivityService
import present.proto.PastActivityRequest
import present.proto.PastActivityResponse

fun ActivityService.getPastActivity(): Single<PastActivityResponse> {
    return Single.fromCallable {
        getPastActivity(PastActivityRequest(null, null))
    }
}