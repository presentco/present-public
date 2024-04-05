//
//  ThemeableTextField.swift
//  Present
//
//  Created by Dan Federman on 1/31/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import UIKit


public final class ThemeableTextField: UITextField {
    
    // MARK: Theme
    
    public struct Theme {
    
        // MARK: Initialization
        
        public init(
            textColor: UIColor = Palette.blackTextColor,
            placeholderTextColor: UIColor = Palette.blackTextColor.withAlphaComponent(0.3),
            backgroundColor: UIColor? = nil,
            font: UIFont = .systemFont(ofSize: 12.0),
            textAlignment: NSTextAlignment = .natural,
            tintColor: UIColor = Palette.cursorColor
        ) {
            self.textColor = textColor
            self.placeholderTextColor = placeholderTextColor
            self.backgroundColor = backgroundColor
            self.font = font
            self.textAlignment = textAlignment
            self.tintColor = tintColor
        }
        
        // MARK: Public Properties
        
        public let textColor: UIColor
        public let placeholderTextColor: UIColor
        public let backgroundColor: UIColor?
        public let font: UIFont
        public let textAlignment: NSTextAlignment
        public let tintColor: UIColor

    }
    
    // MARK: Public Methods
    
    public func apply(theme: Theme) {
        self.theme = theme
    }
    
    // MARK: Public Properties
    
    public override var placeholder: String? {
        didSet {
            applyPlaceholderAttributes()
        }
    }
    
    // MARK: Private Properties
    
    private var theme: Theme = Theme() {
        didSet {
            textColor = theme.textColor
            applyPlaceholderAttributes()
            backgroundColor = theme.backgroundColor
            font = theme.font
            textAlignment = theme.textAlignment
            tintColor = theme.tintColor
        }
    }
    
    // MARK: Private Methods
    
    private func applyPlaceholderAttributes() {
        if let placeholder = placeholder {
            self.attributedPlaceholder = NSAttributedString(string: placeholder, attributes: [ NSAttributedStringKey.foregroundColor : theme.placeholderTextColor ])
        }
    }
    
}


// MARK: - TextTransform

public enum TextFieldTransform {
    case proposedReplacementDisallowed
    case proposedReplacementAllowed
    case reformat(text: String, selectedStartOffset: Int)
}


// MARK: – TextFieldTransformDelegate


public protocol TextFieldTransformDelegate {
    
    func transformFor(proposedReplacement string: String, forCharactersIn range: NSRange, in textInputView: UITextInput) -> TextFieldTransform
    
}
