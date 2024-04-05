//
//  SignupEnterPhoneViewController.swift
//  Present
//
//  Created by Kaming Li on 4/25/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import UIKit
import RxSwift

protocol SignupEnterPhoneDelegate: class {
    func sendVerificationCode(phoneNumber: PhoneNumber)->Completable
    func verifyCode(code: String)->Single<Bool>
    func connectWithFacebook()
}

class SignupEnterPhoneViewController : PresentViewController, WorkflowStep
{
    public weak var workflowDelegate: WorkflowStepDelegate?
    
    deinit {
        log("deinit")
    }

    enum ScreenState {
        case EnterPhoneNumber
        case EnterVerificationCode
    }
    
    private var currentScreenState : ScreenState?
    
    public weak var delegate: SignupEnterPhoneDelegate?
    
    let EMPTY_PHONE_NUMBER_DISPLAY_TEXT = " "
    let MAX_DIGIT_COUNT = 10
    let MAX_CODE_COUNT = 6
    
    private var phoneNumber = ""
    private var verificationCode = ""
    
    @IBOutlet weak var phoneView : UIView!
    @IBOutlet weak var verificationView : UIView!
    
    @IBOutlet weak var termsPrivacyTextView: UITextView! {
        didSet {
            let attributedString = NSMutableAttributedString(attributedString: termsPrivacyTextView.attributedText)
            let linkRangeTerms = (attributedString.string as NSString).range(of: "Terms")
            attributedString.addAttribute(NSAttributedStringKey.link, value: PresentLinks.terms, range: linkRangeTerms)
            let linkRangePrivacy = (attributedString.string as NSString).range(of: "Privacy Policy")
            attributedString.addAttribute(NSAttributedStringKey.link, value: PresentLinks.privacyPolicy, range: linkRangePrivacy)
            let linkAttributes: [String : Any] = [
                NSAttributedStringKey.foregroundColor.rawValue: UIColor.Present.Text.LightGray,
                NSAttributedStringKey.underlineColor.rawValue: UIColor.Present.Text.LightGray,
                NSAttributedStringKey.underlineStyle.rawValue: NSUnderlineStyle.styleSingle.rawValue]
            termsPrivacyTextView.linkTextAttributes = linkAttributes
            termsPrivacyTextView.attributedText = attributedString
            termsPrivacyTextView.delegate = self
        }
    }
    
    
    @IBOutlet weak var phoneLabel: UILabel! {
        didSet {
            phoneLabel.text = EMPTY_PHONE_NUMBER_DISPLAY_TEXT
        }
    }

    // The display of the previously entered phone number on the code entry screen
    @IBOutlet weak var sentToPhoneNumberButton: ThemeableButton! {
        didSet {
            self.sentToPhoneNumberButton.setTitleColor(UIColor.Present.Purple.Main, for: .normal)
            self.sentToPhoneNumberButton.addTarget { [weak self] button in
                // They want to enter a new number, consider this code cancelled
                logEvent(.phone_connect_code_verification_cancel)
                self?.makeVisibleScreen(state: .EnterPhoneNumber)
                self?.phoneLabel.text = self?.phoneNumber
                self?.updatePhoneNumberDisplay()
                self?.nextButton.isEnabled = true
                self?.animateMovingToPhoneEntryScreen()
            }
        }
    }
    
    @IBOutlet weak var connectWithFacebookButton: ThemeableButton! {
        didSet {
            self.connectWithFacebookButton.setTitleColor(UIColor.Present.Purple.Main, for: .normal)
            self.connectWithFacebookButton.addTarget { [weak self] button in
                self?.delegate?.connectWithFacebook()
            }
        }
    }
    
    @IBOutlet weak var nextButton: ThemeableButton! {
        didSet {
            self.nextButton.backgroundColor = UIColor.Present.Purple.Main
            self.nextButton.setTitleColor(.white, for: .normal)
            self.nextButton.setTitleColor(UIColor(white: 1.0, alpha: Palette.defaultDisabledButtonTitleAlpha), for: UIControlState.disabled)
            self.nextButton.roundCornersToHeight()
        }
    }
    
