package co.present.present.feature.image

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.*
import co.present.present.BaseFragment
import co.present.present.BuildConfig
import co.present.present.R
import co.present.present.extensions.WriteStoragePermissionHandler
import co.present.present.extensions.downloadPictureToGallery
import co.present.present.extensions.load
import co.present.present.extensions.snackbar
import kotlinx.android.synthetic.main.fragment_image.*


class ImageFragment : BaseFragment() {
    private val TAG = javaClass.simpleName

    private val writeStoragePermissionHandler = WriteStoragePermissionHandler
    private val url: String by lazy { arguments!!.getString(urlKey) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_image, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        image.load(url)
        image.setOnClickListener {
            activity?.apply {
                (this as? FullscreenActivity)?.toggle()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater) {
        inflater.inflate(R.menu.image_gallery, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.download -> tryToDownload()
        }
        return true
    }

    private fun tryToDownload() {
        writeStoragePermissionHandler.request(this) { download() }
    }

    private fun download() {
        snackbar(R.string.image_downloading)
        activity?.downloadPictureToGallery(url)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        writeStoragePermissionHandler.onRequestPermissionsResult(activity as AppCompatActivity, requestCode, permissions, grantResults) {
            download()
        }
    }

    companion object {
        private val urlKey = "${BuildConfig.APPLICATION_ID}.url"


        fun newInstance(url: String): ImageFragment {
            return ImageFragment().apply {
                val bundle = Bundle().apply { putString(urlKey, url) }
                arguments = bundle
            }
        }
    }
}