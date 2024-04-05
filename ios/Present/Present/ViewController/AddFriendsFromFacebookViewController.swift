//
//  Present
//
//  Created by Patrick Niemeyer on 6/18/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import PresentProtos
import RxSwift

public class AddFriendsFromFacebookViewController: AddUsersTableViewController
{
    var facebookFriendsSection: TableViewModel.Section<PresentFriendListCell>?

    override func initTable()
    {
        self.tableView.rowHeight = 60

        tableViewModel.do {
            $0.bind(searchTextSource: searchText)
            tableView.bind(model: $0)
        }
        
        emptyView.do {
            $0.showButton = false
            $0.emptyText = "No Facebook friends found"
        }
        
        let showProfile = screenPresenter.presentPersonProfile
        let filter = { (row: Person, text: String)->Bool in
            return row.nameData.fullName.lowercased().contains(text.lowercased())
        }
        
        // Facebook Friends On Present section
        self.facebookFriendsSection = tableViewModel.addSection(
            cellType: PresentFriendListCell.self)
            .initCell { [weak self] in
                $0.delegate = self
            }.then { section in
                section.selected.bind(onNext: showProfile).disposed(by: disposal)
                section.filter = filter
            }

        fetchData()
    }
    
    override func fetchData()
    {
        startActivity()
        service.getFacebookFriends()
            .do {
               self.endActivity()
            }.onSuccess { facebookFriends in
                self.facebookFriendsSection?.items = self.personManager.getPeople(forProtos: facebookFriends)
            }.disposed(by: disposal)
    }
}

