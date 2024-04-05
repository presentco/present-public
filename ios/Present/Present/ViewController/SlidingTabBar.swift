//
//  SlidingTabBar.swift
//  SlidingTabBar
//
//  Created by Martin Mroz on 4/24/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import Relativity
import UIKit


public protocol SlidingTabBarDelegate : class {
    
    /// Invoked at the beginning of an animated slide from the currently selected tab to `item` at `index`.
    func slidingTabBar(_ slidingTabBar: SlidingTabBar, willSelect item: SlidingTabBar.Item, at index: Int)
    
    /// Invoked at the end of an animated slide from the currently selected tab to `item` at `index`.
    func slidingTabBar(_ slidingTabBar: SlidingTabBar, didSelect item: SlidingTabBar.Item, at index: Int)
    
}


public final class SlidingTabBar : UIView
{
    public let bullet:Character = "•"
    public let badgedTextColor = UIColor(hex: 0x5337C2)
    
    // MARK: Animation Parameters
    
    public static let animationDuration: TimeInterval = 0.25
    public static let animationOptions: UIViewAnimationOptions = .curveEaseInOut
    
    // MARK: View Model
    
    public enum Item {
        case text(String)
        case attributedText(NSAttributedString)
        case fixedSpace(CGFloat)
        case flexibleSpace
        
        /// True if the receiver can be selected.
        public var isSelectable: Bool {
            switch self {
            case .text, .attributedText:
                return true
                
            case .fixedSpace, .flexibleSpace:
                return false
            }
        }
    }
    
    // MARK: View Theme
    
    public struct Theme {
        public var textColor: UIColor
        public var unselectedTextColor: UIColor
        public var textFont: UIFont
        public var unselectedTextFont: UIFont
        public var backgroundColor: UIColor
        public var underlineColor: UIColor
        public var underlineHeight: CGFloat
        public var underlineOffset: UIOffset
        
        /// A particularly obnoxious theme to remind implementors to specify their own.
        static let Default = Theme(
            textColor: .white,
            unselectedTextColor: .orange,
            textFont: .systemFont(ofSize: 17.0),
            unselectedTextFont: .systemFont(ofSize: 17.0),
            backgroundColor: .red,
            underlineColor: .green,
            underlineHeight: 3.0,
            underlineOffset: UIOffset(horizontal: 0.0, vertical: 5.0)
        )
    }
    
    // MARK: Initialization
    
    public required override init(frame: CGRect) {
        super.init(frame: frame)
        
        commonInitializer()
    }
    
    public convenience init() {
        self.init(frame: .zero)
        
        commonInitializer()
    }
    
