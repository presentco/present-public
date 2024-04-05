package co.present.present.feature.discovery

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import co.present.present.PresentApplication
import co.present.present.config.FeatureDataProvider
import co.present.present.extensions.Optional
import co.present.present.feature.common.item.OnCircleJoinClickListener
import co.present.present.feature.common.item.SmallCircleItem
import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.feature.detail.info.JoinCircle
import co.present.present.location.LocationDataProvider
import co.present.present.model.Category
import co.present.present.model.Circle
import co.present.present.model.CurrentUser
import com.xwray.groupie.Group
import io.reactivex.FlowableTransformer
import io.reactivex.functions.Function
import io.reactivex.rxkotlin.combineLatest
import javax.inject.Inject

typealias Filter = Function<Triple<List<Circle>, String, String>, Triple<List<Circle>, String, String>>
typealias CirclesSearchCategory = Triple<List<Circle>, String, String>

open class CirclesViewModel @Inject constructor(val locationDataProvider: LocationDataProvider,
                                                open val featureDataProvider: FeatureDataProvider,
                                                private val joinCircle: JoinCircle,
                                                private val getCurrentUser: GetCurrentUser,
                                                private val refreshCircles: RefreshCircles,
                                                application: Application)
    : AndroidViewModel(application), JoinCircle by joinCircle, GetCurrentUser by getCurrentUser, RefreshCircles by refreshCircles {
    private val TAG = javaClass.simpleName

    val context: Context get() = getApplication<PresentApplication>().applicationContext

    data class CirclesInfo(val circles: List<Circle>, val currentUserOptional: Optional<CurrentUser>)

    /**
     * Takes a list of circles, filters them by search and category, and makes UI list items
     */
    protected fun toItems(onCircleJoinClickListener: OnCircleJoinClickListener? = null,
                          featureDataProvider: FeatureDataProvider)
            : FlowableTransformer<CirclesSearchCategory, List<Group>> {
        return FlowableTransformer {
            it.map(FilterBySearch())
                    .map(FilterByCategory())
                    .combineLatest(currentUserOptional)
                    .map { (triple, currentUserOptional) -> CirclesInfo(triple.first, currentUserOptional)}
                    .map(ToCircleItems(onCircleJoinClickListener, featureDataProvider))
        }
    }

    private class FilterByCategory : Filter {
        override fun apply(input: CirclesSearchCategory): CirclesSearchCategory {
            input.let { (circles, searchTerm, category) ->
                if (listOf(Category.ALL, Category.NONE).contains(category)) return input
                return Triple(circles.filter { it.categories.contains(category) }, searchTerm, category)
            }
        }
    }

    private class FilterBySearch : Filter {
        override fun apply(input: CirclesSearchCategory): CirclesSearchCategory {
            input.let { (circles, searchTerm, category) ->
                if (searchTerm.isEmpty()) return input

                val filteredCircles = circles.filter { circle ->
                    circle.title.contains(searchTerm, ignoreCase = true)
                }
                return Triple(filteredCircles, searchTerm, category)
            }
        }
    }

    protected class ToCircleItems(val onCircleJoinClickListener: OnCircleJoinClickListener?, val featureDataProvider: FeatureDataProvider): Function<CirclesInfo, List<Group>> {
        override fun apply(t: CirclesInfo): List<Group> {
            t.let { (circles, currentUserOptional) ->
                val currentUser = currentUserOptional.value
                return circles.map { circle ->
                    SmallCircleItem(currentUser, circle, onCircleJoinClickListener)
                }
            }
        }
    }
}

