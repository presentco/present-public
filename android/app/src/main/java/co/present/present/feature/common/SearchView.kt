package co.present.present.feature.common

import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import co.present.present.R
import co.present.present.extensions.inputMethodManager
import co.present.present.feature.discovery.Searchable
import co.present.present.view.AfterTextChangedWatcher
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_search.*


class StandaloneSearchView(context: Context, attributeSet: AttributeSet) : ConstraintLayout(context, attributeSet), LayoutContainer, LifecycleObserver {

    override val containerView: View = rootView
    private val disposable = CompositeDisposable()

    private var searchable: Searchable? = null

    init {
        inflate(context, R.layout.view_search, this)
        clear.setOnClickListener {
            clearSearch()
        }
        searchEditText.addTextChangedListener(object : AfterTextChangedWatcher() {
            var previousText = ""

            override fun afterTextChanged(editable: Editable) {
                val newText = editable.toString().trim()
                if (previousText != newText) {
                    previousText = newText
                    clear.visibility = if (newText.isEmpty()) View.GONE else View.VISIBLE
                    onSearchChanged(newText)
                }
            }
        })
    }

    private fun onSearchChanged(searchText: String) {
        searchable?.searchChanged(searchText)
    }

    fun clearSearch() {
        searchEditText.setText("", TextView.BufferType.EDITABLE)
    }

    fun setSearch(searchText: String) {
        searchEditText.setText(searchText, TextView.BufferType.EDITABLE)
    }

    fun setFocus() {
        searchEditText.requestFocus()
        searchEditText.setSelection(searchEditText.text.length)
        context.inputMethodManager.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT)
    }

    fun init(searchable: Searchable, activity: AppCompatActivity, onBackClickedListener: View.OnClickListener) {
        this.searchable = searchable
        back.setOnClickListener(onBackClickedListener)
        activity.lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onResume() {
        searchable?.getSearchTerm()?.subscribe { newSearchTerm ->
            if (searchEditText.text.toString() == newSearchTerm) return@subscribe
            setSearch(newSearchTerm)
        }?.addTo(disposable)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun disconnectListener() {
        disposable.clear()
    }


}