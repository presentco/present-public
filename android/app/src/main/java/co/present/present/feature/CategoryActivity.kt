package co.present.present.feature

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.ViewModelProviders
import co.present.present.BaseActivity
import co.present.present.R
import co.present.present.extensions.applyFlowableSchedulers
import co.present.present.extensions.hide
import co.present.present.extensions.show
import co.present.present.extensions.transaction
import co.present.present.feature.discovery.CategoryFragment
import co.present.present.feature.discovery.CategoryViewModel
import co.present.present.model.Category
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.header_category.*
import kotlinx.android.synthetic.main.toolbar.*

class CategoryActivity: BaseActivity() {

    val category: String by lazy { intent.getStringExtra(Category.CATEGORY) }

    private val onBackClicked = View.OnClickListener { viewModel.goBack() }

    override fun performInjection() {
        activityComponent.inject(this)
    }

    private lateinit var viewModel: CategoryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(CategoryViewModel::class.java)


        val tag = "content"
        if (supportFragmentManager.findFragmentByTag(tag) == null) {
            supportFragmentManager.transaction {
                add(R.id.content, CategoryFragment.newInstance(category))
            }
        }

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
        toolbarTitle.text = if (category == Category.ALL) "Nearby" else category

        // Set up and connect the search view
        searchView.init(viewModel, this, onBackClicked)
        searchIcon.setOnClickListener { viewModel.setSearchMode() }
    }

    override fun onResume() {
        super.onResume()
        viewModel.getState().compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = {},
                        onNext = { onStateChanged(it) }
                ).addTo(disposable)
    }

    private fun onStateChanged(it: CategoryViewModel.State) {
        when (it) {
            CategoryViewModel.State.Normal -> {
                toolbarTitle.show()
                searchView.hide()
                searchIcon.show()
            }
            else -> {
                toolbarTitle.hide()
                searchView.show()
                searchView.setFocus()
                searchIcon.hide()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return true
    }

    companion object {

        fun newIntent(context: Context, category: String): Intent {
            val intent = Intent(context, CategoryActivity::class.java)
            intent.putExtra(Category.CATEGORY, category)
            return intent
        }
    }
}