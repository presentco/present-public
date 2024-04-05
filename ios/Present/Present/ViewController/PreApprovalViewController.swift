//
//  PreApprovalViewController.swift
//  Present
//
//  Created by Patrick Niemeyer on 6/13/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import RxSwift
import Then
import PresentProtos

public class PreApprovalViewController: PresentViewController
{
    public var completion: ((_ selectedPreApproval: Present.GroupMemberPreapproval)->Void)?
    
    public var selectedPreApproval: Present.GroupMemberPreapproval? {
        didSet {
            guard let preApproval = selectedPreApproval else { return }
            if originalPreApproval == nil {
                originalPreApproval = preApproval
            }
            
            loadViewIfNeeded()
            clearChecks()
            switch preApproval {
                case .unknownGroupMemberPreapproval:
                    break
                case .anyone:
                    anyoneCheck.isShown = true
                case .friendsOfMembers:
                    friendsOfMembersCheck.isShown = true
                case .friends:
                    myFriendsCheck.isShown = true
                case .inviteOnly:
                    inviteOnlyCheck.isShown = true
            }
        }
    }
    private func clearChecks()  {
        [anyoneCheck, friendsOfMembersCheck, myFriendsCheck, inviteOnlyCheck].forEach {
            $0?.isHidden = true
        }
    }
    
    public var womenOnly: Bool?
    
    public var originalPreApproval: Present.GroupMemberPreapproval?

    @IBOutlet weak var navBar: NavBar! {
        didSet {
            navBar.do {
                $0.title.text = "Who can join?"
                // TODO: Cancel button on design is on the right
                //$0.cancelButton.isShown = true
                //$0.cancelButton.addTarget { [weak self] _ in self?.cancel() }
                $0.backButton.isShown = true
                $0.backButton.addTarget { [weak self] _ in self?.goBack() }
            }
        }
    }
    
    @IBOutlet weak var anyoneTitle: UILabel!
    @IBOutlet weak var anyoneSubtitle: UILabel!
    @IBOutlet weak var anyoneCheck: UIImageView! {
        didSet {
            UITapGestureRecognizer().do {
                anyoneCheck.superview?.addGestureRecognizer($0)
                $0.rx.event.bind { _ in
                    self.selectedPreApproval = .anyone
                }.disposed(by: disposal)
            }
        }
    }
    
    @IBOutlet weak var friendsOfMembersTitle: UILabel!
    @IBOutlet weak var friendsOfMembersSubtitle: UILabel!
    @IBOutlet weak var friendsOfMembersCheck: UIImageView! {
        didSet {
            UITapGestureRecognizer().do {
                friendsOfMembersCheck.superview?.addGestureRecognizer($0)
                $0.rx.event.bind { _ in
                    self.selectedPreApproval = .friendsOfMembers
                }.disposed(by: disposal)
            }
            
        }
    }
    
    @IBOutlet weak var myFriendsTitle: UILabel!
    @IBOutlet weak var myFriendsSubtitle: UILabel!
    @IBOutlet weak var myFriendsCheck: UIImageView! {
        didSet {
            UITapGestureRecognizer().do {
                myFriendsCheck.superview?.addGestureRecognizer($0)
                $0.rx.event.bind { _ in
                    self.selectedPreApproval = .friends
                }.disposed(by: disposal)
            }
            
        }
    }
    
    @IBOutlet weak var inviteOnlyTitle: UILabel!
    @IBOutlet weak var inviteOnlySubtitle: UILabel!
    @IBOutlet weak var inviteOnlyCheck: UIImageView! {
        didSet {
            UITapGestureRecognizer().do {
                inviteOnlyCheck.superview?.addGestureRecognizer($0)
                $0.rx.event.bind { _ in
                    self.selectedPreApproval = .inviteOnly
                }.disposed(by: disposal)
            }
        }
    }
    
    override public func viewDidLoad() {
        super.viewDidLoad()
        clearChecks()
        selectedPreApproval = originalPreApproval
        
        // At this point, we should probably be using a TableView ... oh well
        let titles = [anyoneTitle, friendsOfMembersTitle, myFriendsTitle, inviteOnlyTitle]
        let subtitles = [anyoneSubtitle, friendsOfMembersSubtitle, myFriendsSubtitle, inviteOnlySubtitle]
        let preapprovalStates = [Present.GroupMemberPreapproval.anyone, Present.GroupMemberPreapproval.friendsOfMembers, Present.GroupMemberPreapproval.friends, Present.GroupMemberPreapproval.inviteOnly]
        for (index, preapproval) in preapprovalStates.enumerated() {
            titles[index]?.text = PrivacyText.getPreApprovalTitle(forPreApproval: preapproval, womenOnly: womenOnly!)
            subtitles[index]?.text = PrivacyText.getPreApprovalDescription(forPreApproval: preapproval, womenOnly: womenOnly!)
            subtitles[index]?.isHidden = preapproval == Present.GroupMemberPreapproval.anyone
        }
    }
        
    private func goBack() {
        if let preApproval = selectedPreApproval {
            completion?(preApproval)
        }
        screenPresenter.goBack()
    }
    
    private func cancel() {
        selectedPreApproval = originalPreApproval
        goBack()
    }
}
