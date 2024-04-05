package co.present.present.feature.create

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import co.present.present.BaseActivity
import co.present.present.R
import co.present.present.extensions.*
import co.present.present.feature.invite.AddToCircleActivity
import co.present.present.model.Circle
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.OnItemClickListener
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_circle_share.*


class CircleShareActivity : BaseActivity(), OnItemClickListener, CircleShareButtonsItem.OnShareButtonClickListener {

    private val circleId: String by lazy { intent.getStringExtra(Circle.ARG_CIRCLE) }
    private val adapter = GroupAdapter<ViewHolder>().apply { setOnItemClickListener(this@CircleShareActivity) }

    override fun performInjection() {
        activityComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_circle_share)

        setSupportActionBar(toolbar)
        supportActionBar?.apply { title = "" }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val viewModel = ViewModelProviders.of(this, viewModelFactory).get(CircleShareViewModel::class.java)
        viewModel.getItems(circleId, this).firstOrError()
                .compose(applySingleSchedulers())
                .subscribeBy(
                        onError = {},
                        onSuccess = {
                            adapter.update(it)
                        }
                ).addTo(disposable)
    }

    override fun onItemClick(item: Item<*>, view: View) {
        when (item) {
            is FriendCircleItem -> addToCircle(item.circle.id)
            is AddYourFriendsHeader -> addToCircle(item.circle.id)
        }
    }

    private fun addToCircle(circleId: String) {
        start(AddToCircleActivity.newIntent(this, circleId))
        finish()
    }

    override fun onEmailClick(item: Item<*>, circle: Circle) {
        shareToEmail(circle)
    }

    override fun onFacebookClick(item: Item<*>, circle: Circle) {
        shareToFacebook(circle)
    }

    override fun onTwitterClick(item: Item<*>, circle: Circle) {
        shareToTwitter(circle)
    }

    override fun onSmsClick(item: Item<*>, circle: Circle) {
        normalShare(circle)
    }

    override fun onLinkClick(item: Item<*>, circle: Circle) {
        copyToClipboard(shareText(circle))
        toast(R.string.link_copied)
    }

    private fun normalShare(circle: Circle) {
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, circle.title)
            putExtra(Intent.EXTRA_TEXT, shareText(circle))
            startActivity(Intent.createChooser(this, getString(R.string.share_circle)))
        }
        finish()
    }

    private fun shareToEmail(circle: Circle) {
        Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:") // only email apps should handle this
            putExtra(Intent.EXTRA_SUBJECT, circle.title)
            putExtra(Intent.EXTRA_TEXT, shareText(circle))

            if (this.resolveActivity(packageManager) != null) {
                startActivity(this)
                finish()
            } else {
                normalShare(circle)
            }
        }
    }

    private fun shareToFacebook(circle: Circle) {
        Intent(Intent.ACTION_SEND).apply {
            `package` = "com.facebook.katana"
            type = "text/plain"
            putExtra("android.intent.extra.TEXT", circle.url)

            if (this.resolveActivity(packageManager) != null) {
                startActivity(this)
            } else {
                launchUrl("https://www.facebook.com/sharer/sharer.php?u=${circle.url}")
            }
        }
        finish()
    }

    private fun shareText(circle: Circle): String {
        return getString(R.string.circle_share_template, circle.title, circle.url)
    }

    private fun shareToTwitter(circle: Circle) {
        Intent(Intent.ACTION_SEND).apply {
            `package` = "com.twitter.android"
            putExtra(Intent.EXTRA_TEXT, shareText(circle))
            type = "text/plain"

            if (this.resolveActivity(packageManager) != null) {
                startActivity(this)
            } else {
                val i = Intent(Intent.ACTION_VIEW)
                i.putExtra(Intent.EXTRA_TEXT, shareText(circle))
                i.data = Uri.parse("https://twitter.com/intent/tweet?text=" + Uri.encode(shareText(circle)))
                startActivity(i)
            }
        }
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.share_circle, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.close -> finish()
        }
        return true
    }

    companion object {

        fun newIntent(context: Context, circleId: String): Intent {
            val intent = Intent(context, CircleShareActivity::class.java)
            intent.putExtra(Circle.ARG_CIRCLE, circleId)
            return intent
        }
    }
}