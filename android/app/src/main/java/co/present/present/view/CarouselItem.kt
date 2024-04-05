package co.present.present.view

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import co.present.present.R
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_carousel.*

/**
 * A horizontally scrolling RecyclerView, for use in a vertically scrolling RecyclerView.
 */
@Deprecated("Try to move to a one way data flow. Use co.present.present.feature.common.CarouselItem instead, " +
        "which is capable of diffing itself")
open class CarouselItem(private val carouselAdapter: GroupAdapter<ViewHolder>, private val carouselDecoration: RecyclerView.ItemDecoration? = null) : Item() {


    override fun getLayout(): Int {
        return R.layout.item_carousel
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.recyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = carouselAdapter

            carouselDecoration?.let {
                // We don't know if the layout we're passed has been bound before so
                // we need to ensure we don't register the item decoration multiple times,
                // by trying to remove it first. (Nothing happens if it's not registered.)
                removeItemDecoration(it)
                addItemDecoration(it)
            }
        }
    }

    override fun getItemCount() = if (carouselAdapter.itemCount == 0) 0 else 1

}