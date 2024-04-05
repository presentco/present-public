//
//  ApplicationServices.swift
//  Present
//
//  Created by Patrick Niemeyer on 4/19/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation

/// Internal services offered by the application to view controllers
// Note: We could split this into a protocol per service and define them with the service impls.
public protocol ApplicationServices
{
    static var applicationServices: ApplicationServices { get }
    var notificationAuthorizationManager: NotificationAuthorizationManager { get }
    var locationManager: LocationManager { get }
    var syncStatusPoller: SyncStatusPoller { get }
    var applicationIconBadgeManager: ApplicationIconBadgeManager { get }
    var service: PresentService { get }
    var userManager: UserManager { get }
    var groupManager: GroupManager { get }
    var personManager: PersonManager { get }
    var urlManager: URLManager { get }
    var rootViewController: RootViewController { get }
    var screenPresenter: RootViewController { get }
    var activityManager: ActivityManager { get }
    var contactsManager: ContactsManager { get }
}

/// Default implmentation of application services
///
/// For testing / mocking we have several options:
///
/// 1) We can provide an alternate implementation by type, e.g.
///      extension ApplicationServices where Self: MyTestViewController
///    We can also do this for many types by tagging them with a protocol e.g.:
///      protocol Test { }
///      extension ApplicationServices where Self: Test
///
/// 2) We can simply provide a different startup sequence that initializes the
///    delegated shared services differently or replaces indidvidual services in the
///    default implementation.
///
public extension ApplicationServices
{
    static var applicationServices: ApplicationServices {
        return AppDelegate.shared.applicationServices
    }
    var applicationServices: ApplicationServices {
        return AppDelegate.shared.applicationServices
    }
    
    var notificationAuthorizationManager: NotificationAuthorizationManager {
        return applicationServices.notificationAuthorizationManager
    }
    
    var locationManager: LocationManager {
        return applicationServices.locationManager
    }
    
    var syncStatusPoller: SyncStatusPoller {
        return applicationServices.syncStatusPoller
    }
    
    var applicationIconBadgeManager: ApplicationIconBadgeManager {
        return applicationServices.applicationIconBadgeManager
    }
    
    var service: PresentService {
        return applicationServices.service
    }
    
    var userManager: UserManager {
        return applicationServices.userManager
    }
    
    var groupManager: GroupManager {
        return applicationServices.groupManager
    }
    
    var personManager: PersonManager {
        return applicationServices.personManager
    }
    
    var urlManager: URLManager {
        return applicationServices.urlManager
    }
    
    var imageManager: ImageManager {
        return .shared
    }
    
    var rootViewController: RootViewController {
        return applicationServices.rootViewController
    }
    
    var screenPresenter: RootViewController {
        return applicationServices.screenPresenter
    }
    var activityManager: ActivityManager {
        return applicationServices.activityManager
    }
    var contactsManager: ContactsManager {
        return applicationServices.contactsManager
    }
}

/// Provide the application services container for older UIViewControllers
/// that are already manually initialized with some services.
public protocol LegacyApplicationServices
{
    static var applicationServices: ApplicationServices { get }
}

/// Provide the application services container for older UIViewControllers
/// that are already manually initialized with some services.
public extension LegacyApplicationServices
{
    static var applicationServices: ApplicationServices {
        return AppDelegate.shared.applicationServices
    }
    var applicationServices: ApplicationServices {
        return AppDelegate.shared.applicationServices
    }
}
