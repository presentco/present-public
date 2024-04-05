//
//  ProfileViewController.swift
//  Present
//
//  Created by Patrick Niemeyer on 6/6/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import MIBadgeButton_Swift
import PresentProtos
import RxSwift
import Then

public protocol ProfileViewControllerConfig {
    func apply(person: Person)
    func setBackButtonVisible(_ visible: Bool)
}

public class ProfileViewController: PresentViewController, ProfileViewControllerConfig
{
    @IBOutlet weak var mainScrollView: UIScrollView!

    @IBOutlet weak var navBar: NavBar!
    
    @IBOutlet weak var tabsView: UIView!

    @IBOutlet weak var backButton: ThemeableButton! {
        didSet {
            backButton.addTarget { [weak self] _ in self?.goBack() }
        }
    }

    // For "My profile" show my friends is a separate button (not a tab)
    @IBOutlet weak var friendsButton: MIBadgeButton! {
        didSet {
            friendsButton.badgeEdgeInsets = UIEdgeInsets(top: 6, left: 0, bottom: 0, right: 6)
            friendsButton.rx.tap.bind {
                [weak self] in self?.showFriends()
            }.disposed(by: disposal)
            
            personManager.myIncomingFriendRequests.observable().onNext {
                self.friendsButton.badgeCount = $0.count
            }.disposed(by: disposal)
        }
    }
    
    @IBOutlet weak var hamburgerButton: ThemeableButton! {
        didSet {
            hamburgerButton.addTarget { [weak self] _ in self?.showActions() }
        }
    }
    
    @IBOutlet weak var profileImageView: UIImageView! {
        didSet {
            profileImageView.roundCornersToHeight()
            profileImageView.clipsToBounds = true
        }
    }
    
    @IBOutlet weak var bioTextView: ThemeableTextView! {
        didSet {
            bioTextView.isScrollEnabled = false
            bioTextView.dataDetectorTypes = .all
            //bioTextView.delegate = self
        }
    }
    
    @IBOutlet weak var optionsContainerView: UIView!
    
    // "My profile" buttons
    @IBOutlet weak var editProfileButton: ThemeableButton! {
        didSet {
            editProfileButton.addTarget { [weak self] _ in self?.editProfile() }
        }
    }
    @IBOutlet weak var inviteFriendsButton: ThemeableButton! {
        didSet {
            inviteFriendsButton.addTarget { [weak self] _ in self?.inviteFriends() }
        }
    }

    // TODO: add two buttons
    // "Other user profile" buttons
    @IBOutlet weak var startConvoButton: ThemeableButton! {
        didSet {
            startConvoButton.addTarget { [weak self] _ in self?.startConvo() }
        }
    }
    @IBOutlet weak var addFriendButton: ThemeableButton! {
        didSet {
            addFriendButton.addTarget { [weak self] _ in self?.friendButtonPressed() }
        }
    }
    @IBOutlet weak var addFriendButtonImage: UIImageView!
    @IBOutlet weak var addFriendButtonLabel: UILabel!
    
    // Common buttons
    @IBOutlet weak var shareProfileButton: ThemeableButton! {
        didSet {
            shareProfileButton.addTarget { [weak self] _ in self?.shareProfile() }
        }
    }
    @IBOutlet weak var contentAreaSeparator: UIView! {
        didSet {
            contentAreaSeparator.alpha = 0
        }
    }
    
    var user: Person?
    
    private var friendStatus: FriendRelationship = .none {
        didSet {
            applyStyleToFriendButton()
        }
    }
    
    var profileTabsViewController = ProfileTabsViewController()
    
    private let presentRefreshController = PresentRefreshController()
    
    private var contactsMessageBatchRetained: ContactsMessageBatch! // Required to retain the NSObject outside of the send method

    let masterScroller = MasterScroller()
    
    // MARK: ViewController
    
    override public func viewDidLoad()
    {
        super.viewDidLoad()

        navBar.title.do {
            $0.apply(theme: UILabel.Theme(font: .presentFont(ofSize: 24.0, weight: .bold), textAlignment: .center))
            $0.textColor = UIColor.black
        }

        profileImageView.do {
            $0.image = nil
            $0.backgroundColor = Palette.avatarLoadingColor
        }

        profileTabsViewController.do {
            installChild(viewController: $0, in: tabsView) {
                $0.constrainToFillSuperview()
            }
        }
        
        mainScrollView.refreshControl = presentRefreshController.refreshControl
        presentRefreshController.addScrollView(scrollView: mainScrollView)
    }
    
