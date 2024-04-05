package co.present.present.feature.create

import co.present.present.R
import co.present.present.extensions.hide
import co.present.present.extensions.show
import co.present.present.feature.common.Payload
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_switch.*


open class SwitchItem(val titleResId: Int, val subtitleResId: Int? = null, val switchValue: Boolean, open val listener: OnSwitchChangedListener? = null, val enabled: Boolean = listener != null): Item() {

    override fun bind(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(SwitchChanged)) {
            bind(holder, position)
            return
        }
        super.bind(holder, position, payloads)
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        // Unset any previous checked change listener
        viewHolder.theSwitch.setOnCheckedChangeListener(null)

        viewHolder.title.setText(titleResId)
        if (subtitleResId == null) {
            viewHolder.subtitle.hide()
        } else {
            viewHolder.subtitle.setText(subtitleResId)
            viewHolder.subtitle.show()
        }
        viewHolder.theSwitch.isChecked = switchValue

        viewHolder.theSwitch.isEnabled = enabled
        viewHolder.title.isEnabled = enabled
        viewHolder.subtitle.isEnabled = enabled
        listener?.let {
            viewHolder.theSwitch.setOnCheckedChangeListener { _, b ->
                it.onSwitchChanged(this, b)
            }
        }

    }

    override fun getLayout() = R.layout.item_switch

    interface OnSwitchChangedListener {
        fun onSwitchChanged(item: SwitchItem, value: Boolean)
    }

    override fun getChangePayload(newItem: com.xwray.groupie.Item<*>?): Any? {
        return if ((newItem as? SwitchItem)?.switchValue != switchValue) SwitchChanged else Payload.FullBind
    }

    companion object {
        const val SwitchChanged = "switchChanged"
    }
}

class NotificationsSwitchItem(listener: SwitchItem.OnSwitchChangedListener, private val notifsEnabled: Boolean)
    : SwitchItem(R.string.notifications,
        switchValue = notifsEnabled,
        listener = listener) {
    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        return other is NotificationsSwitchItem
    }
}

class DiscoverabilitySwitchItem(listener: SwitchItem.OnSwitchChangedListener, subtitleResId: Int, val discoverable: Boolean)
    : SwitchItem(R.string.discoverable,
        subtitleResId,
        switchValue = discoverable,
        listener = listener) {
    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        return other is DiscoverabilitySwitchItem
    }
}

class WomenOnlySwitchItem(listener: SwitchItem.OnSwitchChangedListener? = null, private val womenOnly: Boolean): SwitchItem(
        R.string.women_only,
        switchValue = womenOnly,
        listener = listener) {
    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        return other is WomenOnlySwitchItem
    }
}

