//
//  PresentFriendListCell.swift
//  Present
//
//  Created by Patrick Niemeyer on 6/21/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import RxSwift

/// A person cell that adds friend relationship management as right button.
/// The terminology of the button can be customized for each state.
class PresentFriendListCell: PresentPersonListCell, ApplicationServices
{
    public weak var delegate: PresentFriendListCellDelegate?
    
    var friendStatus: FriendRelationship = .none {
        didSet {
            updateRightButton(status: friendStatus)
        }
    }
    
    enum Term { case add, added, requested }
    
    // Localize
    var terms: [Term:String] = [
        .add: "Add",
        .added: "Added",
        .requested: "Requested"
    ]
    
    override func apply(model user: Person)
    {
        super.apply(model: user)
        showRightButton = false
        
        let personId = user.id
        personManager.getRelationshipStatus(user)
            .onNext{ [weak self] status in
                //log("friend list cell got friend status for: \(user.id) = \(status)")
                guard personId == self?.model?.id else { return } // guard stale result
                self?.friendStatus = status
            }.disposed(by: disposal)
    }
    
    func updateRightButton(status: FriendRelationship)
    {
        if model?.isMe ?? false {
            showRightButton = false
            return
        }
        
        showRightButton = true
        rightButton.isEnabled = true

        switch status {
        case .updating:
            setButtonLightColor()
            rightButton.setTitle("", for: .normal)
            addSpinner()
            break
        case .none, .incomingRequest:
            setButtonDarkColor()
            self.rightButton.setTitle(terms[.add], for: .normal)
        case .friend:
            setButtonLightColor()
            self.rightButton.setTitle(terms[.added], for: .normal)
        case .outgoingRequest:
            setButtonLightColor()
            self.rightButton.setTitle(terms[.requested], for: .normal)
        }
        
        if status != .updating { removeSpinner() }
    }
    
    override func rightButtonPressed()
    {
        guard let model = model else { return }
        rightButton.isEnabled = false
        let currentFriendStatus = friendStatus
        if currentFriendStatus == .none {
            rightButton.pulse(1.2)
        }
        let personId = model.id
        delegate?.toggleFriendStatus(user: model, fromStatus: currentFriendStatus)
            .do {
                guard model.id == personId else { return } // guard reused cell
                self.rightButton.isEnabled = true
            }.onSuccess { [model] status in
                guard model.id == personId else { return } // guard reused cell
            }.onError { error in
                log("on error: \(error)")
                let rootVC = self.rootViewController.currentViewController
                UIAlertController.showAcknowledgeAlert(fromViewController: rootVC, title: "Error", message: "Error in friend request")
                guard model.id == personId else { return } // guard reused cell
                self.friendStatus = currentFriendStatus
            }.neverDisposed()
    }
}

public protocol PresentFriendListCellDelegate: class {
    func toggleFriendStatus(user: Person, fromStatus: FriendRelationship)->Single<ToggleFriendStatusResult>
}

/// Make all of our view controllers support friend lists automatically
extension PresentViewController: PresentFriendListCellDelegate
{
    /// Change the current friend status:
    ///     - Remove a friend or outgoing friend request
    ///     - Add a new friend, or accept an incoming friend request
    public func toggleFriendStatus(user: Person, fromStatus: FriendRelationship)->Single<ToggleFriendStatusResult>
    {
        switch fromStatus {
            case .updating:
                return Single.error(PresentError.apiError) // invalid state
            case .none, .incomingRequest:
                // Adding
                return doToggleFriendStatus(userId: user.id, fromStatus: fromStatus)
            case .outgoingRequest, .friend:
                // Removing
                let target: String = fromStatus == .friend ? "friend" : "friend request"
                let titleTarget: String = fromStatus == .friend ? "Friend" : "Friend Request"
                return Single.create { observer in
                    self.confirmDestructiveAction(
                        title: "Remove \(titleTarget)",
                        message: "Are you sure you want to remove \(target): \(user.name)?",
                        destroyTitle: "Remove",
                        onCancel: {
                            observer(.success(Single.just(.noChange)))
                        },
                        onDestroy: {
                            observer(.success(self.doToggleFriendStatus(userId: user.id, fromStatus: fromStatus)))
                        }
                        )
                    return Disposables.create()
                }.flatMap { $0 }
        }
    }
    private func doToggleFriendStatus(userId: String, fromStatus: FriendRelationship)->Single<ToggleFriendStatusResult>
    {
        return personManager.toggleFriendStatus(userId: userId, fromStatus: fromStatus)
    }
}

