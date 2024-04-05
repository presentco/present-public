//
//  AddFriendsViewController.swift
//  Present
//
//  Created by Patrick Niemeyer on 7/13/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import RxSwift

/*
 AddFriends
    AddFriendsTabs
        AddFriendsFromMembers
        AddFriendsFromContacts
 */
class AddFriendsViewController: AddUsersViewController
{
    override func initContent()
    {
        // Set the label above search
        searchLabel.do {
            $0.isHidden = false
            $0.text = "We’ll notify you when friends start circles nearby."
            $0.font = UIFont.presentFont(ofSize: 15, weight: .regular)
            $0.numberOfLines = 0
            $0.textColor = UIColor(hex: 0x666666)
        }
        // Add the tabs
        AddUserTabsViewController<AddFriendsFromFacebookViewController, AddFriendsFromContactsViewController>().do {
            $0.searchText = self.searchText
            installChild(viewController: $0, in: content) {
                $0.constrainToFillSuperview()
            }
        }
    }
}

class AddFriendsOnboardingViewController: AddFriendsViewController, WorkflowStep {
    
    public weak var workflowDelegate: WorkflowStepDelegate?

    override func initContent() {
        super.initContent()
       
        navBar.do {
            $0.backButton.isShown = false
            $0.saveButton.setTitle("Done", for: .normal)
            $0.saveButton.isShown = true
            $0.saveButton.isEnabled = true
            $0.saveButton.rx.tap.bind { [weak self] in
                self?.userManager.userSession.onboardingAddFriendsCompleted = true
                self?.workflowDelegate?.workflowStepComplete()
            }.disposed(by: disposal)
        }
        
    }
}

