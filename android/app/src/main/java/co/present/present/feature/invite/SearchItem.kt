package co.present.present.feature.invite

import android.text.Editable
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import co.present.present.R
import co.present.present.feature.common.Payload
import co.present.present.view.AfterTextChangedWatcher
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_search.*


open class SearchItem(val listener: OnSearchChangedListener, val searchText: String, @StringRes val hint: Int) : Item() {
    override fun getLayout() = R.layout.item_search

    override fun bind(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(Payload.DontUpdate)) return
        else bind(holder, position)
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.searchEditText.setText(searchText, TextView.BufferType.EDITABLE)
        viewHolder.searchEditText.setHint(hint)
        viewHolder.searchEditText.requestFocus()
        viewHolder.searchEditText.setSelection(searchText.length)
    }

    override fun createViewHolder(itemView: View): ViewHolder {
        val holder = super.createViewHolder(itemView)
        holder.clear.setOnClickListener {
            holder.searchEditText.setText("", TextView.BufferType.EDITABLE)
        }
        holder.searchEditText.addTextChangedListener(object : AfterTextChangedWatcher() {
            var previousText = searchText

            override fun afterTextChanged(editable: Editable) {
                val newText = editable.toString()
                if (previousText != newText) {
                    previousText = newText
                    holder.clear.visibility = if (newText.isEmpty()) View.GONE else View.VISIBLE
                    listener.onSearchChanged(newText)
                }
            }
        })
        return holder
    }

    override fun getId() = 1L

    override fun getChangePayload(newItem: com.xwray.groupie.Item<*>?) = Payload.DontUpdate

    interface OnSearchChangedListener {
        fun onSearchChanged(searchText: String)
    }
}