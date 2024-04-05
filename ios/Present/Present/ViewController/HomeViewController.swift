//
//  HomeViewController.swift
//  Present
//
//  Created by Pat Niemeyer on 4/30/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import MIBadgeButton_Swift
import RxSwift

/// The primary screen container holding the bottom navigation tabs for
/// feed, create, and profile.
/// RootViewController --> HomeViewController
public final class HomeViewController: PresentViewController
{
    // MARK: Properties
    
    @IBOutlet weak var contentView: UIView!
    
    @IBOutlet weak var feedButton: ThemeableButton! {
        didSet {
            feedButton.addTarget { button in self.showFeed() }
        }
    }
    @IBOutlet weak var createButton: ThemeableButton! {
        didSet {
            createButton.addTarget { button in self.showCreateGroup() }
        }
    }
    
    @IBOutlet weak var profileButton: BadgeableProfileButton! {
        didSet {
            profileButton.addTarget(self, action: #selector(showProfile), for: .touchUpInside)
            
            userManager.me.observable.onNext { person in
                self.themeProfileButton()
            }.disposed(by: disposal)
            
            // TODO: Why does referencing groupManager directly here blow up?
            userManager.groupManager.joinedGroupsAndFriendsUnreadCount.onNext { [weak self] count in
                log("joinedGroupsUnreadCountDidUpdate: \(count)")
                self?.profileButton.badgeCount = count
            }.disposed(by: disposal)
        }
    }

    // The child tab view controllers (in tab order)
    public var feedViewController: FeedViewController!
    public var createViewController: GatedCreateGroupViewController!
    public var profileViewController: (UIViewController & ProfileViewControllerConfig)!

    // MARK: Initialization
    
    override public init() {
        super.init()
    }
    
    public required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    // MARK: Public Properties
    
    public var navigationTab = NavigationTab.feed {
        didSet {
            guard oldValue != navigationTab else { return }

            logEvent(type: .view, "HomeViewController presenting tab \(type(of: viewController(for: navigationTab))), dimissing \(type(of: viewController(for: oldValue)))")
            themeNavigationButtons()
            
            let newVC = viewController(for: navigationTab)
            let oldVC = viewController(for: oldValue)
            
            // Manually trigger when appearance transitions (willAppear, didAppear, etc.)
            // See shouldAutomaticallyForwardAppearanceMethods
            oldVC.beginAppearanceTransition(false, animated: false)
            newVC.beginAppearanceTransition(true, animated: false)
            
            showSelectedViewController()
            
            oldVC.endAppearanceTransition()
            newVC.endAppearanceTransition()
        }
    }

    // MARK: UIViewController

    public override func viewDidLoad()
    {
        logDebug("HomeViewController: viewDidLoad")
        super.viewDidLoad()
        view.backgroundColor = .white
        contentView.backgroundColor = .white

        feedViewController = FeedViewController()
        installChild(viewController: feedViewController, in: contentView)
        feedViewController.view.constrainToFillSuperview()
        
        profileViewController = GatedProfileViewController()
            .configurePlaceholder {
                $0.titleText.text = "Your Profile Appears Here"
                $0.subtitleText.text = "Keep track of all your important circles in one place!"
                $0.titleImage.image = #imageLiteral(resourceName: "ProfileImage")
                $0.verifyPhoneNumberButton.isShown = true
                // The "get started" button
                $0.buttonEventMapping[$0.verifyPhoneNumberButton] = .profile_placeholder_tap_get_started
            } .then {
                $0.setBackButtonVisible(false)
                installChild(viewController: $0, in: contentView)
                $0.view.constrainToFillSuperview()
            }
        // Fetch the latest profile for the current user from the user manager
        userManager.me.observable.onNext { person in
            self.profileViewController.apply(person: person)
        }.disposed(by: disposal)
        
        createViewController = GatedCreateGroupViewController()
            .configurePlaceholder {
                $0.isModal = false
            }.then {
                installChild(viewController: $0, in: contentView)
                $0.view.constrainToFillSuperview()
            }

        themeNavigationButtons()
        showSelectedViewController()
    }
    
    // Don't send the tabs our home view appearance lifecycle methods, we'll pass them manually
    public override var shouldAutomaticallyForwardAppearanceMethods: Bool {
        return false
    }
    
    public override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        viewController(for: navigationTab).beginAppearanceTransition(true, animated: animated)
    }
    
