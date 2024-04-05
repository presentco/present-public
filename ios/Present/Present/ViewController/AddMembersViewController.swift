//
//  AddMembersViewController.swift
//  Present
//
//  Created by Patrick Niemeyer on 7/19/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation

protocol AddMembersSelection: class {
    func isSelected(userId: String)->Bool
    func selectMember(person: Person)
    func deselectMember(person: Person)
    func selectMember(phoneContact: PhoneContact)
    func deselectMember(phoneContact: PhoneContact)
}

/*
 AddMembersViewController: AddUsersViewController : SearchViewController
    AddMembersTabsViewController<AddMembersFromFacebookViewController, AddMembersFromContactsViewController>
        AddMembersFromContactsViewController: AddMembersFromPersonDataViewController: AddUsersTableViewController
        AddMembersFromFacebookViewController: AddMembersFromPersonDataViewController: AddUsersTableViewController
*/
/// This class adds members to a group.  It is similar to the add friends view controller but includes
/// batching functionality (a scrolling list of users at the top).
/// This class maintains the selection list of PersonData for the facebook and contacts tabs.
class AddMembersViewController: AddUsersViewController, AddMembersSelection
{
    var group: Group?
    
    // The list of selected users or contacts to be added
    var selectedPeople: [PersonData] = [] {
        didSet {
            updateSelectedPeople()
        }
    }
    
    var selectedPeopleScroller = UserScroller().then {
        $0.imageHeight = 45.0
        $0.stackView.spacing = 18
        //$0.delegate = self
    }
    
    var selectedPeopleScrollerHeight: NSLayoutConstraint! {
        didSet {
            //selectedPeopleScrollerHeight.constant = 0 // initially hidden
            //selectedPeopleScroller.isHidden = true
        }
    }

    override public func viewDidLoad() {
        super.viewDidLoad()
        log("add members to group: \(group?.title ?? "")")
        navBar.do {
            $0.title.text = "Add Members"
            $0.saveButton.setTitle("Done", for: .normal)
            $0.saveButton.isShown = true
            $0.saveButton.addTarget { [weak self] _ in self?.saveMembers() }
        }
    }
    
    // Init the tabs
    override func initContent()
    {
        guard let group = group else { return }
        startActivity()
        service.getMembers(forGroupId: group.id)
            .do {
               self.endActivity()
            }
            .onSuccess { members in
                self.initContent(currentMemberIds: members.map { $0.id })
            }.disposed(by: disposal)
    }
    
    private func initContent(currentMemberIds: [String])
    {
        selectedPeopleScroller.then {
            $0.contentInset = UIEdgeInsets(top: 0, left: 20, bottom: 0, right: 0)
            content.addSubview($0)
            $0.isHidden = true
            }.anchorHeight(constant: 0) { constraint in
                self.selectedPeopleScrollerHeight = constraint
            }.anchor(
                top: content.topAnchor, left: content.leftAnchor, right: content.rightAnchor)
        
        AddMembersTabsViewController()
        .do {
            $0.searchText = self.searchText
            installChild(viewController: $0, in: content) {
                $0.anchor(top: selectedPeopleScroller.bottomAnchor, left: content.leftAnchor, bottom: content.bottomAnchor, right: content.rightAnchor)
            }
            $0.presentFriendsViewController.configureViewController {
                $0.delegate = self
                $0.currentMemberIds = currentMemberIds
            }
            $0.facebookViewController.configureViewController {
                $0.delegate = self
                $0.currentMemberIds = currentMemberIds
            }
            $0.contactsViewController.configureViewController {
                $0.delegate = self
                $0.currentMemberIds = currentMemberIds
            }
        }
    }
    
    private func updateSelectedPeople()
    {
        selectedPeopleScroller.isHidden = selectedPeople.isEmpty
        let desiredHeight: CGFloat = selectedPeople.isEmpty ? 0 : 72.0
        if desiredHeight != selectedPeopleScrollerHeight.constant {
            UIView.animate(withDuration: 0.3) {
                self.selectedPeopleScrollerHeight.constant = desiredHeight
                self.view.layoutIfNeeded()
            }
        }
        
        selectedPeopleScroller.configureUsers(users: selectedPeople)
        let placeholderCount = 6
        if selectedPeople.count < placeholderCount {
            for _ in 0..<(placeholderCount-selectedPeople.count) {
                selectedPeopleScroller.add(placeHolder: #imageLiteral(resourceName: "image-placeholder-empty"))
            }
        }
        updateAddButtonEnabledState()
    }
    
    private func doAdd() {
        guard !selectedPeople.isEmpty else { return }
        //logEvent(type: .tap, "Add Members")
        
        UIResponder.resignAnyResponders()
        // TODO: Add members now
        screenPresenter.goBack()
    }
    
    private func updateAddButtonEnabledState() {
        navBar.saveButton.isEnabled = !selectedPeople.isEmpty
    }
    
    private func saveMembers() {
        guard let group = group else { return }
        let members = selectedPeople.compactMap { $0 as? Person }
        let contacts = selectedPeople.compactMap { $0 as? PhoneContact }
        //log("save members: members: \(members.count), contacts: \(contacts.count)")
        startActivity()
        service.addMembers(
                userIds: members.map { $0.id },
                phoneNumbers: contacts.map { $0.phoneNumber },
                toGroupId: group.id)
            .do {
                self.endActivity()
            }.onCompleted {
                self.screenPresenter.goBack()
            }.onError { _ in
                self.showAcknowledgeAlert(title: "Error", message: "Error adding members") {
                    self.screenPresenter.goBack()
                }
            }.disposed(by: disposal)
    }

    // MARK: AddMembersSelection
    
    func isSelected(userId: String)->Bool {
        return selectedPeople.contains { $0.id == userId }
    }
    
    func selectMember(person user: Person) {
        selectMember(user)
    }
    func deselectMember(person user: Person) {
        deselectMember(user)
    }
    func selectMember(phoneContact user: PhoneContact) {
        selectMember(user)
    }
    func deselectMember(phoneContact user: PhoneContact) {
        deselectMember(user)
    }
    
    func selectMember(_ user: PersonData) {
        //log("select: \(user.id)")
        selectedPeople.append(user)
    }
    func deselectMember(_ user: PersonData) {
        //log("deselect: \(user.id)")
        selectedPeople = selectedPeople.filter { $0.id != user.id }
    }
}


/// Specialize the add users table base class for table views used in the add friends and add members to
/// circles view controllers to contain the member selection delegate.
/// Base contains the table view and a reference to the enclosing container search text observable
public class AddMembersFromPersonDataViewController: AddUsersTableViewController
{
    weak var delegate: AddMembersSelection?
    var currentMemberIds = [String]()
    
    let filter = { (row: PersonData, text: String) in
        return row.nameData.fullName.lowercased().contains(text.lowercased())
    }
    
    override func initTable()
    {
        self.tableView.rowHeight = 60
        self.tableView.separatorStyle = .none
        
        tableViewModel.do {
            $0.bind(searchTextSource: searchText)
            tableView.bind(model: $0)
        }
    }
    
    @objc public func launchAddFriends() {
        screenPresenter.presentModal(AddFriendsViewController())
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
    
    // MARK: AddMemberListCellDelegate (partial)
    
    public func isMember(userId: String) -> Bool {
        return currentMemberIds.contains(userId)
    }
    
    public func isSelected(userId: String) -> Bool {
        return delegate?.isSelected(userId: userId) ?? false
    }
    
}

