//
//  ProfileTabsViewController.swift
//  Present
//
//  Created by Patrick Niemeyer on 7/10/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import PresentProtos
import RxSwift
import Then
import Tabman
import Pageboy
import StringStylizer

// TODO: Rebase on PresentTabsViewController
public class ProfileTabsViewController: TabmanViewController, PageboyViewControllerDataSource, ApplicationServices, TabmanBarDelegate
{
    private let disposal = DisposeBag()
    
    enum Tab {
        case circles, notifications, friends
    }
    
    // Tabs to display
    var showTabs: [Tab] = [.circles, .notifications, .friends] {
        didSet {
            // Don't call update until after the initial setup
            guard !(self.bar.items ?? []).isEmpty else { return }
            refreshTabs()
        }
    }
    
    // The displayed view controllers for the tabs
    var tabViewControllers = [UIViewController]()
    
    public let selectedTab = BehaviorSubject<Int>(value: 0)
    
    private var user: Person?
    
    // MARK: Tab view controllers
    
    lazy var myCirclesViewController = MyCirclesViewController()
    
    lazy var otherUserCirclesViewController = OtherUserCirclesViewController()
    
    lazy var notificationsViewController =
        NotificationsViewController(activityVendor: activityManager, imageManager: imageManager, screenPresenter: screenPresenter)

    // This will be either the ManageFriendsViewController or FriendsListViewController
    var friendsViewController: SearchTableViewController?

    // Latest Badge counts
    
    var circlesBadgeCount = 0
    var friendsBadgeCount = 0
    var activityBadgeCount = 0
    
    // MARK: UIViewController

    override public func viewDidLoad()
    {
        // Note: Default true, necessary to inset content below the tabs
        // header view. Must be done before other setup.
        // Note: We are having lots of problems with this so I have turned it
        // off and set the insets manually for now.
        self.automaticallyAdjustsChildViewInsets = false
        
        super.viewDidLoad()
        self.view.backgroundColor = .white
        
        dataSource = self
        self.isScrollEnabled = false
        PresentTabs.configurePresentTabs(bar: bar)
        self.bar.delegate = self
        transition = Transition(style: .push, duration: 0.2)
        
        updateTabBadges()
    }
    
