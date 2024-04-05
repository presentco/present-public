//
//  AddMembersFromContactsViewController.swift
//  Present
//
//  Created by Patrick Niemeyer on 7/19/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import PresentProtos
import RxSwift

public class AddMembersFromContactsViewController: AddMembersFromPersonDataViewController, AddMemberListCellSelection
{
    var contactsSection: TableViewModel.Section<AddMemberListCell>?

    override func initTable()
    {
        super.initTable()
        
        emptyView.do {
            $0.showButton = false
            $0.emptyText = "No contacts found."
        }
        
        // Note: This uses the (older) AddMemberListCell for the required batching behavior
        self.contactsSection = tableViewModel.addSection(
            cellType: AddMemberListCell.self)
            .initCell { [weak self] in
                $0.delegate = self
            }.applyModel { cell, model in
                // TODO: Specialize the cell: Move this logic into the cell
                guard let phoneContactPerson = model as? PhoneContactPerson else { return }
                if phoneContactPerson.user == nil ||
                    !self.isMember(userId: phoneContactPerson.user.unwrappedOrFatal().id)
                {
                    cell.phoneLabel.text =
                        (phoneContactPerson.phoneContact.phoneNumbers.count > 1 ||
                            phoneContactPerson.nameData.fullName.trimmingWhitespace().isEmpty) ?
                            phoneContactPerson.phoneContact.phoneNumber.displayString : nil
                }
            }.then { section in
                section.filter = filter
            }
        
        fetchData()
    }
    
    override func fetchData()
    {
        startActivity()
        contactsManager.getPhoneContacts()
            .do {
                self.endActivity()
            }.onSuccess { contacts in
                self.contactsSection?.items = contacts
            }.disposed(by: disposal)
    }
    
    // MARK: AddMemberListCellDelegate
    
    public func selectMemberListCell(cell: AddMemberListCell) {
        // TODO: Specialize the cell to get rid of the cast.
        guard let phoneContactPerson = cell.user as? PhoneContactPerson else { return }
        if let user = phoneContactPerson.user {
            delegate?.selectMember(person: user)
        } else {
            delegate?.selectMember(phoneContact: phoneContactPerson.phoneContact)
        }
    }
    
    public func deselectMemberListCell(cell: AddMemberListCell) {
        // TODO: Specialize the cell to get rid of the cast.
        guard let phoneContactPerson = cell.user as? PhoneContactPerson else { return }
        if let user = phoneContactPerson.user {
            delegate?.deselectMember(person: user)
        } else {
            delegate?.deselectMember(phoneContact: phoneContactPerson.phoneContact)
        }
    }
}
