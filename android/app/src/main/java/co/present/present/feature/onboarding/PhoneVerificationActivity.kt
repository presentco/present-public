package co.present.present.feature.onboarding

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import co.present.present.BaseActivity
import co.present.present.LaunchActivity
import co.present.present.R
import co.present.present.analytics.AmplitudeEvents
import co.present.present.extensions.*
import co.present.present.service.rpc.requestVerification
import co.present.present.service.rpc.verify
import co.present.present.user.UserDataProvider
import co.present.present.view.DigitKeyboard
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.activity_verify_phone.*
import present.proto.Authorization
import present.proto.AuthorizationResponse
import present.proto.UserService
import javax.inject.Inject


class PhoneVerificationActivity : BaseActivity() {
    private val TAG = javaClass.simpleName

    lateinit var viewModel: PhoneVerificationViewModel

    val codeLength by lazy { intent.getIntExtra(ARG_CODE_LENGTH, -1) }
    val phoneNumber by lazy { intent.getStringExtra(ARG_PHONE_NUMBER) }
    val formattedPhoneNumber by lazy { intent.getStringExtra(ARG_FORMATTED_PHONE_NUMBER) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_phone)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = ""
        }

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(PhoneVerificationViewModel::class.java)

        assert(codeLength > 0)
        viewModel.codeLength = codeLength

        inflateEditTextChain()
        subtitle.text = formattedPhoneNumber
        subtitle.setOnClickListener { finishAndSlideBackOverToRight() }

        resendCode.setOnClickListener {
            viewModel.resend(phoneNumber).compose(applySingleSchedulers()).subscribeBy(
                    onError = {},
                    onSuccess = {
                        analytics.log(AmplitudeEvents.PHONE_CONNECT_CODE_VERIFICATION_RESEND)
                        snackbar(R.string.code_resent)
                    }
            )
        }
        keyboard.setListener(viewModel)
        analytics.log(AmplitudeEvents.PHONE_CONNECT_CODE_VERIFICATION_VIEW)
    }

    private fun inflateEditTextChain() {
        val inputs = mutableListOf<TextView>()
        for (i in 0 until codeLength) {

            val numberInput = layoutInflater.inflate(R.layout.number_edit_text, root, false) as TextView
            numberInput.id = View.generateViewId()
            root.addView(numberInput)
            inputs.add(numberInput)
        }

        val set = ConstraintSet()
        set.clone(root)

        for (i in 0 until codeLength) {
            val input = inputs[i]

            if (i > 0) {
                // Connect to the right side of the EditText to the left
                set.connect(input.id, ConstraintSet.LEFT, inputs[i - 1].id, ConstraintSet.RIGHT, 0)
            } else {
                // Connect to the parent's left
                set.connect(input.id, ConstraintSet.LEFT, root.id, ConstraintSet.LEFT, 0)
            }

            if (i < codeLength - 1) {
                // Connect to the left side of the EditText to the right
                set.connect(input.id, ConstraintSet.RIGHT, inputs[i + 1].id, ConstraintSet.LEFT, 0)
            } else {
                // Connect to the parent's right
                set.connect(input.id, ConstraintSet.RIGHT, root.id, ConstraintSet.RIGHT, 0)
            }

            set.connect(input.id, ConstraintSet.TOP, subtitle.id, ConstraintSet.BOTTOM, 0)
        }

        // Place error view below textviews
        set.connect(error.id, ConstraintSet.TOP, inputs[0].id, ConstraintSet.BOTTOM, 0)

        set.applyTo(root)

        listenForCompletion(inputs)
        listenForCodeUpdates(inputs)
    }

    private fun listenForCodeUpdates(inputs: MutableList<TextView>) {
        viewModel.getCode().subscribeBy(
                onError = {},
                onNext = { code ->
                    val chars = code.toCharArray()
                    inputs.forEachIndexed { index, textView ->
                        textView.text = if (chars.size > index) { chars[index].toString() } else ""
                    }
                }
        )
    }

    private fun listenForCompletion(inputs: MutableList<TextView>) {
        viewModel.complete(codeLength).subscribeBy(
                onError = {},
                onNext = { complete ->
                    if (complete) {
                        spinner.show()
                        verifyCode(inputs)
                    }
                }
        ).addTo(disposable)
    }


    private fun verifyCode(inputs: MutableList<TextView>) {
        disposable += viewModel.verify()
                .compose(applySingleSchedulers())
                .subscribeBy(
                        onError = {
                            spinner.hide()
                            error.show()
                            inputs.forEach { it.text = "" }
                            viewModel.clearDigits()
                        },
                        onSuccess = {
                            analytics.log(AmplitudeEvents.PHONE_CONNECT_CODE_VERIFICATION_SUBMIT)
                            handleAuthorizationResponse(it)
                            setResult(Activity.RESULT_OK)
                            supportFinishAfterTransition()
                        }
                )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.close, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                setResult(Activity.RESULT_CANCELED)
                finishAndSlideBackOverToRight()
            }
            R.id.close -> {
                analytics.log(AmplitudeEvents.PHONE_CONNECT_CODE_VERIFICATION_CANCEL)
                setResult(Activity.RESULT_OK)
                finishAndSlideBackOverToBottom()
            }
        }
        return true
    }

    override fun onBackPressed() {
        finishAndSlideBackOverToRight()
    }

    override fun performInjection() {
        activityComponent.inject(this)
    }

    private fun handleAuthorizationResponse(authorizationResponse: AuthorizationResponse) {
        when (authorizationResponse.authorization.nextStep) {
            Authorization.NextStep.PROCEED -> {
                Log.d(TAG, "Verification code linking succeeded and user already exists; proceed to app")
                onUserLoggedIn()
            }
            Authorization.NextStep.SIGN_UP -> {
                Log.d(TAG, "Verification code linking succeeded; please complete sign up")
                onUserCreated()
            }
            else -> {
                Log.e(TAG, "Verification code linking failed, next step: " + authorizationResponse.authorization.nextStep)
                onUserWaitlisted()
            }
        }
    }

    private fun onUserLoggedIn() {
        start<LaunchActivity>()
    }

    private fun onUserWaitlisted() {
        start<WaitlistActivity>()
    }

    private fun onUserCreated() {
        slideOverFromRight<OnboardingActivity>()
    }

    companion object {
        const val ARG_CODE_LENGTH = "codeLength"
        const val ARG_PHONE_NUMBER = "phoneNumber"
        const val ARG_FORMATTED_PHONE_NUMBER = "formattedPhoneNumber"


        fun newIntent(context: Context, codeLength: Int, phoneNumber: String, formattedPhoneNumber: String): Intent {
            val intent = Intent(context, PhoneVerificationActivity::class.java)
            intent.putExtra(ARG_CODE_LENGTH, codeLength)
            intent.putExtra(ARG_PHONE_NUMBER, phoneNumber)
            intent.putExtra(ARG_FORMATTED_PHONE_NUMBER, formattedPhoneNumber)
            return intent
        }
    }
}

