//
//  Present
//
//  Created by Patrick Niemeyer on 5/3/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation

/// Base class for each tab of the feed view of the home screen.
/// Guards for the necessary signup status and permissions, showing either the
/// underlying feed view controller or the appropriate signup placeholder screen as needed.
public class GatedFeedContentViewController: GatedViewController
{
    public var feedContentViewController: FeedContentViewController?
    
    // TODO: Factor this out into parent if we add more of these
    public var showPlaceholder: Bool? {
        didSet {
            guard let showPlaceholder = showPlaceholder else { return }
            if showPlaceholder {
                self.showPlaceholderViewcontroller()
            } else {
                self.showFeedContentViewController()
            }
            logView()
        }
    }
    
    public override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        logView()
    }
    
    private func logView() {
        if showPlaceholder ?? false {
            logEvent(.home_placeholder_view, space: placeholderViewController?.space)
        } else {
            logEvent(.home_feed_view)
        }
    }

    // TODO: The parent GatedViewController install() method constrains the view to the safe areas.
    // TODO: If we rely on that here for the web view it seems to leave extra space at the top after a refresh pull.
    // TODO: e.g. home screen main feed tab and also MyCircles in profile.
    // TODO: Need to figure out why and remove this...
    override func install(viewController: UIViewController) {
        installChild(viewController: viewController, in: view)
        viewController.view.constrainToFillSuperview()
    }
    
    func showFeedContentViewController()
    {
        if let feedContentViewController = self.feedContentViewController {
            feedContentViewController.view.toFront()
        } else {
            self.feedContentViewController = FeedContentViewController()
            .configure { _ in
                self.configureViewControllerQueue.run()
            }.then {
                self.feedContentViewController = $0 // Must be set before config runs
                install(viewController: $0)
            }
        }
    }
    
    @discardableResult
    public func configure(withBlock block: @escaping ((FeedContentViewController)->Void)) -> Self
    {
        configureViewControllerQueue.enqueue {
            guard let feedContentViewController = self.feedContentViewController else { return }
            block(feedContentViewController)
        }
        return self
    }
    
}
