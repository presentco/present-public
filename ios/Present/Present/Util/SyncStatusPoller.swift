//
//  SyncStatusPoller.swift
//  Present
//
//  Created by Patrick Niemeyer on 9/15/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import PresentProtos
import UserNotifications

public class SyncStatusPoller
{
    public static var shared: SyncStatusPoller!

    // The interval for polling auth status while waiting at the blocked screen
    static let BlockedUserInterval = 5 // seconds

    // The interval for polling auth status while using the app normally
    static let BackgroundInterval = 15*60 // seconds

    public let features = PromisedValue<[Present.Feature]>()

    private let rootViewController: RootViewController
    private let userManager: UserManager
    private let service: PresentService
    private var authPollingTimer: Timer?
    private let notificationAuthorizationManager: NotificationAuthorizationManager
    private var pollingInterval: Int? // The current desired polling interval

    public init(rootViewController: RootViewController, service: PresentService, notificationAuthorizationManager: NotificationAuthorizationManager )
    {
        self.rootViewController = rootViewController
        self.userManager = rootViewController.userManager
        self.service = service
        self.notificationAuthorizationManager = notificationAuthorizationManager
        NotificationCenter.default.addObserver(self, selector: #selector(applicationWillEnterForeground), name: NSNotification.Name.UIApplicationWillEnterForeground, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(applicationWillResignActive), name: NSNotification.Name.UIApplicationWillResignActive, object: nil)
        SyncStatusPoller.shared = self
    }

    /// Make an async call to check the user's auth status and update the root view controller
    /// which will potentially redirect the user on the fly back to a signup or hold screen.
    /// @See FacebookOnboardingFlow which evaluates the auth state and persists the last known state in user manager.
    @objc private func pollAuth()
    {
        // synchronizeStatus() does a number of things including reporting back our notification status to 
        // the server.  So we need to fetch it here.
        notificationAuthorizationManager.retrieveStatus { status in
            let notificationsEnabled: Bool = {
                if case .authorized = status {
                    return true
                } else {
                    return false
                }
            }()
            
            self.service.synchronizeStatus(notificationsEnabled: notificationsEnabled) { response in
                switch(response) {
                    case let .success(syncResponse):
                        self.didReceiveSyncResponse(syncResponse: syncResponse)
                    case let .error(err):
                        logDebug("Synchronized auth status return error: \(err)")
                }
            }
        }
    }

    private func didReceiveSyncResponse(syncResponse: Present.SynchronizeResponse)
    {
        logDebug("SyncStatusPoller: received sync response: \(syncResponse.authorization.nextStep)")
        
        // Persist and publish the changed state via user manager
        if syncResponse.hasAuthorization {
            userManager.authorizationStatus.value = syncResponse.authorization.nextStep
        }
        
        features.fulfill(with: syncResponse.features)
    }
    
    // Perform one synchronize call to the server
    public func pollOnce() {
        pollAuth()
    }

    // Begin polling at the background (less frequent) interval if not currently
    public func startPollingAtBackgroundInterval() {
        startPollingAuthStatus(intervalSeconds: SyncStatusPoller.BackgroundInterval)
    }
    
    // Begin polling at the blocked user (more frequent) interval if not currently
    public func startPollingAtBlockedUserInterval() {
        startPollingAuthStatus(intervalSeconds: SyncStatusPoller.BlockedUserInterval)
    }

    private func startPollingAuthStatus(intervalSeconds interval: Int)
    {
        // If already polling at desired interval don't restart
        let timeInterval = TimeInterval(interval)
        if let authPollingTimer = authPollingTimer, authPollingTimer.timeInterval == timeInterval {
            return
        }

        // Set new polling interval
        logDebug("start polling at interval: \(interval)")
        pollingInterval = interval
        stopPolling()
        pollAuth() // kick one off immediately
        authPollingTimer = Timer.scheduledTimer(timeInterval: timeInterval, target: self, selector: #selector(pollAuth), userInfo: nil, repeats: true)
    }
    
    func stopPolling() {
        if let timer = authPollingTimer {
            timer.invalidate()
            authPollingTimer = nil
        }
    }

    @objc
    func applicationWillResignActive() {
        logDebug("Auth poller: app will resign active")
        stopPolling()
    }

    @objc
    func applicationWillEnterForeground() {
        logDebug("Auth poller: app will enter foreground")
        if let interval = pollingInterval {
            startPollingAuthStatus(intervalSeconds: interval)
        } else {
            startPollingAtBackgroundInterval()
        }
    }
}

public protocol FeatureObserver: class {
    func setFeatures(features: [Present.Feature])
}
