package co.present.present.feature.create

import co.present.present.R
import co.present.present.extensions.setVisible
import co.present.present.feature.common.Payload
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_pre_approval.*
import present.proto.GroupMemberPreapproval


open class ActionablePreapprovalItem(val preapproval: GroupMemberPreapproval, val womenOnly: Boolean): Item() {

    override fun bind(viewHolder: ViewHolder, position: Int) {
        // 6/6/2018: There is a measurement bug in CL + TextView right now where it's not measured correctly.
        // Setting the hint seems to force it to measure
        viewHolder.value.setHint(preApprovalTitleResId(preapproval, womenOnly))
        viewHolder.value.setText(preApprovalTitleResId(preapproval, womenOnly))

        // Subtitle is redundant in Anyone / Women case.
        viewHolder.subtitle.setVisible(preapproval != GroupMemberPreapproval.ANYONE)
        viewHolder.subtitle.setText(preApprovalSubtitleResId(preapproval, womenOnly))
    }

    override fun getLayout() = R.layout.item_pre_approval

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        return other is ActionablePreapprovalItem
    }

    override fun getChangePayload(newItem: com.xwray.groupie.Item<*>?): Any? {
        return Payload.FullBind
    }
}

