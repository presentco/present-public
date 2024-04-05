package co.present.present.model

import androidx.annotation.StringRes
import co.present.present.BuildConfig
import co.present.present.R

sealed class Space(val id: String, val name: String, @StringRes val descriptionResId: Int) {

    object Everyone: Space(everyoneId, "Everyone", R.string.everyone_description)
    object WomenOnly: Space(womenOnlyId, "Women Only", R.string.women_only_description)

    companion object {
        @JvmStatic val SPACE = "${BuildConfig.APPLICATION_ID}.Space"
        const val womenOnlyId = "women-only"
        const val everyoneId = "everyone"
    }

}

