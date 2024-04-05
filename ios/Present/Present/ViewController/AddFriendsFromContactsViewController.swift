//
//  Present
//
//  Created by Patrick Niemeyer on 6/18/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import PresentProtos
import RxSwift
import Then

public class AddFriendsFromContactsViewController: AddUsersTableViewController
{
    // Contacts available for friend request
    var contactsSection: TableViewModel.Section<PresentContactListCell>?
    
    // Contacts which have already been requested are segregated here
    var outgoingMemberRequestsContactsSection: TableViewModel.Section<PresentContactListCell>?
    
    private class AddFriendsFromContactsModel: Then {
        var phoneContacts: [PhoneContactPerson] = []
        var friendRelationshipMap: PersonManager.FriendRelationshipMap = [:]
    }
    
    private var model: AddFriendsFromContactsModel = AddFriendsFromContactsModel() {
        didSet {
            //log("phone contacts"); dump(model.phoneContacts)
            // Contacts section excludes:
            //   0) The current user, 800 numbers, etc. (excluded by the contact manager import)
            //   1) Current friends
            //   2) Already (pending) requested friends
            self.contactsSection?.items = model.phoneContacts.filter {
                guard let user = $0.user, let status = model.friendRelationshipMap[user.id] else { return true }
                switch status {
                    case .none, .updating, .incomingRequest:
                        return true
                    case .friend, .outgoingRequest:
                        return false
                }
            }
            
            // Pending section includes outgoing requested friends
            self.outgoingMemberRequestsContactsSection?.items = model.phoneContacts.filter {
                guard let user = $0.user, let status = model.friendRelationshipMap[user.id] else { return false }
                switch status {
                    case .none, .updating, .incomingRequest, .friend:
                        return false
                    case .outgoingRequest:
                        return true
                    }
                }
        }
    }
    
    override func initTable()
    {
        self.tableView.rowHeight = 60
        
        tableViewModel.do {
            $0.bind(searchTextSource: searchText)
            tableView.bind(model: $0)
        }
        
        emptyView.do {
            $0.showButton = false
            $0.emptyText = "No contacts found"
        }
        
        let showContactPersonProfile = { [weak self] (row: PhoneContactPerson) in
            if let user = row.user, user.member {
                self?.screenPresenter.presentPersonProfile(forPerson: user)
            }
        }
        
        let filter = { (row: PersonData, text: String) in
            row.nameData.fullName.lowercased().contains(text.lowercased())
        }
        
        self.contactsSection = tableViewModel.addSection(
            cellType: PresentContactListCell.self, title: /*"Contacts"*/nil)
            .initCell { [weak self] in
                $0.userCellDelegate = self
                $0.contactCellDelegate = self
            }.then { section in
                section.selected.bind(onNext: showContactPersonProfile).disposed(by: disposal)
                section.filter = filter
        }

        self.outgoingMemberRequestsContactsSection = tableViewModel.addSection(
            cellType: PresentContactListCell.self, title: "Pending")
            .initCell { [weak self] in
                $0.userCellDelegate = self
                $0.contactCellDelegate = self
            }.then { section in
                section.selected.bind(onNext: showContactPersonProfile).disposed(by: disposal)
                section.filter = filter
        }

        fetchData()
    }
    
    override func fetchData()
    {
        startActivity()
        Single.zip(
            contactsManager.getPhoneContacts(),
            personManager.myFriendRelationshipsSingle
        )
            .do {
                self.endActivity()
            }.onSuccess {
                (arg: ([PhoneContactPerson], PersonManager.FriendRelationshipMap)) in
                let (contacts, friendRelationshipMap) = arg
                self.model = AddFriendsFromContactsModel().then {
                    $0.phoneContacts = contacts
                    $0.friendRelationshipMap = friendRelationshipMap
                }
            }.disposed(by: disposal)
    }
    
    override public func viewDidLoad() {
        super.viewDidLoad()
        // TOOD: HACK: Why isn't our first tab getting its viewDidAppear() called?
        self.activityIndicator.viewDidAppear(false)
    }
    override public func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        // TODO: Why isn't this being called when the tab is first shown?
        //log("add from contacts view did appear")
    }
}

extension AddFriendsFromContactsViewController: PresentPhoneContactListCellDelegate
{
    public func addContactAsFriend(phoneContact: PhoneContact) -> Single<(FriendRelationship,Person)> {
        return personManager.addFriend(phoneContact: phoneContact)
    }
}
