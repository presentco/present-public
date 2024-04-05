package co.present.present.feature.common.item

import android.text.SpannableString
import android.text.style.TextAppearanceSpan
import co.present.present.R
import co.present.present.extensions.setSpan
import co.present.present.extensions.string
import co.present.present.feature.common.Payload
import com.xwray.groupie.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_editable_text.*

/**
 * An EditText whose hint looks the same as the normal headers in our app, and whose text looks
 * bigger than normal body text
 */
open class HeaderEditableTextItem(private val hintResId: Int, onTextChangedListener: OnTextChangedListener, string: String = "")
    : EditableTextItem(hintResId, onTextChangedListener, string) {

    override fun bind(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(Payload.DontUpdate)) return
        super.bind(holder, position, payloads)
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        super.bind(viewHolder, position)
        val context = viewHolder.itemView.context
        val span = TextAppearanceSpan(context, R.style.H2)
        val spannable = SpannableString(context.string(hintResId)).apply { setSpan(span) }
        viewHolder.text.hint = spannable
    }

    override fun isSameAs(other: Item<*>?): Boolean {
        return other is HeaderEditableTextItem
    }

    override fun getChangePayload(newItem: Item<*>?): Any {
        return Payload.DontUpdate
    }

    override fun getLayout() = R.layout.item_editable_header
}

