//
//  ApplicationIconBadgeManager.swift
//  Present
//
//  Created by Dan Federman on 3/29/17.
//  Pat Niemeyer
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import RxSwift

/// Observe unread counts and apply them to the application icon badge
public final class ApplicationIconBadgeManager
{
    // MARK: Private Properties

    private let applicationIconBadgeSetter: ApplicationIconBadgeSetter = UIApplication.shared

    private var badgeCount: Int {
        set { applicationIconBadgeSetter.applicationIconBadgeNumber = newValue }
        get { return applicationIconBadgeSetter.applicationIconBadgeNumber }
    }

    // MARK: Initialization
    
    public required init(groupManager: GroupManager) {
        groupManager.joinedGroupsAndFriendsUnreadCount
            .observeOn(MainScheduler.instance)
            .onNext {
                self.updateAppBadgeCount(joinedGroupsAndFriendsUnreadCount: $0)
            }.neverDisposed()
    }
    
    // MARK: Private Methods

    /// Combine unread counts and set the app badge
    private func updateAppBadgeCount(joinedGroupsAndFriendsUnreadCount count: Int)
    {
        log("updateAppBadgeCount: unreadJoinedGroupCount=\(count)")
        
        // If the count has changed update the badge
        if badgeCount != count {
            log("updateAppBadgeCount: Changing application badge count from \(badgeCount) to \(count)")
            badgeCount = count
        } else {
            log("updateAppBadgeCount: badge count unchanged")
        }
    }
}

public protocol ApplicationIconBadgeSetter: class {
    var applicationIconBadgeNumber: Int { get set }
}

extension UIApplication: ApplicationIconBadgeSetter { }

