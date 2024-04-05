//
//  URLManager.swift
//  Present
//
//  Created by Patrick Niemeyer on 2/17/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import PresentProtos

public class URLManager
{
    static var shared: URLManager!
    
    public enum DispatchType {
        case openCircle, createCircle, changeLocation, openCategory, openUser, openTour, verifyAuthLink
    }

    let urlOpener: URLOpener
    let screenPresenter: RootViewController
    let groupManager: GroupManager
    let service: PresentService
    let userManager: UserManager

    public init(urlOpener: URLOpener, rootViewController: RootViewController, groupManager: GroupManager, service: PresentService)
    {
        self.urlOpener = urlOpener
        self.screenPresenter = rootViewController
        self.groupManager = groupManager
        self.service = service
        self.userManager = rootViewController.userManager
        URLManager.shared = self
    }

    public func isPresentUrl(string: String) -> Bool {
        return string.starts(with: PresentLinks.presentBase) ||
               string.starts(with: PresentLinks.presentBaseStaging) ||
               string.starts(with: PresentLinks.presentAppBase) ||
               string.starts(with: PresentLinks.presentAppBaseStaging)
    }

    public func isPresentUrl(url: URL) -> Bool {
        return isPresentUrl(string: url.absoluteString)
    }

    /// Determine if the link is internal or external and open it appropriately
    public func dispatchUrl(string: String, completion: (()->Void)? = nil)
    {
        guard let url = URL(string: string) else {
            logError("can't parse url string: \(string)")
            return
        }
        dispatchUrl(url: url, completion: completion)
    }
    
    // TODO: Remove space logging
    /// Determine if the link is internal or external and open it appropriately
    public func dispatchUrl(url: URL, dispatchSpace: PresentSpace? = nil, dispatchEvents: [DispatchType: LoggingEvent] = [:], completion: (()->Void)? = nil)
    {
        if isPresentUrl(url: url) {
            openInternal(url: url, dispatchSpace: dispatchSpace, dispatchEvents: dispatchEvents, completion: completion)
        } else {
            openExternal(url: url, completion: completion)
        }
    }

    /// Open an external link in Safari
    public func openExternal(url: URL, completion: (()->Void)? = nil) {
        logDebug("URLManager: open external url: \(url)")
        urlOpener.open(url) { _ in completion?() }
    }

    // TODO: Remove space logging
    /// Open an internal deep link into the application
    public func openInternal(string: String, dispatchSpace: PresentSpace? = nil, dispatchEvents: [DispatchType: LoggingEvent] = [:], completion: (()->Void)? = nil)
    {
        guard let url = URL(string: string) else {
            logError("can't parse url string: \(string)")
            return
        }
        openInternal(url: url, dispatchSpace: dispatchSpace, dispatchEvents: dispatchEvents, completion: completion)
    }
    
