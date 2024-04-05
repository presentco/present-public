//
//  PresentPhoneContactListCell.swift
//  Present
//
//  Created by Patrick Niemeyer on 7/16/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import RxSwift

// Note: Currently used for add friends from contacts. Adding members to circles uses another
// Note: cell that supports batching. We should combine in favor of this one.
class PresentPhoneContactListCell: PresentPersonDataListCell<PhoneContact>, ApplicationServices
{
    public weak var delegate: PresentPhoneContactListCellDelegate?
    
    enum ContactRelationship {
        case updating, none, requested
    }
    
    var friendStatus: ContactRelationship = .none {
        didSet {
            updateRightButton(status: friendStatus)
        }
    }
    
    enum Term { case add, requested }
    
    // Localize
    var terms: [Term:String] = [
        .add: "Add",
        .requested: "Added"
        //.requested: "Requested"
    ]
    
    override func apply(model user: PhoneContact)
    {
        super.apply(model: user)
        self.subtitleLabel.do {
            // Add the phone number subtitle to the friend display if needed to disambiguate multiple phone numbers for the user.
            $0.text = (user.phoneNumbers.count > 1 || user.nameData.fullName.trimmingWhitespace().isEmpty) ? user.phoneNumber.displayString : nil
            $0.isShown = true
        }
        
        let personId = user.id
        personManager.getRelationshipStatus(userId: user.id)
            .onNext{ [weak self] status in
                guard personId == self?.model?.id else { return } // guard stale result
                switch status {
                case .updating:
                    self?.friendStatus = .updating
                case .none:
                    self?.friendStatus = .none
                case .outgoingRequest:
                    self?.friendStatus = .requested
                case .friend, .incomingRequest:
                    // These don't make sense in this context, show spinner
                    self?.friendStatus = .updating
                    break
                }
            }.disposed(by: disposal)
    }
    
    func updateRightButton(status: ContactRelationship)
    {
        //log("update contact button status for user: \(String(describing: model?.nameData.fullName)): to: \(status)")
        showRightButton = true
        rightButton.isEnabled = true
        
        switch status {
        case .updating:
            setButtonLightColor()
            self.rightButton.setTitle("", for: .normal)
            addSpinner()
        case .none:
            setButtonDarkColor()
            self.rightButton.setTitle(terms[.add], for: .normal)
        case .requested:
            setButtonLightColor()
            self.rightButton.setTitle(terms[.requested], for: .normal)
            self.rightButton.isEnabled = false
        }
        if status != .updating { removeSpinner() }
    }
    
    override func rightButtonPressed()
    {
        guard let model = model else {
            log("contact button pressed but no model, ignoring")
            return
        }
        log("contact button pressed for friend: \(model.nameData.fullName), current status: \(friendStatus)")
        rightButton.isEnabled = false
        rightButton.pulse(1.2)
        let requestId = nextId()
        delegate?.addContactAsFriend(phoneContact: model)
            .onSuccess { addContactResult in
                guard self.currentId() == requestId else { return }
                let (friendStatus, person) = addContactResult
                log("addContactAsFriend succeeded, status=\(friendStatus), person=\(person)")
            }.onError { error in
                log("addContactAsFriend error: \(error)")
                guard self.currentId() == requestId else { return }
                self.friendStatus = .none
                self.rightButton.isEnabled = true
                let rootVC = self.rootViewController.currentViewController
                UIAlertController.showAcknowledgeAlert(fromViewController: rootVC, title: "Error", message: "Error in friend request")
            }.neverDisposed()
    }
}

public protocol PresentPhoneContactListCellDelegate: class {
    func addContactAsFriend(phoneContact: PhoneContact) -> Single<(FriendRelationship,Person)>
}

