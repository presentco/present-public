package co.present.present.feature.create

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import co.present.present.BaseActivity
import co.present.present.R
import co.present.present.extensions.finishAndSlideBackOverToRight
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.activity_preapprove.*
import kotlinx.android.synthetic.main.toolbar.*
import present.proto.GroupMemberPreapproval

class PreApproveActivity: BaseActivity() {

    override fun performInjection() {
        activityComponent.inject(this)
    }

    private val adapter = GroupAdapter<ViewHolder>().apply { setOnItemClickListener(this@PreApproveActivity) }
    private val initialPreapproval: GroupMemberPreapproval by lazy {
        GroupMemberPreapproval.fromValue(intent.getIntExtra(EXTRA_PREAPPROVAL, 0))
    }
    private val womenOnly: Boolean by lazy {
        intent.getBooleanExtra(EXTRA_WOMEN_ONLY, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preapprove)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setTitle(R.string.pre_approve_title)
            setDisplayHomeAsUpEnabled(true)
        }

        adapter.addAll(GroupMemberPreapproval.values()
                .filterNot { it == GroupMemberPreapproval.UNKNOWN }
                .map {
                    PreapproveCheckboxItem(it, womenOnly, initialPreapproval == it)
                })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finishAndSlideBackOverToRight()
        }
        return true
    }

    override fun onItemClick(item: Item<*>, view: View) {
        if (item is PreapproveCheckboxItem) {
            val data = Intent().apply { putExtra(EXTRA_PREAPPROVAL, item.preapproval.value) }
            setResult(Activity.RESULT_OK, data)
            finishAndSlideBackOverToRight()
        }
    }

    companion object {
        const val EXTRA_PREAPPROVAL = "preapproval"
        const val EXTRA_WOMEN_ONLY = "womenOnly"

        fun newIntent(context: Context, preapproval: GroupMemberPreapproval, womenOnly: Boolean): Intent {
            return Intent(context, PreApproveActivity::class.java).apply {
                putExtra(EXTRA_PREAPPROVAL, preapproval.value)
                putExtra(EXTRA_WOMEN_ONLY, womenOnly)
            }
        }

        fun fromIntent(intent: Intent?): GroupMemberPreapproval {
            if (intent == null) error("${PreApproveActivity::class.java.simpleName} should never return RESULT_OK with a null intent")
            return GroupMemberPreapproval.fromValue(intent.getIntExtra(EXTRA_PREAPPROVAL, 0))
        }
    }
}