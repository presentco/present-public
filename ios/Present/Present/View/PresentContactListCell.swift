//
//  PresentContactListCell.swift
//  Present
//
//  Created by Patrick Niemeyer on 8/1/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import PresentProtos
import RxSwift

/// Composes a friend list cell and a phone contact list cell and displays the appropriate one
/// for the phone contact person data.
public class PresentContactListCell: UITableViewCell, TableViewModelCell
{
    public typealias M = PhoneContactPerson
    private var model: PhoneContactPerson?
    
    private let userListCell = PresentFriendListCell()
    public weak var userCellDelegate: PresentFriendListCellDelegate?
    private let contactListCell = PresentPhoneContactListCell()
    public weak var contactCellDelegate: PresentPhoneContactListCellDelegate?
    
    override public required init(style: UITableViewCellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        initCell()
    }
    
    public init() {
        super.init(style: .default, reuseIdentifier: PresentListCellReuseIdentifier);
        initCell()
    }
    
    required public init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        initCell()
    }
    
    override public func prepareForReuse() {
        userListCell.prepareForReuse()
        contactListCell.prepareForReuse()
    }
    
    func initCell() {
        self.selectionStyle = .none
        [userListCell, contactListCell].forEach {
            self.addSubview($0)
            $0.constrainToFillSuperview()
            $0.isHidden = true
        }
        userListCell.terms = [
            .add: "Add",
            .added: "Added",
            .requested: "Added"
            //.requested: "Requested"
        ]
    }
    
    public func apply(model: PhoneContactPerson)
    {
        //log("apply phonecontactperson: \(model.nameData.fullName), hasUser=\(model.user != nil)")
        self.model = model
        
        [userListCell, contactListCell].forEach { $0.isHidden = true }
        if let user = model.user {
            // There is user info for this contact, set up the PresentFriendListCell
            userListCell.do {
                $0.apply(model: user)
                $0.isShown = true
                $0.delegate = userCellDelegate
                
                // PhoneContactPerson prefers the user (server) name over the local (contact)
                // name when available and not empty.
                let contactName = model.nameData.fullName
                $0.titleLabel.text = contactName
                
                // Add the phone number subtitle to the friend display if needed to disambiguate multiple phone numbers for the user.
                // TODO: The contact list cell does the same, consolidate.
                $0.subtitleLabel.do {
                    $0.text = (model.phoneContact.phoneNumbers.count > 1 || contactName.trimmingWhitespace().isEmpty) ? model.phoneContact.phoneNumber.displayString : nil
                    $0.isShown = true
                }
                
                // If there is no phone showing label the type SMS/Member
                if $0.subtitleLabel.text == nil { $0.subtitleLabel.text = "Member" }
            }
        } else {
            // This is a raw phone contact, set up the PresentPhoneContactListCell
            contactListCell.do {
                $0.apply(model: model.phoneContact)
                $0.isShown = true
                $0.delegate = self
                
                // If there is no phone showing label the type SMS/Member
                if $0.subtitleLabel.text == nil { $0.subtitleLabel.text = "SMS" }
            }
        }
    }
}

extension PresentContactListCell: PresentPhoneContactListCellDelegate
{
    public func addContactAsFriend(phoneContact: PhoneContact) -> Single<(FriendRelationship,Person)> {
        guard let contactCellDelegate = contactCellDelegate else {
            return Single.error(PresentError.apiError)
        }
        return contactCellDelegate.addContactAsFriend(phoneContact: phoneContact)
            .map {
                // Capture the new user created by the add phone contact and apply to the model
                guard let model = self.model else { return $0 }
                let (_,user) = $0
                //log("addContactAsFriend applying upgraded user: \(user.name) to model")
                model.user = user
                self.prepareForReuse()
                self.apply(model: model)
                return $0
            }
    }
}