    @IBAction func nextButtonTapped() {
        self.errorLabel.text = ""
        self.errorLabel.isHidden = true
        if currentScreenState == .EnterPhoneNumber {
            self.requestVerificationCode()
            self.makeVisibleScreen(state: .EnterVerificationCode)
            if isFakePhone(phoneNumber) {
                self.verificationCode = "111111"
                self.updateVerificationCodeUI()
                self.nextButton.isEnabled = true
            } else {
                self.verificationCode = ""
            }
            animateMovingToVerificationScreen()
        } else {
            self.sendVerificationCode()
            self.nextButton.isEnabled = false
            self.view.isUserInteractionEnabled = false
        }
    }
    
    @IBOutlet weak var errorLabel: UILabel! {
        didSet {
            errorLabel.isHidden = true
            errorLabel.textColor = UIColor.Present.Text.Error
        }
    }
    
    @IBOutlet weak var resendCodeButton: ThemeableButton! {
        didSet {
            resendCodeButton.isHidden = true
            resendCodeButton.addTarget { [weak self] button in
                logEvent(.phone_connect_code_verification_resend)
                self?.requestVerificationCode()
                self?.makeVisibleScreen(state: .EnterVerificationCode)
                self?.changeVerificationCodeTextColor(color: .black)
                self?.verificationCode = ""
                self?.errorLabel.text = ""
            }
        }
    }
    
    @IBOutlet weak var code1: UILabel!
    @IBOutlet weak var code2: UILabel!
    @IBOutlet weak var code3: UILabel!
    @IBOutlet weak var code4: UILabel!
    @IBOutlet weak var code5: UILabel!
    @IBOutlet weak var code6: UILabel!
    
    @IBOutlet weak var key1: ThemeableButton! {
        didSet {
            key1.addTarget { [weak self] button in
                self?.handleKeyPressed(digit: "1")
            }
        }
    }
    
    @IBOutlet weak var key2: ThemeableButton! {
        didSet {
            key2.addTarget { [weak self] button in
                self?.handleKeyPressed(digit: "2")
            }
        }
    }
    
    @IBOutlet weak var key3: ThemeableButton! {
        didSet {
            key3.addTarget { [weak self] button in
                self?.handleKeyPressed(digit: "3")
            }
        }
    }
    
    @IBOutlet weak var key4: ThemeableButton! {
        didSet {
            key4.addTarget { [weak self] button in
                self?.handleKeyPressed(digit: "4")
                
            }
        }
    }
    
    @IBOutlet weak var key5: ThemeableButton! {
        didSet {
            key5.addTarget { [weak self] button in
                self?.handleKeyPressed(digit: "5")
            }
        }
    }
    
    @IBOutlet weak var key6: ThemeableButton! {
        didSet {
            key6.addTarget { [weak self] button in
                self?.handleKeyPressed(digit: "6")
            }
        }
    }
    
    @IBOutlet weak var key7: ThemeableButton! {
        didSet {
            key7.addTarget { [weak self] button in
                self?.handleKeyPressed(digit: "7")
            }
        }
    }
    
    @IBOutlet weak var key8: ThemeableButton! {
        didSet {
            key8.addTarget { [weak self] button in
                self?.handleKeyPressed(digit: "8")
            }
        }
    }
    
    @IBOutlet weak var key9: ThemeableButton! {
        didSet {
            key9.addTarget { [weak self] button in
                self?.handleKeyPressed(digit: "9")
            }
        }
    }
    
    @IBOutlet weak var key0: ThemeableButton! {
        didSet {
            key0.addTarget { [weak self] button in
                self?.handleKeyPressed(digit: "0")
            }
        }
    }
    
    @IBOutlet weak var keyBackspace: ThemeableButton! {
        didSet {
            keyBackspace.addTarget { [weak self] button in
                self?.handleKeyPressed(digit: "")
            }
        }
    }

    @IBOutlet weak var closeButton: ThemeableButton! {
        didSet {
            closeButton.addTarget { [weak self] _ in self?.close() }
        }
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        makeVisibleScreen(state: .EnterPhoneNumber)
        self.delegate = self
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        logEvent(.signup_view)
    }

