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

public class CircleMembersViewController: SearchTableViewController
{
    var creatorSection: TableViewModel.Section<PresentFriendListCell>?
    var requestsSection: TableViewModel.Section<PresentMemberRequestListCell>?
    var membersSection: TableViewModel.Section<PresentFriendListCell>?
    
    override public func viewDidLoad()
    {
        initTable() // init the table before we run config
        super.viewDidLoad()
        
        navBar.applyCircleViewStyle()
        searchBar.placeholder = "Search members"

        /*
        navBar.rightButton.do {
            $0.isShown = true
            $0.isEnabled = true
            $0.setTitle("Add Members", for: .normal)
            $0.addTarget { [weak self] _ in
                self?.screenPresenter.presentModal(
                    AddMembersViewController().then {
                        $0.group = self?.group
                    }
                )
            }
        }*/
    }
    
    var group: Group?

    let tableViewModel = TableViewModel() // retained

    private func initTable()
    {
        tableView.do {
            $0.delegate = self
            $0.rowHeight = 60
            $0.bind(model: tableViewModel)
        }

        tableViewModel.bind(searchTextSource: searchText)

        let showProfile = screenPresenter.presentPersonProfile
        let filter = { (row: Person, text: String) in row.name.lowercased().contains(text.lowercased()) }
        
        // Creator (single row) section
        self.creatorSection = tableViewModel.addSection(
            cellType: PresentFriendListCell.self, title: "Creator")
            .initCell { [weak self] in
                $0.delegate = self
            }.then { section in
                section.selected.bind(onNext: showProfile).disposed(by: disposal)
                section.filter = filter
        }

        // Member requests section
        self.requestsSection = tableViewModel.addSection(
            cellType: PresentMemberRequestListCell.self, title: "Member Requests")
            .initCell { [weak self] in
                $0.delegate = self
            }.then { section in
                section.selected.bind(onNext: showProfile).disposed(by: disposal)
                section.filter = filter
            }
        
        // Members section
        self.membersSection = tableViewModel.addSection(
            cellType: PresentFriendListCell.self, title: "Members")
            .initCell { [weak self] in
                $0.delegate = self
            }.then { section in
                section.selected.bind(onNext: showProfile).disposed(by: disposal)
                section.filter = filter
        }
    }
    
    override public func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        fetchData()
    }
    
    public func apply(group: Group) {
        self.group = group
        self.navBar.applyGroupTitle(group: group)
        fetchData()
    }
    
    private func fetchData()
    {
        guard let group = group else { return }

        // creator
        creatorSection?.items = [group.owner]

        // requests
        if group.owner.isMe || group.isJoined {
            service.getMembershipRequests(forGroupId: group.id)
                .onSuccess { requests in
                    //log("membership requests for group: \(group.title), count=\(requests.count)")
                    let people = requests
                        .sorted { $0.timestamp < $1.timestamp }
                        .compactMap { $0.user }
                    self.requestsSection?.items = self.personManager.getPeople(forProtos: people)
                }.disposed(by: disposal)
        }

        // members
        service.getMembers(forGroupId: group.id)
            .onSuccess { people in
                self.membersSection?.items =
                    self.personManager.getPeople(forProtos: people)
                        .filter { !($0.id == group.owner.id) }
            }.disposed(by: disposal)
    }
}

extension CircleMembersViewController: PresentMemberRequestListCellDelegate
{
    // approve a member request
    func addMember(userId: String)->Completable {
        guard let group = group else { return Completable.never() }
        startActivity()
        return service.addMembers(userIds: [userId], toGroupId: group.id)
            .do {
                self.endActivity()
            }.onCompleted {
                guard let index = self.requestsSection?.items.index(where: { $0.id == userId }) else { return }
                let requestsSectionIndex = 1
                let indexPath = IndexPath(item: index, section: requestsSectionIndex)
                self.tableViewModel.remove(atIndex: indexPath, suppressReload: true)
                self.tableView.deleteRows(at: [indexPath], with: .fade)
                group.joinRequests -= 1
            }
    }
    
    func removeMember(userId: String)->Completable {
        guard let group = group else { return Completable.never() }
        startActivity()
        return service.removeMembers(userIds: [userId], fromGroup: group.id)
            .do {
                self.endActivity()
            }
    }
}

protocol PresentMemberRequestListCellDelegate: class {
    func addMember(userId: String)->Completable
    func removeMember(userId: String)->Completable
}

