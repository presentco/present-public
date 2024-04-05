//
//  AppDelegate.swift
//  Present
//
//  Created by Patrick Niemeyer on 11/5/15.
//  Copyright © 2016 Present Company. All rights reserved.
//

import Aardvark
import Crashlytics
import Fabric
import FBSDKCoreKit
import PresentProtos
import UserNotifications
import UIKit

@UIApplicationMain public final class AppDelegate: UIResponder, UIApplicationDelegate
{
    public static var shared: AppDelegate!
    
    public var applicationServices: ApplicationStartup!
    public let applicationDidFinishLaunching = PromiseQueue()

    var mainWindow = UIWindow()

    var defaultLogStore: ARKLogStore!
    public var fabricLogger: FabricLogger!
    public var amplitudeLogger: AmplitudeLogger!
    //fileprivate var bugReporter: ARKEmailBugReporter?

    public var window: UIWindow? {
        get {
            return mainWindow
        }
        set {
            guard let window = window else {
                fatalError("Should not be setting the app window to nil")
            }
            
            mainWindow = window
        }
    }

    var screenPresenter: RootViewController {
        return applicationServices.rootViewController
    }

    public static var launchTime: Date = Date()
    public class var sinceLaunch: TimeInterval {
        return Date().timeIntervalSince(launchTime)
    }
    
    // MARK: Lifecycle

    public func application(_ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplicationLaunchOptionsKey: Any]?) -> Bool
    {
        AppDelegate.shared = self
        logDebug("AppDelegate: application launch with options: \(String(describing: launchOptions)), sinceLaunch=\(AppDelegate.sinceLaunch)")
        
        // Initialize all services
        let applicationStartup = ApplicationStartup(appDelegate: self)
        applicationServices = applicationStartup

        // Install our root view controller (UINavigationController) in the main window
        mainWindow.rootViewController = applicationStartup.rootViewController
        mainWindow.frame = UIScreen.main.bounds
        mainWindow.makeKeyAndVisible()
        logDebug("AppDelegate: makeKeyAndVisible sinceLaunch=\(AppDelegate.sinceLaunch)")

        applicationStartup.showInitialScreen()

        FBSDKApplicationDelegate.sharedInstance().application(application, didFinishLaunchingWithOptions: launchOptions)
        
        UNUserNotificationCenter.current().delegate = self

        // Handle any launch notification
        if let remoteNotificationDictionary = launchOptions?[.remoteNotification] as? [AnyHashable: Any],
            //let me = applicationServices.userManager.me.value,
            let remoteNotification = RemoteNotification(userInfo: remoteNotificationDictionary, personManager: applicationServices.personManager) {
            logDebug("Launched due to remote notification: \(remoteNotification)")
            
            retrieveContent(forRemoteNotification: remoteNotification)
        }
        
        logDebug("AppDelegate: appDidFinishLaunching sinceLaunch=\(AppDelegate.sinceLaunch)")
        applicationDidFinishLaunching.fulfill()
        return true
    }

    func setupBugReporting() {
        let bugReporter = Aardvark.addDefaultBugReportingGestureWithEmailBugReporter(withRecipient: "support@present.co")
        bugReporter.emailBodyAdditionsDelegate = self
        // TODO: Set our own log format here
        // bugReporter.logFormatter =
    }

    public func applicationDidBecomeActive(_ application: UIApplication) {
        FBSDKAppEvents.activateApp()
    }
    
    public func applicationWillEnterForeground(_ application: UIApplication) {
        logEvent(.app_launched)
        
        applicationServices.locationManager.startMonitoringLocationWhenPermissionGranted() // also logs location perm status
        applicationServices.notificationAuthorizationManager.logNotificationStatus()

        applicationServices.userManager.updateServicesAfterShortDelay()
    }
    
    public func applicationDidEnterBackground(_ application: UIApplication) {
        applicationServices.locationManager.stopMonitoringLocation()
    }

    public func applicationDidReceiveMemoryWarning(_ application: UIApplication) {
        ImageManager.shared.clearMemoryCache()
    }
}

// TODO: Move to logging manager
/// Logging setup
extension AppDelegate: ARKEmailBugReporterEmailBodyAdditionsDelegate
{
    // MARK: ARKEmailBugReporterEmailBodyAdditionsDelegate

    public func emailBodyAdditions(for emailBugReporter: ARKEmailBugReporter) -> [AnyHashable : Any]? {
        return [
            "Build" : Bundle.main.applicationVersion,
            "Server" : Server.Endpoint.current.rawValue
        ]
    }

    /// userToken may be null if the user has not authenticated yet
    func setupExternalLogging(withUserId userId: String?)
    {
        fabricLogger = FabricLogger(withUserId: userId)
        ARKLogDistributor.default().add(fabricLogger)

        amplitudeLogger = AmplitudeLogger(withUserId: userId)
        ARKLogDistributor.default().add(amplitudeLogger)

        logEvent(type: .action, "Application Launched")
    }

    /// Set the user id on the external loggers (e.g. after user authenticates from anon status)
    func updateExternalLogging(withUserId userId: String) {
        fabricLogger.setUserId(userId: userId)
        amplitudeLogger.setUserId(userId: userId)
    }

