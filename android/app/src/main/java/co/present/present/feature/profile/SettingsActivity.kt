package co.present.present.feature.profile

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import co.present.present.BaseActivity
import co.present.present.R
import co.present.present.extensions.*
import co.present.present.location.LocationDataProvider
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.OnItemClickListener
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_city_picker.*
import kotlinx.android.synthetic.main.toolbar.*
import javax.inject.Inject


class SettingsActivity : BaseActivity(), OnItemClickListener {

    private val TAG = javaClass.simpleName
    @Inject lateinit var viewModel: SettingsViewModel
    @Inject lateinit var locationDataProvider: LocationDataProvider
    private val adapter = GroupAdapter<ViewHolder>().apply {
        setOnItemClickListener(this@SettingsActivity)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_city_picker)
        citiesRecyclerView.adapter = adapter
        citiesRecyclerView.layoutManager = LinearLayoutManager(this)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setTitle(R.string.settings)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onResume() {
        super.onResume()

        disposable += viewModel.getItems().compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = { e ->
                            Log.e(TAG, "Error getting city list", e)
                        },
                        onNext = { items ->
                            adapter.update(items)
                        }
                )
    }

    override fun onItemClick(item: Item<*>, view: View) {
        when (item) {
            is LinkActionItem -> launchUrl(item.link)
            is LogOutActionItem -> confirmLogout()
            is DeleteAccountActionItem -> confirmDeleteAccount()
        }
    }

    private fun confirmDeleteAccount() {
        dialog(message = R.string.delete_account_message, title = R.string.delete_account_title, positiveButtonText = R.string.ok) {
            viewModel.deleteAccount().compose(applyCompletableSchedulers())
                    .subscribeBy(
                            onError = { snackbar(R.string.network_error) },
                            onComplete = { logOut() }
                    )
        }
    }

    private fun confirmLogout() {
        dialog(message = R.string.log_out_message, title = R.string.log_out_title, positiveButtonText = R.string.ok) {
            logOut()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }

    override fun performInjection() {
        activityComponent.inject(this)
    }
}