    @objc private func refreshView() {
        // The refresh is triggered by the master scroller's refresh control, so
        // signal our refresh control to begin refreshing.
        presentRefreshController.refreshControl.beginRefreshing()
        presentRefreshController.beginAnimatingPullToRefreshLogo()
        
        // TODO: These need to offer completions to allow us to end refreshing.
        profileTabsViewController.myCirclesViewController.refreshCircles()
        profileTabsViewController.notificationsViewController.refreshNotifications()
        personManager.refreshFriendData()

        // TODO: HACK: End refreshing after 1.5s.
        mainAfter(1.5) {
            // End both refresh controls update
            self.masterScroller.masterRefreshControl.endRefreshing()
            self.presentRefreshController.endRefreshing()
        }
    }
    
    override public func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
    }

    override public func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        if user?.isMe ?? false {
            logEvent(.profile_view_info)
        }
        updateMasterScrollerForSelectedTab()
    }
    
    override public func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        personManager.myIncomingFriendRequests.refreshIfExpired()
        // TODO: Why does referring to groupManager here loop?
        userManager.groupManager.refreshJoinedGroups()
    }
    
    // MARK: ProfileViewControllerConfig
    
    // Note: In the isMe case this may be called before the view appears.
    public func apply(person user: Person)
    {
        // TODO: Workaround for getting incomplete profiles during signup
        // TODO: Generally harmless but we don't want to see them.
        // TODO: Need to isolate the profile update to avoid repeated calls
        guard user.name.trimmingWhitespace().count > 0 else { return }
        
        self.user = user
        log("apply person=\(user.name), thread=\(Thread.current)")
        
        inviteFriendsButton.superview?.isShown = user.isMe
        editProfileButton.superview?.isShown = user.isMe
        startConvoButton.superview?.isShown = !user.isMe
        // NO Messaging in this release: always hide the message button
        startConvoButton.superview?.isShown = false
        addFriendButton.superview?.isShown = !user.isMe
        
        profileTabsViewController.configure(user: user)
        if user.isMe {
            profileTabsViewController.showTabs = [.circles, .notifications, .friends]
        } else {
            profileTabsViewController.showTabs = [.circles, .friends]
            initFriendStatus()
        }
        configureMasterScrolling() // This must happen after configuring the tabs.

        log("user photo: \(user.photoURL)")
        imageManager.getImage(atURL: user.photoURL).onSuccess { [weak self] urlimage in
            self?.profileImageView.image = urlimage.image
        }.disposed(by: disposal)

        navBar.do {
            $0.title.text = user.name
            $0.title.sizeToFit()
            $0.title.setNeedsLayout()
            $0.setNeedsLayout()
        }

        self.bioTextView.text = user.bio
        self.personManager.recentlyViewedProfiles.insert(user)
    }
    
    private func initFriendStatus() {
        guard let user = user else { return }
        personManager.getRelationshipStatus(user)
            .onNext { status in
                self.friendStatus = status
            }.disposed(by: disposal)
    }
    
    public func setBackButtonVisible(_ visible: Bool) {
        backButton.isShown = visible
        //friendsButton.isShown = !backButton.isShown
    }
    
    // MARK: Button Handling
    
    /// Dismiss (for the modal case)
    private func goBack() {
        screenPresenter.goBack()
    }
    
    @objc private func showFriends() {
        screenPresenter.presentModal(ManageFriendsViewController())
    }
    
    private func showActions()
    {
        guard let user = user else { return }
        if user.isMe {
            showMyProfileActions()
        } else {
            showOtherUserProfileActions()
        }
    }

    private func startConvo()
    {
        logDebug("message compose")
        // Note: We don't have a completion callback for screen presenter (yet) but it
        // will queue the two transitions and peform them in order.
        //goBack()
        //self.activityScreenPresenter.presentChatMessagesViewController(...
    }
    
    private func editProfile() {
        screenPresenter.presentEditProfile()
    }
    
    private func inviteFriends() {
        screenPresenter.presentAddFriends()
    }
    
    private func shareProfile() {
        guard let text = shareText else { return }
        ShareSheetUtil.showShareSheet(from: self, withText: text)
    }
    
    private func transitionUserState()
    {
        guard let user = user else { return }
        startActivity()
        service.getValidStateTransitions(forUser: user.id)
            .do { [weak self] in
               self?.stopActivity()
            }.onSuccess { [weak self] transitions in
                self?.transitionUserState(withOptions: transitions)
            }.disposed(by: disposal)
    }
    
    private func transitionUserState(withOptions transitions: [Present.ValidStateTransitionResponse])
    {
        let cancelString = NSLocalizedString( "PersonProfileViewControllerCancelButtonTitle", tableName: nil, bundle: .main, value: "Cancel", comment: "Title for button dismissing sheet.")
        let alertController = UIAlertController(title: nil, message: nil, preferredStyle: .actionSheet)
        
        func confirmAndPerformTransition(_ transition: Present.ValidStateTransitionResponse) {
            guard let user = user else { return }
            confirmDestructiveAction(
                title: "Change User State",
                message: "Are you sure you want to \(transition.verb.capitalized) \(user.name)",
                onDestroy: { [weak self] in
                    guard let sself = self else { return }
                    logx("do transition: \(transition.verb)")
                    sself.startActivity()
                    sself.service.transitionState(forUser: user.id, toState: transition.id)
                        .onCompleted { [weak self] in
                            self?.stopActivity()
                        }.disposed(by: sself.disposal)
                }
            )
        }

        // User state options
        transitions.forEach { transition in
            alertController.addAction(withTitle: transition.verb.capitalized, style: .destructive) { _ in
                confirmAndPerformTransition(transition)
            }
        }
        
        // Cancel
        alertController.addAction(withTitle: NSLocalizedString(
            "PersonProfileViewControllerCancelButtonTitle",
            tableName: nil, bundle: .main, value: "Cancel",
            comment: "Title for button dismissing sheet."
        ), style: .cancel)

        present(alertController, animated: true, completion: nil)
    }

    private func friendButtonPressed()
    {
        guard let user = user else { return }
        addFriendButton.isEnabled = false
        startActivity()
        personManager.toggleFriendStatus(userId: user.id, fromStatus: friendStatus)
            .do {
                self.endActivity()
                self.addFriendButton.isEnabled = true
            }.onSuccess { status in
                //self.friendStatus = status
                self.addFriendButtonImage.pulse(1.2)
            }.onError { _ in
                self.showAcknowledgeAlert(title: "Error", message: "Error updating friend status")
            }.disposed(by: disposal)
    }
    
    // addFriend button style // Localize
    func applyStyleToFriendButton()
    {
        addFriendButton.isEnabled = true
        switch friendStatus {
            case .updating:
                addFriendButtonImage.image = #imageLiteral(resourceName: "AddFriend")
                addFriendButtonLabel.text = "(updating)"
                addFriendButton.isEnabled = false
            case .friend:
                addFriendButtonImage.image = #imageLiteral(resourceName: "Added")
                addFriendButtonLabel.text = "Added"
            case .none, .incomingRequest:
                addFriendButtonImage.image = #imageLiteral(resourceName: "AddFriend")
                addFriendButtonLabel.text = "Add Friend"
            case .outgoingRequest:
                addFriendButtonImage.image = #imageLiteral(resourceName: "Added")
                addFriendButtonLabel.text = "Requested"
        }
    }
}