    public override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        logDebug("HomeViewController: viewDidAppear, sinceLaunch=\(AppDelegate.sinceLaunch)")
        viewController(for: navigationTab).endAppearanceTransition()
    }

    public override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        
        viewController(for: navigationTab).beginAppearanceTransition(false, animated: animated)
    }
    
    public override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        
        viewController(for: navigationTab).endAppearanceTransition()
    }
    
    // MARK: Private Methods
    
    private func showFeed() {
        navigationTab = .feed
    }
    
    @objc private func showProfile() {
        navigationTab = .profile
    }

    private func showCreateGroup() {
        if createViewController.shouldProceedToCreateGroup {
            screenPresenter.presentCreateCircle()
        } else {
            navigationTab = .create
        }
    }
    
    private func themeNavigationButtons() {
        themeFeedButton()
        themeCreateButton()
        themeProfileButton()
    }

    private func themeFeedButton()
    {
        if navigationTab == .feed {
            feedButton.apply(viewModel: ThemeableButton.ViewModel(title: .none, image: #imageLiteral(resourceName: "HomeIconSelected"), highlightedImage: #imageLiteral(resourceName: "HomeIconSelected")))
        } else {
            feedButton.apply(viewModel: ThemeableButton.ViewModel(title: .none, image: #imageLiteral(resourceName: "HomeIcon"), highlightedImage: #imageLiteral(resourceName: "HomeIcon")))
        }
    }
    
    private func themeCreateButton()
    {
        if navigationTab == .create {
            createButton.apply(viewModel: ThemeableButton.ViewModel(title: .none, image: #imageLiteral(resourceName: "PlusIconSelected"), highlightedImage: #imageLiteral(resourceName: "PlusIconSelected")))
        } else {
            createButton.apply(viewModel: ThemeableButton.ViewModel(title: .none, image: #imageLiteral(resourceName: "PlusIcon"), highlightedImage: #imageLiteral(resourceName: "PlusIcon")))
        }
    }

    private func themeProfileButton()
    {
        // If the user is logged in show the profile photo
        if userManager.authorizedToProceedToApp,
            let url = userManager.me.value?.photoURL
        {
            profileButton.url = url
            profileButton.isSelected = navigationTab == .profile
        } else
        {
            profileButton.url = nil // clear the user image if set
            if navigationTab == .profile {
                profileButton.setImage(#imageLiteral(resourceName: "SmileyIconSelected"), for: .normal)
                profileButton.setImage(#imageLiteral(resourceName: "SmileyIconSelected"), for: .selected)
            } else {
                profileButton.setImage(#imageLiteral(resourceName: "SmileyIcon"), for: .normal)
                profileButton.setImage(#imageLiteral(resourceName: "SmileyIcon"), for: .selected)
            }
        }
    }

    private func showSelectedViewController() {
        // todo: my circles
        let allControllers: [UIViewController] = [feedViewController, createViewController, profileViewController]
        let selectedViewController = viewController(for: navigationTab)
        logDebug("HomeViewController selected view controller for: \(navigationTab) = \(selectedViewController)")
        allControllers.forEach { $0.view.isHidden = $0 != selectedViewController }
    }
    
    // TODO: Replace with a map
    private func viewController(for navigationTab: NavigationTab) -> UIViewController {
        switch navigationTab {
        case .feed:
            return feedViewController
        case .create:
            return createViewController
        case .profile:
            return profileViewController
        }
    }

    // MARK: NavigationTab
    
    public enum NavigationTab {
        case feed
        case create
        case profile
    }
}

public class BadgeableProfileButton: MIBadgeButton, ApplicationServices
{
    let roundView = UIImageView()
    private let disposal = DisposeBag()
    let purple = UIColor(hex: 0x743CCE)
    let black = UIColor(hex: 0x000000)
    let borderWidth: CGFloat = 2.0
    
    public var url: URL? {
        didSet {
            guard let url = url else {
                roundView.image = nil
                return
            }
            guard url != oldValue else { return }
            imageManager.getImage(atURL: url)
                .onSuccess { urlimage in
                    self.roundView.image = urlimage.image
                    self.setImage(nil, for: .normal)
                    self.setImage(nil, for: .selected)
                }.disposed(by: disposal)
        }
    }
    
    override public var isSelected: Bool {
        didSet {
            if isSelected {
                roundView.layer.borderColor = purple.cgColor
                roundView.layer.borderWidth = borderWidth
            } else {
                roundView.layer.borderColor = black.cgColor
                roundView.layer.borderWidth = 1.0
            }
        }
    }
    
    public init()
    {
        super.init(frame: .zero)
        initRoundView()
    }
    
    required public init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        initRoundView()
    }
    
    private func initRoundView() {
        addSubview(roundView)
        roundView.constrainToFillSuperview()
        roundView.clipsToBounds = true
        sendSubview(toBack: roundView)
    }
    
    override public func layoutSubviews() {
        super.layoutSubviews()
        roundView.roundCornersToHeight()
    }
    
    /// Insets that modify the hit target of the button. Negative values expand the hit target.
    public var hitTargetInset = UIEdgeInsets(top: -25.0, left: -25.0, bottom: -25.0, right: -25.0)
    
    public override func hitTest(_ point: CGPoint, with event: UIEvent?) -> UIView? {
        guard !self.isHidden && self.alpha > 0.0 && self.isUserInteractionEnabled else {
            // We can't be hit unless we're visible and user interaction is enabled.
            return nil
        }
        
        let hitRect = CGRect(x: bounds.origin.x + hitTargetInset.left, y: bounds.origin.y + hitTargetInset.top, width: bounds.size.width - hitTargetInset.left - hitTargetInset.right, height: bounds.size.height - hitTargetInset.top - hitTargetInset.bottom)
        
        if hitRect.contains(point) {
            return self
            
        } else {
            return super.hitTest(point, with: event)
        }
    }
}

