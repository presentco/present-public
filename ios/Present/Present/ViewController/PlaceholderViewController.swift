//
//  Created by Patrick Niemeyer on 10/10/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import TTTAttributedLabel

public class PlaceholderViewController: PresentViewController
{
    // Context for logging events
    var space: PresentSpace?

    @IBOutlet weak var titleImage: UIImageView!
    @IBOutlet weak var titleText: UILabel!
    @IBOutlet weak var subtitleText: UILabel!

    @IBOutlet weak var closeButton: ThemeableButton! {
        didSet {
            closeButton.isHidden = true
            closeButton.addTarget { button in self.close() }
        }
    }
    
    public var isModal: Bool = false {
        didSet {
            closeButton.isShown = isModal
        }
    }
    
    // The permission request buttons
    // Note: There are not an unlimited number of these so we'll just include them
    // Note: all here for now.
    
    @IBOutlet weak var buttonStack: UIStackView!

    @IBOutlet weak var connectWithFacebookButton: ThemeableButton! {
        didSet {
            connectWithFacebookButton.addTarget { [weak self] _ in self?.connectWithFacebook() }
        }
    }
    
    @IBOutlet weak var allowLocationAccessButton: ThemeableButton! {
        didSet {
            allowLocationAccessButton.addTarget { [weak self] _ in self?.allowLocationAccess() }
        }
    }
    
    @IBOutlet weak var allowContactsAccessButton: ThemeableButton! {
        didSet {
            allowContactsAccessButton.addTarget { [weak self] _ in self?.allowContactsAccess() }
        }
    }
    

    /// terms will be shown automatically when allow location button is shown
    @IBOutlet weak var termsView: UIView!
    @IBOutlet weak var termsText: TTTAttributedLabel! {
        didSet {
            termsText.delegate = self
            let text = termsText.attributedText ?? NSAttributedString()
            termsText.linkAttributes = [
                NSAttributedStringKey.foregroundColor.rawValue: UIColor.Present.Text.LightGray,
                NSAttributedStringKey.underlineColor.rawValue: UIColor.Present.Text.LightGray,
                NSAttributedStringKey.underlineStyle.rawValue: NSUnderlineStyle.styleSingle.rawValue]
            let linkRangeTerms = (text.string as NSString).range(of: "Terms")
            termsText.addLink(to: URL(string: PresentLinks.terms), with: linkRangeTerms)
            let linkRangePrivacy = (text.string as NSString).range(of: "Privacy Policy")
            termsText.addLink(to: URL(string: PresentLinks.privacyPolicy), with: linkRangePrivacy)
        }
    }

    // "Get Started" button
    @IBOutlet weak var verifyPhoneNumberButton: ThemeableButton! {
        didSet {
            verifyPhoneNumberButton.addTarget { _ in self.verifyPhoneNumber() }
        }
    }

    @IBOutlet weak var whyFacebookButton: ThemeableButton! {
        didSet {
            whyFacebookButton.addTarget { [weak self] button in
                self?.showWhyFacebook()
            }
        }
    }
    
    // TODO: Remove
    @IBOutlet weak var notInterestedButton: ThemeableButton! {
        didSet {
            notInterestedButton.addTarget { [weak self] button in
                self?.notInterested()
            }
        }
    }

    /// The event to send when the user taps the corresponding button.
    /// (Configure based on context if needed)
    public lazy var buttonEventMapping: [ThemeableButton:LoggingEvent] = [
        verifyPhoneNumberButton : .home_placeholder_tap_get_started,
        allowLocationAccessButton : .home_placeholder_tap_allow_location,
        //allowContactsAccessButton : .home_placeholder_tap_allow_contacts,
        connectWithFacebookButton : .home_placeholder_tap_connect_with_facebook,
        notInterestedButton : .home_placeholder_tap_hide_this_tab,
        whyFacebookButton : LoggingEvent.home_placeholder_tap_why_facebook,
    ]

    override public func viewDidLoad()
    {
        // Hide all permission buttons by default
        buttonStack.arrangedSubviews.forEach {
            $0.isHidden = true
            $0.roundCornersToHeight()
        }
        
        // Run any configuration
        super.viewDidLoad()
    }
    
    override public func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
    }
    
    override public func viewDidLayoutSubviews() {
        termsView.isShown = allowLocationAccessButton.isShown
        whyFacebookButton.isShown = connectWithFacebookButton.isShown
    }

    public func close() {
        screenPresenter.goBack()
    }
}

// MARK: Logging

extension PlaceholderViewController
{
    func logEventWithSpace(_ constant: LoggingEvent) {
        logEvent(constant, space: space)
    }
}


// MARK: Verify Phone Number

extension PlaceholderViewController
{
    private func verifyPhoneNumber() {
        if let event = buttonEventMapping[verifyPhoneNumberButton] { logEventWithSpace(event) }
        //verifyPhoneNumberButton.isEnabled = false
        screenPresenter.presentPhoneSignupFlow()
    }
}

// MARK: Facebook Connect