// MARK: Master scrolling

extension ProfileViewController
{
    /// Set up as single scrolling control that governs the info area and child scrollviews in the tabs.
    /// The basic idea here is that we have an invisible master scrollview that drives all of the
    /// content interaction including scrolling the subordinate table views within the tabs.
    /// The logic is relatively self contained here, but it's messy because it requires knowledge of
    /// the subordinate views that will be involved including knowing when their content size changes.
    private func configureMasterScrolling()
    {
        mainScrollView.isScrollEnabled = false
        masterScroller.target = self.view

        // Drive changes from our master (offscreen) scroller
        masterScroller.rx.contentOffset.changed.bind { offset in
            let maxY = self.tabsView.frame.origin.y // maxY may change on edit
            // Adjust main scrollview up to maxY
            self.mainScrollView.contentOffset.y = min(offset.y, maxY)
            // Adjust child scrollview with residual, adjusted for tab inset
            self.currentChildTableView?.contentOffset.y =
                max(0,offset.y-maxY)
                - (self.currentChildTableView?.contentInset.top ?? 0)

            // Adjust the image size and button transparency as it rolls off
            var infoFrac = max(0, maxY - self.mainScrollView.contentOffset.y)/maxY
            infoFrac = min(1.2, infoFrac)
            self.profileImageView.transform = .init(scaleX: infoFrac, y: infoFrac)
            self.optionsContainerView.alpha = infoFrac * infoFrac
            self.updateContentAreaSeparator(y: offset.y, maxY: maxY)
            
            self.view.endEditing(false) // dismiss any keyboard showing
        }.disposed(by: disposal)
        
        // Watch for tab changes, update virtual height and reset scroll position
        // TODO: Remember scroll position per tab?
        profileTabsViewController.selectedTab.onNext { _ in
            let maxY = self.tabsView.frame.origin.y
            self.updateMasterScrollerForSelectedTab()
            // Reset the master scroller position
            self.masterScroller.contentOffset.y = min(maxY, self.masterScroller.contentOffset.y)
            // Reset the child to the tabs inset top
            main {
            self.currentChildTableView?.setContentOffset(
                CGPoint(x:0, y:-(self.currentChildTableView?.contentInset.top ?? 0)), animated: true)
            }
        }.disposed(by: disposal)
        
        configureObserveContent()
        
        // The master scroller's refresh control is the one that actually fires the refresh
        masterScroller.masterRefreshControl.addTarget(self, action: #selector(refreshView), for: .valueChanged)
    }

    // TODO: We've used this twice, next time make a util
    /// show the content area separator when info area is in partial scroll
    private func updateContentAreaSeparator(y: CGFloat, maxY: CGFloat) {
        let steps: CGFloat = 50
        let frac = min(steps, min(y, abs(maxY-min(y,maxY))))/steps // normalized 0-1 distance from edges
        self.contentAreaSeparator.alpha = frac
    }

    /// Observe the child table views for size changes (ug).
    private func configureObserveContent() {
        let watch = { (name: String, tableView: UITableView) in
            tableView.rx.observeWeakly(CGSize.self, "contentSize").onNext { size in
                //log("tab: \(name) content size changed to: \(String(describing: size))")
                main {
                    self.updateMasterScrollerForSelectedTab()
                }
                }.disposed(by: self.disposal)
        }
        profileTabsViewController.myCirclesViewController.configure { watch("circles", $0.tableView) }
        profileTabsViewController.otherUserCirclesViewController.configure { watch("otherusercircles", $0.tableView) }
        if let friendsViewController = profileTabsViewController.friendsViewController {
            friendsViewController.configure { watch("friends", $0.tableView) }
        } else {
            logError("master scroller configured friends view controller is nil")
        }
        profileTabsViewController.notificationsViewController.do { watch("notifs", $0.tableView) }
    }

    /// This property has the knowledge of every contained child tableview.
    private var currentChildTableView: UITableView? {
        //log("current view controller table view = \(profileTabsViewController.currentChildTableView)")
        return profileTabsViewController.currentChildTableView
    }
    
    // Update the height of the virtual scroll area for the selected tab
    func updateMasterScrollerForSelectedTab() {
        guard let currentChildTableView = self.currentChildTableView else { return }
        currentChildTableView.isScrollEnabled = false
        // Note: contentSize does not match our calculated height.  Why?
        let sections = currentChildTableView.numberOfSections
        var height: CGFloat = 0
        for section in 0..<sections {
            // Try the delegate for the row height
            var rowHeight: CGFloat = 0
            if let delegate = currentChildTableView.delegate {
                rowHeight = delegate.tableView!(currentChildTableView, heightForRowAt: IndexPath(item: 0, section: 0))
            }
            // Then the table view's default row height
            if rowHeight <= 0 {
                rowHeight = currentChildTableView.rowHeight
            }
            height += CGFloat(currentChildTableView.numberOfRows(inSection: section)) * rowHeight
        }
        
        // Top of the tabs container
        let tabsContainerStart = tabsView.frame.origin.y
        // Top of the tableview within the tabs container
        let tableViewStart =
            tabsView.convert(currentChildTableView.frame, from: currentChildTableView.superview).origin.y

        let tabsY = tabsContainerStart + tableViewStart
        // Height to tab area plus scrollable or at least tabs plus screen
        masterScroller.height = max(tabsY + CGFloat(height) + 80, tabsY + view.bounds.height)
        //log("update master scroller for tab: \(name), height: \(masterScroller.height)")
    }
}

// MARK: Actions (hamburger menu)

extension ProfileViewController
{
    /// Show the options when viewing my own profile
    private func showMyProfileActions()
    {
        let alertController = UIAlertController(title: nil, message: nil, preferredStyle: .actionSheet)
        
        // Get support
        let supportText = NSLocalizedString( "profileViewControllerSupport", tableName: nil, bundle: .main, value: "Get Support", comment: "Title of the button to get support")
        alertController.addAction(withTitle: supportText) { _ in
            self.getSupport()
        }
        alertController.addAction(withTitle: NSLocalizedString(
            "PersonProfileViewControllerCancelButtonTitle",
            tableName: nil, bundle: .main, value: "Cancel",
            comment: "Title for button dismissing sheet."
        ), style: .cancel)
        
        present(alertController, animated: true, completion: nil)
    }
    
