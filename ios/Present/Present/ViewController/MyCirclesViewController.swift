//
//  MyCirclesViewController.swift
//  Present
//
//  Created by Patrick Niemeyer on 1/8/16.
//  Copyright © 2016 Present Company. All rights reserved.
//

import Foundation
import CoreGraphics
import PresentProtos
import UIKit

/// Populates list of current user's circles in the profile tab
public final class MyCirclesViewController : CirclesListViewController
{
    
    public override func viewDidLoad()
    {
        super.viewDidLoad()

        userManager.groupManager.joinedGroups.observable
            .distinctUntilChanged()
            .onNext { [weak self] joinedGroups in
                log("joined groups changed")
                let groups = joinedGroups.sorted {
                    $0.lastCommentTime > $1.lastCommentTime
                }
                self?.configure(groups: groups)
            }.disposed(by: disposal)
        
        searchBar.placeholder = "Search my circles"
    }

    public override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        // TODO: Rename this event (no longer under profile)
        logEvent(.profile_view_circles)
    }
    
    public func refreshCircles() {
        userManager.groupManager.refreshJoinedGroups()
    }
}

/// Populates list of current another user's circles in the profile tab
public final class OtherUserCirclesViewController : CirclesListViewController
{
    
    public func configure(user: Person)
    {
        configure { _ in
            // Note: reminder that we are loading *another* user's circles here.
            self.service.getJoinedGroups(forUserId: user.id).onSuccess { response in
                // TODO: why does accessing groupManager directly here cause a loop?
                self.userManager.groupManager.getGroups(from: response.groups) { groups in
                    self.configure(groups: groups)
                        self.view.setNeedsLayout()
                    }
            }.disposed(by: self.disposal)
        }
    }
    
}
