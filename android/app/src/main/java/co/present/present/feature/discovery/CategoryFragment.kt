package co.present.present.feature.discovery

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProviders
import co.present.present.analytics.AmplitudeEvents
import co.present.present.analytics.AmplitudeKeys
import co.present.present.di.ActivityScope
import co.present.present.feature.common.item.CircleItem
import co.present.present.feature.common.item.OnCircleJoinClickListener
import co.present.present.feature.common.item.SmallCircleItem
import co.present.present.model.Category
import com.xwray.groupie.Group
import com.xwray.groupie.Item
import io.reactivex.Flowable
import kotlinx.android.synthetic.main.fragment_display_circles.*


@ActivityScope
open class CategoryFragment : CircleListFragment() {

    private val category: String by lazy { arguments?.getString(Category.CATEGORY)!! }
    private lateinit var categoryViewModel: CategoryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        categoryViewModel = ViewModelProviders.of(baseActivity, viewModelFactory).get(CategoryViewModel::class.java)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        emptyView.init(categoryViewModel, baseActivity, AmplitudeEvents.CATEGORY_TAP_CREATE_CIRCLE, analytics)
    }

    override fun getItems(onCircleJoinClickListener: OnCircleJoinClickListener): Flowable<List<Group>> {
        return categoryViewModel.getItems(category, onCircleJoinClickListener)
    }

    override fun performInjection() {
        activityComponent.inject(this)
    }

    override fun onItemClick(item: Item<*>, view: View) {
        if (item is CircleItem || item is SmallCircleItem) {
            analytics.log(AmplitudeEvents.CATEGORY_TAP_CIRCLE, AmplitudeKeys.CATEGORY_ID, category)
        }
        super.onItemClick(item, view)
    }

    companion object {
        internal fun newInstance(category: String): CategoryFragment {
            return CategoryFragment().apply {
                arguments = Bundle().apply {
                    putString(Category.CATEGORY, category)
                }
            }
        }
    }
}