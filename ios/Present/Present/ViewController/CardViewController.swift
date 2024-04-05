//
//  CardViewController.swift
//  Present
//
//  Created by Dan Federman on 2/3/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import PresentProtos


/// CardViewController manages a single CardView within a CardScrollingViewController
public final class CardViewController: UIViewController, CardViewDelegate
{
    // MARK: Static Properties
    
    public static let footerHeight: CGFloat = 74.0
    public static let separatorHeight: CGFloat = 3.0
    
    // MARK: Properties

    // Note: Set by the card scroller, configured when visibility
    public var currentGroup: Group?

    // The card view that we manage
    private var cardView: CardView?

    // The view model for our card view.  (Why do we own this?)
    private var cardViewModel: CardView.ViewModel? {
        didSet {
            // Update the card view
            if let currentViewModel = cardViewModel {
                cardView?.apply(viewModel: currentViewModel)
            }
        }
    }

    // Set the visibility status of the card. This will trigger building the card if it had not been shown.
    // Note: Set by the card scroller.
    public var isVisibleInCardScroller: Bool = false {
        didSet {
            guard oldValue != isVisibleInCardScroller else { return }
            updateCardView()
        }
    }

    private let mapSnapshotManager: MapSnapshotManager
    private let imageManager: ImageManager
    private let locationProvider: LocationProvider
    private var groupManager:GroupManager!
    
    // MARK: Initialization
    
    public required init(imageManager: ImageManager, mapSnapshotManager: MapSnapshotManager, locationProvider: LocationProvider) {
        self.imageManager = imageManager
        self.mapSnapshotManager = mapSnapshotManager
        self.locationProvider = locationProvider
        
        super.init(nibName: nil, bundle: nil)
    }
    
    required public init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    // MARK: UIViewController
    
    public override func viewDidLoad() {
        super.viewDidLoad()
        updateCardView()
    }
    
    public override func viewDidLayoutSubviews()
    {
        super.viewDidLayoutSubviews()
        
        guard let contentView = cardView else { return }
       
        let currentSnapshotSize = contentView.bounds.size
        let desiredSize = view.bounds.size
        contentView.sizeToFitSuperview()
        
        //logDebug("CardViewController: desired size (view bounds size)= \(desiredSize)")
        if currentSnapshotSize != .zero,
            currentSnapshotSize != desiredSize,
            currentGroup != nil
        {
            // Make sure our snapshot is the right size.
            //logDebug("CardViewController: update card size")
            applyGroupToCardViewModel()
        }
    }
    
    // MARK: Private Methods

    // Update the current view model using the specified block. This triggers the card view to update.
    /// Capture the group associated with the request and attempt to atomically apply the view model changes
    /// only if still applicable (if this card hasn't been recycled?)
    private func updateViewModelForGroup(group: Group, withBlock update: @escaping (inout CardView.ViewModel) -> Void)
    {
        // The group associated with his view controller has changed since the async load completed.
        guard group == currentGroup else { return }

        // The card has scrolled offscreen between the time the update was required and executed.
        guard isVisibleInCardScroller else { return }

        guard var viewModelToUpdate = self.cardViewModel else {
            logError("Card is visible in scrolling view controller, however no view model exists.")
            return
        }

        update(&viewModelToUpdate)

        self.cardViewModel = viewModelToUpdate
    }
    
