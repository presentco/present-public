//
//  GroupViewController.swift
//  Present
//
//  Created by Patrick Niemeyer on 7/26/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import PresentProtos
import MessageUI
import RxSwift
import StringStylizer
import MIBadgeButton_Swift

public class CircleViewController : PresentViewController, GroupMessageObserver, NotificationSupressionCapable, CircleDetailViewDelegate, MessagesViewControllerDelegate, EditCircleDelegate
{
    // MARK: Private Properties
    
    private var group: Group?

    // MARK: UI Elements
    
    @IBOutlet weak var navBar: NavBar! {
        didSet {
            navBar.applyCircleViewStyle()
            navBar.backButton.isShown = true
            navBar.backButton.addTarget { [weak self] _ in self?.goBack() }
            navBar.rightButton.do {
                $0.adjustsImageWhenHighlighted = false
                let height: CGFloat = 36
                $0.anchorWidthHeight(widthConstant: height, heightConstant: height)
                $0.roundedCorners = height/2
                $0.clipsToBounds = true
                $0.isEnabled = true
                $0.imageView!.contentMode = .scaleAspectFill
                $0.addTarget { [weak self] _ in self?.detailsPressed() }
            }
            [navBar.title, navBar.subtitle].forEach { view in
                UITapGestureRecognizer().do {
                    view!.isUserInteractionEnabled = true
                    view!.addGestureRecognizer($0)
                    $0.rx.event.bind { [weak self] _ in
                        self?.detailsPressed()
                        }.disposed(by: disposal)
                }
            }
        }
    }
    
    @IBOutlet weak var buttonContainer: UIView!
    
    @IBOutlet weak var joinButtonView: UIView!
    @IBOutlet weak var joinButton: ThemeableButton! {
        didSet {
            joinButton.addTarget { [weak self] _ in self?.joinOrLeavePressed() }
        }
    }
    @IBOutlet weak var joinButtonImage: UIImageView! // rename: Icon
    @IBOutlet weak var joinButtonLabel: UILabel!
    
    // Mute button
    @IBOutlet weak var muteButtonView: UIView!
    @IBOutlet weak var muteButtonImage: UIImageView!
    @IBOutlet weak var muteButtonLabel: UILabel!
    @IBOutlet weak var muteButton: ThemeableButton! {
        didSet {
            muteButton.addTarget { [weak self] _ in self?.mutePressed() }
        }
    }
    
    @IBOutlet weak var addMembersButton: ThemeableButton! {
        didSet {
            addMembersButton.addTarget { [weak self] _ in self?.addMembersPressed() }
        }
    }
    @IBOutlet weak var addMembersButtonIcon: UIImageView!
    @IBOutlet weak var addMembersButtonLabel: UILabel!
    
    // TODO: Really need to encapsulate this into a better reusable component
    var addMembersButtonEnabled: Bool = false {
        didSet {
            LabeledButton(button: addMembersButton, label: addMembersButtonLabel, icon: addMembersButtonIcon).isEnabled = addMembersButtonEnabled
        }
    }
    var joinButtonEnabled: Bool = false {
        didSet {
            LabeledButton(button: joinButton, label: joinButtonLabel, icon: joinButtonImage).isEnabled = joinButtonEnabled
        }
    }
    
    @IBOutlet weak var shareButton: ThemeableButton! {
        didSet {
            shareButton.addTarget { [weak self] _ in self?.sharePressed() }
        }
    }
    
    @IBOutlet weak var detailsButton: MIBadgeButton! {
        didSet {
            detailsButton.rx.tap.bind { [weak self] in
                self?.detailsPressed()
            }.disposed(by: disposal)
            detailsButton.badgeEdgeInsets = UIEdgeInsets(top: 14, left: 0, bottom: 0, right: 5)
        }
    }

    @IBOutlet weak var chatContainer: UIView!

    private let circleDetailViewController  = CircleDetailViewController()
    private var messagesViewController: MessagesViewController?
    
    fileprivate let loadingOverlay = PresentLogoActivityIndicator()
    
    // MARK: UIViewController
    
    public override func viewDidLoad()
    {
        super.viewDidLoad()

        installChild(viewController: loadingOverlay, in: view) {
            $0.sizeToFitSuperview()
        }
    }
    
