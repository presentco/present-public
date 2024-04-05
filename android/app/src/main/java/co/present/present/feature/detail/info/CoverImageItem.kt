package co.present.present.feature.detail.info

import co.present.present.R
import co.present.present.extensions.loadCoverImage
import co.present.present.feature.common.Payload
import co.present.present.model.Circle
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_cover_image.*

data class CoverImageItem(val circle: Circle?): Item() {

    override fun getLayout() = R.layout.item_cover_image

    override fun bind(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(Payload.DontUpdate)) return
        super.bind(holder, position, payloads)
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        circle?.let {
            viewHolder.coverImage.loadCoverImage(circle)
        }
    }

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        return other is CoverImageItem
    }

    override fun getChangePayload(newItem: com.xwray.groupie.Item<*>?): Any? {
        return if (newItem is CoverImageItem && newItem.circle?.coverPhoto == circle?.coverPhoto) Payload.DontUpdate else null
    }
}