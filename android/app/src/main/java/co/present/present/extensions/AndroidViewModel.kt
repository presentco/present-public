package co.present.present.extensions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

val AndroidViewModel.application get() = getApplication<Application>()

fun AndroidViewModel.string(@StringRes stringResId: Int): String = application.string(stringResId)

fun AndroidViewModel.string(@StringRes stringResId: Int, formatArg: String): String = application.string(stringResId, formatArg)

fun AndroidViewModel.string(@PluralsRes pluralsRes: Int, quantity: Int, formatArg: Any): String = application.string(pluralsRes, quantity, formatArg)

val AndroidViewModel.context get() = getApplication<Application>()


