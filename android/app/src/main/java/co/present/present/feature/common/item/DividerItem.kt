package co.present.present.feature.common.item

import co.present.present.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder

class DividerItem: Item() {
    override fun bind(viewHolder: ViewHolder, position: Int) { }

    override fun getLayout() = R.layout.item_divider
}