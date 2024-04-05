package co.present.present.feature.common.item

import co.present.present.R
import co.present.present.model.Space
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_space.*


data class SpaceRadioButtonItem(val space: Space, val isSelected: Boolean): Item() {
    override fun getLayout(): Int = R.layout.item_space

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.headerText.text = space.name
        viewHolder.text.setText(space.descriptionResId)

        viewHolder.itemView.isSelected = isSelected
    }

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        return other is SpaceRadioButtonItem && other.space == space
    }
}