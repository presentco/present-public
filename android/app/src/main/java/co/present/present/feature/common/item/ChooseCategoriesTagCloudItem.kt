package co.present.present.feature.common.item

import android.content.Context
import co.present.present.R
import co.present.present.extensions.string
import co.present.present.feature.common.Payload
import co.present.present.model.Interest
import com.xwray.groupie.Item
import com.xwray.groupie.Section

class CategoriesGroup(val categories: List<String>, context: Context,
                      categoryToggleListener: ChipCategoryItem.OnCategoryToggleListener)
    : Section(
        GrayHeaderItem(R.string.categories),
        listOf(TextItem(context.string(R.string.choose_up_to_three)),
                ChooseCategoriesTagCloudItem(categories, categoryToggleListener)))


data class ChooseCategoriesTagCloudItem(val categories: List<String>,
                                        private val categoryToggleListener: ChipCategoryItem.OnCategoryToggleListener)
    : FlowLayoutItem(Interest.values().toChips(categories, categoryToggleListener)) {

    override fun isSameAs(other: Item<*>?): Boolean {
        return other is ChooseCategoriesTagCloudItem
    }

    override fun getChangePayload(newItem: Item<*>?): Any {
        return Payload.DontUpdate
    }

    companion object {
        fun Array<Interest>.toChips(selectedCategories: List<String>,
                                    categoryToggleListener: ChipCategoryItem.OnCategoryToggleListener)
                : List<ChipCategoryItem> {
            return map {
                val category = it.canonicalString
                ChipCategoryItem(category, selectedCategories.contains(category), categoryToggleListener)
            }
        }
    }
}

data class CategoriesTagCloudItem(val categories: List<String>) : FlowLayoutItem(categories.distinct().map { CategoryItem(it) }) {
    override fun isSameAs(other: Item<*>?): Boolean {
        return other is CategoriesTagCloudItem
    }

    override fun getChangePayload(newItem: Item<*>?): Any {
        return Payload.DontUpdate
    }
}