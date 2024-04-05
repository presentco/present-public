//
//  PhoneSignupWorkflow.swift
//  Present
//
//  Created by Patrick Niemeyer on 5/6/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import RxSwift

public final class PhoneSignupFlow: Flow, WorkflowStepDelegate, ApplicationServices
{
    public var description = "PhoneSignupFlow"
    
    private let disposal = DisposeBag()
    
    // Local flag indicating whether we have the notification auth status during
    // this signup session
    private var checkedNotificationAuthorizationStatus = false
    
    // Local flag indicating whether the user created a new Present account during
    // this auth session
    private var createdNewAccount = false
    
    deinit { log("deinit") }
    
    // MARK: Signup steps
    
    enum Step {
        case none, phone, signup, addFriends, block, notifications, complete
    }
    
    private var step = Step.none
    {
        didSet {
            guard step != oldValue else {
                log("Resuming step: \(step)")
                return
            }
            log("Setting step to: \(step)")
            switch step {
                case .none:
                    break
                case .phone:
                    screenPresenter.presentPhoneEntry(workflowDelegate: self)
                case .signup:
                    screenPresenter.presentSignupPhotoAndName(workflowDelegate: self)
                case .block:
                    // TODO:
                    screenPresenter.presentWaitForApproval(allowRetreat: false, authorization: nil)
                case .notifications:
                    screenPresenter.presentNotificationsPrompt(workflowDelegate: self)
                case .addFriends:
                    screenPresenter.presentAddFriendsOnboarding(workflowDelegate: self)
                case .complete:
                    self.completeFlow()
            }
        }
    }
    
    /// Check conditions and present the next screen in the flow
    // Note: If this needs to be driven asynchronously we should enum the status again.
    public func advanceToNextUnfulfilledStep()
    {
        // TODO: Make an observable value with initial value non-optional
        guard let authorizationStatus = userManager.authorizationStatus.value else {
            fatalError("expected to always have an auth value")
        }
        
        // Check server auth status
        switch authorizationStatus
        {
            // Never authenticated: do it by phone now.
            case .authenticate:
                return step = .phone

            // Blocked user
            case .block:
                return step = .block
            
            // We are authorized by some means, fall through to check phone signup status
            case .signUp, .proceed:
                break
        }
        
        // We have passed verification and should have a user profile
        guard let userProfile = userManager.userProfile else {
            logError("PhoneSignupFlow: verified but no user profile")
            return step = .phone
        }
        
        // Have we verified by phone yet?
        // (Could already be verified by Facebook)
        if !userProfile.phoneVerified {
            return step = .phone
        }
        
        // Have we prompted for notification status yet?
        if !checkedNotificationAuthorizationStatus
        {
            notificationAuthorizationManager.retrieveStatus { status in
                self.checkedNotificationAuthorizationStatus = true
                switch status {
                    case .authorized:
                        return self.advanceToNextUnfulfilledStep()
                    case .denied, .partial, .unrequested:
                        return self.step = .notifications
                }
            }
            return // status callback will advance
        }

        // Have we completed signup?
        if authorizationStatus == .signUp {
            createdNewAccount = true
            return step = .signup
        }
        
        // Authorization status at this point is always .proceed
        if (createdNewAccount && !userManager.userSession.onboardingAddFriendsCompleted) {
            return step = .addFriends
        }

        // We are authorized and have completed signup.
        return step = .complete
    }
    
    // MARK: Flow
    
    public var completionHandler: (() -> Void)? // TODO: Part of Flow. Get rid of this.

    public func beginFlow() {
        userManager.authorizationStatus.observable.onNext { [weak self] authStatus in
            log("auth status = \(authStatus)")
            self?.advanceToNextUnfulfilledStep()
            }.disposed(by: disposal)
    }

    // MARK: WorkflowStepDelegate
    
    // Screens pushed by this are workflow elements that call back when complete
    public func workflowStepComplete() {
        advanceToNextUnfulfilledStep()
    }
    
    public func workflowStepCancelWorkflow() {
        self.completeFlow()
    }
}
