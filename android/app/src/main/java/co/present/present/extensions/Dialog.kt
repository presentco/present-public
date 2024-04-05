package co.present.present.extensions


import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager


fun Fragment.dialog(@StringRes message: Int? = null,
                    @StringRes title: Int? = null,
                    @ArrayRes items: Int? = null,
                    @StringRes positiveButtonText: Int? = null,
                    @StringRes negativeButtonText: Int? = null,
                    cancelable: Boolean = true,
                    tag: String = "SimpleDialogFragment",
                    onCancel: () -> Unit = {},
                    onNegative: () -> Unit = {},
                    onItemClick: (DialogInterface, Int) -> Unit = { dialogInterface, itemIndex -> },
                    onPositive: () -> Unit = {}): DialogFragment {
    return dialog(activity!!, fragmentManager!!, message, title, items, positiveButtonText, negativeButtonText, cancelable, tag, onCancel, onNegative, onItemClick, onPositive)
}

fun AppCompatActivity.dialog(@StringRes message: Int? = null,
                             @StringRes title: Int? = null,
                             @ArrayRes items: Int? = null,
                             @StringRes positiveButtonText: Int? = null,
                             @StringRes negativeButtonText: Int? = null,
                             cancelable: Boolean = true,
                             tag: String = "SimpleDialogFragment",
                             onCancel: () -> Unit = {},
                             onNegative: () -> Unit = {},
                             onItemClick: (DialogInterface, Int) -> Unit = { dialogInterface, itemIndex -> },
                             onPositive: () -> Unit = {}): DialogFragment {
    return dialog(this, supportFragmentManager, message, title, items, positiveButtonText, negativeButtonText, cancelable, tag, onCancel, onNegative, onItemClick, onPositive)
}

fun Fragment.dialog(message: String? = null,
                    title: String? = null,
                    positiveButtonText: String? = null,
                    negativeButtonText: String? = null,
                    cancelable: Boolean = true,
                    tag: String = "SimpleDialogFragment",
                    onCancel: () -> Unit = {},
                    onNegative: () -> Unit = {},
                    onPositive: () -> Unit = {}): DialogFragment {
    return dialog(activity!!, fragmentManager!!, message, title, positiveButtonText, negativeButtonText, cancelable, tag, onCancel, onNegative, onPositive)
}

fun AppCompatActivity.dialog(message: String? = null,
                             title: String? = null,
                             positiveButtonText: String? = null,
                             negativeButtonText: String? = null,
                             cancelable: Boolean = true,
                             tag: String = "SimpleDialogFragment",
                             onCancel: () -> Unit = {},
                             onNegative: () -> Unit = {},
                             onPositive: () -> Unit = {}): DialogFragment {
    return dialog(this, supportFragmentManager, message, title, positiveButtonText, negativeButtonText, cancelable, tag, onCancel, onNegative, onPositive)
}

private fun dialog(activity: Activity, fragmentManager: FragmentManager,
                   @StringRes message: Int? = null,
                   @StringRes title: Int? = null,
                   @ArrayRes items: Int? = null,
                   @StringRes positiveButtonText: Int? = null,
                   @StringRes negativeButtonText: Int? = null,
                   cancelable: Boolean = true,
                   tag: String = "SimpleDialogFragment",
                   onCancel: () -> Unit = {},
                   onNegative: () -> Unit = {},
                   onItemClick: (DialogInterface, Int) -> Unit = { dialogInterface, itemIndex -> },
                   onPositive: () -> Unit = {}): DialogFragment {

    val getDialogBuilder: () -> AlertDialog.Builder = {
        AlertDialog.Builder(activity).apply {
            title?.let { setTitle(title) }
            items?.let { setItems(items, onItemClick) }
            message?.let { setMessage(message) }
            positiveButtonText?.let { setPositiveButton(positiveButtonText) { _, _ -> onPositive() } }
            negativeButtonText?.let { setNegativeButton(negativeButtonText) { _, _ -> onNegative() } }
        }
    }
    return dialog(fragmentManager, getDialogBuilder, cancelable, tag, onCancel)
}

private fun dialog(activity: Activity, fragmentManager: FragmentManager,
                   message: String? = null,
                   title: String? = null,
                   positiveButtonText: String? = null,
                   negativeButtonText: String? = null,
                   cancelable: Boolean = true,
                   tag: String = "SimpleDialogFragment",
                   onCancel: () -> Unit = {},
                   onNegative: () -> Unit = {},
                   onPositive: () -> Unit = {}): DialogFragment {

    val getDialogBuilder: () -> AlertDialog.Builder = {
        AlertDialog.Builder(activity).apply {
            title?.let { setTitle(title) }
            message?.let { setMessage(message) }
            positiveButtonText?.let { setPositiveButton(positiveButtonText) { _, _ -> onPositive() } }
            negativeButtonText?.let { setNegativeButton(negativeButtonText) { _, _ -> onNegative() } }
        }
    }
    return dialog(fragmentManager, getDialogBuilder, cancelable, tag, onCancel)
}

private fun dialog(fragmentManager: FragmentManager,
                   getDialogBuilder: () -> AlertDialog.Builder,
                   cancelable: Boolean = true,
                   tag: String = "SimpleDialogFragment",
                   onCancel: () -> Unit = {}
                ): DialogFragment {
    class SimpleDialogFragment : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return getDialogBuilder().apply {

                if (!cancelable) {
                    // Default behavior of non-cancelable dialog is to trap the back key, which
                    // we don't want.
                    setOnKeyListener { dialog, keycode, event ->
                        if (keycode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                            dialog.cancel()
                            onCancel.invoke()
                        }
                        false
                    }
                }

                setCancelable(cancelable)
                setOnCancelListener { onCancel.invoke() }
            }.create()
        }
    }

    return SimpleDialogFragment().apply {
        isCancelable = cancelable
        show(fragmentManager, tag)
    }
}

fun Fragment.toast(@StringRes message: Int) {
    Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
}

