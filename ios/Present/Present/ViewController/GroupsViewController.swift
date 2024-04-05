//
//  GroupsViewController.swift
//  Present
//

import Foundation
import CoreGraphics
import Foundation
import PresentProtos
import UIKit
import Relativity

/// Present a pre-loaded set of groups with a title
public final class GroupsViewController : CardHomeViewController
{
    let backButton = ThemeableButton()

    // MARK: Initialization

    public init(
        // Needed by the base card view controller for rendering group distance
        locationProvider: LocationProvider,
        // Supports a retry behavior on card loading that isn't used in this context
        userManager: UserManager,
        screenPresenter: RootViewController,
        title: String,
        groups: [Group]
    ) {
        super.init(locationProvider: locationProvider, screenPresenter: screenPresenter, userManager: userManager)
        
        self.title = title
        view.backgroundColor = .white
        super.searchBar.isHidden = true
        super.mapButton.isHidden = true
        super.addCircleButton.isHidden = true

        //backButton
        backButton.apply(viewModel: ThemeableButton.ViewModel(title: .none, image: #imageLiteral(resourceName: "LeftChevronGlyph")))
        backButton.addTarget(self, action: #selector(goBack), for: .touchUpInside)
        navigationBar.addSubview(backButton)
        
        groupsDidChange(to: groups, forceUpdate: true)
    }

    public required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    // MARK: UIView

    public override func viewDidLoad() {
        super.viewDidLoad()
    }

    public override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
    }

    public override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        backButton.sizeToFit()
        backButton.left --> .left + 9.horizontalOffset
    }

    // MARK: Private / Protected Methods

    // TODO: Factor out with similar themed titles in other cases
    override func initTitleView() -> UIView {
        let titleText = UILabel()
        titleText.text = title
        titleText.apply(theme: UILabel.Theme(font: UIFont.presentFont(ofSize: 20.0, weight: .semibold), textAlignment: .center, numberOfLines: 1))
        return titleText
    }

    @objc private func goBack() {
        screenPresenter.goBack()
    }

    // invoked by refresh control
    @objc override func refreshAllCards() {
        super.refreshAllCards()
        // TODO:
    }
}
