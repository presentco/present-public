//
//  NotificationAuthorizationManager.swift
//  Present
//
//  Created by Patrick Niemeyer on 12/6/16.
//  Copyright © 2016 Present Company. All rights reserved.
//

import Foundation
import UIKit
import UserNotifications


public final class NotificationAuthorizationManager
{
    public enum Status {
        /// Authorizations have not yet been requested.
        case unrequested
        /// All of the authorizations desired have been denied.
        case denied
        /// A partial set of desired authorizations are granted.
        case partial(authorizedPermissions: UNAuthorizationOptions)
        /// All of the authorizations desired are granted.
        case authorized
    }
    
    // MARK: Initialization
    
    public init(desired: UNAuthorizationOptions,
                application: UIApplication = UIApplication.shared,
                userNotificationCenter: UNUserNotificationCenter = UNUserNotificationCenter.current(),
                notificationCenter: NotificationCenter = NotificationCenter.default) {
        
        desiredAuthorization = desired
        self.application = application
        self.userNotificationCenter = userNotificationCenter
        self.notificationCenter = notificationCenter
        
        updateNotificationSettings()
        registerForRemoteNotifications()
        notificationCenter.addObserver(self, selector: #selector(applicationWillEnterForeground), name: NSNotification.Name.UIApplicationWillEnterForeground, object: application)
        notificationCenter.addObserver(self, selector: #selector(applicationDidEnterBackground), name: NSNotification.Name.UIApplicationDidEnterBackground, object: application)
    }
    

    // MARK: Public Methods
    
    public func requestAuthorization(withCompletionHandler completionHandler: @escaping (Status) -> Void) {
        logEvent(type: .action, "Requesting User Notification Permissions")
        userNotificationCenter.requestAuthorization(options: desiredAuthorization) { [weak self] (granted, error) in
            DispatchQueue.main.async {
                if granted {
                    //logEvent(type: .tap, "iOS Accept Notification Permissions")
                    logEvent(.signup_notifications_allowed)
                    completionHandler(.authorized)
                } else {
                    //logEvent(type: .action, "iOS Deny Notification Permissions")
                    logEvent(.signup_notifications_denied)
                    completionHandler(.denied)
                }
            }
            
            self?.updateNotificationSettings() {
                self?.registerForRemoteNotifications()
            }
        }
    }
    
    public func retrieveStatus(withCompletionHandler completionHandler: @escaping (Status) -> Void) {
        notificationSettingsVendingQueue.enqueue { [weak self] in
            guard let strongSelf = self else {
                return
            }
            
            guard let notificationSettings = strongSelf.notificationSettings else {
                assertionFailure("Should not execute block without notificationSettings")
                completionHandler(.unrequested)
                return
            }
            
            switch notificationSettings.authorizationStatus {
            case .notDetermined:
                completionHandler(.unrequested)
                
            case .denied:
                completionHandler(.denied)
                
            case .authorized, .provisional:
                var authorizedPermissions: UNAuthorizationOptions = []
                
                if case .enabled = notificationSettings.badgeSetting {
                    authorizedPermissions.insert(.badge)
                }
                if case .enabled = notificationSettings.soundSetting {
                    authorizedPermissions.insert(.sound)
                }
                if case .enabled = notificationSettings.alertSetting {
                    authorizedPermissions.insert(.alert)
                }
                if case .enabled = notificationSettings.carPlaySetting {
                    authorizedPermissions.insert(.carPlay)
                }
                
                guard strongSelf.desiredAuthorization.contains(.badge) == authorizedPermissions.contains(.badge)
                    || strongSelf.desiredAuthorization.contains(.sound) == authorizedPermissions.contains(.sound)
                    || strongSelf.desiredAuthorization.contains(.alert) == authorizedPermissions.contains(.alert)
                    || strongSelf.desiredAuthorization.contains(.carPlay) == authorizedPermissions.contains(.carPlay) else {
                        completionHandler(.partial(authorizedPermissions: authorizedPermissions))
                        return
                }
                
                return completionHandler(.authorized)
            }
        }
    }

    /// log the notification status event to the server
    public func logNotificationStatus()
    {
        retrieveStatus { status in
            if case .authorized = status {
                logEvent(.permission_notifications_status, [LoggingKey.status: LoggingValue.allowed])
            } else {
                logEvent(.permission_notifications_status, [LoggingKey.status: LoggingValue.denied])
            }
        }
    }

    // MARK: Private Properties
    
    private let desiredAuthorization: UNAuthorizationOptions
    private let application: UIApplication
    private let userNotificationCenter: UNUserNotificationCenter
    private let notificationCenter: NotificationCenter
    
    private static let notificationSettingsPrefKey = "NotificationAuthorizationNotificationSettings"
    
    /// Notification settings retrieved from the OS since the last time the application entered the foreground.
    private var notificationSettings: UNNotificationSettings? {
        didSet {
            if notificationSettings != nil {
                notificationSettingsVendingQueue.resume()
                
            } else {
                notificationSettingsVendingQueue.suspend()
            }
        }
    }
    private let notificationSettingsVendingQueue = PromiseQueue()
    
    // MARK: Private Methods
    
    private func updateNotificationSettings(withCompletionHandler completionHandler: @escaping () -> Void = {}) {
        userNotificationCenter.getNotificationSettings { (notificationSettings) in
            self.notificationSettings = notificationSettings
            completionHandler()
        }
    }
    
    private func registerForRemoteNotifications() {
        // Since we have background push notification permissions, we should always register. Doing so will not prompt the user.
        DispatchQueue.main.async {
            self.application.registerForRemoteNotifications()
        }
    }
    
    @objc
    private func applicationWillEnterForeground() {
        // Update our notification settings, since they could have changed while the app was backgrounded.
        updateNotificationSettings()
        
        // Re-register for remote notifications to ensure we have the most up-to-date token.
        registerForRemoteNotifications()
    }
    
    @objc
    private func applicationDidEnterBackground() {
        // We no longer know for sure what our notifications settings are, since they can be changed in the background.
        notificationSettings = nil
    }
}


public final class NotificationAuthorizationRequester
{
    
    // Prompt the user for notification permissions based on the current iOS authorization status.
    // If never requested we ask iOS to prompt for permissions.  If previously denied or reverted
    // send the user to settings.
    static func promptForPushNotifications(from viewController: UIViewController,
                                           with notificationAuthorizationManager: NotificationAuthorizationManager,
                                           userNotificationsRequestedHandler: @escaping (NotificationAuthorizationManager.Status) -> Void = { _ in },
                                           userNotificationsPromptDismissedHandler: @escaping () -> Void = {}) {
        
        notificationAuthorizationManager.retrieveStatus { (status) in
            switch status {
            case .authorized:
                logError("Prompting for push notifications even though they are already enabled! Bailing")
                userNotificationsRequestedHandler(status)
                
            case .partial, .denied:
                logEvent(type: .view, "Prompt Accept Notification Permission -- Previously Denied")
                let promptTitle = NSLocalizedString("NotificationAuthorizationRequesterPromptTitle",
                                                    tableName: nil, bundle: .main,
                                                    value: "Allow Notification Services",
                                                    comment: "Title for push notification permission prompt.")
                
                let promptMessage = NSLocalizedString("NotificationAuthorizationRequesterPreviouslyDeniedPromptMessage",
                                                      tableName: nil, bundle: .main,
                                                      value: "Notification permissions were previously denied. Go to Settings to enable them.",
                                                      comment: "Message for push notification permission prompt when push notifications have been previously denied.")
                
                let notNowButtonTitle = NSLocalizedString("NotificationAuthorizationRequesterPromptNotNowButtonTitle",
                                                          tableName: nil, bundle: .main,
                                                          value: "Not Now",
                                                          comment: "Title for button on push notification permission prompt that stops the push notification prompt flow.")
                
                let settingButtonTitle = NSLocalizedString("NotificationAuthorizationRequesterPromptAcceptButtonTitle",
                                                           tableName: nil, bundle: .main,
                                                           value: "Settings",
                                                           comment: "Title for button on push notification permission prompt that allows push notifications.")

                let alertController = UIAlertController(title: promptTitle,
                                                        message: promptMessage,
                                                        preferredStyle: .alert)
                
                alertController.addAction(withTitle: settingButtonTitle) { _ in
                    logEvent(type: .tap, "App Accept Notification Permission Via iOS Settings")
                    UIApplication.shared.open(URL(string: UIApplicationOpenSettingsURLString)!)
                }
                
                alertController.addAction(withTitle: notNowButtonTitle, style: .cancel) { _ in
                    logEvent(type: .tap, "App Deny Notification Permission")
                    userNotificationsPromptDismissedHandler()
                }
                
                viewController.present(alertController, animated: true)
                
            case .unrequested:
                logEvent(type: .view, "Prompt Accept Notification Permission")
                notificationAuthorizationManager.requestAuthorization(withCompletionHandler: { (status) in
                    userNotificationsRequestedHandler(status)
                })
            }
        }
    }
    
}
