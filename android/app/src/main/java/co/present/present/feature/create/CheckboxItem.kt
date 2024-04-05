package co.present.present.feature.create

import co.present.present.R
import co.present.present.extensions.setVisible
import co.present.present.feature.common.Payload
import co.present.present.model.Circle
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_checkbox.*
import present.proto.GroupMemberPreapproval


open class CheckboxItem(val titleResId: Int, val subtitleResId: Int?, val checked: Boolean): Item() {

    override fun bind(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(CheckedChanged)) {
            bind(holder, position)
            return
        }
        if (payloads.contains(Payload.DontUpdate)) return
        super.bind(holder, position, payloads)
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.title.setText(titleResId)
        viewHolder.subtitle.setVisible(subtitleResId != null)
        subtitleResId?.let { viewHolder.subtitle.setText(it) }
        viewHolder.checkmark.setVisible(checked)
    }

    override fun getLayout() = R.layout.item_checkbox

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        return other is CheckboxItem && titleResId == other.titleResId && subtitleResId == other.subtitleResId
    }

    override fun getChangePayload(newItem: com.xwray.groupie.Item<*>?): Any? {
        return if ((newItem as? CheckboxItem)?.checked != checked) CheckedChanged else Payload.DontUpdate
    }

    companion object {
        const val CheckedChanged = "checkedChanged"
    }
}

class PreapproveCheckboxItem(val preapproval: GroupMemberPreapproval, womenOnly: Boolean, checked: Boolean)
    : CheckboxItem(
        titleResId = preApprovalTitleResId(preapproval, womenOnly),
        // The subtitle is redundant in the Anyone / Women case.
        subtitleResId = if (preapproval == GroupMemberPreapproval.ANYONE) null else preApprovalSubtitleResId(preapproval, womenOnly),
        checked = checked)

fun preApprovalTitleResId(preapproval: GroupMemberPreapproval, womenOnly: Boolean): Int {
    return when (preapproval) {
        GroupMemberPreapproval.ANYONE -> if (womenOnly) R.string.preapprove_anyone_women_only_title else R.string.preapprove_anyone_title
        GroupMemberPreapproval.FRIENDS_OF_MEMBERS -> R.string.preapprove_friends_of_members_title
        GroupMemberPreapproval.FRIENDS -> R.string.preapprove_my_friends_title
        GroupMemberPreapproval.INVITE_ONLY -> R.string.preapprove_invite_only_title
        GroupMemberPreapproval.UNKNOWN -> error("Unknown is not a valid value!")
    }
}

fun Circle.preApprovalSubtitleResId(): Int {
    return co.present.present.feature.create.preApprovalSubtitleResId(getGroupMemberPreapproval(), isWomenOnly())
}

fun preApprovalSubtitleResId(preapproval: GroupMemberPreapproval, womenOnly: Boolean): Int {
    return when (preapproval) {
        GroupMemberPreapproval.ANYONE -> if (womenOnly) R.string.preapprove_anyone_women_only_subtitle else R.string.preapprove_anyone_subtitle
        GroupMemberPreapproval.FRIENDS_OF_MEMBERS -> if (womenOnly) R.string.preapprove_friends_of_members_women_only_subtitle else R.string.preapprove_friends_of_members_subtitle
        GroupMemberPreapproval.FRIENDS -> if (womenOnly) R.string.preapprove_my_friends_women_only_subtitle else R.string.preapprove_my_friends_subtitle
        GroupMemberPreapproval.INVITE_ONLY -> if (womenOnly) R.string.preapprove_invite_only_women_only_subtitle else R.string.preapprove_invite_only_subtitle
        GroupMemberPreapproval.UNKNOWN -> error("Unknown is not a valid value!")
    }
}



