//
//  NotWhitelistedApproveNotificationPermissionViewController.swift
//
//  Created by Dan Federman on 5/26/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Relativity
import UIKit

public final class WaitForApprovalViewController: UIViewController
{
    @IBOutlet weak var contentView: UIView!
    
    @IBOutlet weak var gradientView: GradientView! {
        didSet {
            gradientView.colors = [UIColor(hex: 0x3023AE), UIColor(hex: 0xC96DD8)]
            gradientView.gradientLayer.startPoint = CGPoint(x:0, y:-0.35)
            gradientView.gradientLayer.endPoint = CGPoint(x:0, y:1.7)
        }
    }
    
    @IBOutlet weak var textView: UILabel! {
        didSet {
            let stringValue = textView.text ?? ""
            setWaitText(stringValue: stringValue)
        }
    }

    @IBOutlet weak var inviteButton: ThemeableButton! {
        didSet {
            inviteButton.addTarget(self, action: #selector(doInvite), for: .touchUpInside)
            inviteButton.titleLabel?.font = UIFont.boldSystemFont(ofSize: 18)
            inviteButton.backgroundColor = UIColor.white
            inviteButton.roundedCorners = inviteButton.bounds.height / 2.0
        }
    }
    
    @IBOutlet weak var facebookButton: UIButton! {
        didSet {
            facebookButton.addTarget(self, action: #selector(doFacebook), for: .touchUpInside)
        }
    }
    
    @IBOutlet weak var instagramButton: UIButton! {
        didSet {
            instagramButton.addTarget(self, action: #selector(doInstagram), for: .touchUpInside)
        }
    }
    
    @IBOutlet weak var twitterButton: UIButton! {
        didSet {
            twitterButton.addTarget(self, action: #selector(doTwitter), for: .touchUpInside)
        }
    }

    public override func viewWillAppear(_ animated: Bool) {
        config?(self)
    }

    public override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        // Start polling at the more frequent interval if we haven't already
        AppDelegate.shared.applicationServices.syncStatusPoller.startPollingAtBlockedUserInterval()
        logEvent(.signup_view_wait_for_approval)
    }

    private let urlOpener: URLOpener
    fileprivate let screenPresenter: RootViewController
    fileprivate var contactsMessageBatchRetained: ContactsMessageBatch! // Required to retain the NSObject outside of the send
    private var config: ((WaitForApprovalViewController)->Void)?

    // MARK: Init

    public init(urlOpener: URLOpener, screenPresenter: RootViewController, config: ((WaitForApprovalViewController)->Void)?=nil) {
        self.urlOpener = urlOpener
        self.screenPresenter = screenPresenter
        self.config = config
        super.init(nibName: nil, bundle: nil)
    }
    
    public required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    // MARK: button handling
    
    @objc func doInvite(){
        shareLink()
    }
    
    @objc func doFacebook() {
        logEvent(.signup_follow_social_link, [LoggingKey.social_media: LoggingValue.facebook])
        urlOpener.open(URL(string: PresentLinks.facebookFollow)!, options: [:], completionHandler: nil)
    }
    
    @objc func doInstagram() {
        logEvent(.signup_follow_social_link, [LoggingKey.social_media: LoggingValue.instagram])
        urlOpener.open(URL(string: PresentLinks.instagramFollow)!, options: [:], completionHandler: nil)
    }
    
    @objc func doTwitter() {
        logEvent(.signup_follow_social_link, [LoggingKey.social_media: LoggingValue.twitter])
        urlOpener.open(URL(string: PresentLinks.twitterFollow)!, options: [:], completionHandler: nil)
    }
    
    // MARK: view prefs
    
    public override var shouldAutorotate: Bool {
        return false
    }
    
    public override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
        return .portrait
    }
    
    public override var preferredStatusBarStyle: UIStatusBarStyle {
        return .lightContent
    }
    
    public override var prefersStatusBarHidden: Bool {
        return true
    }
    
    public func setWaitText(stringValue: String)
    {
        logDebug("WaitForApprovalViewController: set wait text to: \(stringValue)")
        let attrString = NSMutableAttributedString(string: stringValue)
        let style = NSMutableParagraphStyle()
        //style.lineSpacing = 8
        //style.alignment = .left
        let range = NSRange(location: 0, length: stringValue.count)
        attrString.addAttribute(NSAttributedStringKey.paragraphStyle, value: style, range: range)
        attrString.addAttribute(NSAttributedStringKey.font, value: UIFont.presentFont(ofSize: 20, weight: .regular), range: range)
        textView.attributedText = attrString
    }

}

extension WaitForApprovalViewController
{
    // Offer to share a link with the system share sheet
    public func shareLink() {
        ShareSheetUtil.showShareSheet(from: self, withText: shareText)
    }
    
    // Used for both link sharing and contact sharing
    fileprivate var shareText: String {
        return "Hi! I thought you'd like to join me on Present! \(PresentLinks.presentGenericInvite)" // Localize
    }
}






