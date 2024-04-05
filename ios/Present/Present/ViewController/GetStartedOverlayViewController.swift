//
//  Created by Patrick Niemeyer on 10/10/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import TTTAttributedLabel

public class GetStartedOverlayViewController: PresentOverlayViewController
{
    @IBOutlet weak var contentView: UIView! {
        didSet {
            contentView.roundedCorners = 8.0
        }
    }
    
    @IBOutlet weak var titleImage: UIImageView!
    @IBOutlet weak var titleText: UILabel!
    @IBOutlet weak var subtitleText: UILabel!

    @IBOutlet weak var closeButton: ThemeableButton! {
        didSet {
            closeButton.addTarget { [weak self] button in self?.close() }
        }
    }
    
    // "Get Started" button
    @IBOutlet weak var getStartedButton: ThemeableButton! {
        didSet {
            getStartedButton.addTarget { [weak self] _ in self?.getStarted() }
        }
    }
    
    override public func viewDidLoad() {
        self.backgroundType = .color(UIColor.init(white: 0.7, alpha: 0.7))
        self.dismissableContentView = contentView
        super.viewDidLoad()
    }
    
    override public func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        logEvent(.join_present_view)
    }
    
    func getStarted() {
        logEvent(.join_present_tap_get_started)
        self.dismiss(animated: true) {
           self.screenPresenter.presentPhoneSignupFlow()
        }
    }
    
    /// User hit the close button
    func close() {
        logEvent(.join_present_cancel)
        self.dismissOverlay()
    }
    
    /// user dismisssed overlay by tapping outside content
    /// Note: override and send to explicit close just for logging.
    override func tapOutsideContent() {
        close()
    }
}

// MARK: Verify Phone Number

extension GetStartedOverlayViewController
{
    private func verifyPhoneNumber() {
        screenPresenter.presentPhoneSignupFlow()
    }
}

// MARK: Facebook Connect

/*
extension GetStartedOverlayViewController
{
    private func connectWithFacebook() {
        screenPresenter.pushFacebookOnboardingFlow()
    }
    
    private func showWhyFacebook() {
        urlManager.openExternal(url: PresentLinks.presentWhyFacebookUrl)
    }
}*/

// MARK: Location Permissions

extension GetStartedOverlayViewController
{
    private func allowLocationAccess() {
        acceptLocationPermissionPrompt()
    }

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
    
    public func didAcceptLocationPermissionPrompt(permissionRequiresOpeningURLHandler: (URL) -> Void)
    {
        // Location was previously denied so we prompt for settings
        if locationManager.locationPermissionDeniedOrRestricted {
            logEvent(.signup_user_prompted_to_enable_location_in_settings)
            permissionRequiresOpeningURLHandler(URL(string: UIApplicationOpenSettingsURLString)!)
            return
        }
        
        // We are going to ask for permissions for the first time, which should prompt
        logEvent(.signup_user_prompted_for_location_permission)
        locationManager.requestLocationPermission() { didGrantPermission in
            if didGrantPermission {
                logEvent(.signup_location_allowed)
            } else {
                logEvent(.signup_location_denied)
            }
        }
    }

}

// MARK: TTTAttributedLabelDelegate

extension GetStartedOverlayViewController: TTTAttributedLabelDelegate
{
    public func attributedLabel(_ label: TTTAttributedLabel!, didSelectLinkWith url: URL!) {
        URLManager.shared.openExternal(url: url)
    }
}