    public override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        updateGroup()
    }
    
    public override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        logEvent(.circle_view)
    }

    public func configure(withGroup group: Group)
    {
        log("view group: \(group.title), cover=\(String(describing: group.coverPhoto))")
        self.group = group
        
        let messagesViewController = MessagesViewController(userManager: userManager, group: group)
        self.messagesViewController = messagesViewController
        circleDetailViewController.delegate = self

        // Init the messages tab
        installChild(viewController: messagesViewController, in: chatContainer)
        messagesViewController.view.constrainToFill(view: chatContainer)
        messagesViewController.delegate = self
        
        let model = GroupMessagesViewControllerModel(group: group, personPostingService: userManager,
                urlResolvingService: service, screenPresenter: screenPresenter)
        messagesViewController.model = model
        
        updateMessageListeningStatus()
        
        // We decide whether to show the join or mute button once for the view unless
        // the user leaves a joined group.
        // (Don't update immediately upon join, allow them to see the joined status)
        updateJoinMuteButton()

        // We update content once immediately so that it appears before the transition
        updateContent()
        
        // The group modified will always fire once so skip the initial signal
        //log("register group modified listener for: \(group.title), from: \(ObjectIdentifier(self))")
        group.rx.modified.skip(1).onNext { [weak self] in
            log("group modified: \(group.title)")
            self?.updateContent()
        }.disposed(by: disposal)
    }
    
    // Show either the join or mute button based on join status
    // Note: that we don't update this on every change, only at specified times.
    private func updateJoinMuteButton() {
        guard let group = group else { return }
        joinButtonView.isShown = !group.isJoined
        muteButtonView.isShown = !joinButtonView.isShown
    }
    
    private func updateCoverPhoto()
    {
        guard let group = group else { return }
        
        let button = navBar.rightButton

        if let coverPhotoUrl = group.coverPhoto?.absoluteString {
            // Strip server scaling params
            //let url = coverPhotoUrl.replacingOccurrences(of: "=w1080-.*", with: "", options: .regularExpression)
            let url = coverPhotoUrl
            log("circle: \(group.title) cover photo url = \(url)")
            ImageManager.shared.getImage(atURLString: url)
                .onSuccess { urlImage in
                    button.setImage(urlImage.image, for: .normal)
                    button.isShown = true
                }.onError { _ in
                    log("Error loading cover photo for group: \(group.groupToken)")
                }.disposed(by: disposal)
        } else {
            MapSnapshotManager.shared.mapSnapshot(
            at: group.location, size: button.bounds.size, name: group.title, id: group.id) {
                mapSnapshot, id in
                button.setImage(mapSnapshot, for: .normal)
                button.isShown = true
            }
        }
    }
    
    private func updateContent()
    {
        log("update content for group: \(group?.title ?? "")")
        guard let group = group else { return }
        
        messagesViewController?.membershipStatus = group.membershipStatus ?? .none
        navBar.applyGroupTitle(group: group)

        updateBadges()
        updateJoinButton()
        updateMuteButton()
        updateAddMembersButton()
        updateCoverPhoto()
        
        circleDetailViewController.configure {
            $0.configure(withGroup: group)
        }
        
    }
    
    // TODO: Make idempotent or at least encapsulate in group
    private var listeningForMessages = false
    private func updateMessageListeningStatus()
    {
        guard let group = group else { return }
        if !listeningForMessages && group.userAuthorizedToParticipate
        {
            // TODO: didUpdateMessages() is supposed to stop the spinner, but we don't currently have a way
            // TODO: to note failure.  So we will time it out as a backup.
            // TODO: This will go away when converted to Rx.
            loadingOverlay.startAnimating()
            mainAfter(seconds: 5) {
                self.loadingOverlay.stopAnimating()
            }
            _ = group.register(groupMessageObserver: self, observerQueue: .main)
            group.beginListeningForUpdates()
            listeningForMessages = true
        }
    }
    
    private func updateBadges() {
        detailsButton.badgeCount = 0
        guard let group = group else { return }
        if group.owner.isMe {
            detailsButton.badgeCount = group.joinRequests
        }
        detailsButton.setNeedsLayout()
    }
    
    // MARK: MessagesViewControllerDelegate
    
    public func handleJoin(from: MessagesViewController) {
        if userManager.authorizedToProceedToApp {
            join()
        } else {
            screenPresenter.presentGetStartedOverlayViewController()
        }
    }
    
    public func shareLink(from: MessagesViewController) {
        sharePressed()
    }
    
    /// Workaround for having no way to know if we were launched from the web view as part of a join
    /// operation. We display the local data but fetch an update in the background.
    private func updateGroup() {
        guard let group = group else { return }
        userManager.groupManager.getGroup(withToken: group.groupToken, forceRefresh: true) { result in
            if case let .success(group) = result {
                // TODO: We should just call our top level configure again...
                // TODO: Need to insure it doesn't do anything heavy.
                self.circleDetailViewController.configure {
                    $0.configure(withGroup: group)
                }
                self.messagesViewController?.membershipStatus = group.membershipStatus ?? .none
            }
        }
    }

    deinit {
        //log("deinit group view")
        group?.endListeningToUpdates()
    }
    
    private func updateJoinButton()
    {
        guard let group = group else { return }
        let membershipStatus = group.membershipStatus ?? .none
        log("join button: status = \(membershipStatus)")
        let emptyHeart = {
            self.joinButtonImage.image = #imageLiteral(resourceName: "JoinHeartEmpty")
        }
        let filledHeart = {
            self.joinButtonImage.image = #imageLiteral(resourceName: "JoinHeartFilled")
        }
        switch membershipStatus {
        case .none, .rejected:
            emptyHeart()
            joinButtonLabel.text = "Join"
        case .requestedGroupMembershipState:
            filledHeart()
            joinButtonLabel.text = "Requested"
        case .invited:
            emptyHeart()
            joinButtonLabel.text = "Invited"
        case .active:
            filledHeart()
            joinButtonLabel.text = "Joined"
        case .unjoined:
            emptyHeart()
            joinButtonLabel.text = "Rejoin"
        }
        joinButtonLabel.setNeedsLayout()
    }
    
    private func updateMuteButton()
    {
        guard let group = group else { return }
        if group.isMuted {
            muteButtonLabel.text = "Muted"
            muteButtonImage.image = #imageLiteral(resourceName: "MuteIconSelected")
        } else {
            muteButtonLabel.text = "Mute"
            muteButtonImage.image = #imageLiteral(resourceName: "MuteIcon")
        }
    }
    
    private func updateAddMembersButton()
    {
        guard let group = group else { return }
        addMembersButtonEnabled = group.owner.isMe || group.isJoined
    }
    
    // MARK: Button Handling
    
    @objc private func goBack() {
        group?.endListeningToUpdates()
        main {
            self.screenPresenter.goBack()
        }
    }
    
    private func markAsRead() {
        group?.markAllRead()
    }
    
    private func logChatView() {
        guard let group = group else { return }
        logEvent(.circle_chat_view, stringProperties: [LoggingKey.circle_id: group.groupToken.uuidString])
    }
    
    private func logInfoView() {
        guard let group = group else { return }
        logEvent(.circle_view_info, stringProperties: [LoggingKey.circle_id: group.groupToken.uuidString])
    }
    
    
    public func slidingTabBar(_ slidingTabBar: SlidingTabBar, didSelect item: SlidingTabBar.Item, at index: Int) {
        // Unused.
    }
    
    // MARK: Menus
    
    private func mutePressed() {
        guard let group = group else { return }
        // toggle the mute status
        setMuted(on: !group.isMuted)
    }
    
    private func setMuted(on: Bool) {
        group?.mark(asMuted: on)
        updateMuteButton()
    }
    
    private func join()
    {
        guard let group = group else { return }
        logEvent(.circle_chat_join)
        joinButtonEnabled = false
        addJoinSpinner()
        group.joinGroup()
            .do {
                self.removeJoinSpinner()
                self.joinButtonEnabled = true
            }.onSuccess { status in
                log("join group result: \(status)")
                self.view.isUserInteractionEnabled = false
                self.view.layoutIfVisible()
    
                UIView.animate(withDuration: 0.2, animations: {
                    self.messagesViewController?.membershipStatus = status
                    self.view.layoutIfVisible()
                }, completion: { _ in
                    self.view.isUserInteractionEnabled = true
                })
            }.onError { error in
                log("Error joining group: \(error)")
                UIAlertController.showAcknowledgeAlert(
                    fromViewController: self,
                    title: "Error",
                    message: "Error joining group")
            }
            .disposed(by: disposal)
    }
    
    private var joinSpinner: PresentLogoActivityIndicator?
    
    func addJoinSpinner() {
        guard self.joinSpinner == nil else { return }
        //joinButtonImage.isHidden = true
        //joinButtonLabel.isHidden = true
        self.joinSpinner = PresentLogoActivityIndicator().then {
            self.joinButton.addSubview($0.view)
            $0.view.anchorCenter(to: self.joinButton)
            $0.logoSize = 25
            $0.backSize = 31
            $0.startAnimating()
        }
    }
    
    func removeJoinSpinner() {
        guard let joinSpinner = joinSpinner else { return }
        joinSpinner.stopAnimating()
        joinSpinner.view.removeFromSuperview()
        self.joinSpinner = nil
        //joinButtonImage.isHidden = false
        //joinButtonLabel.isHidden = false
    }
    
    // unjoin the group
    public func leave()
    {
        guard let group = group else { return }
        joinButtonEnabled = false
        UIAlertController.showDestructiveAlert(
            fromViewController: self,
            title: "Leave this group?",
            message: "Stop receiving notifications from group: \(group.title)?",
            destroyTitle: "Leave",
            onCancel: {
                self.joinButtonEnabled = true
            },
            onDestroy: {
                self.doLeave()
            }
        )
    }
    
    private func doLeave()
    {
        guard let group = group else { return }
        logEvent(.circle_chat_leave)
        joinButtonEnabled = false
        addJoinSpinner()
        group.leaveGroup()
            .do {
                self.removeJoinSpinner()
                self.joinButtonEnabled = true
                self.updateJoinMuteButton()
            } .onCompleted {
                self.view.isUserInteractionEnabled = false
                self.view.layoutIfVisible()
    
                UIView.animate(withDuration: 0.2, animations: {
                    self.messagesViewController?.membershipStatus = .unjoined
                    self.view.layoutIfVisible()
                }, completion: { _ in
                    self.view.isUserInteractionEnabled = true
                })
            }.disposed(by: disposal)
    }
    
    public func report() {
        guard let group = group else { return }
        logEvent(type: .tap, "Report Circle")
        func reportCircle(asBeing abuse: Group.AbuseReason) {
            logEvent(type: .tap, "Confirm Report Circle")
            group.reportAsAbusive(for: abuse)
            
            let didReportAbuseAlert = UIAlertController(title: NSLocalizedString(
                "GroupViewControllerReportAbusiveCircleAlertTitle",
                tableName: nil,
                bundle: .main,
                value: "Thanks! We’ll look into it ASAP.",
                comment: "Title for alert confirming that the user just reported a circle as abuse"
            ), message: nil, preferredStyle: .alert)
            
            didReportAbuseAlert.addAction(withTitle: NSLocalizedString(
                "GroupViewControllerReportAbusiveCircleOkayButtonTitle",
                tableName: nil,
                bundle: .main,
                value: "Close",
                comment: "Title for button dismissing alert confirming that the user just reported a circle as abuse"
            ))
            
            present(didReportAbuseAlert, animated: true)
        }
        
        let reportCircleTitle = NSLocalizedString(
            "GroupViewControllerReportAbusiveCircleSheetTitle",
            tableName: nil,
            bundle: .main,
            value: "Tell us why you’re reporting this circle so we can help",
            comment: "Title for sheet asking the user why they are reporting a circle as abusive"
        )
        
        let reportAbuseSheet = UIAlertController(title: reportCircleTitle, message: nil, preferredStyle: .actionSheet)
        
        let inappropriateButtonTitle = NSLocalizedString(
            "GroupViewControllerReportAbusiveCircleSheetInapropriateButtonTitle",
            tableName: nil,
            bundle: .main,
            value: "It’s inappropriate",
            comment: "Title for button that will report a circle as abusive due to being inappropriate"
        )
        
        reportAbuseSheet.addAction(withTitle: inappropriateButtonTitle, style: .destructive, handler: { (_) in
            reportCircle(asBeing: .inappropriate)
        })
        
        let spamButtonTitle = NSLocalizedString(
            "GroupViewControllerReportAbusiveCircleSheetSpamButtonTitle",
            tableName: nil,
            bundle: .main,
            value: "It’s spam",
            comment: "Title for button that will report a circle as abusive due to being spam"
        )
        
        reportAbuseSheet.addAction(withTitle: spamButtonTitle, style: .destructive, handler: { (_) in
            reportCircle(asBeing: .spam)
        })
        
        let cancelButtonTitle = NSLocalizedString(
            "GroupViewControllerReportAbusiveCircleSheetCancelButtonTitle",
            tableName: nil,
            bundle: .main,
            value: "Cancel",
            comment: "Title for button that cancel reporting a circle as abusive"
        )
        
        reportAbuseSheet.addAction(withTitle: cancelButtonTitle, style: .cancel) { _ in
            logEvent(type: .tap, "Cancel Report Circle")
        }
        
        present(reportAbuseSheet, animated: true)
    }
    
    public func delete(deletedCompletion: @escaping ()->Void)
    {
        guard let group = group else { return }
        
        logEvent(type: .tap, "Delete Circle")
        let confirmDeleteTitle = NSLocalizedString(
            "GroupViewControllerEditGroupConfirmDeletionAlertTitle",
            tableName: nil,
            bundle: .main,
            value: "Delete Circle",
            comment: "Title of the alert confirming deletion of the current group"
        )
        let confirmDeleteMessage = NSLocalizedString(
            "GroupViewControllerEditGroupConfirmDeletionAlertMessage",
            tableName: nil,
            bundle: .main,
            value: "Are you sure?",
            comment: "Message explaining deleting the group will permanently erase all history and cannot be undone"
        )
        
        let alertController = UIAlertController(title: confirmDeleteTitle, message: confirmDeleteMessage, preferredStyle: .alert)
        
        // Cancel action.
        let cancelButtonTitle = NSLocalizedString(
            "GroupViewControllerEditGroupCancelDeletionButtonTitle",
            tableName: nil,
            bundle: .main,
            value: "Cancel",
            comment: "Title of the button in the delete group confirmation which cancels deletion"
        )
        alertController.addAction(withTitle: cancelButtonTitle, style: .cancel) { _ in
            logEvent(type: .tap, "Cancel Delete Circle")
        }
        
        // Delete the group.
        let proceedWithDeletionTitle = NSLocalizedString(
            "GroupViewControllerEditGroupProceedWithDeletionButtonTitle",
            tableName: nil,
            bundle: .main,
            value: "Yes I'm Sure",
            comment: "Title of the button in the delete group confirmation which proceeds with deletion"
        )
        alertController.addAction(
            UIAlertAction(title: proceedWithDeletionTitle, style: .destructive) { _ in
                logEvent(type: .tap, "Confirm Delete Circle")
                self.goBack()
                self.startActivity()
                self.userManager.groupManager.delete(groupWithToken: group.groupToken) { (_) in
                    self.stopActivity()
                    deletedCompletion()
                    // Nothing to do here.
                }
            }
        )
        
        present(alertController, animated: true)
    }
    
    @objc
    private func edit() {
        guard let group = group else { return }
        logEvent(type: .tap, "Edit Circle")
        screenPresenter.presentEditCircle(forEditing: group, withDelegate: self)
    }
    
    // MARK: GroupMessageObserver
    
    public func didUpdate(messages: [GroupMessage], in group: Group) {
        // Our model is observing message changes. Just process spinner changes here.
        loadingOverlay.stopAnimating()
        logDebug("Group view controller: did update, message count = \(messages.count)")
        updateReadStatus()
    }

    private func updateReadStatus() {
        markAsRead()
    }

    // MARK: NotificationSupressionCapable
    
    public func shouldSuppress(notification: RemoteNotification) -> Bool
    {
        guard let group = group else { return false }
        switch notification {
        case let .updateGroupWithCommentTokens(groupId, _):
            return group.groupToken.uuidString == groupId
            
        case let .updateGroupWithComment(groupComment):
            return group.groupToken == groupComment.groupId
            
        case let .updateGroupWithToken(groupId):
            return group.groupToken.uuidString == groupId
            
        case let .failedToSendGroup(message):
            return message.groupId == group.groupToken
            
        case .userId:
            return false
        }
    }
    
    // MARK: CreateGroupViewDelegate
    
    // Share button on group view screen pressed
    public func sharePressed() {
        shareLink()
    }

    
    func joinOrLeavePressed()
    {
        logEvent(.circle_tap_join)
        guard userManager.authorizedToProceedToApp else {
            return screenPresenter.presentGetStartedOverlayViewController()
        }
        guard let group = group else { return }

        // User cannot leave their own group
        if group.isJoined && group.owner.isMe { return }
        
        let membershipStatus = group.membershipStatus ?? .none
        switch membershipStatus {
            case .none, .rejected, .invited, .unjoined:
                join()
            case .requestedGroupMembershipState, .active:
                leave()
        }
    }
    
    private func addMembersPressed()
    {
        logEvent(.circle_tap_invite_friends)
        guard userManager.authorizedToProceedToApp else {
            return screenPresenter.presentGetStartedOverlayViewController()
        }
        screenPresenter.presentModal(
            AddMembersViewController().then {
                $0.group = self.group
            }
        )
    }
    
    private func detailsPressed() {
        screenPresenter.present(screen: circleDetailViewController, allowRetreat: true, with: .slideInHorizontal)
    }
    
    
    // MARK: EditCircleDelegate
    
    public func groupWasEdited(group: Group) {
        // TODO: Needed? 
        configure(withGroup: group)
    }

    // Offer to share a link with the system share sheet
    public func shareLink() {
        ShareSheetUtil.showShareSheet(from: self, withText: shareText)
    }
    // Used for both link sharing and contact sharing
    private var shareText: String {
        guard let group = group else { return "" }
        return "Join me in the “\(group.title.trimmingWhitespace())” circle on Present! \(group.shareURL)" // Localize
    }
}

