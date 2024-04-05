package co.present.present

import android.net.Uri
import androidx.fragment.app.Fragment
import android.view.View
import co.present.present.analytics.Analytics
import co.present.present.di.ActivityComponent
import co.present.present.view.OnLinkClickedListener
import com.xwray.groupie.Item
import com.xwray.groupie.OnItemClickListener
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

open class BaseFragment : Fragment(), OnItemClickListener, OnLinkClickedListener {

    protected val disposable = CompositeDisposable()
    @Inject lateinit var analytics: Analytics

    /**
     * Get the Activity component for dependency injection.
     */
    protected val activityComponent: ActivityComponent by lazy {
        (activity as BaseActivity).activityComponent
    }

    override fun onItemClick(item: Item<*>, view: View) {
        baseActivity.onItemClick(item, view)
    }

    override fun onLinkClick(uri: Uri) {
        baseActivity.launchDeepLink(uri)
    }

    override fun onStop() {
        disposable.clear()
        super.onStop()
    }

    val baseActivity: BaseActivity get() = activity!! as BaseActivity

    override fun onResume() {
        super.onResume()
        baseActivity.invalidateOptionsMenu()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)

        // Since all the main fragments are resumed the whole time (regardless of
        // being shown or hidden) this is the only lifecycle method where we
        // can tell which one should own the support action bar
        if (!hidden) {
            setSupportActionBar()
        }
    }

    private fun setSupportActionBar() {
        baseActivity.setSupportActionBar(view?.findViewById(R.id.toolbar))
        baseActivity.supportActionBar?.title = ""
    }
}