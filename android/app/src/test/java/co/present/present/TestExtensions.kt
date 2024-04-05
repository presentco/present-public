package co.present.present

import android.app.Activity
import co.present.present.support.IntentHelper
import org.hamcrest.CoreMatchers
import org.junit.Assert

fun <T: Activity> Activity.assertLaunched(clazz: Class<T>) {
    Assert.assertThat(IntentHelper.getLastStartedIntent(this).intentClass.canonicalName,
            CoreMatchers.equalTo(clazz.name))
}