    func setupLocalLogging() {
#if DEBUG
        let logStore = ARKLogStore(persistedLogFileName: "DefaultLogStore", maximumLogMessageCount: 500)!
        logStore.name = "Default"
        logStore.prefixNameWhenPrintingToConsole = false
        logStore.printsLogsToConsole = true
        defaultLogStore = logStore
        ARKLogDistributor.default().defaultLogStore = logStore
#endif
    }
}

// TODO: Move to notification manager
/// Register for and receive notifications
extension AppDelegate: UNUserNotificationCenterDelegate
{
    // Receive the notifications device token.
    public func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data)
    {
        NSLog("did register for remote notifs: thread = \(Thread.current)")
        let tokenChars = (deviceToken as NSData).bytes.bindMemory(to: CChar.self, capacity: deviceToken.count)
        var tokenString = ""
        for i in 0..<deviceToken.count {
            tokenString += String(format: "%02.2hhx", arguments: [tokenChars[i]])
        }
        logDebug("Received device token: \(tokenString)")

        // Wait for the application to finish launching.
        // Note: This is currently called as a side effect of initializing the notifications manager in app delegate.
        // Note: So we use the applicationDidFinishLaunching promise here.  We could just move that to the end of startup.
        applicationDidFinishLaunching.enqueue {
            logDebug("registering device token (launch complete)")
            self.applicationServices.service.updateDeviceToken(deviceToken: tokenString) { error in
                if let error = error {
                    logError("Could not update device token due to \(error)")

                } else {
                    logDebug("Updated device token.")
                }
            }
        }
    }

    // Handle an error on registration
    public func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        guard !UIDevice.current.isSimulator else {
            // We really don't care that we failed to register – this is expected.
            return
        }

        logError("Failed to register for remote notifications: \(error)")
    }

    // Receive remote notification with content.
    // This method is called when content-available is set, whether in the foreground or background.
    // We will be woken from a suspended state but not if we have been force-quit.
    // In the foreground case the willPresent is called first, giving us an opportunity to suppress it (I think).
    public func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable : Any], fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void)
    {
        logDebug("application didReceiveRemoteNotification, fetch completion handler")

        //reportNotificationReceived(userInfo: userInfo)

        guard let remoteNotification = RemoteNotification(userInfo: userInfo, personManager: applicationServices.personManager)
        else {
            logDebug("Received un-parsable remote notification: \(userInfo)")
            completionHandler(.failed)
            return
        }

        retrieveContent(forRemoteNotification: remoteNotification)

        // The completion handler must be called when our activity completes
        // It tracks our time/energy usage and reports whether data was downloaded.
        // TODO: We don't currently have completion APIs consistently enough throughout the code to
        // TODO: properly observe the content retrieval above. So we will give it a fixed time to complete.
        // TODO: After server badging we should revisit this and better differentiate foreground and background activity.
        DispatchQueue.main.asyncAfter(seconds: 3) {
            completionHandler(.newData)
        }
    }

    // Log the notification here and on the server
    private func reportNotificationReceived(userInfo: [AnyHashable: Any])
    {
        if let jsonData = try? JSONSerialization.data(withJSONObject: userInfo ),
           let jsonString = String(data: jsonData, encoding: String.Encoding.utf8)
        {
            logDebug("Received notification: \(jsonString)")
            applicationServices.service.notificationReceived(notificationJson: jsonString) { response in
                if case .error = response {
                    logError("Error reporting notification to server.")
                }
            }
        } else {
            logError("Unable to log notification")
        }
    }

    // MARK: UNUserNotificationCenterDelegate

    // Called when a notification is delivered to a foreground app (only foreground) and determines whether the
    // notification is displayed or suppressed. Invokes the completion handler with the set
    // of notification types that should be allowed (empty to suppress).
    // We decide to show or suppress the notification based on what the user is viewing in the app.
    // If not suppressed, application:didReceiveRemoteNotification:fetchCompletionHandler is called after this method.
    public func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void)
    {
        logEvent(type: .action, "Received Push Notification for app in foreground", properties: ["notification" as NSObject: notification.request.content])
        logDebug("notification payload: \(notification.request.content)")

        guard let remoteNotification = RemoteNotification(notificationContent: notification.request.content, personManager: applicationServices.personManager)
            else {
                log("Received un-parsable or badge-only remote notification: \(notification.request.content.userInfo)")
                
                // We don't know how to parse this notification. Don't show it.
                completionHandler([])
                
                // Make sure that our activity list is up to date though.
                //applicationServices.userManager.activityManager.fetchActivity()
                return
            }
        
        // Always retrieve content for a notification.
        //retrieveContent(forRemoteNotification: remoteNotification)

        guard !screenPresenter.shouldSuppress(notification: remoteNotification) else {
            // We should suppress this notification. Don't do anything.
            logDebug("Suppressing notification for app in foreground")
            completionHandler([])
            return
        }

        logDebug("Displaying notification for app in foreground")
        completionHandler([.badge, .sound, .alert])
    }

    // Called when the user taps / interacts with a notification either while the app is in the foreground or background.
    // (e.g. app launched by a notification)
    public func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void)
    {
        logEvent(type: .tap, "Tapped Push Notification", properties: ["content" as NSObject:response.notification.request.content])

        guard let remoteNotification = RemoteNotification(notificationContent: response.notification.request.content, personManager: applicationServices.personManager) else {
            logDebug("User tapped on un-parsable remote notification: \(response.notification.request.content.userInfo)")
            completionHandler()
            return
        }

        if response.actionIdentifier == UNNotificationDefaultActionIdentifier {
            // The user tapped on the notification.
            showContent(forRemoteNotification: remoteNotification)
        }

        completionHandler()
    }

    // MARK: Private Methods

    fileprivate func retrieveContent(forRemoteNotification remoteNotification: RemoteNotification)
    {
        logDebug("Retrieving content for remote notification: \(remoteNotification)")

        let groupManager = applicationServices.userManager.groupManager
        switch remoteNotification {
            case let .updateGroupWithComment(comment):
                //applicationIconBadgeManager.ensureAppBadgeReflectsNewJoinedGroupContent()
                groupManager.didReceive(groupMessage: comment)

            case let .updateGroupWithCommentTokens(groupId, commentId):
                //applicationIconBadgeManager.ensureAppBadgeReflectsNewJoinedGroupContent()
                groupManager.didReceive(groupId: groupId, commentId: commentId)

            case .updateGroupWithToken:
                // This is a notification about showing a group.
                // TODO: Should kick off an update of the group.
                break

            case let .failedToSendGroup(message):
                // Make sure the failed message is in our group history.
                applicationServices.userManager.groupManager.didReceive(groupMessage: message)
            
            case .userId:
                break
            }
    }

    private func showContent(forRemoteNotification remoteNotification: RemoteNotification) {
        logDebug("Showing content for remote notification: \(remoteNotification)")

        switch remoteNotification {
            case let .updateGroupWithCommentTokens(groupId, _):
                screenPresenter.transitionToGroup(withToken: PresentUUID(uuidString: groupId), showChat: true)

            case let .updateGroupWithComment(groupComment):
                screenPresenter.transitionToGroup(withToken: groupComment.groupId, showChat: true)

            case let .updateGroupWithToken(groupId):
                screenPresenter.transitionToGroup(withToken: PresentUUID(uuidString: groupId), showChat: false)

            case let .failedToSendGroup(message):
                screenPresenter.transitionToGroup(withToken: message.groupId, showChat: true)
            
            case .userId(let userId):
                applicationServices.screenPresenter.presentProfile(forUserId: userId)
        }
    }

}