public extension CircleViewController
{
    public override func viewDidLayoutSubviews()
    {
        super.viewDidLayoutSubviews()
        
        // Workaround for issue where status bar isn't taken into account in safe area.
        // https://stackoverflow.com/questions/46184197/ios-11-safe-area-layout-guide-backwards-compatibility
        /*
         if #available(iOS 11, *) {
         // safe area constraints already set
         } else {
         navBarTopConstraint.constant = 20
         }*/
    }
}


// TODO: This is used elsewhere
public extension FauxNavigationBar
{
    public func applyGroupViewTheme() {
        title.apply(theme: UILabel.Theme(font: .presentFont(ofSize: 18.0, weight: .semibold), textAlignment: .center))
        title.textColor = UIColor.black
        backButton.setImage(#imageLiteral(resourceName: "LeftChevronGlyph"), for: .normal)
        backgroundColor = UIColor.white
    }
}

extension NavBar {
    func applyCircleViewStyle() {
        self.title.font = UIFont.presentFont(ofSize: 18, weight: .bold)
    }
    func applyGroupTitle(group: Group) {
        self.title.text = group.title
        if let locationName = group.locationName {
            let locationIcon = String.attributedStringFromImage(
                image: #imageLiteral(resourceName: "Location"),
                xoffset: 0, yoffset: -1.0)
            let spacer = String.attributedStringSpacerImage(width: 2.5, height: 10)
            self.subtitle.attributedText = locationIcon + spacer + locationName.stylize().attr
            self.subtitle.isShown = true
            self.subtitle.textColor = UIColor(hex: 0x333333)
        } else {
            self.subtitle.isHidden = true
        }
    }
}

// TODO: Stand-in until we have a proper reusable and IBDesignable button
// TODO: allowing a centered bottom label
// TODO: Really need to encapsulate this into a reusable component
/// Encapsulate a button with associated label and icon
public class LabeledButton
{
    let button: UIButton
    let label: UILabel
    let icon: UIImageView
    
    public init(
        button: UIButton,
        label: UILabel,
        icon: UIImageView
        ) {
        self.button = button
        self.label = label
        self.icon = icon
    }
    
    public var isEnabled: Bool {
        get {
            return button.isEnabled
        }
        set {
            button.isEnabled = newValue
            label.isEnabled = newValue
            icon.alpha = newValue ? 1.0 : 0.5
        }
    }
}
