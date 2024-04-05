package co.present.present.feature.common.item

import androidx.annotation.StringRes
import co.present.present.R


/**
 * A header item with a right carat on the far side, indicating you can do something with it
 */
open class ActionableHeaderItem constructor(@StringRes stringRes: Int, string: CharSequence = ""): HeaderItem(stringRes, string) {

    constructor(string: CharSequence): this(R.string.empty, string)

    override fun getLayout(): Int = R.layout.item_actionable_header

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        return other is ActionableHeaderItem && other.stringRes == stringRes && other.string == string
    }
}

open class ViewAllHeaderItem constructor(@StringRes stringRes: Int, string: CharSequence = ""): HeaderItem(stringRes, string) {

    constructor(string: CharSequence): this(R.string.empty, string)

    override fun getLayout(): Int = R.layout.item_header_view_all

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        return other is ViewAllHeaderItem
    }
}
