//
//  AddFriendsViewController.swift
//  Present
//
//  Created by Patrick Niemeyer on 7/13/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import Foundation
import PresentProtos
import RxSwift
import Then
import Tabman
import Pageboy

/// A container with configurable tabs using Tabman
public class PresentTabsViewController: TabmanViewController, PageboyViewControllerDataSource, ApplicationServices
{
    deinit { log("present tabs viewcontroller deinit \(type(of: self))") }
    
    var tabViewControllers = [UIViewController]()

    public override func viewDidLoad()
    {
        // Note: Must be set before other setup.
        // Note: Default true, necessary to inset content below the tabs header view.
        // Note: We are having lots of problems with this so I have turned it off in some
        // Note: cases.
        //self.automaticallyAdjustsChildViewInsets = false
        
        super.viewDidLoad()

        dataSource = self
        self.isScrollEnabled = false
        PresentTabs.configurePresentTabs(bar: bar)
        transition = Transition(style: .push, duration: 0.2)
    }
    
    override public func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        view.setNeedsLayout()
    }
    
    // MARK: PageboyViewControllerDataSource
    
    public func numberOfViewControllers(in pageboyViewController: PageboyViewController) -> Int {
        // Init the view controllers when asked for the count.
        return initTabs()
    }
    
    /// Call this if the number of tabs may have cha
    public func refreshTabs() {
        initTabs()
        reloadPages()
    }
    
    /// Override to set the desired tabs and return the tab count
    @discardableResult
    public func initTabs() -> Int
    {
        tabViewControllers = [UIViewController(), UIViewController()]
        bar.items = [Item(title: "TAB1"), Item(title: "TAB2")]
        return tabViewControllers.count
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
        // TODO: Why is this needed?
        view.setNeedsLayout()
    }
    
    override public func pageboyViewController(_ pageboyViewController: PageboyViewController, didReloadWith currentViewController: UIViewController, currentPageIndex: PageboyViewController.PageIndex) {
    }
}
