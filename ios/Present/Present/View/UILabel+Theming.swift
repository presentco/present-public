//
//  UILabel+Theming.swift
//  Present
//
//  Created by Dan Federman on 1/12/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import Relativity
import UIKit


public extension UILabel {
    
    // MARK: Theme
    
    public struct Theme {
        
        // MARK: Initialization
        
        public init(textColor: UIColor = Palette.blackTextColor, font: UIFont, textAlignment: NSTextAlignment = .natural, lineBreakMode: NSLineBreakMode = .byTruncatingTail, numberOfLines: Int = 0) {
            self.textColor = textColor
            self.font = font
            self.textAlignment = textAlignment
            self.lineBreakMode = lineBreakMode
            self.numberOfLines = numberOfLines
        }
        
        // MARK: Public Properties
        
        public let textColor: UIColor
        public let font: UIFont
        public let textAlignment: NSTextAlignment
        public let lineBreakMode: NSLineBreakMode
        public let numberOfLines: Int
        
    }
    
    // MARK: Public Methods
    
    public func apply(theme: Theme) {
        textColor = theme.textColor
        font = theme.font
        textAlignment = theme.textAlignment
        lineBreakMode = theme.lineBreakMode
        numberOfLines = theme.numberOfLines
    }
}


// TODO: Move these somewhere more appropriate
// MARK: Theming

public extension UILabel {
    public func apply_onboardingHeaderTheme() {
        apply(theme: UILabel.Theme(textColor: Palette.blackTextColor, font: .presentFont(ofSize: 20.0, weight: .medium, in: self)))
    }
    public func apply_onboardingSubtextTheme() {
        apply(theme: UILabel.Theme(textColor: UIColor(hex: 0xA4AAB3), font: .presentFont(ofSize: 14.0, weight: .regular, in: self)))
    }
}

// TODO: Move these somewhere more appropriate
// MARK: Sizing

public extension UILabel {
    public func size_defaultOnboardingHeaderSize(insideContentViewWidth width: CGFloat, pixelRounder: PixelRounder) {
        sizeToFit(fixedWidth: width - pixelRounder.convert(2.0 * 40.0, to: self))
    }
}