/// Launched with URL Handling
public extension AppDelegate
{
    // Application launched via universal URL
    public func application(_ application: UIApplication, continue userActivity: NSUserActivity, restorationHandler: @escaping ([Any]?) -> Void) -> Bool
    {
        guard userActivity.activityType == NSUserActivityTypeBrowsingWeb,
              let url = userActivity.webpageURL else {
            return false
        }
        logEvent(type: .action, "Opened by universal link", properties: ["url" as NSObject: url.absoluteString as AnyObject])
        applicationServices.urlManager.openInternal(url: url)
        return true
    }

    // Application launched via URL scheme
    public func application(_ application: UIApplication, open url: URL, options: [UIApplicationOpenURLOptionsKey : Any] = [:]) -> Bool
    {
        logEvent(type: .action, "Opened by url scheme", properties: ["url" as NSObject: url.absoluteString as AnyObject])

        if FBSDKApplicationDelegate.sharedInstance().application(application, open: url, options: options) {
            return true
        }

        // Sanity check: the FB SDK has been failing to handle its own fb:// url scheme under some circumstances
        let fbUrlScheme = "fb656395011206413" // Match the scheme in Info.plist
        if url.absoluteString.starts(with: fbUrlScheme) {
            logError("FBSDK didn't recognize its own url: \(url.absoluteString)")
            // Allow the app to continue and try again...
            return true
        }

        // Transform url scheme, e.g. present://g/xxx  => https://present.co/g/xxx
        let urlString = url.absoluteString
                .replacingOccurrences(of: "present://", with: "https://present.co/")
        applicationServices.urlManager.openInternal(string: urlString)

        return true
    }
}

/// Debug Menu
public extension AppDelegate
{
    public override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?)
    {
        super.touchesBegan(touches, with: event)

        if let firstTouch = event?.allTouches?.first,
           firstTouch.tapCount == 3,
           firstTouch.location(in: window).y < 20
        {
            presentDebugMenu()
        }
    }

    public func presentDebugMenu()
    {
        logDebug("DebugMenu: presentDebugMenu")
        
        #if APP_STORE
        guard applicationServices.userManager.userIsAdmin else { return }
        #endif

        let debugMenu = DebugMenu(applicationServices: applicationServices)
        // TODO: Create a method to show pop-overs like this in screen presenter.
        self.screenPresenter.currentViewController.present(debugMenu, animated: true)
    }
}


