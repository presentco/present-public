package co.present.present.feature.detail

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import co.present.present.R
import co.present.present.extensions.string

import co.present.present.feature.detail.chat.ChatFragment
import co.present.present.feature.detail.info.CircleDetailFragment

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class CircleDetailPagerAdapter internal constructor(fragmentManager: FragmentManager, val context: Context, private val circleId: String) : FragmentPagerAdapter(fragmentManager) {

    override fun getItem(position: Int): Fragment {
        return if (position == CircleActivity.chatTabIndex) {
            ChatFragment.newInstance(circleId)
        } else {
            CircleDetailFragment.newInstance(circleId)
        }
    }

    override fun getCount(): Int {
        return NUMBER_OF_PAGES
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> context.string(R.string.tab_chat)
            else -> context.string(R.string.tab_info)
        }
    }

    companion object {
        private val NUMBER_OF_PAGES = 2
    }
}