    private func applyGroupToCardViewModel()
    {
        guard let group = currentGroup else {
            //logDebug("apply group with no current group")
            return
        }

        // Init a new card view model if needed
        if cardViewModel == nil {
            //logDebug("new empty model")
            cardViewModel = emptyViewModel()
        } else {
            //logDebug("reusing card view model")
        }

        // convenience to pass the group
        func updateViewModel(withBlock block: @escaping (inout CardView.ViewModel) -> Void) {
            updateViewModelForGroup(group: group, withBlock: block)
        }

        locationProvider.executeWhenLocationAvailable { (location) in
            updateViewModel() {
                $0.distance = HumanReadable.distanceShortMiles(location.distanceTo(group.location))
            }
        }

        updateReadStatus()
        
        // TODO: Anything to factor out with the formatting in circle view?
        /// Feb 22, 10:52AM-4:37PM
        /// Feb 22, 2:52-4:37PM
        func formatSchedule(schedule: Present.Schedule) -> String
        {
//            let start = Date(millisecondsSince1970: schedule.startTime)
//            let end: Date? = (schedule.hasEndTime && schedule.endTime != schedule.startTime) ? Date(millisecondsSince1970: schedule.endTime) : nil
            let start = schedule.startDate
            let end = schedule.endDate
            
            var elideFirstPeriod = false
            if let end = end {
                let startHour = Calendar.current.component(.hour, from: start)
                let endHour = Calendar.current.component(.hour, from: end)
                elideFirstPeriod = (startHour < 12 && endHour < 12) || (startHour >= 12 && endHour >= 12)
            }
            let format = DateFormatter()
            format.dateFormat = elideFirstPeriod ? "MMM dd, h:mm" : "MMM dd, h:mma"
            let startStr = format.string(from: start)
            if let end = end {
                format.dateFormat = "h:mma"
                let endStr = format.string(from: end)
                return "\(startStr)-\(endStr)"
            } else {
                return startStr
            }
        }

        updateViewModel() {
            $0.title = group.title
            $0.isJoined = group.isJoined
            $0.location = ImageTextView.ViewModel(image: #imageLiteral(resourceName: "pinLocationLabel"), text: group.locationName ?? "")
            
            if let schedule = group.schedule {
                let text = formatSchedule(schedule: schedule)
                $0.schedule = ImageTextView.ViewModel(image: #imageLiteral(resourceName: "time"), text: text)
            }
        }
        
        imageManager.getImage(atURL: group.owner.photoURL, successHandler: { (profileImage) in
            updateViewModel() {
                $0.profile = profileImage
                $0.additionalPariticpantCount = (Int(group.joinedCount) - 1)
            }
        })

        // TODO: We shouldn't be working with sizes here.
        var backgroundImageSize = view.bounds.size
        if let cardView = cardView {
            backgroundImageSize.height -= cardView.gradientHeight
        }
        
        func loadMapImage() {
            mapSnapshotManager.mapSnapshot(at: group.location, size: backgroundImageSize, name: group.title) { (mapSnapshot) in
                updateViewModel() {
                    $0.backgroundImage = mapSnapshot
                }
            }
        }
        
        if let coverPhotoUrl = group.coverPhoto
        {
            imageManager.getImage(atURL: coverPhotoUrl,
                errorHandler: {
                    logDebug("Error loading cover photo for group: \(group.groupToken)")
                    loadMapImage()
                },
                successHandler: { coverPhoto in
                    updateViewModel() {
                        $0.backgroundImage = coverPhoto
                    }
                }
            )
        } else {
            loadMapImage()
        }

        //logDebug("Applied group: \(group.title)")
    }

    // Set the new content badge if needed.
    public func updateReadStatus()
    {
        guard let group = currentGroup else { return }

        //logDebug("Update read status for card: \(title)")
        self.updateViewModelForGroup(group: group) { viewModel in
            viewModel.showNewContentBadge = group.isJoined && (group.isNew || group.isUnread)
        }
    }

    // Set the joined button appropriately
    public func updateJoinedStatus()
    {
        guard let group = currentGroup else { return }

        //logDebug("Update joined status for group: \(group.title)")
        self.updateViewModelForGroup(group: group) { viewModel in
            viewModel.isJoined = group.isJoined
        }
    }

    private func emptyViewModel() -> CardView.ViewModel
    {
        //logDebug("CardViewController: empty view model")
        let defaultImageTextViewModel = ImageTextView.ViewModel(image: UIImage(), text: "")
        let model = CardView.ViewModel(
            backgroundImage: MapSnapshotManager.loadingMapImage,
            profile: nil,
            additionalPariticpantCount: 0,
            title: "",
            location: defaultImageTextViewModel,
            schedule: defaultImageTextViewModel,
            distance: "1.9mi",
            showNewContentBadge: false,
            isJoined: false
        )

        return model
    }

    // If we are visible construct the card view and view model for the group
    // else remove the model and view to save memory.
    private func updateCardView()
    {
        guard isViewLoaded else { return }

        if isVisibleInCardScroller
        {
            // Get the current card view or initialize one if needed.
            let cardViewToUpdate: CardView
            if let cardView = cardView {
                cardViewToUpdate = cardView
            } else {
                cardViewToUpdate = CardView()
                cardViewToUpdate.delegate = self
                cardView = cardViewToUpdate

                // Note: the theme doesn't change per card so we can do this once.
                cardViewToUpdate.applyDefaultTheme()
            }

            if currentGroup != nil {
                //logDebug("CardViewController: Applying current group: \(currentGroup.title)")
                applyGroupToCardViewModel()
            } else {
                logDebug("CardViewController: no current group, constructing empty view.")
                cardViewModel = emptyViewModel()
            }
            
            if cardViewToUpdate.superview == nil {
                view.addSubview(cardViewToUpdate)
                view.layoutIfVisible()
            }
        } else
        {
            //logDebug("clearing non-visible card, group = \(currentGroup?.title ?? "nil")")
            cardView?.removeFromSuperview()

            //  TODO: Are both of these needed?
            cardView = nil
            cardViewModel = nil
        }
    }

    // MARK: Join functionality

    // Return true if there was a change in joined status
    public func joinPressed() -> Bool
    {
        guard let currentGroup = currentGroup else { return false }
        
        // User cannot leave her own group
        if currentGroup.owner.isMe {
            return false
        }

        if currentGroup.isJoined {
            leave()
        } else {
            join()
        }
        return true
    }

    private func join(){
        guard let currentGroup = currentGroup else {return}
        logEvent(.circle_chat_join)
        currentGroup.joinGroup().do { self.updateCardView() }.neverDisposed()

    }

    private func leave() {
        guard let currentGroup = currentGroup else {return}
        logEvent(.circle_chat_leave)
        currentGroup.leaveGroup().onCompleted { self.updateCardView() }.neverDisposed()
    }
}


