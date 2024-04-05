package co.present.present.feature.common.item

import co.present.present.R
import co.present.present.view.LinkTouchMovementMethod
import co.present.present.view.OnLinkClickedListener
import co.present.present.view.replaceSpans
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_text.*


open class TextItem(val string: String, private val onLinkClickedListener: OnLinkClickedListener? = null): Item() {

    override fun getLayout(): Int = R.layout.item_text

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.text.text = string
        onLinkClickedListener?.let { viewHolder.text.replaceSpans(it) }
        viewHolder.text.movementMethod = LinkTouchMovementMethod.getInstance()
    }

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        return other is TextItem && other.string == string
    }
}

class CenterTextItem(string: String): TextItem(string) {
    override fun getLayout(): Int = R.layout.item_center_text

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        return other is CenterTextItem
    }
}