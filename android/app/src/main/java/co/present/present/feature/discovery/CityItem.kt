package co.present.present.feature.discovery

import android.graphics.Typeface
import co.present.present.R
import co.present.present.model.City
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_dropdown.*

sealed class BaseCityItem(val city: City? = null, val selected: Boolean = false): Item() {
    override fun getLayout() = R.layout.item_dropdown

    override fun bind(viewHolder: ViewHolder, position: Int) {
        city?.let {
            viewHolder.text.text = city.name
            viewHolder.icon.setImageResource(R.drawable.ic_pin_outline)
        }
        if (city == null) {
            viewHolder.text.setText(R.string.current_location)
            viewHolder.icon.setImageResource(R.drawable.ic_near_me_outline)
        }

        viewHolder.text.typeface = Typeface.create(if (selected) "sans-serif-medium" else "sans-serif", Typeface.NORMAL)
    }
}

class CityItem(city: City, selected: Boolean): BaseCityItem(city, selected)

class NearbyCityItem(selected: Boolean): BaseCityItem(selected = selected)