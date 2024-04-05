//
//  PresentTabmanLineIndicator.swift
//  Present
//
//  Created by Patrick Niemeyer on 5/15/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import Tabman
import Then
import MIBadgeButton_Swift

/// Present Tabman extensions and utilities
public class PresentTabs
{
    public class func configurePresentTabs(bar: TabmanBar.Config, noTabs: Bool = false)
    {
        bar.style = .buttonBar
        bar.location = .top
        //bar.behaviors = [.autoHide(.withOneItem)]
        bar.appearance = TabmanBar.Appearance {
            $0.indicator.bounces = false
            $0.indicator.compresses = false
            $0.style.background = .solid(color: .white)
            $0.style.imageRenderingMode = .alwaysOriginal
            $0.state.color = UIColor(hex: 0x999999) // text unselected
            $0.state.selectedColor = UIColor(hex: 0x333333) // text selected
            $0.indicator.color = UIColor(hex: 0x743CCE) // the bar indicator
            $0.indicator.lineWeight = .normal
            $0.indicator.preferredStyle = TabmanIndicator.Style.custom(type: PresentLineIndicator.self)
            
            // Padding added above and below the item text.
            // Note: Seems to have a minimum of about the text height. We are compensating in the FeedTabViewController.xib by leaving little or no padding below the title
            $0.layout.itemVerticalPadding = 0.0
            //$0.layout.height = TabmanBar.Height.explicit(value: 32.0) // defaults to auto
            
            // The leading inset of the first tab item and trailing of the last tab item
            // (This does not affect the content, just the tab itself)
            $0.layout.edgeInset = 20.0
            
            $0.layout.itemDistribution = .centered
            $0.layout.interItemSpacing = 0.0
            
            $0.bottomSeparator.height = SeparatorHeight.custom(height: 1.0)
            $0.bottomSeparator.color = UIColor(hex: 0xF2F2F2)
            $0.bottomSeparator.edgeInsets = .zero
            $0.text.font = UIFont.presentFont(ofSize: 14, weight: .semibold)
        }
        
        // Override appearance for no tabs
        if noTabs {
            class EmptyTabmanBar: TabmanBar { }
            bar.style = .custom(type: EmptyTabmanBar.self)
            bar.appearance = bar.appearance?.with {
                $0.layout.height = TabmanBar.Height.explicit(value: 12.0)
            }
        }
    }
    
    // Generate an image badge for the item if the count is greater than zero
    public class func badgedItem(title: String, badgeCount: Int) -> TabmanBar.Item {
        return badgeCount > 0 ?
            TabmanBar.Item(title: title, image: PresentTabs.renderBadgeForTab(count: badgeCount)) :
            TabmanBar.Item(title: title)
    }
    
    /// Render the badge to be placed to the left of the tab text.
    // Note: Was planning to render the full text and badge button here but TabMan seems to insist that the image be square,
    // Note: so we only render the badge portion and use the Item's built in left image.
    public class func renderBadgeForTab(count: Int)->UIImage
    {
        let button = MIBadgeButton()
        
        // Adjust the badge position here
        button.badgeEdgeInsets = UIEdgeInsets(top: 17, left: 12, bottom: 0, right: 0)
        
        //button.contentEdgeInsets = .zero
        //button.setTitleColor(UIColor(hex: 0x333333), for: .normal)
        //button.setAttributedTitle(
        //title.stylize()
        //.color(UIColor(hex: 0x333333))
        //.font(UIFont.presentFont(ofSize: 24, weight: .semibold))
        //.attr,
        //for: .normal)
        
        button.badgeCount = count
        button.sizeToFit()
        button.setNeedsLayout()
        button.layoutIfNeeded()
        
        UIGraphicsBeginImageContextWithOptions(button.bounds.size, false, UIScreen.main.scale)
        button.drawHierarchy(in: button.bounds, afterScreenUpdates: true)
        let image = UIGraphicsGetImageFromCurrentImageContext().unwrappedOrFatal("unable to render image")
        UIGraphicsEndImageContext()
        return image
    }
    
}

