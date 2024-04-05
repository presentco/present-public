//
//  Application.swift
//  Present
//
//  Created by Patrick Niemeyer on 4/19/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import PresentProtos // for version check, could move
import Then

/// Creates all services and performs startup activities in the correct order.
public final class ApplicationStartup : ApplicationServices
{
    // Services
    public let notificationAuthorizationManager = NotificationAuthorizationManager(desired: [.badge, .sound, .alert])
    public let locationManager: LocationManager = LocationManager(useLastLocationAtStartup: true)
    public var syncStatusPoller: SyncStatusPoller
    public var applicationIconBadgeManager: ApplicationIconBadgeManager
    public var service: PresentService
    public var userManager: UserManager
    public var personManager: PersonManager
    public var urlManager: URLManager
    public var rootViewController: RootViewController
    public var activityManager: ActivityManager
    public var contactsManager: ContactsManager
    
    public var screenPresenter: RootViewController {
        return rootViewController
    }

    public init(appDelegate: AppDelegate)
    {
        let application = UIApplication.shared
        
        appDelegate.setupLocalLogging()
        locationManager.startMonitoringLocationWhenPermissionGranted() // also logs location permission status
        
        // The UserSession scopes user data using an identifier.
        // We use the server endpoint description as a namespace to ensure we store different user data per server.
        let userSession = UserSession(namespace: Server.api.endpoint.host!)
        let requestHeaderGenerator = RequestHeaderGenerator(clientToken: userSession.clientToken, selectedLocation: userSession.selectedLocation)
        log("Client uuid = \(userSession.clientToken)")
        
        let persistedUserId = userSession.persistedUserProfile?.id // The user id if available at launch
        appDelegate.setupExternalLogging(withUserId: persistedUserId)
        
        service = PresentService(locationProvider: locationManager, requestHeaderGenerator: requestHeaderGenerator, backgroundTaskManager: application)
        activityManager = ActivityManager(service: service)
        let groupManager = GroupManager(service: service, requestHeaderGenerator: requestHeaderGenerator)

        userManager = UserManager(userSession: userSession, activityManager: activityManager, groupManager: groupManager, service: service)
        personManager = PersonManager(userManager: userManager, service: service)
        
        // Note: Dealing with the mutual dependency of user manager and group manager here.
        // Note: We should try to break this coupling.
        groupManager.personManager = personManager
        groupManager.userManager = userManager

        // If the user hasn't authenticated before, update logging when she has (requires userManager).
        if persistedUserId == nil {
            userManager.whenUserProfileAvailable { profile in
                appDelegate.updateExternalLogging(withUserId: profile.id)
            }
        }

        let notificationAuthorizationManager = self.notificationAuthorizationManager
        userManager.whenUserAuthorizedToProceedToApp {
            notificationAuthorizationManager.logNotificationStatus()
        }
        
        applicationIconBadgeManager = ApplicationIconBadgeManager(groupManager: groupManager)
        
        rootViewController = RootViewController(locationProvider: locationManager, notificationAuthorizationManager: notificationAuthorizationManager, userManager: userManager, personManager: personManager, urlOpener: application, eventSupressor: application, imageManager: .shared, applicationIconBadgeManager: applicationIconBadgeManager, service: service)
        
        syncStatusPoller = SyncStatusPoller(rootViewController: rootViewController, service: service, notificationAuthorizationManager: notificationAuthorizationManager)
        syncStatusPoller.pollOnce()

        urlManager = URLManager(urlOpener: application, rootViewController: rootViewController, groupManager: groupManager, service: service)
        
        contactsManager = ContactsManager()
        
        // todo: delay these?
        Features.initialize()
        appDelegate.setupBugReporting()
    }
    
    public func showInitialScreen()
    {
        presentUpgradeAlertIfNeeded()
        screenPresenter.presentHomeViewController()
    }
}

// TODO: Move to upgrade manager
/// Suggest or force upgrade dialog
public extension ApplicationStartup
{
    fileprivate func presentUpgradeAlertIfNeeded() {
        service.fetchCurrentBuildSupportStatus { (supportedStatus, upgradeURL) in
            self.presentUpgradeDialog(for: supportedStatus, upgradeURL: upgradeURL)
        }
    }
    
    fileprivate func presentUpgradeDialog(for supportedStatus: Present.VersionCheckResult.Status, upgradeURL: URL) {
        switch supportedStatus {
        case .current:
            // We're current! Nothing to do here.
            break
            
        case .upgradeAvailable:
            let upgradeAvailablePromptTitle = NSLocalizedString(
                "AppDelegateUpgradeAvailablePromptTitle",
                tableName: nil,
                bundle: .main,
                value: "Update Available",
                comment: "Title on alert prompting customers to update their app."
            )
            let upgradeAvailablePromptMessage = NSLocalizedString(
                "AppDelegateUpgradeAvailablePromptMessage",
                tableName: nil,
                bundle: .main,
                value: "Don’t be stuck in the past. Get the latest now!",
                comment: "Message on alert prompting customers to update their app."
            )
            let upgradeAvailablePromptUpgradeButtonTitle = NSLocalizedString(
                "AppDelegateUpgradeAvailableUpgradeButtonTitle",
                tableName: nil,
                bundle: .main,
                value: "Get it!",
                comment: "Button title to upgrade app on alert prompting customers to update their app."
            )
            
            let upgradeAvailablePrompt = UIAlertController(title: upgradeAvailablePromptTitle,
                                                           message: upgradeAvailablePromptMessage,
                                                           preferredStyle: .alert)
            upgradeAvailablePrompt.addAction(withTitle: upgradeAvailablePromptUpgradeButtonTitle) { _ in
                UIApplication.shared.open(upgradeURL)
            }
            upgradeAvailablePrompt.addAction(withTitle: "¯\\_(ツ)_/¯", style: .cancel)
            self.screenPresenter.presentModal(alert: upgradeAvailablePrompt)
            
        case .upgradeRequired:
            let upgradeRequiredPromptTitle = NSLocalizedString(
                "AppDelegateUpgradeRequiredPromptTitle",
                tableName: nil,
                bundle: .main,
                value: "Update Required",
                comment: "Title on alert requiring customers to update their app."
            )
            let upgradeRequiredPromptMessage = NSLocalizedString(
                "AppDelegateUpgradeRequiredPromptMessage",
                tableName: nil,
                bundle: .main,
                value: "Looks like you got stuck in the past. Get the latest now!",
                comment: "Message on alert requiring customers to update their app."
            )
            let upgradeRequiredPromptUpgradeButtonTitle = NSLocalizedString(
                "AppDelegateUpgradeRequiredUpgradeButtonTitle",
                tableName: nil,
                bundle: .main,
                value: "Okay fine",
                comment: "Button title to upgrade app on alert requiring customers to update their app."
            )
            
            let upgradeRequiredPrompt = UIAlertController(title: upgradeRequiredPromptTitle,
                                                          message: upgradeRequiredPromptMessage,
                                                          preferredStyle: .alert)
            upgradeRequiredPrompt.addAction(withTitle: upgradeRequiredPromptUpgradeButtonTitle) { _ in
                UIApplication.shared.open(upgradeURL) { _ in
                    // Present the dialog again to ensure that the customer is stuck viewing this prompt.
                    self.presentUpgradeDialog(for: supportedStatus, upgradeURL: upgradeURL)
                }
            }
            self.screenPresenter.presentModal(alert: upgradeRequiredPrompt)
        }
    }
}