    override public func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        view.setNeedsLayout()
        activityManager.fetchActivity()
        updateTabBadges()
    }
    
    public func configure(user: Person)
    {
        guard user != self.user else { return }
        self.user = user
        
        if user.isMe {
            friendsViewController = ManageFriendsViewController()

            // Monitor badges for my circles
            // TODO: Why does referencing the group manager directly here cause a loop?
            userManager.groupManager.joinedGroupsUnreadCount
                .distinctUntilChanged()
                .onNext { [weak self] count in
                    self?.circlesBadgeCount = count
                    self?.updateTabBadges()
            }.disposed(by: disposal)
            
            // Monitor badges for my notifications
            activityManager.unreadActivityCount
                .distinctUntilChanged()
                .onNext { [weak self] count in
                    self?.activityBadgeCount = count
                    self?.updateTabBadges()
            }.disposed(by: disposal)

            // Monitor badges for my friend requests
            personManager.myIncomingFriendRequests.observable()
                .distinctUntilChanged()
                .onNext { [weak self] requests in
                    self?.friendsBadgeCount = requests.count
                    self?.updateTabBadges()
                }.disposed(by: disposal)

        } else {
            otherUserCirclesViewController.configure(user: user)
            friendsViewController = FriendsListViewController().then {
                $0.configure(user: user)
            }
        }
        refreshTabs()
    }
    
    /// Note: This property has the knowledge of every contained child tableview.
    /// This is in support of the master scroller.
    public var currentChildTableView: UITableView?
    {
        if currentViewController == friendsViewController {
            return friendsViewController?.tableView
        }
        if currentViewController == notificationsViewController{
            return notificationsViewController.tableView
        }
        if currentViewController == myCirclesViewController {
            return myCirclesViewController.tableView
        }
        if currentViewController == otherUserCirclesViewController {
            return otherUserCirclesViewController.tableView
        }
        return nil
    }
    
    // MARK: PageboyViewControllerDataSource
    
    public func numberOfViewControllers(in pageboyViewController: PageboyViewController) -> Int {
        // Init the view controllers when asked for the count.
        return initTabs()
    }
    
    /// Call this if the number of tabs may have cha
    private func refreshTabs() {
        initTabs()
        reloadPages()
    }
    
    /// Set the desired tabs and return the tab count
    @discardableResult
    private func initTabs() -> Int
    {
        var viewControllers = [UIViewController]()
        var items = [TabmanBar.Item]()
        
        let notificationsItem = PresentTabs.badgedItem(title: "NOTIFICATIONS", badgeCount: activityBadgeCount)
        let friendsItem = PresentTabs.badgedItem(title: "FRIENDS", badgeCount: friendsBadgeCount)
        let circlesItem = PresentTabs.badgedItem(title: "CIRCLES", badgeCount: circlesBadgeCount)

        if showTabs.contains(.circles), let user = user {
            if user.isMe {
                viewControllers.append(myCirclesViewController)
            } else {
                viewControllers.append(otherUserCirclesViewController)
            }
            items.append(circlesItem)
        }

        if showTabs.contains(.notifications) {
            viewControllers.append(notificationsViewController)
            items.append(notificationsItem)
        }
        if let friendsViewController = friendsViewController, showTabs.contains(.friends) {
            viewControllers.append(friendsViewController)
            items.append(friendsItem)
        }
        
        self.tabViewControllers = viewControllers
        self.bar.items = items

        return viewControllers.count
    }
    
    // TODO
    private func updateTabBadges() {
        initTabs()
    }
    
    public func viewController(for pageboyViewController: PageboyViewController, at index: PageboyViewController.PageIndex) -> UIViewController? {
        return self.tabViewControllers[index]
    }
    
    public func defaultPage(for pageboyViewController: PageboyViewController) -> PageboyViewController.Page? {
        return .first
    }
    
    // MARK: PageboyViewControllerDelegate
    
    override public func pageboyViewController(_ pageboyViewController: PageboyViewController, didScrollTo position: CGPoint, direction: PageboyViewController.NavigationDirection, animated: Bool)
    {
        super.pageboyViewController(pageboyViewController, didScrollTo: position, direction: direction, animated: animated)
        
        // TODO: Why is this needed?
        view.setNeedsLayout()
    }
    
    override public func pageboyViewController(_ pageboyViewController: PageboyViewController, didScrollToPageAt index: Int, direction: PageboyViewController.NavigationDirection, animated: Bool)
    {
        super.pageboyViewController(pageboyViewController, didScrollToPageAt: index, direction: direction, animated: animated)
        
        // Publish the selected tab
        selectedTab.onNext(index)
        
        // TODO: Why is this needed?
        view.setNeedsLayout()
    }
    
    override public func pageboyViewController(_ pageboyViewController: PageboyViewController, didReloadWith currentViewController: UIViewController, currentPageIndex: PageboyViewController.PageIndex) {
    }
    
    override public func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        
        // Note: the auto-insetting is causing too many problems, just
        // Note: setting it manually for now.
        //if let tableView = myCirclesViewController.tableView {
            //tableView.contentOffset = .zero
            //log("bar required inset: \(bar.requiredInsets)")
            //log("content inset: \(tableView.contentInset)")
            //if #available(iOS 11.0, *) {
            //log("adjusted content inset: \(tableView.adjustedContentInset)")
            //log("adjustment behavior: \(tableView.contentInsetAdjustmentBehavior)")
            //log("extra safe area insets = \(self.additionalSafeAreaInsets)")
            //tableView.contentInsetAdjustmentBehavior = .never
            //tableView.insetsContentViewsToSafeArea = false
            //tableView.insetsLayoutMarginsFromSafeArea = false
            //tableView.setNeedsLayout()
            //}
            //log("content offset: \(tableView.contentOffset)")
        //}
        
        // Note: the auto-insetting is causing too many problems, just
        // Note: setting it manually for now.
        
        notificationsViewController.tableView.do {
            $0.contentInset.top = 40
            $0.contentInset.bottom = 140
        }
        
        friendsViewController?.tableView.do {
            $0.contentInset.bottom = 140
        }
    }
    
    // MARK: TabmanBarDelegate
    
    public func bar(shouldSelectItemAt index: Int) -> Bool {
        // TODO: Called three times?
        //log("item: \(index)")
        return true
    }
}