extension TabmanBar.Appearance: Then { }

/// A custom Tabman selected tab line indicator.
/// This is necessary to have complete control over the width and height.
public class PresentLineIndicator: TabmanIndicator
{
    private var lineView = UIView()
    
    override public var intrinsicContentSize: CGSize {
        return CGSize(width: 110.0, height: 3.0)
    }
    
    override public var tintColor: UIColor! {
        didSet {
            lineView.backgroundColor = tintColor
        }
    }
    
    public override func constructIndicator()
    {
        self.backgroundColor = .clear
        self.addSubview(lineView)
        lineView.set(.height, to: intrinsicContentSize.height)
        lineView.set(.width, to: intrinsicContentSize.width)
        lineView.alignToSuperviewAxis(.vertical)
        lineView.pinToSuperviewEdge(.bottom, inset: 0.0)
        lineView.backgroundColor = self.tintColor
    }
}

// Copy Tabman's layout extensions for convenience here until we choose ones we prefer.
private extension UIView
{
    enum Edge {
        case top
        case leading
        case bottom
        case trailing
        case left
        case right
    }
    
    enum Dimension {
        case width
        case height
    }
    
    enum Axis {
        case horizontal
        case vertical
    }
    
    @available (iOS 11, *)
    @discardableResult
    func pinToSafeArea(layoutGuide: UILayoutGuide) -> [NSLayoutConstraint] {
        return addConstraints(priority: .required, { () -> [NSLayoutConstraint] in
            return [
                self.topAnchor.constraint(equalTo: layoutGuide.topAnchor),
                self.leadingAnchor.constraint(equalTo: layoutGuide.leadingAnchor),
                self.bottomAnchor.constraint(equalTo: layoutGuide.bottomAnchor),
                self.trailingAnchor.constraint(equalTo: layoutGuide.trailingAnchor)
            ]
        })
    }
    
    @discardableResult
    func pinToSuperviewEdges(priority: UILayoutPriority = .required) -> [NSLayoutConstraint] {
        let superview = guardForSuperview()
        
        return addConstraints(priority: priority, { () -> [NSLayoutConstraint] in
            return [
                self.topAnchor.constraint(equalTo: superview.topAnchor),
                self.leadingAnchor.constraint(equalTo: superview.leadingAnchor),
                self.bottomAnchor.constraint(equalTo: superview.bottomAnchor),
                self.trailingAnchor.constraint(equalTo: superview.trailingAnchor)
            ]
        })
    }
    
    @discardableResult
    func pinToSuperviewEdge(_ edge: Edge, inset: CGFloat = 0.0, priority: UILayoutPriority = .required) -> NSLayoutConstraint {
        let superview = guardForSuperview()
        return pinEdge(edge, to: edge, of: superview, inset: inset, priority: priority)
    }
    
    @discardableResult
    func pinEdge(_ edge: Edge,
                 to otherEdge: Edge,
                 of view: UIView,
                 inset: CGFloat = 0.0,
                 priority: UILayoutPriority = .required) -> NSLayoutConstraint {
        
        let constraints = addConstraints(priority: priority, { () -> [NSLayoutConstraint] in
            switch edge {
            case .top:
                return [self.topAnchor.constraint(equalTo: yAnchor(for: otherEdge, of: view))]
            case .leading:
                return [self.leadingAnchor.constraint(equalTo: xAnchor(for: otherEdge, of: view))]
            case .bottom:
                return [self.bottomAnchor.constraint(equalTo: yAnchor(for: otherEdge, of: view))]
            case .trailing:
                return [self.trailingAnchor.constraint(equalTo: xAnchor(for: otherEdge, of: view))]
            case .left:
                return [self.leftAnchor.constraint(equalTo: xAnchor(for: otherEdge, of: view))]
            case .right:
                return [self.rightAnchor.constraint(equalTo: xAnchor(for: otherEdge, of: view))]
            }
        })
        guard let constraint = constraints.first else {
            fatalError("Failed to add constraint for some reason")
        }
        
        constraint.constant = actualInset(for: edge, value: inset)
        return constraint
    }
    
