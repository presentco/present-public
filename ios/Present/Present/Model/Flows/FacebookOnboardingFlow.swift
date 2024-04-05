//
//  FacebookOnboardingFlow.swift
//  Present
//
//  Created by Dan Federman on 3/7/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import CoreLocation
import Foundation
import PresentProtos
import UserNotifications

// TODO: Rename this and convert it to a Workflow or something simpler.
/**
    See state var step and advanceToNextUnfulfilledStep() for the screen sequence logic.
*/
public final class FacebookOnboardingFlow: Flow, LoginWithFacebookDelegate, LegacyApplicationServices
{
    // MARK: Private Properties

    private let service: PresentService
    private let screenPresenter: RootViewController
    private let facebookLoginService: FacebookLoginService
    private let userManager: UserManager

    // MARK: Initialization

    public required init(service: PresentService, screenPresenter: RootViewController, facebookLoginService: FacebookLoginService, userManager: UserManager)
    {
        self.service = service
        self.screenPresenter = screenPresenter
        self.facebookLoginService = facebookLoginService
        self.userManager = userManager
    }

    // MARK: Step logic

    public func beginFlow() {
        advanceToNextUnfulfilledStep()
    }

    public func advanceToNextUnfulfilledStep()
    {
        let facebookLinked = userManager.facebookLinked

        //  Show terms and perform Facebook login (link faceboook on the server)
        if !facebookLinked {
            step = .loginWithFacebook
        } else {
            step = .complete
        }
    }
    
    private var step: Step = .none
    {
        didSet {
            logEvent(type: .action, "Step: \(String(describing: type(of: self))) \(oldValue) -> \(step)")
            
            switch step {
                case .none:
                    // Nothing to do here.
                    break

                    // Show the terms of service screen and perform the facebook login.
                case .loginWithFacebook:
                    screenPresenter.presentWaitingIndicator()
                    loginWithFacebook(from: applicationServices.rootViewController,
                        didBeginNetworkRequestHandler: { },
                        errorHandler: {error in
                            log("User cancelled or there was an error")
                            self.step = .complete })
                
                case .complete:
                    completionHandler?()
            }
        }
    }

    // Perform the SDK facebook login in response to the user selecting the continue button
    // Then call the user manager to link facebook with the server and update the authorization status here.
    public func loginWithFacebook(
        from viewController: UIViewController,
        didBeginNetworkRequestHandler: @escaping () -> Void,
        errorHandler: @escaping (UserDisplayableError) -> Void)
    {
        facebookLoginService.login(from: viewController) { [weak self] (facebookAccessToken) in
            guard let sself = self else { return }
            
            guard let facebookAccessToken = facebookAccessToken else {
                let couldNotGetFacebookDataErrorText = NSLocalizedString(
                    "LoginWithFacebookViewControllerHeaderLabelText",
                    tableName: nil,
                    bundle: .main,
                    value: "Present", // TODO:(dan) Actually communicate to user what went wrong.
                    comment: "Welcoming header label text on launch screen."
                    ).localizedCapitalized

                // TODO: Differentiate user cancelled here
                logEvent(.facebook_connect_error)

                errorHandler(couldNotGetFacebookDataErrorText)
                return
            }

            logEvent(.facebook_connect_success)

            // Perform the present server login with the facebook info
            didBeginNetworkRequestHandler()
            sself.userManager.linkFacebook(withFacebookAccessToken: facebookAccessToken.tokenString, completionHandler:
            { [weak sself] (response) in
                guard let sself = sself else { return }
                log("FacebookOnboardingFlow: linkFacebook response: \(response)")

                switch response
                {
                    //case let .success(loginResponse):
                    case .success:
                        // Anything to do here?
                        /*
                        if let userProfile = loginResponse.userProfile {
                            logDebug("Setting user profile from login response")
                            sself.userManager.storeUserProfile(userProfile: userProfile)
                        }*/
                        break
                    case .error:
                        let rootVC = sself.screenPresenter.currentViewController
                        UIAlertController.showAcknowledgeAlert(fromViewController: rootVC, title: "Error", message: "Error linking Facebook")
                }
                
                sself.advanceToNextUnfulfilledStep()
            })
        }
    }

    // MARK: CustomStringConvertible
    
    public var description: String {
        return "\(String(describing: type(of: self))): \(step)"
    }
    

    public var completionHandler: (() -> Void)?
    

    // MARK: Private Methods

    // MARK: Private Enums
    
    private enum Step: CustomStringConvertible {
        case none
        case loginWithFacebook
        case complete
        
        // MARK: CustomStringConvertible
        
        public var description: String {
            switch self {
            case .none:
                return "none"
            case .loginWithFacebook:
                return "loginWithFacebook"
            case .complete:
                return "complete"
            }
        }
    }
}

public protocol LoginWithFacebookDelegate: class {
    func loginWithFacebook(
        from viewController: UIViewController,
        didBeginNetworkRequestHandler: @escaping () -> Void,
        errorHandler: @escaping (UserDisplayableError) -> Void)
}