class PhoneVerificationViewModel @Inject constructor(val userService: UserService, val userDataProvider: UserDataProvider) : ViewModel(), DigitKeyboard.KeyListener {

    var codeLength = 0
    override fun onDigit(digit: Int) {
        if (codeSubject.value.length == codeLength) return
        codeSubject.onNext(codeSubject.value + digit.toString())
    }

    override fun onDelete() {
        if (codeSubject.value.isEmpty()) return
        codeSubject.onNext(codeSubject.value.substring(0, codeSubject.value.length - 1))
    }

    fun getCode(): Flowable<String> {
        return codeSubject.toFlowable(BackpressureStrategy.LATEST)
    }

    private val codeSubject = BehaviorSubject.createDefault("")

    fun clearDigits() {
        codeSubject.onNext("")
    }

    fun verify(): Single<AuthorizationResponse> {
        return userService.verify(codeSubject.value).flatMap { userDataProvider.persistAuthResponseRx(it) }
    }

    fun resend(phoneNumber: String): Single<Int> {
        return userService.requestVerification(phoneNumber)
    }

    fun focus(codeLength: Int): Flowable<Int> {
        return codeSubject.toFlowable(BackpressureStrategy.LATEST).map { it.length }.filter { it < codeLength }
    }

    fun complete(codeLength: Int): Flowable<Boolean> {
        return codeSubject.toFlowable(BackpressureStrategy.LATEST).map { it.length == codeLength }
    }


}