    public required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        
        commonInitializer()
    }
    
    private func commonInitializer() {
        addSubview(underlineView)
    }
    
    // MARK: Hashable
    
    public override var hashValue: Int {
        return uuid.hashValue
    }
    
    // MARK: UIView
    
    public override func layoutSubviews() {
        super.layoutSubviews()
        
        let layout = computeLayout(of: items, over: bounds.width)
        
        var lastLayoutReferenceView: UIView? = nil
        
        // Lay out all the buttons within the bar.
        for (itemMetrics, itemView) in zip(layout, itemViews) {
            itemView.bounds.size = CGSize(width: itemMetrics.width!, height: bounds.height)
            
            if let referenceView = lastLayoutReferenceView {
                itemView.bottomLeft --> referenceView.bottomRight
                
            } else {
                itemView.bottomLeft --> bottomLeft
            }
            
            lastLayoutReferenceView = itemView
        }
        
        // Hide the underline view until layout determines it should be shown.
        underlineView.isHidden = true
        
        // Lay out the selection line in relation to a button where possible.
        if let selectionIndex = selectedItemIndex {
            bringSubview(toFront: underlineView)
            
            let relativeButton = itemViews[selectionIndex]
            
            if let titleLabel = relativeButton.titleLabel {
                relativeButton.layoutIfVisible()
                
                underlineView.bounds.size = CGSize(width: layout[selectionIndex].width!, height: theme.underlineHeight)
                underlineView.top --> titleLabel.bottom + theme.underlineOffset
                underlineView.isHidden = false
            }
        }
    }
    
    // MARK: UI Bound Methods
    
    @objc private func selectItemCorrespondingTo(_ sender: ThemeableButton) {
        guard let index = itemViews.index(of: sender) else {
            return
        }
        
        if selectedItemIndex == index {
            return
        }
        
        let targetItem = items[index]
        
        // Notify the delegate that a user initiated a switch from one tab to another.
        delegate?.slidingTabBar(self, willSelect: targetItem, at: index)
        
        // Select the tab and notify the delegate when complete.
        let successful = selectItemAt(index: index, animated: true) { [weak self] in
            guard let strongSelf = self else {
                return
            }
            
            strongSelf.delegate?.slidingTabBar(strongSelf, didSelect: targetItem, at: index)
        }
        
        if !successful {
            logError("Selection of \(sender) in view hierarchy should not fail.")
        }
    }
    
    // MARK: Public Properties
    
    /// The index of the currently selected item, or `nil`.
    private(set) public var selectedItemIndex: Int? = nil
    
    /// Appearance configuration.
    public var theme = Theme.Default {
        didSet {
            applyTheme()
            setNeedsLayout()
        }
    }
    
    /// Array of tab bar items to display.
    /// On change, invalidates the selected item index and any specified enabled/disabled tab states.
    public var items: [Item] = [] {
        didSet {
            selectedItemIndex = nil
            rebuildItemViews()
            applyTheme()
            setNeedsLayout()
        }
    }
    private var badgeState = [Bool]() // see rebuildItemViews
    
    /// The delegate for the sliding tab bar.
    public weak var delegate: SlidingTabBarDelegate? = nil
    
    // MARK: Public Methods
    
    /// Selects the item at `index`. This method does not invoke any delegate methods and is
    /// used for programmatic selection of items. When `animated`, utilizes the class-level animation
    /// curve and duration parameters.
    ///
    /// - Parameter index: The index of the sliding tab bar item to select.
    /// - Parameter animated: When `true`, select the item animated with the class-level curve and duration.
    /// - Parameter completion: Block to be invoked once the specified item has been selected.
    ///   This happens synchronously when `animated` is `false`.
    /// - Returns: `true` if the item was selected successfully.
    @discardableResult
    public func selectItemAt(index: Int, animated: Bool, completion: @escaping () -> Void = {}) -> Bool
    {
        //guard case .text = items[index] else { return false }
        switch items[index] {
        case .fixedSpace, .flexibleSpace:
            return false
        case .text, .attributedText:
            break
        }

        guard superview != nil && window != nil else {
            selectedItemIndex = index
            applyTheme()
            completion()
            return true
        }

        let hasExistingSelection = selectedItemIndex != nil
        
        // Lay the bar out as it stands right now, update the selected index and lay it out again.
        layoutIfVisible()
        selectedItemIndex = index
        
        let animations: () -> Void = { [weak self] in
            self?.applyTheme()
            self?.layoutIfVisible()
        }
        
        // If animation is requested, perform the next layout pass inside a block.
        if animated && hasExistingSelection {
            isUserInteractionEnabled = false
            
            let duration = SlidingTabBar.animationDuration
            let options = SlidingTabBar.animationOptions
            
            UIView.animate(withDuration: duration, delay: 0.0, options: options, animations: animations, completion: { [weak self] _ in
                guard let strongSelf = self else {
                    return
                }
                
                strongSelf.isUserInteractionEnabled = true
                completion()
            })
            
        } else {
            animations()
            completion()
        }
        
        return true
    }
    
    /// Marks the item at `index` enabled or disabled. This only applies to selectable items.
    /// This will not deselect the current item if it is to be made disabled. If the list of items
    /// is changed, these settings will be discarded.
    ///
    /// - Parameter index: The index of the item to set the enabled state of.
    /// - Parameter enabled: When `true`, the specified item can be selected by a user.
    public func setItem(at index: Int, enabled: Bool) {
        guard items[index].isSelectable else {
            logError("Attempt to change enabled state of non-selectable item at index \(index)")
            return
        }
        
        itemViews[index].isEnabled = enabled
    }

    public func setBadge(at index: Int, on: Bool) {
        badgeState[index] = on
        applyTheme()
    }
    
    private func isBadged(at index: Int) -> Bool {
        return badgeState[index]
    }
    
    public func updateTitle(at index: Int) {
        let button = view(at: index)
        guard case let .text(itemTitle) = items[index] else { return }
        let title = isBadged(at: index) ? String(bullet) + " " + itemTitle : "  " + itemTitle
        button.setTitle(title, for: .normal)
    }

    public func view(at index: Int) -> ThemeableButton {
        return itemViews[index]
    }


    // MARK: Private Methods
    
    private func rebuildItemViews()
    {
        itemViews.forEach { (itemView) in
            itemView.removeFromSuperview()
        }
        
        badgeState = Array(repeating: false, count: items.count)
        itemViews = []

        items.forEach { (item) in
            let newItemView = ThemeableButton()

            switch item
            {
            case let .attributedText(title):
                newItemView.isEnabled = true
                newItemView.apply(viewModel: ThemeableButton.ViewModel(title: .attributedText(title), image: nil))
                newItemView.addTarget(self, action: #selector(selectItemCorrespondingTo(_:)), for: .touchUpInside)
                

            case let .text(title):
                newItemView.isEnabled = true
                newItemView.apply(viewModel: ThemeableButton.ViewModel(title: .text(title), image: nil))
                newItemView.addTarget(self, action: #selector(selectItemCorrespondingTo(_:)), for: .touchUpInside)
                
            case .flexibleSpace, .fixedSpace:
                newItemView.isEnabled = false
            }
            
            itemViews.append(newItemView)
            addSubview(newItemView)
        }
    }

    private func applyTheme() {
        backgroundColor = theme.backgroundColor
        
        for (itemIndex, itemView) in itemViews.enumerated()
        {
            updateTitle(at: itemIndex)
            
            let isBadged = self.isBadged(at: itemIndex)
            let textColor = isBadged ? badgedTextColor : (itemIndex == selectedItemIndex ? .black : theme.unselectedTextColor)
            itemView.backgroundColor = .clear
            // Style the selected tab
            if let selectedItemIndex = selectedItemIndex, itemIndex == selectedItemIndex {
                itemView.apply(theme: ThemeableButton.Theme(
                        textColor: textColor,
                        highlightedTextColor: textColor,
                        font: theme.textFont
                    )
                )
            } else {
                // Style the non-selected tabs
                itemView.apply(theme: ThemeableButton.Theme(
                        textColor: textColor,
                        highlightedTextColor: textColor,
                        font: theme.unselectedTextFont
                    )
                )
            }
        }
        
        underlineView.backgroundColor = theme.underlineColor
    }
    
    // MARK: Private Properties
    
    private let uuid = PresentUUID()
    
    private var itemViews: [ThemeableButton] = []
    private var underlineView: UIView = UIView()
    
    // MARK: Private Types
    
    fileprivate struct ItemLayoutMetrics {
        public var width: CGFloat?
    }
    
}

fileprivate func computeLayout(of items: [SlidingTabBar.Item], over width: CGFloat) -> [SlidingTabBar.ItemLayoutMetrics] {
    var metrics: [SlidingTabBar.ItemLayoutMetrics] = []
    for _ in items {
        metrics.append(SlidingTabBar.ItemLayoutMetrics(width: nil))
    }
    
    var fixedSpaceUsed: CGFloat = 0.0
    var flexibleItemCount: Int = 0
    
    // Pass 1: Assign widths for all fixed-width elements and count flexible items and space occupied.
    for itemIndex in 0 ..< items.count {
        switch items[itemIndex] {
        case let .fixedSpace(space):
            metrics[itemIndex].width = space
            fixedSpaceUsed += space
            
        case .attributedText, .text, .flexibleSpace:
            flexibleItemCount += 1
        }
    }
    
    // If layout is complete, return the result.
    if flexibleItemCount == 0 {
        return metrics
    }
    
    let flexibleSpaceRemaining = max(width - fixedSpaceUsed, 0.0)
    let flexibleSpacePerItem = floor(flexibleSpaceRemaining / CGFloat(flexibleItemCount))
    
    // Pass 2: Assign flexible space to remaining items.
    for itemIndex in 0 ..< items.count {
        if metrics[itemIndex].width == nil {
            metrics[itemIndex].width = flexibleSpacePerItem
        }
    }
    
    return metrics
}
