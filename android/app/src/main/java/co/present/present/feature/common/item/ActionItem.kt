package co.present.present.feature.common.item

import androidx.annotation.StringRes
import co.present.present.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_action.*



open class ActionItem(@StringRes val stringRes: Int, val string: CharSequence = ""): Item() {

    constructor(string: String): this(R.string.empty, string)

    override fun getLayout(): Int = R.layout.item_action

    override fun bind(viewHolder: ViewHolder, position: Int) {
        if (stringRes != R.string.empty) {
            viewHolder.title.setText(stringRes)
        } else {
            viewHolder.title.text = string
        }
    }

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        return other is ActionItem && other.stringRes == stringRes && other.string == string
    }
}