    private func makeVisibleScreen(state : ScreenState) {
        if state == .EnterPhoneNumber {
            logEvent(.phone_connect_enter_number_view)
            currentScreenState = .EnterPhoneNumber
            phoneView.isHidden = false
            verificationView.isHidden = true
            phoneLabel.text = EMPTY_PHONE_NUMBER_DISPLAY_TEXT
            nextButton.isEnabled = false
            errorLabel.isHidden = true
            resendCodeButton.isHidden = true
            termsPrivacyTextView.isHidden = false
        } else {
            logEvent(.phone_connect_code_verification_view)
            currentScreenState = .EnterVerificationCode
            phoneView.isHidden = true
            verificationView.isHidden = false
            resetVerificationCodeUI()
            nextButton.isEnabled = false
            sentToPhoneNumberButton.setTitle(phoneNumberFormattedText(phoneNumber: self.phoneNumber), for: .normal)
            resendCodeButton.isEnabled = true
            resendCodeButton.isHidden = false
            errorLabel.isHidden = false
            termsPrivacyTextView.isHidden = true
        }
    }
    
    func phoneNumberFormattedText(phoneNumber:String) -> String {
        // format: (xxx) xxx-xxxx
        let characters = Array(phoneNumber)
        if characters.count <= 3 {
            return String(format: "%@", String(characters))
        } else if characters.count <= 7 {
            return String(format: "%@-%@", String(characters[0...2]), String(characters[3...characters.endIndex-1]))
        } else if characters.count <= 10 {
            return String(format: "(%@) %@-%@", String(characters[0...2]), String(characters[3...5]), String(characters[6...characters.endIndex-1]))
        }
        return ""
    }
    
    func handleKeyPressed(digit: String) {
        if currentScreenState == .EnterPhoneNumber {
            updatePhoneNumber(digit: digit)
            updatePhoneNumberDisplay()
            let isValid = isValidPhone(number: phoneNumber)
            if isValid {
                phoneLabel.textColor = .blue
                nextButton.pulse()
            } else {
                phoneLabel.textColor = .black
            }
            nextButton.isEnabled = isValid
        } else {
            updateVerificationCode(digit: digit)
            updateVerificationCodeUI()
            let canSendVerificationCode = (verificationCode.count == MAX_CODE_COUNT ? true : false)
            if canSendVerificationCode {
                nextButton.pulse()
                changeVerificationCodeTextColor(color: .blue)
            } else {
                changeVerificationCodeTextColor(color: .black)
            }
            nextButton.isEnabled = canSendVerificationCode
        }
        errorLabel.text = ""
    }
    
    func updatePhoneNumber(digit: String) {
        if digit == "" {
            phoneNumber = String(phoneNumber.dropLast())
        } else {
            if phoneNumber.count < MAX_DIGIT_COUNT {
                phoneNumber += digit
            }
        }
    }
    
    func updateVerificationCode(digit: String) {
        if digit == "" {
            verificationCode = String(verificationCode.dropLast())
        } else {
            if verificationCode.count < MAX_CODE_COUNT {
                verificationCode += digit
            }
        }
    }
    
    func resetVerificationCodeUI() {
        code1.text = ""
        code2.text = ""
        code3.text = ""
        code4.text = ""
        code5.text = ""
        code6.text = ""
    }
    
    func changeVerificationCodeTextColor(color : UIColor) {
        code1.textColor = color
        code2.textColor = color
        code3.textColor = color
        code4.textColor = color
        code5.textColor = color
        code6.textColor = color
    }
    
    func updatePhoneNumberDisplay() {
        if phoneNumber.count == 0 {
            phoneLabel.text = EMPTY_PHONE_NUMBER_DISPLAY_TEXT
        } else {
            phoneLabel.text = phoneNumberFormattedText(phoneNumber: phoneNumber)
        }
    }
    
    func updateVerificationCodeUI() {
        resetVerificationCodeUI()
        let characters = Array(verificationCode)
        for (index, element) in characters.enumerated() {
            switch index {
            case 0:
                code1.text = String(element)
            case 1:
                code2.text = String(element)
            case 2:
                code3.text = String(element)
            case 3:
                code4.text = String(element)
            case 4:
                code5.text = String(element)
            case 5:
                code6.text = String(element)
            default:
                break;
            }
        }
    }
    
    func isValidPhone(number : String) -> Bool {
        return PhoneNumber(string: number) != nil
    }
    func isFakePhone(_ number : String) -> Bool {
        return PhoneNumber(string: number)?.isFake() ?? false
    }

