package co.present.present.extensions

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.app.DownloadManager
import android.content.*
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Context.DOWNLOAD_SERVICE
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import co.present.present.R
import com.google.android.material.snackbar.Snackbar
import java.io.File


fun Activity.launchUrl(@StringRes urlStringRes: Int) {
    launchUrl(getString(urlStringRes))
}

fun Activity.launchUrl(urlString: String) {
    val uri = Uri.parse(urlString)
    with(Intent(Intent.ACTION_VIEW, uri)) {
        startActivity(this)
    }
}

val Context.inputMethodManager get() = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

fun Activity.isCallable(intent: Intent): Boolean {
    return packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .isNotEmpty()
}

val Context.clipboardManager get() = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

val Context.layoutInflater get() = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

val Context.downloadManager get() = getSystemService(DOWNLOAD_SERVICE) as DownloadManager

fun Context.copyToClipboard(string: String) {
    val clip = ClipData.newPlainText(string, string)
    clipboardManager.primaryClip = clip
}

fun Activity.setStatusBarColor(color: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = color
    }
}

fun Activity.uri(@StringRes url: Int) = Uri.parse(getString(url))

@SuppressLint("CommitTransaction")
inline fun FragmentManager.transaction(func: FragmentTransaction.() -> Unit) {
    beginTransaction().apply {
        func()
        commit()
    }
}

fun AppCompatActivity.add(fragment: Fragment, @IdRes frameId: Int) {
    supportFragmentManager.transaction { add(frameId, fragment) }
}

fun AppCompatActivity.replace(fragment: Fragment, @IdRes frameId: Int) {
    supportFragmentManager.transaction { replace(frameId, fragment) }
}

fun AppCompatActivity.slideOverFromRight(fragment: Fragment, @IdRes frameId: Int) {
    supportFragmentManager.transaction {
        setCustomAnimations(R.anim.slide_from_right, R.anim.slide_to_left)
        replace(frameId, fragment)
    }
}

@Deprecated("Use Activity.start<T>()")
fun <T : Activity> Activity.start(clazz: Class<T>) {
    startActivity(Intent(this, clazz))
}

inline fun <reified T : Activity> Activity.start() {
    startActivity(Intent(this, T::class.java))
}

fun Activity.start(intent: Intent) {
    startActivity(intent)
}

inline fun <reified T : Activity> Fragment.start() {
    requireActivity().start<T>()
}

fun Fragment.start(intent: Intent) {
    requireActivity().start(intent)
}

fun <T : Activity> Activity.startWithSharedElement(clazz: Class<T>, view: View) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        start(clazz)
    } else {
        val options = ActivityOptions.makeSceneTransitionAnimation(this, view, view.transitionName)
        startActivity(Intent(this, clazz), options.toBundle())
    }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
private fun View.asSharedElementPair(): Pair<View, String> = Pair(this, transitionName)

inline fun <reified T : Activity> Activity.slideOverFromRight() {
    start<T>()
    overridePendingTransition(R.anim.slide_from_right, R.anim.stay)
}

inline fun <reified T : Activity> Activity.slideOverFromBottom() {
    start<T>()
    overridePendingTransition(R.anim.slide_from_bottom, R.anim.stay)
}

fun Activity.finishAndSlideBackOverToRight() {
    finish()
    overridePendingTransition(R.anim.stay, R.anim.slide_to_right)
}

inline fun <reified T : Activity> Activity.slideFromRight() {
    start<T>()
    overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left)
}

fun Activity.slideFromRight(intent: Intent) {
    start(intent)
    overridePendingTransition(R.anim.slide_from_right, R.anim.stay)
}

fun Activity.slideFromRight(intent: Intent, requestCode: Int) {
    startActivityForResult(intent, requestCode)
    overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left)
}

fun Activity.slideOverFromRight(intent: Intent) {
    start(intent)
    overridePendingTransition(R.anim.slide_from_right, R.anim.stay)
}

fun Activity.slideOverFromRight(intent: Intent, requestCode: Int) {
    startActivityForResult(intent, requestCode)
    overridePendingTransition(R.anim.slide_from_right, R.anim.stay)
}

