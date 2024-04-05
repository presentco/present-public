//
//  ThemeableButton.swift
//  Present
//
//  Created by Dan Federman on 1/31/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Relativity
import UIKit

public final class ThemeableButton: BlockTargetButton
{
    // MARK: Theme
    
    public struct Theme {
        
        // MARK: Initialization
        
        public init(textColor: UIColor = .white,
                    highlightedTextColor: UIColor = UIColor.white.withAlphaComponent(Palette.defaultHighlightedButtonTitleAlpha),
                    disabledTitleAlpha: CGFloat = Palette.defaultDisabledButtonTitleAlpha,
                    backgroundColor: UIColor? = nil,
                    font: UIFont = .presentFont(ofSize: 12.0, weight: .regular))
        {
            self.textColor = textColor
            self.highlightedTextColor = highlightedTextColor
            self.disabledTitleAlpha = disabledTitleAlpha
            self.backgroundColor = backgroundColor
            self.font = font
        }
        
        // MARK: Public Properties
        
        public let textColor: UIColor
        public let highlightedTextColor: UIColor
        public let disabledTitleAlpha: CGFloat
        public let backgroundColor: UIColor?
        public let font: UIFont
        
    }
    
    // MARK: Layout
    
    public enum Layout: Equatable {
        case standard
        case horizontalImageTitle(imageTitleHorizontalMargin: CGFloat)
        case verticalImageTitle(imageTitleVerticalMargin: CGFloat)
        case fixed(size: CGSize)
        case inRange(minimumWidth: CGFloat, maximumWidth: CGFloat, height: CGFloat, horizontalPadding: CGFloat)
        
        // MARK: Equatable
        
        public static func ==(lhs: Layout, rhs: Layout) -> Bool {
            switch lhs {
            case .standard:
                if case .standard = rhs {
                    return true
                }
                
            case let .horizontalImageTitle(lhsImageTitleHorizontalMargin):
                if case let .horizontalImageTitle(rhsImageTitleHorizontalMargin) = rhs {
                    return lhsImageTitleHorizontalMargin == rhsImageTitleHorizontalMargin
                }
                
            case let .verticalImageTitle(lhsImageTitleVerticalMargin):
                if case let .verticalImageTitle(rhsImageTitleVerticalMargin) = rhs {
                    return lhsImageTitleVerticalMargin == rhsImageTitleVerticalMargin
                }
                
            case let .fixed(lhsSize):
                if case let .fixed(rhsSize) = rhs {
                    return lhsSize == rhsSize
                }
                
            case let .inRange(min, max, height, horizontalPadding):
                if case let .inRange(rhsMin, rhsMax, rhsHeight, rhsHorizontalPadding) = rhs {
                    return rhsMin == min
                        && rhsMax == max
                        && rhsHeight == height
                        && horizontalPadding == rhsHorizontalPadding
                }
                
            }

            return false
        }
    }
    
    // MARK: View Model
    
    public struct ViewModel {
        
        public init(title: Title, image: UIImage?, highlightedImage: UIImage? = nil) {
            self.title = title
            self.image = image
            self.highlightedImage = highlightedImage
        }
        
        public enum Title {
            case none
            case text(String)
            case attributedText(NSAttributedString)
            case attributed(normal: NSAttributedString, highlighted: NSAttributedString, disabled: NSAttributedString?)
        }
        
        public let title: Title
        public let image: UIImage?
        public let highlightedImage: UIImage?
    }
    
    // MARK: Public Properties
    
    /// Insets that modify the hit target of the button. Negative values expand the hit target.
    public var hitTargetInset = UIEdgeInsets(top: -20.0, left: -20.0, bottom: -20.0, right: -20.0)
    
    public var layout: Layout = .standard {
        didSet {
            if oldValue != layout {
                setNeedsLayout()
            }
        }
    }
    
    // MARK: Initialization
    
    required public init(frame: CGRect = .zero) {
        super.init(frame: frame)
    }
    
    required public init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
    
    // MARK: UIView
    
    public override func hitTest(_ point: CGPoint, with event: UIEvent?) -> UIView? {
        guard !self.isHidden && self.alpha > 0.0 && self.isUserInteractionEnabled else {
            // We can't be hit unless we're visible and user interaction is enabled.
            return nil
        }
        
        let hitRect = CGRect(x: bounds.origin.x + hitTargetInset.left, y: bounds.origin.y + hitTargetInset.top, width: bounds.size.width - hitTargetInset.left - hitTargetInset.right, height: bounds.size.height - hitTargetInset.top - hitTargetInset.bottom)
        
        if hitRect.contains(point) {
            return self
            
        } else {
            return super.hitTest(point, with: event)
        }
    }
    
