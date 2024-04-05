package co.present.present.feature.detail

import android.content.Context
import android.location.Location
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import co.present.present.R
import co.present.present.config.FeatureDataProvider
import co.present.present.extensions.fromApi
import co.present.present.location.formattedDistanceTo
import co.present.present.model.Circle
import kotlinx.android.synthetic.main.toolbar_circle.view.*

class CircleToolbar(context: Context?, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    init {
        View.inflate(context, R.layout.toolbar_circle, this)
        fromApi(21) {
            setBackgroundResource(R.color.white)
        }
    }

    fun configure(circle: Circle, location: Location, featureDataProvider: FeatureDataProvider) {
        toolbarTitle.text = circle.title
        neighborhood.text = circle.locationName
        if (featureDataProvider.isLocationMocked) {
            this.distance.visibility = View.GONE
        } else {
            this.distance.visibility = View.VISIBLE
            this.distance.text = location.formattedDistanceTo(circle.location)
        }
    }

}