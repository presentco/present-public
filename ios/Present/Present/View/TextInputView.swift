//
//  TextInputView.swift
//  Present
//
//  Created by Dan Federman on 7/21/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation


public protocol TextInputView: class, UITextInput {
    
    var textInput: String? { get set }
    
    // Redeclare variables from UITextInputTraits as mutable on this type.
    var autocapitalizationType: UITextAutocapitalizationType { get set }
    var autocorrectionType: UITextAutocorrectionType { get set }
    var keyboardType: UIKeyboardType { get set }
    var keyboardAppearance: UIKeyboardAppearance { get set }
    var returnKeyType: UIReturnKeyType { get set }
    
    @discardableResult
    func becomeFirstResponder() -> Bool
    
    @discardableResult
    func resignFirstResponder() -> Bool
}


public extension TextInputView {
    
    public func copyAttributes(from textInputView: TextInputView) {
        textInput = textInputView.textInput
        autocapitalizationType = textInputView.autocapitalizationType
        autocorrectionType = textInputView.autocorrectionType
        keyboardType = textInputView.keyboardType
        returnKeyType = textInputView.returnKeyType
    }
    
}


extension UITextView: TextInputView {
    
    public var textInput: String? {
        get {
            return text
        }
        set {
            text = newValue
        }
    }
    
}


extension UITextField: TextInputView {
    
    public var textInput: String? {
        get {
            return text
        }
        set {
            text = newValue
        }
    }
    
}
