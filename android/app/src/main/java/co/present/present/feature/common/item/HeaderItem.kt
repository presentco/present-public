package co.present.present.feature.common.item

import androidx.annotation.StringRes
import co.present.present.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_header.*



open class HeaderItem(@StringRes val stringRes: Int, val string: CharSequence = ""): Item() {

    constructor(string: String): this(R.string.empty, string)

    override fun getLayout(): Int = R.layout.item_header

    override fun bind(viewHolder: ViewHolder, position: Int) {
        if (stringRes != R.string.empty) {
            viewHolder.headerText.setText(stringRes)
        } else {
            viewHolder.headerText.text = string
        }
    }

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        return other is HeaderItem && other.stringRes == stringRes && other.string == string
    }
}

open class GrayHeaderItem(@StringRes stringRes: Int, string: CharSequence = ""): HeaderItem(stringRes, string) {

    constructor(string: CharSequence): this(R.string.empty, string)

    override fun getLayout(): Int = R.layout.item_header_gray

}

open class GraySmallCapsHeaderItem(@StringRes stringRes: Int, string: CharSequence = ""): HeaderItem(stringRes, string) {

    constructor(string: CharSequence): this(R.string.empty, string)

    override fun getLayout(): Int = R.layout.item_header_small_caps_gray

}


