package co.present.present.model

import co.present.present.BuildConfig

data class Category(val name: String) {

    companion object {
        val CATEGORY = "${BuildConfig.APPLICATION_ID}.Category"
        val shortLinkPath = "t"

        val NONE = ""
        val ALL = "All"
    }
}



