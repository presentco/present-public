//
//  AppDelegate+URLDispatch.swift
//  Present
//
//  Created by Patrick Niemeyer on 2/13/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import PresentProtos

// TODO: Separate from AppDelegate and move this into a URLManager
public extension AppDelegate
{
    // Application was launched with either a universal URL or from a local URL Scheme.  Show the desired screen.
    public func dispatchUrl(url: URL, completion: (()->Void)? = nil)
    {
        logDebug("AppDelegate: resolve url: \(url)")

        //
        // If we recognize the URL pattern handle it directly
        //

        // Category
        if url.path.starts(with: "/t/") {
            guard let category = url.lastPathComponent.removingPercentEncoding else {
                logError("Can't interpret category: \(url.lastPathComponent)")
                return
            }
            logDebug("dispatchUrl: handle category URL locally: \(category)")
            let presentable = Presentable.showCategoryById(category)
            completion?()
            self.screenPresenter.presentScreen(for: presentable)
            return
        }

        // Group
        if url.path.starts(with: "/g/") {
            let hash = url.lastPathComponent
            if let group = userManager.groupManager.getGroup(byURLHash: hash) {
                logDebug("dispatchUrl: handle group URL locally: \(group.title)")
                let presentable = Presentable.showGroupByGroup(group)
                completion?()
                self.screenPresenter.presentScreen(for: presentable)
                return
            }
        }

        // Create Group
        if url.path.starts(with: "/app/createCircle")
        {
            logDebug("dispatchUrl: create group")
            
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
                        logError("bad query item: \(queryItem)")
                        return
                    }
                    if name == "title" { title = value }
                    if name == "description" { description = value }
                    if name == "category" { categories.insert(value) }
                }
            }

            screenPresenter.presentCreateGroup {
                if let title = title {
                    $0.setGroupTitle(title)
                }
                if let description = description {
                    $0.setGroupDescription(description)
                }
                if categories.count > 0 {
                    $0.setCategories(categories: categories)
                }
            }
            completion?()
            return
        }

        logDebug("dispatchUrl: resolve url on the server: \(url)")
        // Resolve the URL on the server
        service.resolve(url: url) { response in
            completion?()
            switch response {
            case let .success(presentable):
                // Load any required data and show the appropriate screen for the presentable
                self.screenPresenter.presentScreen(for: presentable)

            case .error:
                // Nothing to do here.
                break
            }
        }

    }
    
    public func dispatchUrl(string: String, completion: (()->Void)? = nil)
    {
        guard let url = URL(string: string) else {
            logError("can't parse url string: \(string)")
            return
        }
        dispatchUrl(url: url, completion: completion)
    }
}

