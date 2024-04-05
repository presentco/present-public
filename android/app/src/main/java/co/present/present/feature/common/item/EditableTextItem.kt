package co.present.present.feature.common.item

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import co.present.present.R
import co.present.present.view.AfterTextChangedWatcher
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_editable_text.*


open class EditableTextItem(private val hintResId: Int, val onTextChangedListener: OnTextChangedListener, var string: String = ""): Item() {
    private val TAG = javaClass.simpleName
    override fun getLayout(): Int = R.layout.item_editable_text

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.text.setText(string, TextView.BufferType.EDITABLE)
        viewHolder.text.setHint(hintResId)
        viewHolder.text.addTextChangedListener()
    }

    private fun EditText.addTextChangedListener() {
        val oldTextWatcher = getTag(R.id.textWatcher) as TextWatcher?
        oldTextWatcher?.let { removeTextChangedListener(it) }

        object: AfterTextChangedWatcher() {
            override fun afterTextChanged(editable: Editable) {
                string = editable.toString()
                onTextChangedListener.onTextChanged(this@EditableTextItem, string)
            }
        }.also {
            addTextChangedListener(it)
            setTag(R.id.textWatcher, it)
        }
    }

    override fun getId() = string.hashCode().toLong()

}

interface OnTextChangedListener {
    fun onTextChanged(item: com.xwray.groupie.Item<*>, text: String)
}