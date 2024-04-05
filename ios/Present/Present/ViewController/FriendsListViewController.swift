//
//  Present
//
//  Created by Patrick Niemeyer on 6/18/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import PresentProtos
import RxSwift

/// Display a single section list of friends. This list is used to populate the "friends" tab when
/// viewing another user's profile, but could be used in other contexts.
/// See ManageFriendsViewController for the current user's friend list.
public class FriendsListViewController: SearchTableViewController, UITableViewDelegate
{
    private let tableViewModel = TableViewModel()
    private var user: Person?
    
    var friendsSection: TableViewModel.Section<PresentFriendListCell>?

    /// Apply the list of friends and optionally specify a title
    public func configure(withTitle title: String = "Friends", withFriends friends: [Person]) {
        configure {
            $0.navBar.title.text = title
            $0.searchBar.placeholder = "Search \(title)"
            $0.initTable()
            $0.friendsSection?.items = friends
        }
    }

    private func initTable()
    {
        self.tableView.rowHeight = 60

        tableViewModel.do {
            $0.bind(searchTextSource: searchText)
            tableView.bind(model: $0)
        }
        
        let showProfile = screenPresenter.presentPersonProfile
        let filter = { (row: Person, text: String) in row.name.lowercased().contains(text.lowercased()) }
        
        self.friendsSection = tableViewModel.addSection(
            cellType: PresentFriendListCell.self)
            .initCell { [weak self] in
                $0.delegate = self
            }.then { section in
                section.selected.bind(onNext: showProfile).disposed(by: disposal)
                section.filter = filter
            }
    }
    
    /// Specify the person who's friends will be displayed
    public func configure(user: Person) {
        self.user = user
        self.configure(withTitle: "Friends of \(user.name)", withFriends: [])
    }
    
    override public func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        guard let user = user else { return }
        startActivity()
        service.getFriends(userId: user.id)
            .do {
                self.endActivity()
            }.onSuccess { users in
                let people = self.personManager.getPeople(forProtos: users)
                if self.friendsSection?.items != people {
                    self.friendsSection?.items = people
                }
            }.disposed(by: disposal)
    }
}