extension PlaceholderViewController
{
    private func connectWithFacebook() {
        if let event = buttonEventMapping[connectWithFacebookButton] { logEventWithSpace(event) }
        //connectWithFacebookButton.isEnabled = false
        screenPresenter.pushFacebookOnboardingFlow()
    }
    
    private func showWhyFacebook() {
        if let event = buttonEventMapping[whyFacebookButton] { logEventWithSpace(event) }
        urlManager.openExternal(url: PresentLinks.presentWhyFacebookUrl)
    }
    
    /// aka "Hide this tab"
    private func notInterested() {
        if let event = buttonEventMapping[notInterestedButton] { logEventWithSpace(event) }
        userManager.hideWomenOnly.value = Date()
    }
}

// MARK: Contacts Permissions

extension PlaceholderViewController
{
    private func allowContactsAccess() {
        if let event = buttonEventMapping[allowContactsAccessButton] { logEventWithSpace(event) }
        self.contactsManager.promptForContactPermissions(from: self)
    }
}

// MARK: Location Permissions

extension PlaceholderViewController
{
    private func allowLocationAccess() {
        if let event = buttonEventMapping[allowLocationAccessButton] { logEventWithSpace(event) }
        acceptLocationPermissionPrompt()
    }

    // TODO: Move this into LocationManager or a LocationAuthorizationManager
    private func acceptLocationPermissionPrompt()
    {
        logEvent(type: .tap, "App Accept Location Permission")
        didAcceptLocationPermissionPrompt(permissionRequiresOpeningURLHandler: { (urlToOpen) in
            // If the user has previously denied the iOS level request we offer to take them to settings.
            let alertTitle = NSLocalizedString(
                "ApproveLocationViewControllerPreviouslyDeniedLocationPermissionPromptTitleText",
                tableName: nil,
                bundle: .main,
                value: "Allow Present to access your location while using the app.",
                comment: "Title of prompt asking you to give Present permission to view your location, when that permission had been previously denied."
            )
            // Note: A similar string appears in Info.plist
            let alertMessage = NSLocalizedString(
                "ApproveLocationViewControllerPreviouslyDeniedLocationPermissionPromptSubtitleText",
                tableName: nil,
                bundle: .main,
                value: "To discover nearby chats, please allow location access in Settings.  We never share your location.",
                comment: "Subtitle of prompt asking you to give Present permission to view your location, when that permission had been previously denied."
            )
            
            let alertController = UIAlertController(title: alertTitle, message: alertMessage, preferredStyle: .alert)
            alertController.addAction(withTitle: NSLocalizedString(
                "ApproveLocationViewControllerPreviouslyDeniedLocationPermissionPromptGoToSettingsButtonTitle",
                tableName: nil,
                bundle: .main,
                value: "Settings",
                comment: "Title of button to take the customer to settings so they can enable location permissions."
            )) { (_) in
                logEvent(type: .tap, "iOS Accept Location Permission")
                
                // Send the user to Settings.
                self.urlManager.openExternal(url: urlToOpen)
            }
            
            alertController.addAction(withTitle: NSLocalizedString(
                "ApproveLocationViewControllerPreviouslyDeniedLocationPermissionPromptGoToSettingsButtonTitle",
                tableName: nil,
                bundle: .main,
                value: "Cancel",
                comment: "Title of button to not go to settings to enable previously denied location permissions."
            )) { (_) in
                logEvent(type: .tap, "iOS Deny Location Permission")
            }
            
            present(alertController, animated: true)
        })
    }
    
    // TODO: Move this into LocationManager or a LocationAuthorizationManager
    public func didAcceptLocationPermissionPrompt(permissionRequiresOpeningURLHandler: (URL) -> Void)
    {
        // Location was previously denied so we prompt for settings
        if locationManager.locationPermissionDeniedOrRestricted {
            logEventWithSpace(.signup_user_prompted_to_enable_location_in_settings)
            permissionRequiresOpeningURLHandler(URL(string: UIApplicationOpenSettingsURLString)!)
            return
        }
        
        // We are going to ask for permissions for the first time, which should prompt
        logEventWithSpace(.signup_user_prompted_for_location_permission)
        locationManager.requestLocationPermission() { didGrantPermission in
            if didGrantPermission {
                self.logEventWithSpace(.signup_location_allowed)
            } else {
                self.logEventWithSpace(.signup_location_denied)
            }
        }
    }

}

// MARK: TTTAttributedLabelDelegate

extension PlaceholderViewController: TTTAttributedLabelDelegate
{
    public func attributedLabel(_ label: TTTAttributedLabel!, didSelectLinkWith url: URL!) {
        if label == termsText, url.isEquivalent(toUrl: PresentLinks.termsUrl) {
            logEventWithSpace(.home_placeholder_tap_tos)
        }
        if label == termsText, url.isEquivalent(toUrl: PresentLinks.privacyPolicyUrl) {
            logEventWithSpace(.home_placeholder_tap_privacy)
        }
        URLManager.shared.openExternal(url: url)
    }
}
