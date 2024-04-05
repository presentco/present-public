package co.present.present.feature.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import co.present.present.BaseFragment
import co.present.present.R
import co.present.present.R.id.*
import co.present.present.ViewModelFactory
import co.present.present.analytics.AmplitudeEvents
import co.present.present.analytics.AmplitudeKeys
import co.present.present.extensions.*
import co.present.present.feature.BottomNavViewModel
import co.present.present.feature.onboarding.PhoneLoginActivity
import co.present.present.feature.onboarding.step.FacebookLinkActivity
import co.present.present.model.Space
import kotlinx.android.synthetic.main.fragment_tour.*
import javax.inject.Inject


sealed class PlaceholderFragment : BaseFragment() {

    protected open val screenshotResId: Int = R.drawable.feed
    protected abstract val titleResId: Int
    protected abstract val subtitleResId: Int

    @Inject lateinit var viewModelFactory: ViewModelFactory

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_tour, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activityComponent.inject(this)

        title.setText(titleResId)
        subtitle.setText(subtitleResId)
        illustration.setImageResource(screenshotResId)

        button.setOnClickListener { launchPhoneVerification() }

    }

    fun launchPhoneVerification() {
        requireActivity().slideOverFromBottom<PhoneLoginActivity>()
    }
}

abstract class LocationPromptFragment : PlaceholderFragment() {
    override val titleResId: Int = R.string.everyone_feed_title
    override val subtitleResId: Int = R.string.everyone_feed_subtitle
    override val screenshotResId: Int = R.drawable.ic_discoverimage
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        disclaimer.show()

        button.setText(R.string.allow_location_access)
        button.setOnClickListener{
            logAllowLocationAccessClicked()
            onAllowLocationAccessClicked()
        }

        disclaimer.setAnalyticsEvents(analytics,
                tos = AmplitudeEvents.HOME_PLACEHOLDER_TAP_TOS,
                privacyPolicy = AmplitudeEvents.HOME_PLACEHOLDER_TAP_PRIVACY)
    }

    open fun logAllowLocationAccessClicked() {
        analytics.log(AmplitudeEvents.HOME_PLACEHOLDER_TAP_ALLOW_LOCATION, AmplitudeKeys.SPACE_ID, Space.everyoneId)
    }

    abstract fun onAllowLocationAccessClicked()

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser) {
//            analytics.log(AmplitudeEvents.HOME_PLACEHOLDER_VIEW, AmplitudeKeys.SPACE_ID, Space.everyoneId)
        }
    }
}

abstract class ContactsPromptFragment : PlaceholderFragment() {
    override val titleResId: Int = R.string.contacts_prompt_title
    override val subtitleResId: Int = R.string.contacts_prompt_subtitle
    override val screenshotResId: Int = R.drawable.ic_discoverimage
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button.setText(R.string.connect_contacts)
        button.setOnClickListener{
            onConnectContactsClicked()
        }
    }

    abstract fun onConnectContactsClicked()

}

class FacebookPromptFragment : PlaceholderFragment() {
    override val titleResId: Int = R.string.facebook_prompt_title
    override val subtitleResId: Int = R.string.facebook_prompt_subtitle
    override val screenshotResId: Int = R.drawable.ic_discoverimage
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button.setText(R.string.connect_facebook)
        button.setOnClickListener{
            onConnectFacebookClicked()
        }
    }

    fun onConnectFacebookClicked() {
        requireActivity().start<FacebookLinkActivity>()
    }

}

class WomenOnlyPlaceholderFragment: PlaceholderFragment() {
    override val titleResId: Int = R.string.women_feed_title
    override val subtitleResId: Int = R.string.women_feed_subtitle
    override val screenshotResId: Int = R.drawable.ic_connectwithwomenimage