    // TODO: Remove space logging
    /// Open an internal deep link into the application
    public func openInternal(url: URL, dispatchSpace: PresentSpace? = nil, dispatchEvents: [DispatchType: LoggingEvent] = [:], completion: (()->Void)? = nil)
    {
        logDebug("URLManager: open internal url: \(url)")
        
        //
        // If we recognize the URL pattern and have the necessary data, handle the dispatch without
        // consulting the server.  Else fall through to the resolve service which will return the type
        // and entity data for some types of dispatch.
        //

        // Launch with verification code URL
        if url.path.starts(with: "/v/") {
            logDebug("dispatchUrl: verify url: \(url)")
            logEvent(.app_authorize_with_magic_link)
            service.verify(url: url.absoluteString) { response in
                completion?()
                switch response {
                case let .success(authResponse):
                    logEvent(.app_authorize_with_magic_link_succeeded)
                    self.userManager.setExternalAuthorization(authResponse: authResponse)
                case let .error(err):
                    logEvent(.app_authorize_with_magic_link_failed)
                    logDebug("dispatchUrl: verify url returned error: \(err)")
                }
            }
            return
        }
        
        // Do the tour
        if url.path.starts(with: "/app/tour")
        {
            log("dispatchUrl: show tour (unimplemented)")
            completion?()
            return
        }

        // Open the home page externally
        if url.absoluteString == PresentLinks.presentBase {
            logDebug("dispatchUrl: handling home page.")
            completion?()
            openExternal(url: url)
            return
        }
        
        // Group link
        if url.path.starts(with: "/g/")
        {
            logDebug("dispatchUrl: show group, fragment = \(String(describing: url.fragment))")
            let hash = url.lastPathComponent
            if let group = groupManager.getGroup(byURLHash: hash) {
                logDebug("dispatchUrl: handle group URL locally: \(group.title)")
                logEvent(dispatchEvents[.openCircle], stringProperties: [LoggingKey.circle_id: group.groupToken.uuidString, LoggingKey.space_id: dispatchSpace?.rawValue ?? ""]
                )
                completion?()
                if (url.fragment ?? "") == "requests" {
                    screenPresenter.presentScreen(for: .showGroupMembershipRequests(group))
                } else {
                    screenPresenter.presentScreen(for: .showGroupByGroup(group))
                }
                return
            } else {
                logDebug("dispatchUrl: Group not found in cache.  Loading from server.")
                // fall through to load from the server
            }
        }

        // Category link
        if url.path.starts(with: "/t/") {
            guard let category = url.lastPathComponent.removingPercentEncoding else {
                logError("Can't interpret category: \(url.lastPathComponent)")
                return
            }
            logDebug("dispatchUrl: handle category URL locally: \(category)")
            logEvent(dispatchEvents[.openCategory], stringProperties: [LoggingKey.category_id: category, LoggingKey.space_id: dispatchSpace?.rawValue ?? ""])
            let presentable = Presentable.showCategoryById(category, dispatchSpace)
            completion?()
            screenPresenter.presentScreen(for: presentable)
            return
        }

        // Create Group link
        if url.path.starts(with: "/app/createCircle")
        {
            logDebug("dispatchUrl: create group")
            logEvent(dispatchEvents[.createCircle], stringProperties: [LoggingKey.space_id: dispatchSpace?.rawValue ?? ""])

            var title: String?
            var description: String?
            var categories = Set<String>()
            if let urlc = URLComponents(url: url, resolvingAgainstBaseURL: false),
                let queryItems = urlc.queryItems
            {
                for queryItem in queryItems
                {
                    let name = queryItem.name
                    guard let value = queryItem.value?.removingPercentEncoding else {
                        logError("dispatchUrl: bad query item: \(queryItem)")
                        return
                    }
                    if name == "title" { title = value }
                    if name == "description" { description = value }
                    if name == "category" { categories.insert(value) }
                }
            }
            
            screenPresenter.presentCreateCircle {
                if let title = title {
                    $0.setCircleTitle(title)
                }
                if let description = description {
                    $0.setCircleDescription(description)
                }
                if categories.count > 0 {
                    $0.setCircleCategories(categories: categories)
                }
            }
            completion?()
            return
        }
        
        // Select location link
        if url.path.starts(with: "/app/changeLocation") {
            completion?()
            logEvent(dispatchEvents[.changeLocation], stringProperties: [LoggingKey.space_id: dispatchSpace?.rawValue ?? ""])
            main {
                self.screenPresenter.presentLocationPicker()
            }
            return
        }
        
        // Open add friends
        if url.path.starts(with: "/app/addFriends") {
            completion?()
            main {
                if self.userManager.authorizedToProceedToApp {
                    self.screenPresenter.presentAddFriends()
                } else {
                    self.screenPresenter.presentGetStartedOverlayViewController()
                }
            }
            return
        }
        
        // Open phone signup if not logged in
        if url.path.starts(with: "/app/login") {
            completion?()
            if !self.userManager.authorizedToProceedToApp {
                main {
                    self.screenPresenter.presentPhoneSignupFlow()
                }
            }
            return
        }
        
        // Open link facebook flow if logged in and facebook not already linked
        if url.path.starts(with: "/app/linkFacebook") {
            completion?()
            if self.userManager.authorizedToProceedToApp && !self.userManager.facebookLinked {
                main {
                    self.screenPresenter.pushFacebookOnboardingFlow()
                }
            }
            return
        }
        
        //
        // Unknown link type:  Resolve the URL on the server
        //

        logDebug("dispatchUrl: resolve url on the server: \(url)")
        service.resolve(url: url) { response in
            completion?()
            switch response {
                case let .success(presentable):
                    // Load any required data and show the appropriate screen for the presentable
                    if case .showProfile = presentable {
                        logEvent(.app_deep_link_to_user_profile)
                    }
                    switch presentable {
                        case let .showCategory(categoryResponse, _):
                            logEvent(dispatchEvents[.openCategory], stringProperties: [LoggingKey.category_id: categoryResponse.name, LoggingKey.space_id: dispatchSpace?.rawValue ?? ""])
                        case let .showGroup(groupResponse):
                            logEvent(dispatchEvents[.openCircle], stringProperties: [LoggingKey.circle_id: groupResponse.uuid, LoggingKey.space_id: dispatchSpace?.rawValue ?? ""])
                        case let .showGroupByGroup(group):
                            logEvent(dispatchEvents[.openCircle], stringProperties: [LoggingKey.circle_id: group.groupToken.uuidString, LoggingKey.space_id: dispatchSpace?.rawValue ?? ""])
                        default:
                            break
                    }
                    
                    // The group url resolution doesn't know about the #requests fragment
                    if (url.fragment ?? "") == "requests" {
                        switch presentable {
                            case let .showGroup(groupResponse):
                                self.groupManager.getGroup(from: groupResponse) { group in
                                    self.screenPresenter.presentScreen(for: .showGroupMembershipRequests(group))
                                }
                            case let .showGroupByGroup(group):
                                self.screenPresenter.presentScreen(for: .showGroupMembershipRequests(group))
                            default:
                                self.screenPresenter.presentScreen(for: presentable)
                        }
                    } else {
                        self.screenPresenter.presentScreen(for: presentable)
                    }

                case let .error(err):
                    logDebug("dispatchUrl: resolve url returned error: \(err), opening url externally.")
                    self.openExternal(url: url)
                    break
            }
        }
    }


}
