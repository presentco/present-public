package co.present.present.feature.invite

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.StyleSpan
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleObserver
import co.present.present.R
import co.present.present.extensions.hide
import co.present.present.extensions.show
import co.present.present.model.CurrentUser
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.chat_empty.view.*


open class SearchFriendsEmptyView(context: Context, attrSet: AttributeSet) : ConstraintLayout(context, attrSet), LifecycleObserver {

    val disposable = CompositeDisposable()

    init {
        inflate(getContext(), R.layout.chat_empty, this)
    }

    fun bind(currentUser: CurrentUser?, searchTerm: String, inviteButtonClickListener: InviteButtonClickListener) {
        button.setOnClickListener { inviteButtonClickListener.onAddFriendsButtonClicked(currentUser) }
        bind(searchTerm)
    }

    open fun bind(searchTerm: String) {
        if (searchTerm.isBlank()) {
            emptyText.setText(R.string.you_dont_have_friends_yet)
            button.show()
        } else {
            val boldedSearchTerm = SpannableString(searchTerm).apply { setSpan(StyleSpan(Typeface.BOLD), 0, searchTerm.length, 0) }
            val template = resources.getString(R.string.no_results_search_template)
            emptyText.text = TextUtils.expandTemplate(template, boldedSearchTerm)
            button.hide()
        }
    }

    interface InviteButtonClickListener {
        fun onAddFriendsButtonClicked(currentUser: CurrentUser?)
    }

}

class SearchContactsEmptyView(context: Context, attrSet: AttributeSet): SearchFriendsEmptyView(context, attrSet) {
    override fun bind(searchTerm: String) {
        super.bind(searchTerm)
        button.hide()
        if (searchTerm.isBlank()) {
            emptyText.setText(R.string.no_contacts)
        }
    }
}

class SearchFacebookFriendsEmptyView(context: Context, attrSet: AttributeSet): SearchFriendsEmptyView(context, attrSet) {
    override fun bind(searchTerm: String) {
        super.bind(searchTerm)
        button.hide()
        if (searchTerm.isBlank()) {
            emptyText.setText(R.string.no_facebook_friends)
        }
    }
}

