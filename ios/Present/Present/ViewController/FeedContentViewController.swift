//
//  FeedContentViewController.swift
//  Present
//
//  Created by Patrick Niemeyer on 5/2/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import RxSwift

public protocol FeedContentViewControllerDelegate: class {
    var feedSearch: ObservableValue<SearchText> { get }
}

/// A view controller for the content of a tab on the feed view,
/// managing the types of content that can appear here, (currently):
///     Feed HTML and Search
///
///     --> GatedFeedWebViewController (women only, nearby)
///         --> FeedContentViewController
///             --> FeedWebViewController
public class FeedContentViewController: PresentViewController
{
    
    // The delegate that gives us our search value
    public weak var delegate: FeedContentViewControllerDelegate?

    var feedViewController = FeedWebViewController().then {
        $0.name = "Explore web view controller"
        $0.dispatchEvents = [
            .openCircle: .home_explore_tap_circle,
            .createCircle: .home_explore_tap_create_circle,
            .changeLocation: .home_explore_tap_change_location,
            .openCategory: .home_explore_tap_view_all_category
        ]
    }
    lazy var searchViewController = CardScrollingViewController()

    // TODO: Change this to use CachedSingle observable and get rid of (last usage of) CacheableSingle
    lazy var feedHtml = CacheableSingle<String>(
    name: "exploreHtml", period: .minutes(5)) {
            return self.service.getExploreHtml(forSpaceId: nil)
        }
    
    override public func viewDidLoad()
    {
        super.viewDidLoad()

        // Init the search view
        searchViewController = CardScrollingViewController().then {
            $0.view.backgroundColor = .white
            installChild(viewController: $0, in: view) { cardScroller in
                cardScroller.constrainToFillSuperview()
            }
            $0.delegate = self
        }
        
        // TODO: Switch to the Rx API
        // TODO: Why does calling groupManager here directly cause a loop?
        userManager.groupManager.register(nearbyGroupsObserver: self)
        
        // (refresh: Bool)->Single<String>
        feedViewController.htmlSource = feedHtml.get
        
        installChild(viewController: feedViewController, in: view)
        selectedContent = .feed

        // Update on search change.
        // See FeedWebViewController for the location observation
        delegate.unwrappedOrFatal("missing delegate").feedSearch.observable
            .onNext { [weak self] search in
                switch search {
                    case .noSearch:
                        self?.selectedContent = .feed
                    case .empty:
                        //self?.selectedContent = .none
                        self?.selectedContent = .feed
                    case let .value(text):
                        self?.selectedContent = .search
                        self?.searchViewController.filter(text: text)
                }
            }.disposed(by: disposal)
    }
    
    override public func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        //print("xx: feed web view controller will appear")
        //searchViewController?.view.setNeedsLayout() // TODO: forward appearance transitions?
    }
    
    // MARK: Content Types
    
    public enum Content {
        case none
        case feed
        case search
    }
    
    private lazy var viewControllers: [Content:UIViewController] = [
        .feed: feedViewController,
        .search: searchViewController
    ]
    private var selectedContent: Content = .feed {
        didSet {
            showSelectedContent()
        }
    }
    
    private func showSelectedContent() {
        if selectedContent == .none {
            viewControllers.values.forEach { $0.view.isHidden = true }
        } else {
            let selectedViewController = viewControllers[selectedContent]
            viewControllers.values.forEach { $0.view.isHidden = $0 != selectedViewController }
        }
    }
}

// TODO: Switch to the Rx API
extension FeedContentViewController: NearbyGroupsObserver
{
    public func nearbyGroupsDidChange(to nearbyGroups: [Group]) {
        searchViewController.configure(with: nearbyGroups)
    }
    public func nearbyGroupsFailedToLoad() { }
}

extension FeedContentViewController: CardScrollingViewControllerDelegate
{
    public func cardScrolling(viewController: CardScrollingViewController, didSelectCardFor group: Group) {
        screenPresenter.pushGroup(for: group)
    }
    public func cardScrolling(viewController: CardScrollingViewController, isShowingGroups groups: [Group]) { }
    public func cardScrollerScrollviewMoved() { }
    public func cardScrollerUserFlippedCards(count: Int) { }
}
