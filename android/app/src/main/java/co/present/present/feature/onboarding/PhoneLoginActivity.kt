package co.present.present.feature.onboarding

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import co.present.present.BaseActivity
import co.present.present.R
import co.present.present.analytics.AmplitudeEvents
import co.present.present.extensions.*
import co.present.present.service.rpc.requestVerification
import co.present.present.view.DigitKeyboard
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.activity_enter_phone.*
import present.proto.UserService
import javax.inject.Inject



class PhoneLoginActivity: BaseActivity() {
    private val TAG = javaClass.simpleName

    lateinit var viewModel: PhoneViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter_phone)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = ""
        }
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(PhoneViewModel::class.java)

        viewModel.getPhoneNumber().subscribeBy(
                onError = { Log.e(TAG, "error", it)},
                onNext = {
                    phoneNumber.text = it
                }
        )
        phoneNumber.post { phoneNumber.showKeyboard() }

        phoneNumber.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE && nextButton.isEnabled) { verifyPhoneNumber() }
            false
        }

        keyboard.setListener(viewModel)

        viewModel.nextButtonEnabled.compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = {},
                        onNext = {
                            nextButton.isEnabled = it
                        }
                )

        nextButton.setOnClickListener { verifyPhoneNumber() }

        disclaimer.setAnalyticsEvents(
                tos = AmplitudeEvents.PHONE_CONNECT_ENTER_NUMBER_TAP_TOS,
                privacyPolicy = AmplitudeEvents.PHONE_CONNECT_ENTER_NUMBER_TAP_PRIVACY,
                analytics = analytics)

        analytics.log(AmplitudeEvents.PHONE_CONNECT_ENTER_NUMBER_VIEW)
    }

    private fun verifyPhoneNumber() {
        viewModel.verify().compose(applySingleSchedulers())
                .doOnSubscribe {
                    spinner.show()
                }
                .subscribeBy(
                        onError = {
                            spinner.hide()
                            snackbar(R.string.generic_error)
                            Log.e(TAG, "error verifying", it)
                        },
                        onSuccess = { codeLength ->
                            Log.d(TAG, "Verified ${viewModel.phoneNumber}")
                            spinner.hide()
                            analytics.log(AmplitudeEvents.PHONE_CONNECT_ENTER_NUMBER_SUBMIT)
                            startActivityForResult(PhoneVerificationActivity.newIntent(this,
                                    codeLength, viewModel.phoneNumber, phoneNumber.text.toString()),
                                    REQUEST_PHONE_VERIFICATION)
                            overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left)
                        }
                )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.close, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.close -> {
                analytics.log(AmplitudeEvents.PHONE_CONNECT_ENTER_NUMBER_CANCEL)
                setResult(Activity.RESULT_CANCELED)
                finishAndSlideBackOverToBottom()
            }
        }
        return true
    }

    override fun performInjection() {
        activityComponent.inject(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "Request code : $requestCode, Result code: $resultCode")
        if (requestCode == REQUEST_PHONE_VERIFICATION && resultCode == Activity.RESULT_OK) {
            finish()
        }
    }

    companion object {
        const val REQUEST_PHONE_VERIFICATION = 1234
    }
}

class PhoneViewModel @Inject constructor(val userService: UserService): ViewModel(), DigitKeyboard.KeyListener {
    override fun onDigit(digit: Int) {
        if (phoneNumber.length == maxPhoneLength(phoneNumber)) return
        phoneNumberSubject.onNext(phoneNumber + digit.toString())
    }

    override fun onDelete() {
        if (phoneNumber.isEmpty()) return
        phoneNumberSubject.onNext(phoneNumber.substring(0, phoneNumber.length - 1))
    }

    private val phoneNumberSubject = BehaviorSubject.createDefault<String>("")


    fun getPhoneNumber(): Flowable<String> {
        return phoneNumberSubject.toFlowable(BackpressureStrategy.LATEST).map {
            val has1 = (it.startsWith("1"))
             "${if (has1) "+1 " else ""}${formatUsPhone(it.getUsOnly())}"
        }
    }

    /**
     * String should be formatted like 10 or fewer consecutive digits, representing an AMerican
     * phone number without the +1.
     */
    private fun formatUsPhone(string: String): String {
        assert(string.length <= 10)
        return if (string.length < 4) {
            string
        } else if (string.length < 8) {
            "${string.substring(0, 3)}-${string.substring(3, string.length)}"
        } else {
            "(${string.substring(0, 3)}) ${string.substring(3, 6)}-${string.substring(6, string.length)}"
        }
    }

    private fun String.isInternational(): Boolean {
        return length == 11 && startsWith("1")
    }

    private fun String.getUsOnly(): String {
        return if (startsWith("1")) substring(1, length) else this
    }

    private fun normalize(string: String): String {
        return if (string.isInternational()) string else "1$string"
    }

    private fun maxPhoneLength(string: String): Int {
        return if (string.startsWith("1")) 11 else 10
    }

    val phoneNumber get() = phoneNumberSubject.value

    val nextButtonEnabled: Flowable<Boolean> by lazy {
        phoneNumberSubject.toFlowable(BackpressureStrategy.LATEST).map {
             it.length == maxPhoneLength(it)
        }
    }

    fun verify(): Single<Int> {
        return userService.requestVerification(normalize(phoneNumber))
    }

}