package co.present.present.feature.common

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewTreeObserver
import android.view.animation.AccelerateInterpolator
import co.present.present.BaseActivity
import co.present.present.R
import co.present.present.extensions.fromApi

/**
 * Most of this code is taken from this excellent article:
 * https://android.jlelse.eu/a-little-thing-that-matter-how-to-reveal-an-activity-with-circular-revelation-d94f9bfcae28
 */
abstract class CircularRevealActivity : BaseActivity() {

    lateinit var rootLayout: View

    private var revealX = 0f
    private var revealY = 0f
    private var radius = 0f

    abstract val layout: Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout)
        val intent = intent

        rootLayout = findViewById(R.id.root)

        if (savedInstanceState == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                intent.hasExtra(EXTRA_CIRCULAR_REVEAL_X) &&
                intent.hasExtra(EXTRA_CIRCULAR_REVEAL_Y)) {
            rootLayout.visibility = View.INVISIBLE

            revealX = intent.getFloatExtra(EXTRA_CIRCULAR_REVEAL_X, revealX)
            revealY = intent.getFloatExtra(EXTRA_CIRCULAR_REVEAL_Y, revealY)
            radius = intent.getFloatExtra(EXTRA_RADIUS, radius)

            val viewTreeObserver = rootLayout.viewTreeObserver
            if (viewTreeObserver.isAlive) {
                viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        revealActivity()
                        rootLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                })
            }
        } else {
            rootLayout.visibility = View.VISIBLE
        }
    }

    protected fun revealActivity() {
        fromApi(Build.VERSION_CODES.LOLLIPOP) @TargetApi(Build.VERSION_CODES.LOLLIPOP) {
            val finalRadius = (Math.max(rootLayout.width, rootLayout.height) * 1.1f)

            // create the animator for this view (the start radius is zero)
            val circularReveal = ViewAnimationUtils.createCircularReveal(rootLayout, revealX.toInt(), revealY.toInt(), radius, finalRadius)
            circularReveal.duration = 300
            circularReveal.interpolator = AccelerateInterpolator()

            // make the view visible and start the animation
            rootLayout.visibility = View.VISIBLE
            circularReveal.start()
        }
    }

    override fun onBackPressed() {
        unRevealActivity()
    }

    private fun unRevealActivity() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
                || !(intent.hasExtra(EXTRA_CIRCULAR_REVEAL_X) && intent.hasExtra(EXTRA_CIRCULAR_REVEAL_Y))) {
            finish()
        } else {
            val finalRadius = (Math.max(rootLayout.width, rootLayout.height) * 1.1f)
            val circularReveal = ViewAnimationUtils.createCircularReveal(
                    rootLayout, revealX.toInt(), revealY.toInt(), finalRadius, radius)

            circularReveal.duration = 300
            circularReveal.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    rootLayout.visibility = View.INVISIBLE
                    finish()
                }
            })

            circularReveal.start()
        }
    }

    companion object {

        const val EXTRA_CIRCULAR_REVEAL_X = "EXTRA_CIRCULAR_REVEAL_X"
        const val EXTRA_CIRCULAR_REVEAL_Y = "EXTRA_CIRCULAR_REVEAL_Y"
        const val EXTRA_RADIUS = "EXTRA_RADIUS"
    }
}

inline fun <reified T : CircularRevealActivity> Activity.startWithCircularReveal(view: View) {
    val options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, view, "transition")
    val revealX = (view.x + view.width / 2f)
    val revealY = (view.y + view.height / 2f)

    val intent = Intent(this, T::class.java)
    intent.putExtra(CircularRevealActivity.EXTRA_CIRCULAR_REVEAL_X, revealX)
    intent.putExtra(CircularRevealActivity.EXTRA_CIRCULAR_REVEAL_Y, revealY)
    intent.putExtra(CircularRevealActivity.EXTRA_RADIUS, Math.min(view.width, view.height).toFloat())

    ActivityCompat.startActivity(this, intent, options.toBundle())
}