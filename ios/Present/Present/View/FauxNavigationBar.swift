//
//  FauxNavigationBar.swift
//  Present
//
//  Created by Dan Federman on 3/27/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import Relativity
import UIKit


//@IBDesignable - Not sure why this isn't working.
public final class FauxNavigationBar: UIView {
    
    // MARK: Initialization

    public init() {
        super.init(frame: .zero)
        initSubviews()
    }
    
    public required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        initSubviews()
    }
    
    override public init(frame: CGRect) {
        super.init(frame: frame)
        initSubviews()
    }
    
    private func initSubviews() {
        addSubview(titleStack)
        addSubview(backButton)
        addSubview(separator)
    }
    
    // MARK: Public Properties
    
    public let titleStack = TitleStack()
    public var title: UILabel {
        return titleStack.titleLabel
    }
    public let backButton = ThemeableButton()
    public var backButtonHorizontalOffset: CGFloat = 20.0 {
        didSet {
            setNeedsLayout()
        }
    }
    public let separator = UIView()
    public var rightView: UIView? {
        didSet {
            oldValue?.removeFromSuperview()
            
            if let rightView = rightView {
                addSubview(rightView)
            }
            
            setNeedsLayout()
        }
    }
    
    // MARK: UIView
    
    public override func layoutSubviews() {
        super.layoutSubviews()
        
        backButton.sizeToFit()
        left + backButtonHorizontalOffset.horizontalOffset <-- backButton.left
        
        if let rightView = rightView {
            rightView.sizeToFit()
            rightView.right --> -20.horizontalOffset + right
        }
        
        let leftInset = (left |--| backButton.right).width
        let rightInset = ((rightView?.left ?? right) |--| right).width
         
        titleStack.sizeToFit(fixedWidth: bounds.width - 2.0 * max(leftInset, rightInset) - 18.0)
        titleStack.middle --> middle
        
        separator.bounds.size = CGSize(width: bounds.width, height: PixelRounder(for: self).pixelHeight)
        separator.bottom --> bottom
    }
    
    // Override hitTest to allow the back button to expand its hit area.
    public override func hitTest(_ point: CGPoint, with event: UIEvent?) -> UIView? {
        guard !self.isHidden && self.alpha > 0.0 && self.isUserInteractionEnabled else {
            // We can't be hit unless we're visible and user interaction is enabled.
            return nil
        }
        if let hit = backButton.hitTest(point, with: event) {
            return hit
        }
        return super.hitTest(point, with: event)
    }
    
    // MARK: Public Methods
    
    public func layoutInSuperview(height: CGFloat = 49.0) {
        guard let superview = superview else {
            logError("Attempting to lay out \(String(describing: type(of: self))) but it lacks a superview")
            return
        }
        
        bounds.size = CGSize(width: superview.bounds.width, height: height)
        
        let statusBarHeight: CGFloat = 20.0
        top --> .top + statusBarHeight.verticalOffset
    }
    
    // MARK: Internal Types
    
    public final class TitleStack: UIView {
        
        // MARK: Initialization
        
        public init() {
            super.init(frame: .zero)
            
            addSubview(titleLabel)
            
            setNeedsLayout()
        }
        
        public required init?(coder aDecoder: NSCoder) {
            fatalError("init(coder:) has not been implemented")
        }
        
        // MARK: Public Properties
        
        public var leftView: UIView? {
            didSet {
                oldValue?.removeFromSuperview()
                
                if let leftView = leftView {
                    addSubview(leftView)
                }
                
                setNeedsLayout()
            }
        }
        
        public let titleLabel = UILabel().then {
            $0.adjustsFontSizeToFitWidth = true
        }
        
        public var rightView: UIView? {
            didSet {
                oldValue?.removeFromSuperview()
                
                if let rightView = rightView {
                    addSubview(rightView)
                }
                
                setNeedsLayout()
            }
        }
        
        public var spaceBetweenTitleLabelAndViews: CGFloat = 9.0 {
            didSet {
                if oldValue != spaceBetweenTitleLabelAndViews {
                    setNeedsLayout()
                }
            }
        }
        
        // MARK: UIView
        
        public override var intrinsicContentSize: CGSize {
            let height = max(
                leftView?.intrinsicContentSize.height ?? 0.0,
                titleLabel.intrinsicContentSize.height,
                rightView?.intrinsicContentSize.height ?? 0.0
            )
            
            var width: CGFloat = 0.0
            
            if let leftView = leftView {
                width += leftView.bounds.width
                width += spaceBetweenTitleLabelAndViews
            }
            
            width += titleLabel.intrinsicContentSize.width
            
            if let rightView = rightView {
                width += spaceBetweenTitleLabelAndViews
                width += rightView.bounds.width
            }
            
            return CGSize(width: width, height: height)
        }
        
        public override func sizeThatFits(_ size: CGSize) -> CGSize {
            return intrinsicContentSize
        }
        
        public override func layoutSubviews() {
            super.layoutSubviews()
            
            // Center the title label, making it no bigger than the bounds.
            //titleLabel.sizeToFit()
            titleLabel.bounds.size = CGSize(width: bounds.width-30, height: bounds.height)
            titleLabel.middle --> middle

            // Figure out how much space is left to attaching left and right controls.
            let remainingSpaceOnLeft = (bounds.width - titleLabel.bounds.width) / 2.0
            let remainingSpaceOnRight = remainingSpaceOnLeft
            
            if let leftView = leftView {
                let spaceRequiredForLeftView = leftView.bounds.width + spaceBetweenTitleLabelAndViews
                
                // Splice out the required amount of space on the left for the left view.
                if remainingSpaceOnLeft < spaceRequiredForLeftView {
                    let shrinkBy = spaceRequiredForLeftView - remainingSpaceOnLeft
                    titleLabel.frame.size.width -= shrinkBy
                    titleLabel.frame.origin.x += shrinkBy
                }
                
                leftView.right --> titleLabel.left - spaceBetweenTitleLabelAndViews.horizontalOffset
            }
            
            if let rightView = rightView {
                let spaceRequiredForRightView = rightView.bounds.width + spaceBetweenTitleLabelAndViews
                
                // Splice out the required amount of space on the right for the left view.
                if remainingSpaceOnRight < spaceRequiredForRightView {
                    let shrinkBy = spaceRequiredForRightView - remainingSpaceOnRight
                    titleLabel.frame.size.width -= shrinkBy
                }
                
                rightView.left --> titleLabel.right + spaceBetweenTitleLabelAndViews.horizontalOffset
            }
        }
        
    }
    
    public var showBackButton: Bool = false {
        didSet {
            if showBackButton {
                backButton.setImage(#imageLiteral(resourceName: "LeftChevronGlyph"), for: .normal)
            }
            backButton.isShown = showBackButton
        }
    }
}