    private func getSupport() {
        URLManager.shared.openExternal(url: PresentLinks.presentSupportUrl)
    }
    
    /// Show the options when viewing another user's profile
    private func showOtherUserProfileActions()
    {
        guard let user = user else { return }
        let alertController = UIAlertController(title: nil, message: nil, preferredStyle: .actionSheet)
        
        // Add the share acction
        let presentMessageComposeButton = NSLocalizedString( "profileViewControllerEditGroupMenuEditTitle", tableName: nil, bundle: .main, value: "Share", comment: "Title of the button to share the user linkto another user")

        alertController.addActionWithTitle(title: presentMessageComposeButton) {
            [weak self] _ in self?.shareProfile()
        }

        // Add the block action
        
        alertController.addAction(withTitle: NSLocalizedString(
            "PersonProfileViewControllerBlockPersonAlertTitle",
            tableName: nil,
            bundle: .main,
            value: "Block \(user.name)",
            comment: "Title for button that blocks another customer."
        ), style: .destructive) { (_) in
            // TODO: Change to user confirmDestructiveAction()
            let confirmBlockPersonAlert = UIAlertController(title: String(format: NSLocalizedString(
                "PersonProfileViewControllerConfirmBlockPersonAlertTitleFormat",
                tableName: nil,
                bundle: .main,
                value: "Are you sure you want to block %@?",
                comment: "Title for alert confirming that we want to block a person. arg0: person name."
            ), user.name), message: NSLocalizedString(
                "PersonProfileViewControllerConfirmBlockPersonAlertMessage",
                tableName: nil,
                bundle: .main,
                value: "Blocking will hide all content created by her.",
                comment: "Message for alert confirming that we want to block a person."
            ), preferredStyle: .alert)
            
            confirmBlockPersonAlert.addActionWithTitle(title: NSLocalizedString(
                "PersonProfileViewControllerConfirmBlockPersonAlertButtonTitle",
                tableName: nil,
                bundle: .main,
                value: "Block",
                comment: "Button that confirms blocking a person"
            ), style: .destructive) { (_) in
                self.blockUser(user: user)
            }
            
            confirmBlockPersonAlert.addAction(withTitle: NSLocalizedString(
                "PersonProfileViewControllerCancelBlockPersonAlertButtonTitle",
                tableName: nil,
                bundle: .main,
                value: "Cancel",
                comment: "Button that cancels blocking a person"
            ), style: .cancel)
            
            self.present(confirmBlockPersonAlert, animated: true, completion: nil)
        }
        
        // Transition user state action (admins)
        
        if userManager.userIsAdmin {
            let transitionUserStateTitle = "Change User State"
            alertController.addActionWithTitle(title: transitionUserStateTitle, style: .destructive) {
                [weak self] _ in self?.transitionUserState()
            }
        }
        
        // Cancel action
        
        alertController.addAction(withTitle: NSLocalizedString(
            "PersonProfileViewControllerBlockPersonAlertCancelButtonTitle",
            tableName: nil,
            bundle: .main,
            value: "Cancel",
            comment: "Title for button dismissing sheet allowing a customer to block another customer."
        ), style: .cancel)
        
        present(alertController, animated: true, completion: nil)
    }
    
