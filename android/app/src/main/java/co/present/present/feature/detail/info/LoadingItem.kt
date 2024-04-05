package co.present.present.feature.detail.info

import co.present.present.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder

class LoadingItem: Item() {
    override fun bind(viewHolder: ViewHolder, position: Int) {}

    override fun getLayout() = R.layout.item_loading
}