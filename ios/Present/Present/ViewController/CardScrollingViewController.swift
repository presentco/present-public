//
//  CardScrollingViewController.swift
//
//  Created by Patrick Niemeyer on 2/16/16.
//  Copyright © 2016 Present Company. All rights reserved.
//

import CoreGraphics
import Foundation
import Relativity
import UIKit
import PresentProtos

public protocol CardScrollingViewControllerDelegate: class {
    func cardScrolling(viewController: CardScrollingViewController, didSelectCardFor group: Group)
    func cardScrolling(viewController: CardScrollingViewController, isShowingGroups groups: [Group])
    func cardScrollerScrollviewMoved()
    func cardScrollerUserFlippedCards(count: Int)
}

/**
    A scrolling list of overlapping CardViews.
*/
public final class CardScrollingViewController: UIViewController, UIScrollViewDelegate, LegacyApplicationServices
{
    public var name = ""
    
    // MARK: Private Static Properties
    
    // If true a tap on a card will push the bubble view from any position
    // If false only the top card when tapped shows the bubble view and others scroll 
    // the card to top.
    private static var tapShowsCardAnyPosition = true
    
    /// If true cards scroll continuously with no adjustment to the stopping position.
    /// If false card scrolling will stop with a card aligned at the top of the view.
    private static var cardsScrollContinuous = false
    
    // The rolloff easing function: y = x^power * const
    private static var power: CGFloat = 4 {
        didSet {
            rolloffFuncMax = getRolloffFuncMax()
        }
    }
    
    private static var const: CGFloat = 3.0 {
        didSet {
            rolloffFuncMax = getRolloffFuncMax()
        }
    }
    
    // Solve for the value at which the rolloff function reaches 1.0.
    // (Note that with a const of 1.0 this would always be 1.0)
    private static var rolloffFuncMax = CardScrollingViewController.getRolloffFuncMax()
    
    private static func getRolloffFuncMax() -> CGFloat {
        return pow(1.0/const, 1.0/power)
    }

    // MARK: Properties

    public private(set) var groups : [Group]?

    // MARK: Public Properties
    
    public var layout: Layout = .stacked {
        didSet {
            if let groups = groups {
                configure(with: groups)
            }
        }
    }
    
    // the height of the non-overlapping bottom of the card views
    public var cardOffset: CGFloat {
        switch layout {
        case .sequential:
            return cardHeight
            
        case .stacked:
            return CardViewController.separatorHeight + CardViewController.footerHeight
        }
    }
    @IBInspectable public var ypad: CGFloat = 0 // top cardview padding
    @IBInspectable public var xpad: CGFloat = 0 // side cardview padding
    
    /// If `true`, the card alpha goes to zero (at the same rate) as it leaves the screen.
    @IBInspectable public let transformAlpha = false
    
    /// Reference to the currently selected card view controller on tap, if any.
    public var selectedCardViewController: CardViewController?
    
    public weak var delegate: CardScrollingViewControllerDelegate?
    
    public var isEmpty: Bool {
        return cardViewControllers.isEmpty
    }
    
    public var refreshControl: UIRefreshControl? {
        willSet {
            refreshControl?.removeFromSuperview()
        }
        didSet {
            if let refreshControl = refreshControl {
                scrollView.addSubview(refreshControl)
                scrollView.sendSubview(toBack: refreshControl)
            }
        }
    }
    
    /// If true items tagged with the "Woman Owned" category only appear when the category is
    /// selected or when a text search is performed.
    // If false the category is treated as any other category.
    public var reserveWomanOwnedCategory = true
    
    // MARK: UIScrollViewDelegate
    
    public func scrollViewWillBeginDragging(_ scrollView: UIScrollView) {
        // Find the view tracking the user touch
        cardBeingDragged = topmostCardAtLocation( scrollView.panGestureRecognizer.location(in: scrollView) )
        prepareFeedbackGenerator()
    }
    
