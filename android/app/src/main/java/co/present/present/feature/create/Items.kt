package co.present.present.feature.create

import co.present.present.R
import co.present.present.feature.common.Payload
import co.present.present.feature.common.item.EditableTextItem
import co.present.present.feature.common.item.OnTextChangedListener
import com.xwray.groupie.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder


class DescriptionEditableTextItem(onTextChangedListener: OnTextChangedListener, description: String)
    : EditableTextItem(R.string.describe_your_circle, onTextChangedListener, description) {

    override fun bind(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(Payload.DontUpdate)) return
        super.bind(holder, position, payloads)
    }

    override fun getLayout() = R.layout.item_edit_description

    override fun isSameAs(other: Item<*>?): Boolean {
        return other is DescriptionEditableTextItem
    }

    override fun getChangePayload(newItem: Item<*>?): Any? {
        return Payload.DontUpdate
    }
}
