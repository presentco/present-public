package co.present.present.feature.create

import co.present.present.R
import co.present.present.model.Circle
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_share_types.*


class CircleShareButtonsItem(val circle: Circle, val listener: CircleShareButtonsItem.OnShareButtonClickListener): Item() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        with(viewHolder) {
            emailButton.setOnClickListener { listener.onEmailClick(this@CircleShareButtonsItem, circle) }
            facebookButton.setOnClickListener { listener.onFacebookClick(this@CircleShareButtonsItem, circle) }
            tweetButton.setOnClickListener { listener.onTwitterClick(this@CircleShareButtonsItem, circle) }
            shareButton.setOnClickListener { listener.onSmsClick(this@CircleShareButtonsItem, circle) }
            linkButton.setOnClickListener { listener.onLinkClick(this@CircleShareButtonsItem, circle) }
        }
    }

    override fun getLayout() = R.layout.item_share_types

    interface OnShareButtonClickListener {
        fun onEmailClick(item: com.xwray.groupie.Item<*>, circle: Circle)
        fun onFacebookClick(item: com.xwray.groupie.Item<*>, circle: Circle)
        fun onTwitterClick(item: com.xwray.groupie.Item<*>, circle: Circle)
        fun onSmsClick(item: com.xwray.groupie.Item<*>, circle: Circle)
        fun onLinkClick(item: com.xwray.groupie.Item<*>, circle: Circle)
    }
}