    public func scrollViewDidScroll(_ scrollView: UIScrollView) {
        //logDebug("CardScrollingViewController: did scroll")
        // Limit bounce on the bottom to keep part of the card in view.
        let maxY = cardOffset * CGFloat(cardViewControllers.count-1) + cardOffset/2
        if (scrollView.contentOffset.y > maxY ) {
            scrollView.contentOffset = CGPoint(x: 0, y: maxY)
        }
        
        updateCardTransforms()
        
        // On scroll, update whether each card is currently visible.
        let visibleIndexes = visibleCardIndexSet(strict: false)
        for (index, cardViewController) in cardViewControllers.enumerated() {
            cardViewController.isVisibleInCardScroller = visibleIndexes.contains(index)
        }
        delegate?.cardScrollerScrollviewMoved()
    }

    private var lastTopCardIndex = 0

    public func scrollViewDidEndDecelerating(_ scrollView: UIScrollView) {
        updateCardTransforms()

        if let topCard = topCard, let index = cardViewControllers.index(of: topCard) {
            let diff = abs(index - lastTopCardIndex)
            //logDebug("card scroller cards flipped = \(diff)")
            delegate?.cardScrollerUserFlippedCards(count: diff)
            lastTopCardIndex = index
        }
    }
    
    // Not called.
    public func scrollViewDidEndScrollingAnimation(_ scrollView: UIScrollView) {
        updateCardTransforms()
        fireFeedbackGenerator()
    }
    
    public func scrollViewWillEndDragging(_ scrollView: UIScrollView, withVelocity velocity: CGPoint, targetContentOffset: UnsafeMutablePointer<CGPoint>)
    {
        if CardScrollingViewController.cardsScrollContinuous {
            // If the user has stopped dragging by the top card leave it tracking so that it
            // doesn't suddenly get a transform and jump upon release.  Otherwise release
            // the card to transform normally.
            if cardBeingDragged != topCard {
                cardBeingDragged = nil
            }
        } else {
            // Calculate the nearest stopping point
            let stopY = nearestScrollviewStoppingY( targetContentOffset.pointee, velocity )
            // Updating our scroll offset while the refresh control is in use dismisses it.
            if refreshControl == nil || !(refreshControl!).isRefreshing {
                targetContentOffset.initialize( to: CGPoint(x: 0, y: stopY) )
            }

            // User has stopped dragging, release the card to transform normally.
            cardBeingDragged = nil
        }
    }
    
    public func scrollViewDidEndDragging(_ scrollView: UIScrollView, willDecelerate decelerate: Bool) {
        updateCardTransforms()
    }

    // MARK: UIViewController
    
    public override func viewDidLoad() {
        super.viewDidLoad()
        
        view.addSubview(scrollView)
        scrollView.delegate = self
        
        let tapGestureRecognizer = UITapGestureRecognizer(target: self, action: #selector(tapDetected(_:)))
        view.addGestureRecognizer(tapGestureRecognizer)
    }
    
    public override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        logn("\(name): view will appear, scrollView.contentOffset = \(scrollView.contentOffset)")
    }
    
