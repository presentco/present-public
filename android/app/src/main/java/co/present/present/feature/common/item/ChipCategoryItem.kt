package co.present.present.feature.common.item

import android.content.Context
import android.graphics.drawable.StateListDrawable
import android.util.StateSet
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import co.present.present.R
import co.present.present.feature.common.Payload
import co.present.present.model.toInterest
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_category_chip.*


open class CategoryItem(val category: String, val deletable: Boolean = false) : Item() {

    override fun bind(holder: ViewHolder, position: Int) {
        val interest = category.toInterest()
        holder.title.text = category

        holder.title.background = ChipBackground(holder.itemView.context,
                tintResId = interest?.background?.colorResId ?: R.color.presentPurple)
        holder.itemView.isSelected = true
    }

    override fun getLayout() = R.layout.item_category_chip

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        return other is CategoryItem && other.category == category
    }
}

class ChipCategoryItem(category: String, var selected: Boolean, private val onCategoryToggleListener: OnCategoryToggleListener) : CategoryItem(category) {

    override fun bind(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(Payload.Checked)) {
            bindSelected(holder, selected)
        } else {
            super.bind(holder, position, payloads)
        }
    }

    override fun bind(holder: ViewHolder, position: Int) {
        super.bind(holder, position)
        bindSelected(holder, selected)
    }

    private fun bindSelected(holder: ViewHolder, selected: Boolean) {
        holder.itemView.isSelected = selected
        holder.itemView.setOnClickListener { onCategoryToggleListener.onCategoryToggled(this, category) }
    }

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        return other is ChipCategoryItem && other.category == category
    }

    override fun getChangePayload(newItem: com.xwray.groupie.Item<*>?): Any {
        return Payload.Checked
    }

    interface OnCategoryToggleListener {
        fun onCategoryToggled(item: com.xwray.groupie.Item<*>, category: String)
    }
}

/**
 * Programmatically create a tinted background selector based on the category's color
 */
class ChipBackground(context: Context, tintResId: Int): StateListDrawable() {
    init {
        val tintColor = ContextCompat.getColor(context, tintResId)
        val drawable = ContextCompat.getDrawable(context, R.drawable.rounded_ends_light_gray_32dp)
        val tintedDrawable = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.rounded_ends_black_32dp)!!)
                .apply {
                    DrawableCompat.setTint(this, tintColor)
                }
        addState(intArrayOf(android.R.attr.state_pressed), tintedDrawable)
        addState(intArrayOf(android.R.attr.state_selected), tintedDrawable)
        addState(StateSet.WILD_CARD, drawable)
    }
}