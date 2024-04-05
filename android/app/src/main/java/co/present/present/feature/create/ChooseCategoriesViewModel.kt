package co.present.present.feature.create

import androidx.lifecycle.ViewModel
import co.present.present.model.Interest
import javax.inject.Inject

class ChooseCategoriesViewModel @Inject constructor(): ViewModel() {

    val categories = mutableListOf<Interest>()
    private val limit = 3

    fun toggleTemporaryInterest(interest: Interest) {
        if (categories.contains(interest)) {
            categories -= interest
        } else {
            categories += interest
        }
    }

    private val canSelect get() = categories.size < limit

    fun canToggle(interest: Interest): Boolean {
        return canSelect || interest.isSelected()
    }

    fun Interest.isSelected(): Boolean {
        return categories.contains(this)
    }

}