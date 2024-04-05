//
//  NotificationAccessViewController.swift
//  Present
//
//  Created by Kaming Li on 5/7/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import UIKit

protocol NotificationAccessViewControllerDelegate: class {
    func promptForNotificationAccessIfNeeded()
}

/// Prompt for turning on notifications as part of a workflow.
class NotificationAccessViewController : PresentViewController, WorkflowStep
{
    weak var workflowDelegate: WorkflowStepDelegate?
    weak var delegate: NotificationAccessViewControllerDelegate?
    
    override public func viewDidLoad() {
        self.delegate = self
    }
    
    @IBOutlet weak var button: ThemeableButton! {
        didSet {
            self.button.backgroundColor = UIColor.Present.Purple.Main
            self.button.setTitleColor(.white, for: .normal)
            self.button.roundCornersToHeight()
            self.button.addTarget { button in self.proceed() }
        }
    }
    
    @IBOutlet weak var skipLabel: UILabel! {
        didSet {
            let tapGestureRecognizer = UITapGestureRecognizer(target: self, action: #selector(skip))
            self.skipLabel.addGestureRecognizer(tapGestureRecognizer)
            self.skipLabel.isUserInteractionEnabled = true
        }
    }
    
    private func proceed() {
        delegate?.promptForNotificationAccessIfNeeded()
    }
    
    @objc private func skip() {
        workflowDelegate?.workflowStepComplete()
    }
}

extension NotificationAccessViewController: NotificationAccessViewControllerDelegate
{
    // Check authorization status and call our prompt logic if needed.
    func promptForNotificationAccessIfNeeded()
    {
        print("NotificationAccessViewController: prompt for notifications")

        notificationAuthorizationManager.retrieveStatus { (status) in
            switch status {
            case .authorized, .partial:
                logEvent(.signup_notifications_allowed)
            case .denied:
                logEvent(.signup_notifications_denied)
            case .unrequested:
                break
            }

            switch status {
            case .authorized:
                print("NotificationAccessViewController: already authorized")
                self.workflowDelegate?.workflowStepComplete()

            case .denied, .partial, .unrequested:
                print("NotificationAccessViewController: auth needed")
                NotificationAuthorizationRequester.promptForPushNotifications(
                    from: self,
                    with: self.notificationAuthorizationManager,
                    userNotificationsRequestedHandler: { _ in
                        print("NotificationAccessViewController: auth granted")
                        self.workflowDelegate?.workflowStepComplete()
                },
                    userNotificationsPromptDismissedHandler: {
                        print("NotificationAccessViewController: auth dismissed")
                        self.workflowDelegate?.workflowStepComplete()
                })
            }
        }
    }
}

