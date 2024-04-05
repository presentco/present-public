package co.present.present.feature.common.item

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.StyleSpan
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import co.present.present.BaseActivity
import co.present.present.R
import co.present.present.analytics.Analytics
import co.present.present.extensions.applyFlowableSchedulers
import co.present.present.extensions.slideOverFromBottom
import co.present.present.feature.create.EditCircleActivity
import co.present.present.feature.discovery.Searchable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.chat_empty.view.*


class SearchEmptyView(context: Context, attrSet: AttributeSet) : ConstraintLayout(context, attrSet), LifecycleObserver {

    val disposable = CompositeDisposable()

    init {
        inflate(getContext(), R.layout.chat_empty, this)
    }

    private var analytics: Analytics? = null
    private lateinit var event: String

    private lateinit var searchable: Searchable

    private lateinit var parentActivity: BaseActivity

    fun init(searchable: Searchable, activity: BaseActivity, event: String, analytics: Analytics) {
        this.searchable = searchable
        this.parentActivity = activity
        this.analytics = analytics
        this.event = event
        activity.lifecycle.addObserver(this)

        button.setText(R.string.create_a_circle)
        button.setOnClickListener {
            analytics.log(event)
            parentActivity.doIfLoggedIn {
                parentActivity.slideOverFromBottom<EditCircleActivity>()
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onResume() {
        searchable.getSearchTerm().compose(applyFlowableSchedulers())
                .subscribe { searchTerm ->
                    if (searchTerm.trim().isNotEmpty()) {
                        val boldedSearchTerm = SpannableString(searchTerm).apply { setSpan(StyleSpan(Typeface.BOLD), 0, searchTerm.length, 0) }
                        val template = resources.getString(R.string.no_results_yet_search_template)
                        emptyText.text = TextUtils.expandTemplate(template, boldedSearchTerm)
                    } else {
                        emptyText.setText(R.string.no_circles_found)
                    }
                }.addTo(disposable)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun disconnectListener() {
        disposable.clear()
    }
}