    public override func sizeThatFits(_ size: CGSize) -> CGSize {
        guard let titleLabel = titleLabel, let font = titleLabel.font else {
            return super.sizeThatFits(size)
        }
        
        func sizeThatFits(sizeBeforeInsets size: CGSize) -> CGSize {
            switch layout {
            case .standard:
                if image(for: .normal) == nil {
                    return titleSize(fittingWithin: size)
                    
                } else {
                    return super.sizeThatFits(size)
                }
                
            case let .horizontalImageTitle(imageTitleHorizontalMargin):
                let size = (size == .zero) ? CGSize(width: .max, height: .max) : size
                if let image = image(for: .normal) {
                    let widthRemainingForTitle = size.width - image.size.width - imageTitleHorizontalMargin
                    
                    guard widthRemainingForTitle > 0 else {
                        // We don't know what to do here... let super figure it out.
                        return super.sizeThatFits(size)
                    }
                    
                    let titleLabelSize = titleSize(fittingWithin: CGSize(width: widthRemainingForTitle, height: size.height))
                    return CGSize(width: image.size.width + imageTitleHorizontalMargin + titleLabelSize.width, height: max(image.size.height, titleLabelSize.height))
                    
                } else {
                    return titleSize(fittingWithin: size)
                }
                
            case let .verticalImageTitle(imageTitleVeritcalMargin):
                let size = (size == .zero) ? CGSize(width: .max, height: .max) : size
                if let image = image(for: .normal) {
                    let heightRemainingForTitle = size.height - image.size.height - imageTitleVeritcalMargin
                    
                    guard heightRemainingForTitle > 0 else {
                        // We don't know what to do here... let super figure it out.
                        return super.sizeThatFits(size)
                    }
                    
                    let titleLabelSize = titleSize(fittingWithin: CGSize(width: size.width, height: heightRemainingForTitle))
                    return CGSize(width: max(image.size.width, titleLabelSize.width), height: image.size.height + imageTitleVeritcalMargin + titleLabelSize.height)
                    
                } else {
                    return titleSize(fittingWithin: size)
                }
                
            case let .fixed(size):
                return size
                
            case let .inRange(minWidth, maxWidth, height, horizontalPadding):
                let preferredWidthAfterPadding = super.sizeThatFits(size).width + (2 * horizontalPadding)
                
                if preferredWidthAfterPadding < minWidth {
                    return CGSize(width: minWidth, height: height)
                } else if preferredWidthAfterPadding > maxWidth {
                    return CGSize(width: maxWidth, height: height)
                } else {
                    return CGSize(width: preferredWidthAfterPadding, height: height)
                }
                
            }
        }
        
        let sizeBeforeInset = sizeThatFits(sizeBeforeInsets: size)
        return CGSize(width: sizeBeforeInset.width - contentEdgeInsets.left - contentEdgeInsets.right, height: sizeBeforeInset.height - contentEdgeInsets.bottom - contentEdgeInsets.top)
    }
    
    public override func layoutSubviews() {
        updateLayoutInsets()
        
        super.layoutSubviews()
    }
    
    // MARK: Public Methods
    
    public func apply(theme: Theme) {
        setTitleColor(theme.textColor, for: .normal)
        setTitleColor(theme.highlightedTextColor, for: .highlighted)
        setTitleColor(theme.textColor.withAlphaComponent(theme.disabledTitleAlpha), for: .disabled)
        backgroundColor = theme.backgroundColor
        titleLabel?.font = theme.font
    }
    
    public func apply(viewModel: ViewModel) {
        switch viewModel.title {
        case .none:
            setTitle("", for: .normal)
            
        case let .text(title):
            setTitle(title, for: .normal)
            
        case let .attributedText(title):
            setAttributedTitle(title, for: .normal)
            
        case let .attributed(normal, highlighted, disabled):
            setAttributedTitle(normal, for: .normal)
            setAttributedTitle(highlighted, for: .highlighted)
            if let disabled = disabled {
                setAttributedTitle(disabled, for: .disabled)
            }
        }
        
        setImage(viewModel.image, for: .normal)
        if let highlightedImage = viewModel.highlightedImage {
            setImage(highlightedImage, for: .highlighted)
        }
    }
    