    public override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        logn("\(name): view did appear, scrollView.contentOffset = \(scrollView.contentOffset)")
        //logn("card[0] frame = \(cardViewControllers[0].view.frame)")
    }
    
    public override func viewWillDisappear(_ animated: Bool) {
        // Stop any scrolling instantly
        let offset = scrollView.contentOffset
        let stopAt = nearestScrollviewStoppingY(offset, CGPoint(x:0, y:0))
        scrollView.setContentOffset(CGPoint(x:0, y:stopAt), animated: false)
    }

    public override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        
        scrollView.sizeToFitSuperview()
        
        let cardWidth = scrollView.bounds.width - 2.0 * xpad
        let cardHeight = PixelRounder(for: view).roundToPixel(9.0 * cardWidth / 16.0)
            + CardViewController.separatorHeight
            + CardViewController.footerHeight
        
        logn("\(name): did layout subviews: cardWidth: \(cardWidth), cardHeight: \(cardHeight)")
        layoutCards()

        if cardWidth != self.cardWidth || cardHeight != self.cardHeight {
            self.cardWidth = cardWidth
            self.cardHeight = cardHeight
            //logDebug("CardScrollingViewController: card width or height changed")
            if let groups = groups {
                configure(with: groups)
            }
        }
    }
    
    private func layoutCards()
    {
        // TODO: Who is changing our bounds after initial install?  Why is this necessary?
        for (cardIndex, cardViewController) in cardViewControllers.enumerated() {
            cardViewController.view.frame = CGRect(x: xpad, y: getCardBaseY(cardIndex), width: cardWidth, height: cardHeight)
            //log("\(name): layout cards, card: \(cardIndex), y=\(getCardBaseY(cardIndex)), frame=\(cardViewController.view.frame)")
        }
    }

    // MARK: Public Methods

    public func configure(with groups: [Group])
    {
        //log("\(name): configure with groups: \(groups.count), space filter = \(spaceFilter)")
        self.groups = groups
        
        guard cardHeight != 0 && cardWidth != 0 else { return }
        
        // Note: This logic is convoluted due to the (changing) requirements surrounding when to show women
        // owned businesses.
        let filteredGroupsToShow = groups.filter { group in

            // group tagged with space
            func matchesSpace(spaceId: String) -> Bool {
                guard let groupSpace = group.space else {
                    // group space is nil, allow to to match "everyone"
                    return spaceId == PresentSpace.everyone.id
                }
                return groupSpace.id == spaceId
            }
            
            // group tagged with category
            func matchesCategory(category: String) -> Bool {
                return group.categories.contains(category)
            }

            // group matches search
            func matchesSearch(search: String) -> Bool {
                return search.components(separatedBy: .whitespaces).reduce(true) { (matches, searchText) -> Bool in
                    return matches && (searchText.count == 0 || group.title.localizedCaseInsensitiveContains(searchText))
                }
            }

            //
            // Apply the filters
            //
            
            if let spaceFilter = spaceFilter {
                if !matchesSpace(spaceId: spaceFilter) { return false }
            }
            
            if let searchFilter = searchFilter {
                if !matchesSearch(search: searchFilter) { return false }
            }
            
            if let categoryFilter = categoryFilter {
                if !matchesCategory(category: categoryFilter) { return false }
            }
            
            return true
        }

        delegate?.cardScrolling(viewController: self, isShowingGroups: filteredGroupsToShow)
        
        // TODO: It's currently awkward to compare the read status of all of the cards.
        // TODO: Once we have this from the server reinstate this basic equivalence comparison.
        let currentGroups = cardViewControllers.compactMap { $0.currentGroup }
        //logDebug("current groups == groupsShown: \(currentGroups == groupsShown)")
        guard currentGroups != filteredGroupsToShow else {
            // TODO: Same cards by equivalence in the same order, just update read status
            // TODO: since that is not yet represented in the metadata.
            updateJoinedAndReadStatus()
            return
        }

        // TODO: There is no reason to rebuild the card scroller from scratch here if we have cards
        // TODO: We should just add/drop and repopulate, minimizing visible disruption.

        // Clear the scrollview and initialize all cards
        cardViewControllers.forEach { uninstallChild(viewController: $0) }
        cardViewControllers.removeAll()
        cardViewControllersToLastOffsetMap.removeAll()
        
        // Lay out the card views
        //logDebug("card scroller laying out cards with height: \(cardHeight)")
        for (_, group) in filteredGroupsToShow.enumerated() {
            // Create the card view controller.
            let cardViewController = CardViewController(imageManager: ImageManager.shared, mapSnapshotManager: MapSnapshotManager.shared, locationProvider: applicationServices.locationManager)
            
            // Install the child view controller.
            installChild(viewController: cardViewController, in: scrollView) { _ in }
            
            // Now that the child is installed and has the correct frame size, set the current group.
            cardViewController.currentGroup = group
            
            // Keep track of the child card view controller.
            cardViewControllers.append(cardViewController)
        }
        layoutCards()
        
        cardViewControllers.reversed().forEach { scrollView.bringSubview(toFront: $0.view) }
        
        if let refreshControl = refreshControl {
            // Make sure the refresh control is underneath all of our content.
            scrollView.sendSubview(toBack: refreshControl)
        }
        
        // Set the scrollview content size to allow all but one to scroll off (if there were no bounce).
        let height = scrollView.frame.height
        // After the top card, how many obscured cards fit on the screen
        let bottomPad : CGFloat = height - (cardHeight+ypad)
        scrollView.contentSize = CGSize(width: view.frame.width, height: cardHeight + cardOffset * CGFloat(filteredGroupsToShow.count - 1) + bottomPad)

        // Do the drop in animation (once).
        if animateCardDropIn {
            doAnimateCardDropIn()
            animateCardDropIn = false
        } else {
            let visibleCardIndexes = visibleCardIndexSet(strict: false)
            for (cardIndex, cardViewController) in cardViewControllers.enumerated() {
                cardViewController.isVisibleInCardScroller = visibleCardIndexes.contains(cardIndex)
            }
        }
        logn("\(name): finished card configure")
        view.setNeedsLayout()
    }

    /// TODO: Temporary, allows us to update the volatile card status without rebuilding the cards for now.
    public func updateJoinedAndReadStatus() {
        cardViewControllers.forEach {
            $0.updateReadStatus()
            $0.updateJoinedStatus()
        }
    }
    
    /// Apply the search category
    public func filter(category: String?) {
        if let category = category, category.count > 0 {
            categoryFilter = category
        } else {
            categoryFilter = nil
        }
        
        scrollToTopCard(animated: false)
    }
    
    public func filter(text: String?) {
        if let searchText = text, searchText.count > 0 {
            searchFilter = searchText
        } else {
            searchFilter = nil
        }
    }
    
    public func filter(spaceId: String?) {
        spaceFilter = spaceId
    }

    @objc public func searchTextDidChange(_ sender: AnyObject) {
        guard let searchField = sender as? UITextField else { return }
        let text = searchField.text
        //logEvent(.home_search_search_text, stringProperties: [LoggingKey.searchText: text])
        logEvent(.home_search_search_text)
        filter(text: text)
    }
    
    // MARK: Private Properties
    
    // Card drop-in animation params. We should make a little HUD to tweak these.
    private let cardDropBaseDuration = 0.5
    private let cardDropInterCardDelay = 0.06
    private let cardDropDamping = 0.7 // 1.0 is critically damped (no oscillation), lower allows more bouncing
    private let cardDropVelocity = 12.0 // 5.0 - 20.0 might be reasonable here
    private var animateCardDropIn = false
    
    private var cardWidth: CGFloat = 0.0
    private var cardHeight: CGFloat = 0.0
    
    private let feedbackGenerator = UISelectionFeedbackGenerator()
    
    let scrollView = UIScrollView()
    
    private var cardViewControllers = [CardViewController]()
    // Map the card view controller to the previous, normalized (0.0-1.0) card offset for the card in the card scroller.
    // This is used in determining when the card should "tick" with haptic feedback by crossing a threshold.
    private var cardViewControllersToLastOffsetMap = [CardViewController : CGFloat]()
    
    /// The card under the user's finger.
    private var cardBeingDragged: CardViewController?
    
    // Get the topmost visible card by looking at frame position.
    private var topCard : CardViewController? {
        return cardViewControllers.filter { $0.view.frame.maxY > scrollView.contentOffset.y }.first
    }
    
    private var categoryFilter: String? {
        didSet {
            if let groups = groups {
                configure(with: groups)
            }
        }
    }
    private var searchFilter: String? {
        didSet {
            if let groups = groups {
                configure(with: groups)
            }
        }
    }
    /// space id
    private var spaceFilter: String? {
        didSet {
            if let groups = groups {
                configure(with: groups)
            }
        }
    }
    
    private func getTranformedFrame(cardViewController: CardViewController) -> CGRect {
        let frame = cardViewController.view.frame
        return frame.offsetBy(dx: 0, dy: getCardOffsetAndAlpha(frame.origin.y).offset)
    }
    
    /// Computes the current set of visible card indexes.
    /// This may exceed the total set of visible indexes in either direction, but will always be
    /// bounded to the number of card view controllers under management.
    ///
    /// By default the visible card set is calculated with some padding (a number of cards) on each side of the
    /// visible area and calculations are based on the full frame of the card.
    /// If the param strict is set to true this method will attempt to include only cards which have an actual 
    /// visible portion showing on the screen, taking into account the overlapping card geometry and including no
    /// extra card padding.
    private func visibleCardIndexSet(strict: Bool = false) -> IndexSet
    {
        let scrollViewVisibleRect = CGRect(
            x: scrollView.contentOffset.x,
            y: scrollView.contentOffset.y,
            width: scrollView.frame.size.width,
            height: scrollView.frame.size.height
        )
        
        var minimumLocatedIndex: Int?
        var maximumLocatedIndex: Int?
        
        // Compute the minimum and maximum card indexes visible on screen at the moment.
        for (cardIndex, cardViewController) in cardViewControllers.enumerated()
        {
            // TODO: Shouldn't be using frame in calcs below.  The view's frame may be invalid when transforms are applied.  We just need to reconstruct it from the center and bounds (here and in getTransformedFrame())
            let frame = strict ?
                // Look at the transformed position of the bottom, non-overlapping portion of each card.
                getTranformedFrame(cardViewController: cardViewController)
                    .divided(atDistance: cardHeight-cardOffset, from: .minYEdge).remainder
                :
                    cardViewController.view.frame
                
            let viewControllerIsOnScreen = frame.intersects(scrollViewVisibleRect)
            if viewControllerIsOnScreen {
                if minimumLocatedIndex == nil {
                    minimumLocatedIndex = cardIndex
                }
                
                maximumLocatedIndex = cardIndex
            }
        }
        
        // If no indexes were located, return an empty index set.
        guard let minimumVisibleIndex = minimumLocatedIndex, let maximumVisibleIndex = maximumLocatedIndex else {
            return IndexSet()
        }
        
        // Expand the index set to ensure smooth scrolling.
        let additionalVisibleCardCount = strict ? 0 : 3
        let expandedMinimumIndex = max((minimumVisibleIndex - additionalVisibleCardCount), 0)
        let expandedMaximumIndex = min((maximumVisibleIndex + additionalVisibleCardCount), cardViewControllers.count - 1)
        
        return IndexSet(integersIn: expandedMinimumIndex ... expandedMaximumIndex)
    }
    
    // MARK: Card drop and restore transition effect
    
    public var cardsAreOffscreen : Bool {
        return !droppedCardIndexes.isEmpty
    }
    
    // Cards that have been "dropped" offscreen, to be restored 
    private var droppedCardIndexes = IndexSet()
    
    let dropCardsOffscreenBaseDuration = 0.5
    let dropCardsOffscreenInterCardDelay = 0.06
    var orgBackgroundColor = UIColor.lightGray
    
    // Drop the cards offscreen towards the bottom
    public func dropCardsOffscreen(complete: @escaping ()->())
    {
        guard !cardsAreOffscreen else { return }
        
        // Stop any scrollview velocity
        if scrollView.isDecelerating {
            scrollView.setContentOffset(
                CGPoint(x:0, y:nearestScrollviewStoppingY(scrollView.contentOffset, CGPoint(x:0,y:0))), animated: false)
        }
        
        // Reveal the background
        orgBackgroundColor = view.backgroundColor ?? orgBackgroundColor
        view.backgroundColor = .clear
        
        // Drop each visible card offscreen with a successively increasing delay
        let visibleCardIndexes = visibleCardIndexSet(strict: true)
        let visibleCardCount = visibleCardIndexes.count
        let firstVisibleCardIndex = visibleCardIndexes.min() ?? 0
        let lastVisibleCardIndex = visibleCardIndexes.max() ?? 0
        for (cardIndex, cardViewController) in cardViewControllers.enumerated()
        {
            guard visibleCardIndexes.contains(cardIndex) else {
                cardViewController.view.isHidden = true
                continue
            }
            
            let duration = dropCardsOffscreenBaseDuration
            let delay = Double((visibleCardCount-1) - (cardIndex-firstVisibleCardIndex)) * dropCardsOffscreenInterCardDelay
            
            UIView.animate(
                withDuration: duration,
                delay: delay,
                options: [.allowUserInteraction],
                animations: {
                    cardViewController.view.transform = CGAffineTransform(translationX: 0, y: self.scrollView.bounds.height)
                }, completion: { isComplete in
                    if cardIndex == lastVisibleCardIndex {
                        self.droppedCardIndexes = visibleCardIndexes
                        complete()
                    }
                }
            )
        }
    }
    
    // TODO: This should not rely on the dropped card set, rather it should parallel the drop method and calculate
    // TODO: the visible cards dynamically (allowing for the card set to have been modified while offscreen).
    // TODO: This is a temporary workaround until we fix the visibleCardIndexes() method to operate properly without
    // TODO: relying on the view frames (which may be transformed or hidden).
    // Return dropped cards to the screen
    public func returnCardsToScreen(complete: @escaping ()->())
    {
        guard !droppedCardIndexes.isEmpty else { return }
        
        let firstDroppedCardIndex = droppedCardIndexes.min() ?? 0
        let lastDroppedCardIndex = droppedCardIndexes.max() ?? 0
        
        for (cardIndex, cardViewController) in cardViewControllers.enumerated()
        {
            guard droppedCardIndexes.contains(cardIndex) else {
                continue
            }
            
            let duration = dropCardsOffscreenBaseDuration
            let delay = Double(cardIndex - firstDroppedCardIndex) * dropCardsOffscreenInterCardDelay
            
            UIView.animate(
                withDuration: duration,
                delay: delay,
                options: [.allowUserInteraction],
                animations: {
                    cardViewController.view.transform = .identity
                }, completion: { isComplete in
                    if cardIndex == lastDroppedCardIndex {
                        for cardViewController in self.cardViewControllers {
                            cardViewController.view.isHidden = false
                        }
                        self.view.backgroundColor = self.orgBackgroundColor
                        self.droppedCardIndexes = IndexSet()
                        complete()
                    }
                }
            )
        }
    }
    
    // MARK: Private Functions
    
    private func doAnimateCardDropIn()
    {
        guard let topCard = cardViewControllers.first?.view else {
            return
        }
        
        // When cards are are allowed to bounce on drop-in the top card may reveal cards behind it briefly.
        // Temporarily extend the top card a view with the background color to hide them.
        let blindFrame = CGRect(
            x: 0,
            y: -topCard.frame.height,
            width: topCard.frame.width,
            height: topCard.frame.height
        )
        
        // The scrollview is transparent, use the card scroller background
        let blindColor = view.backgroundColor
        
        let blind = UIView(frame: blindFrame)
        blind.backgroundColor = blindColor
        topCard.addSubview(blind)
        
        let visibleCardIndexes = visibleCardIndexSet(strict: false)
        let lastVisibleCardIndex = visibleCardIndexes.max() ?? 0
        prepareFeedbackGenerator()
        
        for (cardIndex, cardViewController) in cardViewControllers.enumerated() {
            let originalFrame = cardViewController.view.frame
            cardViewController.isVisibleInCardScroller = visibleCardIndexes.contains(cardIndex)
            
            // Don't animate drop in of cards that are fully offscreen
            if cardIndex > lastVisibleCardIndex { continue }
            
            // Add the card off-screen at the top
            let offset = originalFrame.origin.y + originalFrame.height
            cardViewController.view.frame = originalFrame.offsetBy(dx: 0, dy: -offset)
            
            // Animate down
            // Giving the cards the same velocity would reveal them one after another as desired,
            // but the ease out allows them to spread out.  So we add a slight delay to each successive card.
            let duration = cardDropBaseDuration * (1.0 + Double(offset) / Double(view.bounds.height))
            let delay = Double(cardIndex) * cardDropInterCardDelay
            
            UIView.animate(
                withDuration: duration,
                delay: delay,
                usingSpringWithDamping: CGFloat(cardDropDamping),
                initialSpringVelocity: CGFloat(cardDropVelocity) * view.bounds.height / offset,
                options: [.allowUserInteraction],
                animations: {
                    cardViewController.view.frame = originalFrame
                }, completion: { complete in
                    if cardIndex == lastVisibleCardIndex {
                        blind.removeFromSuperview()
                    }
                }
            )
        }
    }
    
    @objc private func tapDetected(_ tapRecognizer: UITapGestureRecognizer)
    {
        selectedCardViewController = topmostCardAtLocation( tapRecognizer.location(in: scrollView) )
        guard let selectedCard = selectedCardViewController, let selectedGroup = selectedCard.currentGroup else {
            return
        }
        
        if selectedCard == topCard || CardScrollingViewController.tapShowsCardAnyPosition {
            delegate?.cardScrolling(viewController: self, didSelectCardFor: selectedGroup)
            
        } else {
            scrollView.setContentOffset(selectedCard.view.frame.origin.offsetBy(dx: -xpad, dy: -ypad), animated: true)
        }
    }
    
    /**
        The scrollview has been released and is decelerating. Find the closest card position 
        to the scrollview's target final content offset and tell the scrollview to stop there.
        There are two cases:
            a) The users was dragging the topmost card (which tracks the user's touch).
            b) The user was dragging a deeper card, allowing cards to roll off above it.
        @return a y value for the content offset
        @param
    */
    private func nearestScrollviewStoppingY( _ target : CGPoint, _ velocity : CGPoint ) -> CGFloat
    {
        // If the user has dragged down past the top show the search bar
        if scrollView.contentOffset.y <= 0 && velocity.y <= 0 { return 0 }
        if target.y <= 0 { return 0 }
        
        // If the user is dragging the top card and releases it with little or no velocity we return it to position.
        let velocityThreshold : CGFloat = 0.2
        if let topCard = topCard, topCard == cardBeingDragged && abs(velocity.y) <= velocityThreshold {
            // This view's frame is not transformed so it's ok to use its frame values
            return topCard.view.frame.origin.y - ypad // return to the card top
        }
        
        // Calculate the closest card boundary.
        let calcPos = rint(target.y/cardOffset)*cardOffset  // pick a card
        let lastCardPos = CGFloat(cardViewControllers.count-1) * cardOffset
        logn("target.y = \(target.y), calcPos = \(calcPos), lastCardPos=\(lastCardPos)")
        return min(calcPos, lastCardPos) // don't go past final card position
    }
    
    /// Get the topmost / visible card at the touch location
    private func topmostCardAtLocation( _ touchLoc : CGPoint ) -> CardViewController?
    {
        // This is inefficient but succinct. Making it .lazy would fix it but what's the tradeoff for small arrays?
        // indexOf is just awkward.
        return cardViewControllers.filter { $0.view.frame.contains(touchLoc) }.first
    }
    
    // Get the un-transformed, base, y position for the card by card position.
    private func getCardBaseY( _ cardNumber : Int ) -> CGFloat {
        return ypad + CGFloat(cardNumber) * cardOffset // <= cardHeight
    }
    
    // Calculate the offset that accelerates the card off the top of the screen as a function of scroll position.
    // @param YPosition is the (initially positive) un-transformed base position of the top edge of the card in the main view. (Where it would be with no transform.)
    // @return The y offset that should be added by the transform, the alpha, and the raw (normalized) offset value.
    private func getCardOffsetAndAlpha(_ yPosition: CGFloat) -> (offset: CGFloat, alpha: CGFloat, rawOffset: CGFloat) {
        
        // Have we reached the y position at which we start accelerating?
        let accelStartY : CGFloat = ypad
        if yPosition > accelStartY {
            return (0.0, 1.0, 0.0)
        }
        
        // We want the card to go from zero offset to offscreen in the span of this distance
        let rolloffDistance = cardOffset
        
        // yPosition is now less than accelStartY (towards or above the top of the screen)
        // How far into the rollout region is the yPosition, normalized to 0-1.
        let normalizedYPos = (accelStartY-yPosition) / rolloffDistance
        
        // This rolloff function maps the normalized y position (0-1) to a normalized
        // transformed y position (0-1) where 0.0 output is no offset and 1.0 output 
        // is fully off the screen.
        // The rolloff function is: yout = (yin^power)*const
        // where Yin is the normalized y position calculated above, re-normalized 
        // using rolloffFuncMax to reach 1.0 at the point the rolloff func does.
        let val = pow(normalizedYPos*CardScrollingViewController.rolloffFuncMax, CardScrollingViewController.power) * CardScrollingViewController.const
        
        // Denormalize the value such that 1.0 has moved the card completely offscreen.
        let offset = -val * ((cardHeight-rolloffDistance)+2*ypad)
        // And alpha is transparent at the same point
        let alpha = transformAlpha ? 1.0-val : 1.0
        return (offset,alpha,val)
    }
    
    private func updateCardTransforms()
    {
        //logDebug("CardScrollingViewController: update card xforms")
        cardViewControllers.forEach {
            $0.view.transform = .identity
        }
        
        for (cardIndex, cardViewController) in cardViewControllers.enumerated() {
            guard cardViewController != cardBeingDragged else {
                // stop at the card the user is driving
                break
            }
            
            let baseY = getCardBaseY(cardIndex)
            
            // card offset using card top edge y distance from top of scrollview
            let vals = getCardOffsetAndAlpha( baseY - scrollView.contentOffset.y )
            cardViewController.view.transform = CGAffineTransform(translationX: 0, y: vals.offset)
            cardViewController.view.alpha = vals.alpha
            fireFeedbackIfNecessary(for: cardViewController, scrollOffset: vals.rawOffset)
            cardViewControllersToLastOffsetMap[cardViewController] = vals.rawOffset
        }
    }
    
    private func prepareFeedbackGenerator() {
        feedbackGenerator.prepare()
    }
    
    private func fireFeedbackGenerator() {
        feedbackGenerator.selectionChanged()
        
        // Now prepare for the next feedback.
        prepareFeedbackGenerator()
    }
    
    /// Scroll to the top card position with the search bar offscreen
    private func scrollToTopCard(animated: Bool = true) {
        self.scrollView.setContentOffset(CGPoint(x:0,y:0), animated: animated)
    }
    
    /// Determine when feedback should be played
    /// @param cardOffset is the raw 0-1.0 offset of the card position.
    private func fireFeedbackIfNecessary(for cardViewController : CardViewController, scrollOffset : CGFloat) {
        let lastOffset = cardViewControllersToLastOffsetMap[cardViewController] ?? 0.0
        
        // Tick if the bottom of a card just crossed the top of the screen.
        if (lastOffset < 1.0 && scrollOffset >= 1.0) || (lastOffset >= 1.0 && scrollOffset < 1.0) {
            fireFeedbackGenerator()
        }
    }
    
    // MARK: Public Enum
    
    public enum Layout {
        case stacked
        case sequential
    }
}
