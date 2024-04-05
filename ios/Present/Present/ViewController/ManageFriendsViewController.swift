//
//  LocationPickerViewController.swift
//  Present
//
//  Created by Patrick Niemeyer on 6/18/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import PresentProtos
import RxSwift

public class ManageFriendsViewController: SearchTableViewController
{
    let tableViewModel = TableViewModel()
    let emptyView = EmptyView()
    
    var incomingRequestsSection: TableViewModel.Section<PresentIncomingFriendRequestListCell>?
    var friendsSection: TableViewModel.Section<PresentPersonListCell>?

    override public func viewDidLoad()
    {
        initTable() // init the table before we run the config
        super.viewDidLoad()

        navBar.title.text = "Friends"
        navBar.isModal = true
        navBar.rightButton.do {
            $0.isShown = true
            $0.isEnabled = true
            $0.setTitle("Add Friends", for: .normal)
            $0.addTarget { [weak self] _ in
                self?.screenPresenter.presentAddFriends()
            }
        }

        searchBar.placeholder = "Search your friends"
        
        emptyView.do {
            content.addSubview($0)
            $0.constrainToFillSuperview()
            $0.emptyText = "Connect with your friends on Present to join the conversation!"
            $0.showButton = true
            $0.button.titleLabel?.text = "Add Friends"
            $0.button.addTarget(self, action: #selector(launchAddFriends), for: .touchUpInside)
            $0.isHidden = true
            $0.bind(searchTextSource: searchText)
        }
        
        tableViewModel.changeObserver.bind { [weak self] isEmpty in
                self?.tableView.isHidden = isEmpty
                self?.emptyView.isShown = isEmpty
            }.disposed(by: disposal)
    }
    
    @objc public func launchAddFriends() {
        screenPresenter.presentModal(AddFriendsViewController())
    }
    
    private func initTable()
    {
        self.tableView.rowHeight = 60
        self.tableView.delegate = self
        
        tableViewModel.do {
            $0.bind(searchTextSource: searchText)
            tableView.bind(model: $0)
        }
        
        let showProfile = screenPresenter.presentPersonProfile
        let filter = { [](row: Person, text: String) in row.name.lowercased().contains(text.lowercased()) }
        
        // Incoming Friend Requests section
        self.incomingRequestsSection = tableViewModel.addSection(
            cellType: PresentIncomingFriendRequestListCell.self, title: "Friend Requests")
                .reuseIdentifier("incoming")
                .initCell { [weak self] in
                    $0.delegate = self
                }.then { section in
                    section.selected.bind(onNext: showProfile).disposed(by: disposal)
                    section.filter = filter
                }

        // Friends on Present section
        self.friendsSection = tableViewModel.addSection(
            cellType: PresentPersonListCell.self, title: "Friends on Present")
            .applyModel { cell, model in
                cell.showRightButton = false
            }.then { section in
                section.selected.bind(onNext: showProfile).disposed(by: disposal)
                section.filter = filter
            }
        
        bindData()
    }
    
    private func bindData() {
        startActivity()
        personManager.myIncomingFriendRequests.observable()
            .distinctUntilChanged()
            .onNext { [weak self] friendRequests in
                self?.incomingRequestsSection?.items = friendRequests
            }.disposed(by: disposal)
        personManager.myFriends.observable()
            .distinctUntilChanged()
            .onNext{ [weak self] friends in
                self?.endActivity()
                // TESTING
                //var all = [Person]()
                //all.append(contentsOf: friends)
                //all.forEach { $0.name = "TEST: "+$0.name }
                //all.append(contentsOf: friends)
                //log("friends count: \(all.count)")
                //self?.friendsSection?.items = all
                self?.friendsSection?.items = friends
            }.disposed(by: disposal)
    }
    
    override public func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        personManager.refreshFriendData()
    }
    
}

// MARK: UITableViewDelegate

extension ManageFriendsViewController: UITableViewDelegate
{
    /// Handle edit actions (e.g. row delete)
    public func tableView(_ tableView: UITableView, editActionsForRowAt indexPath: IndexPath) -> [UITableViewRowAction]?
    {
        let section = tableViewModel.sectionFor(indexPath)
        
        if section === incomingRequestsSection
        {
            // TODO: Get the friend status in the model
            if let cell = tableViewModel.tableView(tableView, cellForRowAt: indexPath) as? PresentIncomingFriendRequestListCell
            {
                // already accepted
                if cell.friendStatus != .incomingRequest {
                    return []
                }
            }
            
            return [UITableViewRowAction(style: .destructive, title: "Delete")
            { [weak self] action, indexPath in
                self?.removeFriendRequest(indexPath: indexPath)
                }.then {
                    $0.backgroundColor = .lightGray
            }]
        }
        else if section === friendsSection {
            return [UITableViewRowAction(style: .destructive, title: "Delete")
            { [weak self] action, indexPath in
                self?.removeFriend(indexPath: indexPath)
                }.then {
                    $0.backgroundColor = .lightGray
            }]
        }
        else {
            return []
        }
    }
    
    private func userFor(indexPath: IndexPath) -> Person? {
        let section = tableViewModel.sectionFor(indexPath)
        if section === incomingRequestsSection {
            return incomingRequestsSection?.modelForIndex(indexPath.item)
        } else
            if section === friendsSection {
                return friendsSection?.modelForIndex(indexPath.item)
        }
        return nil
    }
    
    // Remove an existing friend or reject a friend request
    // Note: This supports the remove friend by swiping action above.
    // Note: We could just get rid of this and use the button as we do elsewhere.
    private func removeFriendRequest(indexPath: IndexPath)
    {
        guard let user = userFor(indexPath: indexPath) else { return }
        
        confirmDestructiveAction(
            title: "Ignore Friend Request",
            message: "Are you sure you want to ignore the friend request from: \(user.name)?",
            destroyTitle: "Ignore"
        ) {
            self.removeFriendItem(userId: user.id, indexPath: indexPath)
        }
    }
    
    private func removeFriend(indexPath: IndexPath)
    {
        guard let user = userFor(indexPath: indexPath) else { return }

        confirmDestructiveAction(
            title: "Remove Friend",
            message: "Are you sure you want to remove: \(user.name)?",
            destroyTitle: "Remove"
        ) {
            self.removeFriendItem(userId: user.id, indexPath: indexPath)
        }
    }
    
    /// Remove the friend or friend request
    private func removeFriendItem(userId: String, indexPath: IndexPath)
    {
        self.startActivity()
        self.personManager.removeFriend(userId: userId)
            .do {
                self.endActivity()
            }.onCompleted {
                self.tableViewModel.remove(atIndex: indexPath, suppressReload: true)
                self.tableView.deleteRows(at: [indexPath], with: .fade)
            }.onError { _ in
                self.showAcknowledgeAlert(title: "Error", message: "There was an error removing your friend")
            }.disposed(by: self.disposal)
    }
}


/// incoming friend request: override terminology and button behavior
class PresentIncomingFriendRequestListCell: PresentFriendListCell
{
    override func initCell() {
        terms = [
            .add: "Accept",
            .added: "Accepted",
            .requested: ""
        ]
        super.initCell()
    }
    
    override func updateRightButton(status: FriendRelationship)
    {
        super.updateRightButton(status: status)
            switch status {
            case .none, .incomingRequest, .outgoingRequest:
                break
            case .friend, .updating:
                self.rightButton.isEnabled = false
        }
    }
}