    public func apply(image: UIImage) {
        self.apply(viewModel: ThemeableButton.ViewModel(title: .none, image: image))
    }

    // MARK: Private Methods
    
    private func titleSize(fittingWithin size: CGSize = .zero) -> CGSize {
        guard let titleLabel = titleLabel, let font = titleLabel.font else {
            return .zero
        }
        
        return CGRect(origin: .zero, size: titleLabel.sizeThatFits(size)).insetBy(capAndBaselineOf: font, with: PixelRounder(for: self)).size
    }
    
    private func updateLayoutInsets() {
        switch layout {
        case .standard, .fixed, .inRange:
            titleEdgeInsets = .zero
            imageEdgeInsets = .zero
            
        case let .horizontalImageTitle(imageTitleHorizontalMargin):
            titleEdgeInsets = UIEdgeInsets(top: 0.0, left: 0.0, bottom: 0.0, right: -imageTitleHorizontalMargin)
            imageEdgeInsets = UIEdgeInsets(top: 0.0, left: -imageTitleHorizontalMargin, bottom: 0.0, right: 0.0)
            
        case let .verticalImageTitle(imageTitleVerticalMargin):
            guard let image = image(for: .normal) else {
                titleEdgeInsets = .zero
                imageEdgeInsets = .zero
                return
            }
            let imageSize = image.size
            let titleSize = self.titleSize()
            
            titleEdgeInsets = UIEdgeInsets(top: (imageSize.height + titleSize.height + imageTitleVerticalMargin), left: -imageSize.width, bottom: 0.0, right: 0.0)
            imageEdgeInsets = UIEdgeInsets(top: 0.0, left: 0.0, bottom: 0.0, right: -titleSize.width)
        }
    }
}


// MARK: – Theming


public extension ThemeableButton {
    
    public func apply_onboardingNavigationButtonTheme() {
        let theme = ThemeableButton.Theme(
            textColor: Palette.onboardingNavigationButtonTitleColor,
            highlightedTextColor: Palette.onboardingNavigationButtonTitleColor.withAlphaComponent(Palette.defaultHighlightedButtonTitleAlpha),
            backgroundColor: Palette.onboardingNavigationButtonBackgroundColor,
            font: .presentFont(ofSize: 17.0, weight: .semibold)
        )
        
        apply(theme: theme)
        
        // Ensure localized strings have the best shot at fitting entirely.
        titleLabel?.lineBreakMode = .byTruncatingMiddle
        titleLabel?.adjustsFontSizeToFitWidth = true
        titleLabel?.minimumScaleFactor = 0.7
        
        // Add a drop shadow.
        layer.shadowColor = UIColor.black.cgColor
        layer.shadowOpacity = 0.13
        layer.shadowRadius = 4.0
        layer.shadowOffset = CGSize(width: 0.0, height: 2.0)
    }
    
    public func apply_onboardingNavigationSkipButtonTheme(){
        let theme = ThemeableButton.Theme(
            textColor: Palette.blackTextColor,
            highlightedTextColor: Palette.blackTextColor.withAlphaComponent(Palette.defaultHighlightedButtonTitleAlpha),
            font: .presentFont(ofSize: 17.0, weight: .regular)
        )
        
         apply(theme: theme)
        
        // Ensure localized strings have the best shot at fitting entirely.
        titleLabel?.lineBreakMode = .byTruncatingMiddle
        titleLabel?.adjustsFontSizeToFitWidth = true
        titleLabel?.minimumScaleFactor = 0.7
    }
    
    public func apply_onboardingSubtextTheme() {
        apply(theme: ThemeableButton.Theme(textColor: UIColor(hex: 0xA4AAB3), font: .presentFont(ofSize: 14.0, weight: .regular, in: self)))
    }
}


// MARK: – Sizing


public extension ThemeableButton {
    
    public func setNavigationButtonSize(boundsWidth: CGFloat, with pixelRounder: PixelRounder) {
        layout = .inRange(minimumWidth: 230.0, maximumWidth: (boundsWidth - 40.0), height: 50.0, horizontalPadding: 20.0)
        sizeToFit()
    }
    
    public func size_defaultOnboardingHeaderSize(insideContentViewWidth width: CGFloat, pixelRounder: PixelRounder) {
        sizeToFit(fixedWidth: width - pixelRounder.convert(2.0 * 40.0, to: self))
    }
}
