package co.present.present.feature.image

import androidx.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import co.present.present.R
import co.present.present.extensions.applySingleSchedulers
import co.present.present.model.Chat
import co.present.present.model.Circle
import co.present.present.view.OnPageScrolledListener
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_fullscreen.*


class ImageGalleryActivity: FullscreenActivity() {

    lateinit var viewModel: ImageGalleryViewModel
    private val circleId: String by lazy { intent.getStringExtra(Circle.ARG_CIRCLE) }
    private val initialChatId: String by lazy { intent.getStringExtra(Chat.ARG_CHAT) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(ImageGalleryViewModel::class.java)

        disposable += viewModel.getImageGalleryData(circleId, initialChatId).compose(applySingleSchedulers()).subscribeBy(
                onError = {},
                onSuccess = { (chats, selectedChatIndex) ->
                    viewPager.adapter = ImageGalleryAdapter(chats, supportFragmentManager)
                    viewPager.pageMargin = resources.getDimensionPixelSize(R.dimen.spacing_large)
                    viewPager.currentItem = selectedChatIndex
                }
        )

        viewPager.addOnPageChangeListener(object: OnPageScrolledListener() {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                viewModel.selectedChatIndex = position
            }
        })
    }

    override fun performInjection() {
        activityComponent.inject(this)
    }

    companion object {
        fun newIntent(context: Context, circleId: String, commentId: String? = null): Intent {
            val intent = Intent(context, ImageGalleryActivity::class.java)
            intent.putExtra(Circle.ARG_CIRCLE, circleId)
            commentId?.let { intent.putExtra(Chat.ARG_CHAT, it) }
            return intent
        }
    }
}

class ImageGalleryAdapter(private val chats: List<Chat>, fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
    override fun getItem(position: Int): Fragment {
        return ImageFragment.newInstance(chats[position].photo!!)
    }

    override fun getCount() = chats.size

}