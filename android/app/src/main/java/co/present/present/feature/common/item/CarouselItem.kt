package co.present.present.feature.common.item

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import co.present.present.R
import co.present.present.feature.common.Payload
import com.xwray.groupie.Group
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.OnItemClickListener
import com.xwray.groupie.OnItemLongClickListener
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_carousel.*

/**
 * A horizontally scrolling RecyclerView, for use in a vertically scrolling RecyclerView.
 *
 * This one's optimized for a one-way data flow.  It plays well with DiffUtil; if you dispatch an
 * updated carousel, the adapter will try to reuse the existing one and update it with new contents.
 */
open class CarouselItem(private val groups: List<Group>, private val carouselDecoration: RecyclerView.ItemDecoration? = null) : Item() {

    override fun getLayout(): Int {
        return R.layout.item_carousel
    }

    override fun bind(holder: ViewHolder, position: Int, payloads: MutableList<Any>, onItemClickListener: OnItemClickListener?, onItemLongClickListener: OnItemLongClickListener?) {
        if (payloads.contains(Payload.DontUpdate)) {
            (holder.recyclerView.adapter as GroupAdapter<*>).update(groups)
        } else {
            super.bind(holder, position, payloads, onItemClickListener, onItemLongClickListener)
            (holder.recyclerView.adapter as GroupAdapter<*>).setOnItemClickListener(onItemClickListener)
        }
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.recyclerView.apply {
            // Only set the adapter and layout manager if they have not already been set
            if (adapter == null) {
                adapter = GroupAdapter<ViewHolder>()
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                carouselDecoration?.let {
                    // We don't know if the layout we're passed has been bound before so
                    // we need to ensure we don't register the item decoration multiple times,
                    // by trying to remove it first. (Nothing happens if it's not registered.)
                    removeItemDecoration(it)
                    addItemDecoration(it)
                }
            }
            (viewHolder.recyclerView.adapter as GroupAdapter<*>).update(groups)
        }
    }

    override fun getItemCount() = if (groups.isEmpty()) 0 else 1

    // All carousel items are interchangable by default; to decrease churning / diffing if we show
    // many onscreen at the same time in the future, we could customize this
    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        return other is CarouselItem
    }

    override fun getChangePayload(newItem: com.xwray.groupie.Item<*>?): Any {
        return Payload.DontUpdate
    }
}

class GrayCarouselItem(groups: List<Group>): CarouselItem(groups) {
    override fun getLayout() = R.layout.item_carousel_lightest_gray
}