    @discardableResult
    func match(_ dimension: Dimension, of view: UIView, priority: UILayoutPriority = .required) -> NSLayoutConstraint {
        let constraints = addConstraints(priority: priority, { () -> [NSLayoutConstraint] in
            let attribute: NSLayoutAttribute = (dimension == .width) ? .width : .height
            return [NSLayoutConstraint(item: self,
                                       attribute: attribute,
                                       relatedBy: .equal,
                                       toItem: view,
                                       attribute: attribute,
                                       multiplier: 1.0,
                                       constant: 0.0)]
        })
        return constraints.first!
    }
    
    @discardableResult
    func set(_ dimension: Dimension, to value: CGFloat, priority: UILayoutPriority = .required) -> NSLayoutConstraint {
        return addConstraints(priority: priority, { () -> [NSLayoutConstraint] in
            switch dimension {
            case .width:
                return [self.widthAnchor.constraint(equalToConstant: value)]
            case .height:
                return [self.heightAnchor.constraint(equalToConstant: value)]
            }
        }).first!
    }
    
    @discardableResult
    func alignToSuperviewAxis(_ axis: Axis, priority: UILayoutPriority = .required) -> NSLayoutConstraint {
        let superview = guardForSuperview()
        
        return addConstraints(priority: priority, { () -> [NSLayoutConstraint] in
            let attribute: NSLayoutAttribute = (axis == .horizontal) ? .centerY : .centerX
            return [NSLayoutConstraint(item: self,
                                       attribute: attribute,
                                       relatedBy: .equal,
                                       toItem: superview,
                                       attribute: attribute,
                                       multiplier: 1.0,
                                       constant: 0.0)]
        }).first!
    }
    
    func setContentCompressionResistance(for axis: Axis, to priority: UILayoutPriority) {
        let axis: UILayoutConstraintAxis = (axis == .horizontal) ? .horizontal : .vertical
        self.setContentCompressionResistancePriority(priority, for: axis)
    }
    
    // MARK: Utilities
    
    private func prepareForAutoLayout(_ completion: () -> Void) {
        self.translatesAutoresizingMaskIntoConstraints = false
        completion()
    }
    
    @discardableResult
    private func addConstraints(priority: UILayoutPriority, _ completion: () -> [NSLayoutConstraint]) -> [NSLayoutConstraint] {
        let constraints = completion()
        constraints.forEach({ $0.priority = priority })
        prepareForAutoLayout {
            NSLayoutConstraint.activate(constraints)
        }
        return constraints
    }
    
    private func guardForSuperview() -> UIView {
        guard let superview = self.superview else {
            fatalError("No superview for view \(self)")
        }
        return superview
    }
    
    private func actualInset(for edge: Edge, value: CGFloat) -> CGFloat {
        switch edge {
        case .trailing, .right, .bottom:
            return -value
            
        default:
            return value
        }
    }
    
    private func yAnchor(for edge: Edge, of view: UIView) -> NSLayoutAnchor<NSLayoutYAxisAnchor> {
        switch edge {
        case .top:
            return view.topAnchor
        case .bottom:
            return view.bottomAnchor
        default:
            fatalError("Not a valid Y axis anchor")
        }
    }
    
    private func xAnchor(for edge: Edge, of view: UIView) -> NSLayoutAnchor<NSLayoutXAxisAnchor> {
        switch edge {
        case .leading:
            return view.leadingAnchor
        case .trailing:
            return view.trailingAnchor
        case .left:
            return view.leftAnchor
        case .right:
            return view.rightAnchor
        default:
            fatalError("Not a valid X axis anchor")
        }
    }
}

