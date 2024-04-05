package co.present.present.feature.detail.info

import co.present.present.R
import co.present.present.extensions.hide
import co.present.present.extensions.show
import co.present.present.model.Circle
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_location.view.*

data class LocationItem(val latitude: Double, val longitude: Double, val locationName: String?): Item() {

    constructor(circle: Circle): this(circle.latitude, circle.longitude, locationName = circle.locationName)

    override fun getLayout() = R.layout.item_location

    override fun bind(viewHolder: ViewHolder, position: Int) {
        if (locationName == null) {
            viewHolder.itemView.text.hide()
        } else {
            viewHolder.itemView.text.text = locationName
            viewHolder.itemView.text.show()
        }
    }

    override fun getId() = hashCode().toLong()
}