    private func blockUser(user: Person) {
        userManager.blockUser(withToken: user.id) { response in
            switch response {
                case .success:
                    break
                case .error:
                    UIAlertController.showAcknowledgeAlert(fromViewController: self, title: "Error", message: "There was an error blocking the user.")
            }

            // Dismiss the model (other user) profile view to get rid of this user
            self.goBack()
        }
    }
}

// MARK: Sharing

extension ProfileViewController
{
    var shareText: String? {
        guard let user = user else { return nil }
        let userName = user.id == userManager.me.value?.id ? "me" : "\(user.name)"
        let userLink = user.link ?? ""
        return "Join \(userName) on Present! \(userLink)"
    }
}

class MasterScroller: UIScrollView
{
    let masterRefreshControl = UIRefreshControl()
    
    var target: UIView? {
        didSet {
            guard let target = target else { return }
            target.addSubview(self)
            target.addGestureRecognizer(self.panGestureRecognizer)
            self.removeGestureRecognizer(self.panGestureRecognizer)
            self.bouncesZoom = false
            // This fixes an issue where the first touch was not being delivered after scrolling
            self.panGestureRecognizer.delaysTouchesBegan = true
            
            self.refreshControl = masterRefreshControl
        }
    }
    
    var height: CGFloat = 0 {
        didSet {
            self.frame = CGRect(x:-1, y: 0, width: 1, height: target?.bounds.height ?? 0)
            self.contentSize = CGSize(width: 1, height: height)
        }
    }
}

