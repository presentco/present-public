package co.present.present.feature.onboarding

import android.content.Context
import android.text.Annotation
import android.text.SpannableString
import android.text.SpannedString
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.getSpans
import co.present.present.R
import co.present.present.analytics.Analytics
import co.present.present.extensions.text

class LegaleseTextView(context: Context, attributeSet: AttributeSet) : AppCompatTextView(context, attributeSet) {

    init {
        val legalese = text(R.string.legalese) as SpannedString
        val linkedLegalese = SpannableString(legalese)
        legalese.getSpans<Annotation>()
                .filter { it.key == "link" }
                .forEachIndexed { index, it ->
                    linkedLegalese.setSpan(object: URLSpan(it.value) {
                        override fun onClick(widget: View?) {
                            super.onClick(widget)
                            when (index) {
                                0 -> analytics?.log(tosEvent)
                                else -> analytics?.log(privacyPolicyEvent)
                            }
                        }
                    }, legalese.getSpanStart(it), legalese.getSpanEnd(it), 0)
                }

        text = linkedLegalese
        movementMethod = LinkMovementMethod.getInstance()
    }

    private lateinit var tosEvent: String
    private lateinit var privacyPolicyEvent: String
    private var analytics: Analytics? = null

    fun setAnalyticsEvents(analytics: Analytics, tos: String, privacyPolicy: String) {
        this.analytics = analytics
        this.tosEvent = tos
        this.privacyPolicyEvent = privacyPolicy
    }
}