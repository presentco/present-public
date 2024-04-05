//
//  ThemeableTextView.swift
//  Present
//
//  Created by Dan Federman on 7/21/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import UIKit


public final class ThemeableTextView: UITextView
{
    // MARK: Theme
    
    public struct Theme {
        
        // MARK: Initialization
        
        public init(
            textColor: UIColor = Palette.blackTextColor,
            backgroundColor: UIColor? = nil,
            font: UIFont = .systemFont(ofSize: 12.0),
            textAlignment: NSTextAlignment = .natural,
            tintColor: UIColor = Palette.cursorColor
            ) {
            self.textColor = textColor
            self.backgroundColor = backgroundColor
            self.font = font
            self.textAlignment = textAlignment
            self.tintColor = tintColor
        }
        
        // MARK: Public Properties
        
        public let textColor: UIColor
        public let backgroundColor: UIColor?
        public let font: UIFont
        public let textAlignment: NSTextAlignment
        public let tintColor: UIColor
        
    }
    
    // MARK: Public Methods
    
    public func apply(theme: Theme) {
        self.theme = theme
    }
    
    // MARK: Private Properties
    
    private var theme: Theme = Theme() {
        didSet {
            textColor = theme.textColor
            backgroundColor = theme.backgroundColor
            font = theme.font
            textAlignment = theme.textAlignment
            tintColor = theme.tintColor
        }
    }
    
}
