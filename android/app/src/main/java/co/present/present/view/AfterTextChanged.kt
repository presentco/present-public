package co.present.present.view

import android.text.Editable
import android.text.TextWatcher


abstract class AfterTextChangedWatcher: TextWatcher {
    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        // We don't need these silly other methods
    }

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        // No we don't!
    }

    // But we like this one so give it nicely named parameters
    abstract override fun afterTextChanged(editable: Editable)
}