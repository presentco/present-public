//
//  Created by Patrick Niemeyer.
//  Copyright © 2017 Present Company. All rights reserved.
//

import CoreGraphics
import Foundation
import PresentProtos
import Relativity
import UIKit

/// A screen comprising a collection of cards with a header area, categories picker, and search filter button.
/// Currently used for the Explore, Nearby Feed, and MyCirclesViewController on the home view.
public class CardHomeViewController : UIViewController,
    CardScrollingViewControllerDelegate, GroupMapViewDelegate
{
    // MARK: Properties

    let locationProvider: LocationProvider
    let screenPresenter: RootViewController
    let userManager: UserManager
    
    public var searchBarPlaceholderText: String?

    private var titleView = UIView()
    let navigationBar = UIView()
    let mapButton = ThemeableButton()
    let addCircleButton = ThemeableButton()

    let searchBar = UIView()
    private let searchButton = ThemeableButton()
    private let searchCancelButton = ThemeableButton()
    private let searchBarTextField = ThemeableTextField()
    private let searchBarUnderline = UIView()

    private let mapView: GroupMapViewController

    // Activity indicator for the card scroller view
    private let cardScrollerActivityIndicator = PresentLogoActivityIndicator()

    private let cardsRefreshController = PresentRefreshController()
    
    /// An updated set of groups received after the initial cards are configured.
    /// We will save the updated groups until a screen change or explicit user refresh.
    /// @see updateGroupsIfNeeded()
    private var updatedGroups: [Group]?

    let cardScroller: CardScrollingViewController
    var isVisible: Bool = false
    var showLoadingErrorWhenVisible = false

    // MARK: Initialization

    public init(locationProvider: LocationProvider,
                screenPresenter: RootViewController,
                userManager: UserManager
    ) {
        self.locationProvider = locationProvider
        self.screenPresenter = screenPresenter
        self.userManager = userManager

        cardScroller = CardScrollingViewController().then {
            $0.name = "My circles card scroller"
        }
        mapView = GroupMapViewController()

        super.init(nibName: nil, bundle: nil)
    }
    
    public required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    // MARK: CardScrollingViewControllerDelegate

    public func cardScrolling(viewController: CardScrollingViewController, didSelectCardFor group: Group) {
        // TODO: Can also be called for category view
        //logEvent(.home_search_tap_circle)
        screenPresenter.pushGroup(for: group)
    }

    // TODO: This is a bit of a hack. We are allowing the card scroller to manage the filters for us and passing the
    // TODO: filtered groups on to the map view.  We should pull the filter logic out.
    public func cardScrolling(viewController: CardScrollingViewController, isShowingGroups groups: [Group]) {
        mapView.configure(with: groups)
    }

    // MARK: UIView

    public override func viewDidLoad()
    {
        super.viewDidLoad()

        titleView = initTitleView()
        navigationBar.addSubview(titleView)

        // map button
        mapButton.apply(viewModel: ThemeableButton.ViewModel(title: .none, image: #imageLiteral(resourceName: "MAP VIEW")))
        mapButton.addTarget(self, action: #selector(toggleMapView), for: .touchUpInside)
        mapButton.isEnabled = false // until groups have loaded
        navigationBar.addSubview(mapButton)
        
        // add circle button
        addCircleButton.apply(viewModel: ThemeableButton.ViewModel(title: .none, image: #imageLiteral(resourceName: "create_circle")))
        addCircleButton.addTarget(self, action: #selector(pushCreateGroup), for: .touchUpInside)
        navigationBar.addSubview(addCircleButton)

        // Init search bar
        searchButton.apply(viewModel: ThemeableButton.ViewModel(title: .none, image: #imageLiteral(resourceName: "search")))
        searchButton.addTarget(self, action: #selector(showSearchBar), for: .touchUpInside)
        searchBar.addSubview(searchButton)

        searchCancelButton.apply(viewModel: ThemeableButton.ViewModel(title: .none, image: #imageLiteral(resourceName:"CancelSearchGlyph")))
        searchCancelButton.addTarget(self, action: #selector(hideSearchBar), for: .touchUpInside)
        searchBar.addSubview(searchCancelButton)
        searchBarTextField.attributedPlaceholder = NSAttributedString(
            string: searchBarPlaceholderText ??
                NSLocalizedString("CardHomeViewControllerSearchPlaceholderText", tableName: nil, bundle: .main, value: "Find your good vibe tribe", comment: "Search placeholder text"),
            attributes: [
                NSAttributedStringKey.font: UIFont.presentFont(ofSize: 14.0, weight: .regular),
                NSAttributedStringKey.foregroundColor: UIColor(hex: 0xA4_AA_B3)
            ]
        )
        searchBarTextField.tintColor = UIColor(hex: 0x71_41_DB)
        searchBarTextField.font = .presentFont(ofSize: 14.0, weight: .medium)
        // Note: The card scroller currently handles the raw search text
        searchBarTextField.addTarget(cardScroller, action: #selector(CardScrollingViewController.searchTextDidChange), for: .editingChanged)
        searchBarTextField.addTarget(self, action: #selector(searchBarDidExit), for: .editingDidEndOnExit)
        searchBarTextField.addTarget(self, action: #selector(showSearchBar), for: .editingDidBegin)
        searchBarTextField.returnKeyType = .search
        searchBar.addSubview(searchBarTextField)
        searchBarUnderline.backgroundColor = UIColor(hex: 0xEF_EF_F4)
        updateSearchBarAlpha(showBar: false)

        searchBar.addSubview(searchBarUnderline)
        searchBar.backgroundColor = .white

        navigationBar.backgroundColor = .white
        view.addSubview(navigationBar)
        view.addSubview(searchBar)

        installChild(viewController: mapView, in: view)
        installChild(viewController: cardScroller, in: view)

        cardsRefreshController.addTarget(self, action: #selector(refreshAllCards))
        cardScroller.refreshControl = cardsRefreshController.refreshControl

        // Show a spinner while we load our groups.
        cardScroller.installChild(viewController: cardScrollerActivityIndicator, in: cardScroller.view) { _ in
            cardScrollerActivityIndicator.view.sizeToFitSuperview()
        }
        
        
        cardScroller.view.sendSubview(toBack: cardScrollerActivityIndicator.view)
        //cardScrollerActivityIndicator.startAnimating()

        let cardScrollerBackgroundColor = UIColor(red: 0.90, green: 0.90, blue: 0.90, alpha: 1.0)
        cardScroller.view.backgroundColor = cardScrollerBackgroundColor

        cardScroller.delegate = self

        mapView.delegate = self
        mapView.view.isHidden = true
    }

    public func cardScrollerScrollviewMoved() {
        cardsRefreshController.updatePullToRefresh()
    }

    private var totalCardsFlipped = 0
    public func cardScrollerUserFlippedCards(count: Int) {
        totalCardsFlipped += count // reset by viewDidAppear
    }

    public override func viewDidLayoutSubviews()
    {
        super.viewDidLayoutSubviews()
        
        // Layout our navigation bar
        navigationBar.bounds.size = CGSize(
            width: view.bounds.width, height: navigationBar.isShown ? 30.0 : 0.0)
        let statusBarHeight: CGFloat = 20.0
        
        if #available(iOS 11.0, *) {
            navigationBar.top --> .top + view.safeAreaInsets.top.verticalOffset
        } else {
            navigationBar.top --> .top + statusBarHeight.verticalOffset
        }

        let searchBarHeight: CGFloat = searchBar.isHidden ? 8 : 41.0
        searchBar.bounds.size = CGSize(width: view.bounds.width, height: searchBarHeight)
        searchBar.top --> navigationBar.bottom

        titleView.sizeToFit()
        titleView.bounds = CGRect(x:0, y:0, width: 250, height: titleView.bounds.height)
        titleView.middle --> .middle

        mapButton.bounds = CGRect(x:0, y:0, width: 30, height: 30)
        mapButton.left --> .left + 9.horizontalOffset
        
        addCircleButton.bounds = CGRect(x:0, y:0, width: 21, height: 21)
        addCircleButton.right --> .right - 9.horizontalOffset

        searchButton.sizeToFit()
        searchButton.left --> .left + 15.horizontalOffset

        searchCancelButton.sizeToFit()
        searchCancelButton.right + 15.horizontalOffset --> .right
        let underlineHeight: CGFloat = 0.0
        searchBarTextField.bounds.size = CGSize(
            width: ((15.horizontalOffset + searchButton.right) |--| (searchCancelButton.left - 15.horizontalOffset)).width,
            height: searchBar.bounds.size.height - underlineHeight
        )
        15.horizontalOffset + searchButton.right <-- searchBarTextField.left
        searchBarUnderline.bounds.size = CGSize(
            width: ((15.horizontalOffset + searchBar.left) |--| (searchBar.right - 15.horizontalOffset)).width,
            height: underlineHeight
        )
        searchBarTextField.bottomLeft <-- searchBarUnderline.topLeft
        
        if #available(iOS 11.0, *) {
            cardScroller.view.bounds.size = CGSize(
                width: view.bounds.size.width,
                height: (searchBar.bottom |--| (.bottom-view.safeAreaInsets.bottom.verticalOffset)).height
            )
        } else {
            cardScroller.view.bounds.size = CGSize(
                width: view.bounds.size.width,
                height: (searchBar.bottom |--| .bottom).height
            )
        }

        cardScroller.view.top --> searchBar.bottom
        
        if #available(iOS 11.0, *) {
            mapView.view.bounds.size = CGSize(
                width: view.bounds.size.width,
                height: (searchBar.bottom |--| (.bottom - view.safeAreaInsets.bottom.verticalOffset)).height
            )
        } else {
            mapView.view.bounds.size = CGSize(
                width: view.bounds.size.width,
                height: (searchBar.bottom |--| .bottom).height
            )
        }
        mapView.view.top --> searchBar.bottom
        
        cardsRefreshController.updatePullToRefresh()
    }

    public override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)

        // View will be shown, update the groups if we have new ones
        updateGroupsIfNeeded()
    }

    // Called when the view in the home view controller tab is shown
    public override func viewDidAppear(_ animated: Bool) {
        logn("view did appear")
        super.viewDidAppear(animated)
       
        totalCardsFlipped = 0
        isVisible = true
        showLoadingErrorIfNecessary()
        
        // Note: the safe area is not set properly when we are layed out offscreen.
        // Note: as a workaround we always update layout when we appear.
        view.setNeedsLayout()
    }

    // Called when the view in the home view controller tab is hidden or the home view controller is left for a group
    public override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        //logEvent(.discovery_scroll, stringProperties: [LoggingKey.cards_flipped: String(totalCardsFlipped)])
        totalCardsFlipped = 0 // Just in case
        isVisible = false
    }
    
    // MARK: Group change

    /// Receive updated groups to populate this card UI.  The groups are stored and the cards are rebuilt at
    /// the next appropriate user interaction boundary such as a screen switch or explicit refresh pull.
    /// @param if forceUpdate is true update the groups without waiting for a user interaction boundary.
    public func groupsDidChange(to groups: [Group], forceUpdate: Bool = false)
    {
        // Stop any refresh behavior
        cardScrollerActivityIndicator.stopAnimating()
        mapButton.isEnabled = true
        let respondingToRefreshPull = cardsRefreshController.refreshControl.isRefreshing
        cardsRefreshController.endRefreshing()

        // New groups arrived, store them and apply update logic
        DispatchQueue.main.async { // sync access to updated groups
            self.updatedGroups = groups

            // If this is the initial configuration or we just had a refresh pull apply the groups
            if self.cardScroller.groups == nil || respondingToRefreshPull || forceUpdate {
                self.updateGroupsIfNeeded()
            }
        }
    }
    
    public func groupsFailedToLoad() {
        if cardScroller.isEmpty {
            showLoadingErrorWhenVisible = true
            showLoadingErrorIfNecessary()
        }
    }

    /// Called at appropriate times to update the visible cards for the user.
    /// e.g. upon a view change or explicit refresh pull
    private func updateGroupsIfNeeded()
    {
        DispatchQueue.main.async { // sync access to updated groups
            // Are there groups to update?
            guard let updatedGroups = self.updatedGroups else { return }

            // TODO: Find another place to squash inappropriate background updates.
            // TODO: This was preventing us from showing cards in the category viewer when launched
            // TOOD: from a cateogry URL
            // Defer if we aren't active in the foreground
            //guard UIApplication.activeInForeground else { return }

            // Perform the update and clear the cached groups
            self.cardScroller.configure(with: updatedGroups)
            self.updatedGroups = nil
        }
    }
    
    // MARK: Private / Protected Methods

    /// Called once to create the title view
    func initTitleView() -> UIView {
        let logo = UIImageView()
        logo.image = #imageLiteral(resourceName:"logo")
        return logo
    }

    // invoked by refresh control for refresh pull on cards
    @objc func refreshAllCards() {
        cardsRefreshController.beginAnimatingPullToRefreshLogo()
        // Note: subclasses implement...
    }
    

    @objc func showSearchBar(_ sender: AnyObject) {
        logEvent(.home_search_view)
        UIView.animate(withDuration: 0.2, animations: {
            self.updateSearchBarAlpha(showBar: true)
            
        }) { (_) in
            self.searchBarTextField.becomeFirstResponder()
        }
    }

    @objc func hideSearchBar(_ sender: AnyObject) {
        searchBarTextField.resignFirstResponder()
        searchBarTextField.text = nil
        searchBarTextField.sendActions(for: .editingChanged)

        UIView.animate(withDuration: 0.2) {
            self.updateSearchBarAlpha(showBar: false)
        }
    }

    @objc func toggleMapView(_ sender: AnyObject) {
        doToggleMapView()
    }

    func doToggleMapView(completion: (()->Void)? = nil)
    {
        mapButton.isEnabled = false
        
        if cardScroller.cardsAreOffscreen {
            cardScroller.view.isHidden = false
            cardScroller.returnCardsToScreen { [weak self] in
                self?.mapView.view.isHidden = true
                self?.mapButton.apply(viewModel: ThemeableButton.ViewModel(title: .none, image: #imageLiteral(resourceName: "MAP VIEW")))
                self?.mapButton.isEnabled = true
                completion?()
            }
        } else {
            mapView.view.isHidden = false
            cardScroller.dropCardsOffscreen { [weak self] in
                self?.cardScroller.view.isHidden = true
                self?.mapButton.apply(viewModel: ThemeableButton.ViewModel(title: .none, image: #imageLiteral(resourceName: "listView")))
                self?.mapButton.isEnabled = true
                completion?()
            }
        }
    }

    @objc private func pushCreateGroup() {
        screenPresenter.presentCreateCircle()
    }
    
    @objc private func searchBarDidExit(_ sender: AnyObject) {
        guard let searchBar = sender as? ThemeableTextField, searchBar == self.searchBarTextField else {
            return
        }
        
        if searchBar.text == nil || searchBar.text?.count == 0 {
            hideSearchBar(sender)
        }
        
        searchBar.resignFirstResponder()
    }
    
    private func updateSearchBarAlpha(showBar: Bool) {
        let searchAlpha: CGFloat = showBar ? 1.0 : 0.0
        searchCancelButton.alpha = searchAlpha
        //searchBarTextField.alpha = searchAlpha
        //searchBarUnderline.alpha = searchAlpha
        //let nonSearchAlpha: CGFloat = showBar ? 0.0 : 1.0
        //searchButton.alpha = nonSearchAlpha
        //titleView.alpha = nonSearchAlpha
        //mapButton.alpha = showBar ? 0.0 : 1.0
    }
    
    public var searchBarIsShowing: Bool {
        get { return searchCancelButton.alpha > 0 }
    }
    
    // MARK: GroupMapViewDelegate
    public func didSelect(group: Group) {
        screenPresenter.pushGroup(for: group)
    }

    fileprivate var contactsMessageBatchRetained: ContactsMessageBatch! // Required to retain the NSObject outside of the send

    func showLoadingErrorIfNecessary()
    {
        guard showLoadingErrorWhenVisible && isVisible else {
            return
        }

        showLoadingErrorWhenVisible = false

        let activityFailedToLoadTitle = NSLocalizedString(
                "CardHomeViewControllerCouldNotLoadCardHomeAlertTitle",
                tableName: nil,
                bundle: .main,
                value: "There was an issue loading Nearby Circles",
                comment: "Title of the alert informing the user there was an issue loading nearby groups"
        )

        let alertController = UIAlertController(title: nil, message: activityFailedToLoadTitle, preferredStyle: .alert)
        let retryButtonTitle = NSLocalizedString(
                "CardHomeViewControllerReloadCardHomeAlertButtonTitle",
                tableName: nil,
                bundle: .main,
                value: "Ok",
                comment: "Title of the alert button prompting user to re-fetch their nearby groups"
        )
        alertController.addAction(withTitle: retryButtonTitle) { /*[weak self]*/ (_) in
            //self?.userManager.groupManager.refreshNearbyGroups()
        }

        present(alertController, animated: true)
    }
}

extension CardHomeViewController
{
    // Offer to share a link with the system share sheet
    public func shareLink() {
        ShareSheetUtil.showShareSheet(from: self, withText: shareText)
    }
    
    // Used for both link sharing and contact sharing
    fileprivate var shareText: String {
        return "Join me on Present! \(PresentLinks.presentGenericInvite)" // Localize
    }
}

public extension CardHomeViewController {
    public override var shouldAutorotate: Bool { return false }
    public override var supportedInterfaceOrientations: UIInterfaceOrientationMask { return .portrait }
    public override var preferredStatusBarStyle: UIStatusBarStyle { return .lightContent }
}