    val bottomNavViewModel: BottomNavViewModel by lazy {
        ViewModelProviders.of(requireActivity(), viewModelFactory).get(BottomNavViewModel::class.java)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button.show()
        button.setText(R.string.connect_with_facebook)
        button.setOnClickListener {
            analytics.log(AmplitudeEvents.HOME_PLACEHOLDER_TAP_CONNECT_WITH_FACEBOOK)
            requireActivity().start<FacebookLinkActivity>()
        }

        textButton1.show()
        textButton1.setText(R.string.why_facebook)
        textButton1.setOnClickListener {
            analytics.log(AmplitudeEvents.HOME_PLACEHOLDER_TAP_WHY_FACEBOOK)
            requireActivity().launchUrl(string(R.string.onboarding_why_facebook))
        }

        textButton2.show()
        textButton2.setText(R.string.im_not_interested)
        textButton2.setOnClickListener {
            analytics.log(AmplitudeEvents.HOME_PLACEHOLDER_TAP_HIDE_THIS_TAB)
            bottomNavViewModel.setNotAWoman()
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser) {
            analytics.log(AmplitudeEvents.HOME_PLACEHOLDER_VIEW, AmplitudeKeys.SPACE_ID, Space.womenOnlyId)
        }
    }
}

class WomenOnlyLoggedOutPlaceholderFragment: PlaceholderFragment() {
    override val titleResId: Int = R.string.women_feed_title
    override val subtitleResId: Int = R.string.women_feed_subtitle
    override val screenshotResId: Int = R.drawable.ic_connectwithwomenimage

    val bottomNavViewModel: BottomNavViewModel by lazy {
        ViewModelProviders.of(requireActivity(), viewModelFactory).get(BottomNavViewModel::class.java)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button.show()
        button.setText(R.string.get_started)
        button.setOnClickListener {
            analytics.log(AmplitudeEvents.HOME_PLACEHOLDER_TAP_GET_STARTED)
            requireActivity().start<PhoneLoginActivity>()
        }

        textButton1.show()
        textButton1.setText(R.string.why_facebook)
        textButton1.setOnClickListener {
            analytics.log(AmplitudeEvents.HOME_PLACEHOLDER_TAP_WHY_FACEBOOK)
            requireActivity().launchUrl(string(R.string.onboarding_why_facebook))
        }

        textButton2.show()
        textButton2.setText(R.string.im_not_interested)
        textButton2.setOnClickListener {
            analytics.log(AmplitudeEvents.HOME_PLACEHOLDER_TAP_HIDE_THIS_TAB)
            bottomNavViewModel.setNotAWoman()
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser) {
            analytics.log(AmplitudeEvents.HOME_PLACEHOLDER_VIEW, AmplitudeKeys.SPACE_ID, Space.womenOnlyId)
        }
    }
}

class CreateCirclePlaceholderFragment: PlaceholderFragment() {
    override val titleResId: Int = R.string.create_tour_title
    override val subtitleResId: Int = R.string.create_tour_subtitle
    override val screenshotResId: Int = R.drawable.ic_createcircleimage

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Actually hide these buttons instead of making them invisible.
        // This lets the remaining content be visually centered in the chain.
        textButton1.hide()
        textButton2.hide()

        button.setOnClickListener {
            analytics.log(AmplitudeEvents.CREATE_PLACEHOLDER_TAP_GET_STARTED)
            launchPhoneVerification()
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            analytics.log(AmplitudeEvents.CREATE_PLACEHOLDER_VIEW)
        }
    }
}

class ProfilePlaceholderFragment: PlaceholderFragment() {
    override val titleResId: Int = R.string.profile_tour_title
    override val subtitleResId: Int = R.string.profile_tour_subtitle
    override val screenshotResId: Int = R.drawable.ic_profileimage

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Actually hide these buttons instead of making them invisible.
        // This lets the remaining content be visually centered in the chain.
        textButton1.hide()
        textButton2.hide()

        button.setOnClickListener {
            analytics.log(AmplitudeEvents.PROFILE_PLACEHOLDER_TAP_GET_STARTED)
            launchPhoneVerification()
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            analytics.log(AmplitudeEvents.PROFILE_PLACEHOLDER_VIEW)
        }
    }
}