/// A person cell that adds friend relationship management as right button.
/// The terminology of the button can be customized for each state.
class PresentMemberRequestListCell: PresentPersonListCell, ApplicationServices
{
    public weak var delegate: PresentMemberRequestListCellDelegate?
    
    override public func apply(model user: Person) {
        super.apply(model: user)
        updateRightButton()
    }
    
    var approvedStatus = false {
        didSet {
            updateRightButton()
        }
    }
    
    func updateRightButton()
    {
        showRightButton = true
        rightButton.isEnabled = true
        
        if approvedStatus == true {
            self.setButtonLightColor()
            self.rightButton.setTitle("Approved", for: .normal)
            self.rightButton.isEnabled = false
        } else {
            self.setButtonDarkColor()
            self.rightButton.setTitle("Approve", for: .normal)
        }
    }
    
    // TODO: We should move this logic and out of the cell and make it completely driven by the model
    override public func rightButtonPressed() {
        guard let user = model else { return }
        rightButton.isEnabled = false
        rightButton.pulse(1.2)
        let requestId = nextId()
        delegate?.addMember(userId: user.id)
            .onCompleted { [model] in
                guard self.currentId() == requestId else { return }
                guard model == self.model else { return } // gaurd reused cell
                self.approvedStatus = true
            }.onError { _ in
                guard self.currentId() == requestId else { return }
                self.rightButton.isEnabled = true
                let rootVC = self.rootViewController.currentViewController
                UIAlertController.showAcknowledgeAlert(fromViewController: rootVC, title: "Error", message: "Error in friend request")
            }.neverDisposed()
    }
}

// MARK: UITableViewDelegate

extension CircleMembersViewController: UITableViewDelegate
{
    /// Handle edit actions (e.g. row delete) for the requests and members sections
    public func tableView(_ tableView: UITableView, editActionsForRowAt indexPath: IndexPath) -> [UITableViewRowAction]?
    {
        // Only give me controls if I am the owner
        guard group?.owner.isMe ?? false else { return [] }
        
        let section = tableViewModel.sectionFor(indexPath)
        
        if section === requestsSection  {
            return [UITableViewRowAction(style: .destructive, title: "Delete")
                { [weak self] action, indexPath in
                    self?.removeRequest(indexPath: indexPath)
                    }.then {
                        $0.backgroundColor = .lightGray
                    }]
        }
        else if section === membersSection {
            return [UITableViewRowAction(style: .destructive, title: "Delete")
            { [weak self] action, indexPath in
                self?.removeMember(indexPath: indexPath)
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
        if section === requestsSection {
            return requestsSection?.modelForIndex(indexPath.item)
        } else
        if section === membersSection {
            return membersSection?.modelForIndex(indexPath.item)
        }
        return nil
    }
    
    // Remove the request
    // Note: This supports the remove friend by swiping action above.
    // Note: We could just get rid of this and use the button as we do elsewhere.
    private func removeRequest(indexPath: IndexPath)
    {
        guard let user = userFor(indexPath: indexPath) else { return }

        confirmDestructiveAction(
            title: "Ignore Request",
            message: "Are you sure you want to ignore the membership request by: \(user.name)?",
            destroyTitle: "Ignore"
        ) {
            self.removeMemberOrRequest(userId: user.id, indexPath: indexPath)
                .onCompleted {
                    self.group?.joinRequests -= 1
                }.disposed(by: self.disposal)
        }
    }
    
    private func removeMember(indexPath: IndexPath)
    {
        guard let user = userFor(indexPath: indexPath) else { return }

        confirmDestructiveAction(
            title: "Remove Member",
            message: "Are you sure you want to remove member: \(user.name)?",
            destroyTitle: "Remove"
        ) {
            self.removeMemberOrRequest(userId: user.id, indexPath: indexPath)
                .onCompleted {
                    self.group?.joinedCount -= 1
                }.disposed(by: self.disposal)
        }
    }
    
    /// Remove the member or member request
    private func removeMemberOrRequest(userId: String, indexPath: IndexPath)->Completable
    {
        return self.removeMember(userId: userId)
            .onCompleted {
                self.tableViewModel.remove(atIndex: indexPath, suppressReload: true)
                self.tableView.deleteRows(at: [indexPath], with: .fade)
            }.onError { _ in
                self.showAcknowledgeAlert(title: "Error", message: "There was an error removing the member or member request")
            }
    }
}