fun Fragment.slideOverFromRight(intent: Intent, requestCode: Int) {
    startActivityForResult(intent, requestCode)
    requireActivity().overridePendingTransition(R.anim.slide_from_right, R.anim.stay)
}

fun Activity.slideOverFromBottom(intent: Intent) {
    start(intent)
    overridePendingTransition(R.anim.slide_from_bottom, R.anim.stay)
}

fun Activity.finishAndSlideBackOverToBottom() {
    finish()
    overridePendingTransition(R.anim.stay, R.anim.slide_to_bottom)
}

fun Activity.fullscreen() {
    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
}

fun Activity.showApplicationSettings() {
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)).apply {
        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(this)
    }
}

fun Activity.showLocationSettings() {
    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
}

fun Fragment.snackbar(string: String, length: Int = Snackbar.LENGTH_LONG) {
    view?.let {
        Snackbar.make(it, string, length).show()
    }
}

fun Fragment.snackbar(@StringRes stringRes: Int, length: Int = Snackbar.LENGTH_LONG) {
    view?.let {
        Snackbar.make(it, stringRes, length).show()
    }
}

fun Fragment.snackbar(@StringRes stringRes: Int,
                      length: Int = Snackbar.LENGTH_LONG,
                      callback: Snackbar.Callback = Snackbar.Callback(),
                      @StringRes actionStringRes: Int,
                      action: (View) -> Unit) {
    view?.let {
        Snackbar.make(it, stringRes, length)
                .setActionTextColor(color(R.color.lightText))
                .setAction(actionStringRes, action)
                .addCallback(callback)
                .show()
    }
}

fun Activity.snackbar(@StringRes stringRes: Int, view: View? = findViewById(android.R.id.content), length: Int = Snackbar.LENGTH_LONG) {
    view?.let {
        Snackbar.make(view, stringRes, length).show()
    }
}

fun Activity.snackbar(string: String, view: View? = findViewById(android.R.id.content), length: Int = Snackbar.LENGTH_LONG) {
    view?.let {
        Snackbar.make(it, string, length).show()
    }
}

fun Context.toast(@StringRes stringRes: Int, length: Int = Toast.LENGTH_LONG) {
    Toast.makeText(this, stringRes, length).show()
}

fun Context.toast(string: String, length: Int = Toast.LENGTH_LONG) {
    Toast.makeText(this, string, length).show()
}

fun Context.string(@StringRes stringRes: Int): String = getString(stringRes)

fun Context.string(@StringRes stringRes: Int, formatArg: String): String = getString(stringRes, formatArg)

fun Context.string(@PluralsRes pluralRes: Int, quantity: Int): String = resources.getQuantityString(pluralRes, quantity)

fun Context.string(@PluralsRes pluralRes: Int, quantity: Int, formatArg: Any): String = resources.getQuantityString(pluralRes, quantity, formatArg)

fun Fragment.string(@StringRes stringRes: Int): String = getString(stringRes)

fun Fragment.string(@StringRes stringRes: Int, formatArg: String): String = getString(stringRes, formatArg)

fun Context.text(@StringRes stringRes: Int): CharSequence = getText(stringRes)

fun View.text(@StringRes stringRes: Int): CharSequence = context.text(stringRes)

fun Fragment.text(@StringRes stringRes: Int): CharSequence = getText(stringRes)

fun View.string(@StringRes stringRes: Int, formatArg: String): String = context.getString(stringRes, formatArg)

@ColorInt
fun Context.color(@ColorRes colorRes: Int): Int = ContextCompat.getColor(this, colorRes)

@ColorInt
fun Fragment.color(@ColorRes colorRes: Int): Int = context!!.color(colorRes)

fun Context.downloadPictureToGallery(url: String) {
    val filename = "present_${System.currentTimeMillis()}.jpg"
    val downloadUri = Uri.parse(url)
    DownloadManager.Request(downloadUri).apply {
        setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        setAllowedOverRoaming(false)
        setTitle(filename)
        setMimeType("image/jpeg")
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, File.separator + filename)
        downloadManager.enqueue(this)
    }
}

fun Activity.launchYoutube(id: String) {
    val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + id))
    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=" + id))
    try {
        startActivity(appIntent)
    } catch (ex: ActivityNotFoundException) {
        startActivity(webIntent)
    }
}
