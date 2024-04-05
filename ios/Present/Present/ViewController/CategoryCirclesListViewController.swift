//
//  CategoryViewController.swift
//  Present
//
//  Created by Patrick Niemeyer on 1/31/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import CoreGraphics
import Foundation
import PresentProtos
import UIKit
import Relativity

public final class CategoryCirclesListViewController : CardHomeViewController, NearbyGroupsObserver
{
    let category: String

    // Last user selected city
    var lastSelectedLocation: Present.City?
    
    let backButton = ThemeableButton()
    
    public var space: PresentSpace? {
        didSet {
            guard let space = space else { return }
            cardScroller.filter(spaceId: space.id)
        }
    }

    // MARK: Initialization

    public init(
            locationProvider: LocationProvider,
            screenPresenter: RootViewController,
            userManager: UserManager,
            category: String
    ) {
        self.category = category
        super.init(locationProvider: locationProvider, screenPresenter: screenPresenter, userManager: userManager)
        
        view.backgroundColor = .white
        if !isAllCategory(category: category) {
            cardScroller.filter(category: category)
        }
        super.mapButton.isHidden = true

        //backButton
        backButton.apply(viewModel: ThemeableButton.ViewModel(title: .none, image: #imageLiteral(resourceName: "LeftChevronGlyph")))
        backButton.addTarget(self, action: #selector(goBack), for: .touchUpInside)
        navigationBar.addSubview(backButton)
    }

    public required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    // MARK: UIView

    public override func viewDidLoad() {
        super.viewDidLoad()
        // TODO: Switch to the Rx API
        userManager.groupManager.register(nearbyGroupsObserver: self, observerQueue: .main)
    }

    public override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        
        // TODO: Rename this event (no longer under profile)
        logEvent(.profile_view_circles)
    }

    public override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        backButton.sizeToFit()
        //backButton.bounds = CGRect(x:0, y:0, width: 30, height: 30)
        backButton.left --> .left + 9.horizontalOffset
    }

    // MARK: Private / Protected Methods

    private func isAllCategory(category: String) -> Bool  {
        return category.lowercased() == "all"
    }

    override func initTitleView() -> UIView {
        let titleText = UILabel()
        titleText.text = isAllCategory(category: category) ? "Nearby" : category
        titleText.apply(theme: UILabel.Theme(font: UIFont.presentFont(ofSize: 20.0, weight: .semibold), textAlignment: .center, numberOfLines: 1))
        return titleText
    }

    @objc private func goBack() {
        screenPresenter.goBack()
    }

    // invoked by refresh control
    @objc override func refreshAllCards() {
        super.refreshAllCards()
        userManager.groupManager.refreshNearbyGroups()
    }

    // TODO: Switch to the Rx API
    // MARK: NearbyGroupsObserver

    // TODO: Switch to the Rx API
    public func nearbyGroupsDidChange(to nearbyGroups: [Group]) {
        //nearbyGroups.forEach { log("nearby group: \($0.title), categories: \($0.categories), spaceId: \($0.space?.id)") }
        groupsDidChange(to: nearbyGroups, forceUpdate: selectedLocationChanged())
    }

    // TODO: This should be consolidated into either location manager or user manager with an observable.
    /// Return true if the user selected location has changed since the last check
    private func selectedLocationChanged() -> Bool {
        // TODO: Integrate major location change into nearby groups api or location manager api?
        // Note location changes to force updates
        let currentSelectedLocation = self.userManager.selectedLocation.value
        let changed = currentSelectedLocation != lastSelectedLocation
        lastSelectedLocation = currentSelectedLocation
        return changed
    }

    public func nearbyGroupsFailedToLoad() {
        groupsFailedToLoad()
    }

    // MARK: UI Prefs

    public override var shouldAutorotate: Bool {
        return false
    }

    public override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
        return .portrait
    }

    public override var preferredStatusBarStyle: UIStatusBarStyle {
        return .default
    }
}
