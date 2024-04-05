//
//  AddMembersTabsViewController.swift
//  Present
//
//  Created by Patrick Niemeyer on 8/20/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import RxSwift

/// This class adds a third tab (present friends) used by add members to circle
/// (beyond the two tabs shared with add friends: facebook and contacts)
class AddMembersTabsViewController : AddUserTabsViewController<
    AddMembersFromFacebookViewController, AddMembersFromContactsViewController>
{
    lazy var presentFriendsViewController =
        ConditionalViewController<AddMembersFromPresentFriendsViewController>()
            .configurePlaceholder {
                $0.titleText.text = "Welcome to Present!"
                $0.subtitleText.text = "Sign in to connect with your friends."
                $0.titleImage.image = #imageLiteral(resourceName: "ProfileImage")
                $0.verifyPhoneNumberButton.isShown = true
            }
            .initViewController(AddMembersFromPresentFriendsViewController.init)
            .configureViewController {
                $0.searchText = self.searchText
            }.configure { [weak self] cvc in
                var weakCvc = cvc // Unused but seems to work around compiler bug
                self?.userManager.authorizationStatus.onNext { [weak cvc] authStatus in
                    cvc?.showPlaceholder = authStatus != .proceed
                    }.disposed(by: cvc.disposal)
    }
    
    /// Set the desired tabs and return the tab count
    @discardableResult
    override public func initTabs() -> Int
    {
        tabViewControllers = [presentFriendsViewController, contactsViewController, facebookViewController]
        bar.items = [Item(title: "FRIENDS"), Item(title: "CONTACTS"), Item(title: "FACEBOOK")]
        return tabViewControllers.count
    }
}

