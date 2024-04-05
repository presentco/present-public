//
//  AddMembersFromFacebookViewController.swift
//  Present
//
//  Created by Patrick Niemeyer on 7/19/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import PresentProtos
import RxSwift

public class AddMembersFromFacebookViewController: AddMembersFromPersonDataViewController, AddMemberListCellSelection
{
    var facebookFriendsSection: TableViewModel.Section<AddMemberListCell>?

    override func initTable()
    {
        super.initTable()
        
        emptyView.do {
            $0.emptyText = "Your Facebook friends aren't on Present yet. Add them to join the conversation!"
            $0.button.addTarget(self, action: #selector(launchAddFriends), for: .touchUpInside)
        }
        
        self.facebookFriendsSection = tableViewModel.addSection(
            cellType: AddMemberListCell.self)
            .initCell { [weak self] in
                $0.delegate = self
            }.applyModel { cell, model in
                //cell.phoneLabel.isHidden = true
            }.then { section in
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
    
    // MARK: AddMemberListCellDelegate
    
    public func selectMemberListCell(cell: AddMemberListCell) {
        // TODO: Specialize the cell to get rid of the cast.
        guard let person = cell.user as? Person else { return }
        delegate?.selectMember(person: person)
    }
    
    public func deselectMemberListCell(cell: AddMemberListCell) {
        // TODO: Specialize the cell to get rid of the cast.
        guard let person = cell.user as? Person else { return }
        delegate?.deselectMember(person: person)
    }
    
}