    func requestVerificationCode()
    {
        guard let phoneNumber = PhoneNumber(string: phoneNumber) else {
            logError("invalid phone for request")
            return
        }
        startActivity()
        delegate?.sendVerificationCode(phoneNumber: phoneNumber)
            .do {
               self.endActivity()
            }.disposed(by: disposal)
    }

    func sendVerificationCode() {
        startActivity()
        delegate?.verifyCode(code: self.verificationCode)
            .do {
                self.endActivity()
            }.onSuccess { codeValid in
                // Note: invalid code is handled by the error case
                self.userManager.userSession.phoneNumber = self.phoneNumber
                self.workflowDelegate?.workflowStepComplete()
            }
            .onError { error in print("Could not send code")
                self.errorLabel.text = "Invalid code"
                self.errorLabel.isHidden = false
                self.view.isUserInteractionEnabled = true
            }
            .subscribe()
            .disposed(by: disposal)
    }
    
    func animateMovingToVerificationScreen() {
        phoneView.isHidden = false
        verificationView.isHidden = false
        verificationView.frame = CGRect(x: view.frame.width,
                                        y: verificationView.frame.origin.y,
                                        width: verificationView.frame.width,
                                        height: verificationView.frame.height)
        UIView.animate(withDuration: 0.18, delay: 0, options: [.curveEaseInOut],
                       animations: {
                        self.phoneView.frame = CGRect(x: -self.view.frame.width,
                                                      y: self.phoneView.frame.origin.y,
                                                      width: self.phoneView.frame.width,
                                                      height: self.phoneView.frame.height)
                        self.verificationView.frame = CGRect(x: 0,
                                                             y: self.verificationView.frame.origin.y,
                                                             width: self.verificationView.frame.width,
                                                             height: self.verificationView.frame.height)
        },
                       completion: nil
        )
    }
    
    func animateMovingToPhoneEntryScreen() {
        phoneView.isHidden = false
        verificationView.isHidden = false
        phoneView.frame = CGRect(x: -view.frame.width,
                                 y: phoneView.frame.origin.y,
                                 width: phoneView.frame.width,
                                 height: phoneView.frame.height)
        UIView.animate(withDuration: 0.18, delay: 0, options: [.curveEaseInOut],
                       animations: {
                        self.phoneView.frame = CGRect(x: 0,
                                                      y: self.view.frame.origin.y,
                                                      width: self.phoneView.frame.width,
                                                      height: self.phoneView.frame.height)
                        self.verificationView.frame = CGRect(x: self.view.frame.width,
                                                             y: self.verificationView.frame.origin.y,
                                                             width: self.verificationView.frame.width,
                                                             height: self.verificationView.frame.height)
                        
        },
                       completion: nil
        )
    }
    
    private func close() {
        self.workflowDelegate?.workflowStepCancelWorkflow()
    }
}

extension SignupEnterPhoneViewController: UITextViewDelegate
{
    public func textView(_ textView: UITextView, shouldInteractWith url: URL, in characterRange: NSRange, interaction: UITextItemInteraction) -> Bool {

        // Log taps on terms and privacy links
        if url.absoluteString == PresentLinks.terms {
            logEvent(.phone_connect_enter_number_tap_tos)
        }
        if url.absoluteString == PresentLinks.privacyPolicy {
            logEvent(.phone_connect_enter_number_tap_privacy)
        }
        
        return true
    }
}

extension SignupEnterPhoneViewController: SignupEnterPhoneDelegate
{
    //  Submit the phone number and request a code
    func sendVerificationCode(phoneNumber: PhoneNumber)->Completable {
        logEvent(.phone_connect_enter_number_submit)
        return service.requestPhoneVerification(number: phoneNumber)
            .asObservable().ignoreElements()
    }

    //  Submit the verification code and get a response
    func verifyCode(code: String)->Single<Bool> {
        logEvent(.phone_connect_code_verification_submit)
        return service.verify(code: code).map { [weak self] authResponse in
            self?.userManager.setAuthorization(authResponse: authResponse)
            // Note: Our current API returns a 400 ClientException in the case
            // Note: where the user entered the wrong code.  So this will be handled
            // Note: by the erorr path.
            return true
        }
    }
    
    func connectWithFacebook() {
        logEvent(.phone_connect_enter_number_tap_connect_with_facebook)
        workflowDelegate?.workflowStepCancelWorkflow()
        main {
            self.screenPresenter.pushFacebookOnboardingFlow()
        }
    }
}

