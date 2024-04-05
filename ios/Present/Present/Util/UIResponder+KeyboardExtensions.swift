//
//  UIResponder+KeyboardExtensions.swift
//  Present
//
//  Created by Dan Federman on 1/31/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation


public protocol KeyboardWillShowHideListener: class {
    
    func keyboardWillShow(with animation: KeyboardAnimation)
    func keyboardWillHide(with animation: KeyboardAnimation)
    
}

public protocol KeyboardDidShowHideListener: class {
    
    func keyboardDidShow(with animation: KeyboardAnimation)
    func keyboardDidHide(with animation: KeyboardAnimation)
    
}


// MARK: – KeyboardAnimation


public struct KeyboardAnimation {
    
    // MARK: Initialization
    
    init?(notification: Notification) {
        guard let startFrame = (notification.userInfo?[UIKeyboardFrameBeginUserInfoKey] as? NSValue)?.cgRectValue,
            let endFrame = (notification.userInfo?[UIKeyboardFrameEndUserInfoKey] as? NSValue)?.cgRectValue,
            let duration = (notification.userInfo?[UIKeyboardAnimationDurationUserInfoKey] as? NSNumber)?.doubleValue,
            let animationCurveAsInt = (notification.userInfo?[UIKeyboardAnimationCurveUserInfoKey] as? NSNumber)?.intValue,
            let animationCurve = UIViewAnimationCurve(rawValue: animationCurveAsInt) else {
                return nil
        }
        
        self.startFrame = startFrame
        self.endFrame = endFrame
        self.duration = TimeInterval(duration)
        self.animationCurve = animationCurve
    }
    
    // MARK: Public Properties
    
    public let startFrame: CGRect
    public let endFrame: CGRect
    
    public let duration: TimeInterval
    public let animationCurve: UIViewAnimationCurve
    
    public var animationOptions: UIViewAnimationOptions {
        // Bit shift the aniamtion curve raw value by 16 to access private animation curve options.
        // UIViewAnimationOptions only exposes 0 through 3 bit shift by 16, but our animation curve has a raw value of 7 on iOS 10.
        return UIViewAnimationOptions(rawValue: UInt(animationCurve.rawValue) << 16)
    }
    
    // MARK: Public Methods
    
    public func animate(animations: @escaping () -> Void, completion: ((Bool) -> Void)?) {
        UIView.animate(withDuration: duration, delay: 0.0, options: animationOptions, animations: animations, completion: completion)
    }
}


// MARK: – UIResponder Extension


public extension UIResponder {
    
    // MARK: Public Methods
    
    public func addKeyboardShowHideObservers() {
        // Remove ourselves as an observer to ensure we don't double-register.
        NotificationCenter.default.removeObserver(self, name: NSNotification.Name.UIKeyboardWillShow, object: nil)
        NotificationCenter.default.removeObserver(self, name: NSNotification.Name.UIKeyboardWillHide, object: nil)
        NotificationCenter.default.removeObserver(self, name: NSNotification.Name.UIKeyboardDidShow, object: nil)
        NotificationCenter.default.removeObserver(self, name: NSNotification.Name.UIKeyboardDidHide, object: nil)
        
        // Register our observing methods.
        NotificationCenter.default.addObserver(self, selector: #selector(keyboardAnimationExtension_keyboardWillShow(_:)), name: NSNotification.Name.UIKeyboardWillShow, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(keyboardAnimationExtension_keyboardWillHide(_:)), name: NSNotification.Name.UIKeyboardWillHide, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(keyboardAnimationExtension_keyboardDidShow(_:)), name: NSNotification.Name.UIKeyboardDidShow, object: nil)
       NotificationCenter.default.addObserver(self, selector: #selector(keyboardAnimationExtension_keyboardDidHide(_:)), name: NSNotification.Name.UIKeyboardDidHide, object: nil)
    }
    
    // MARK: Private Methods
    
    @objc
    private func keyboardAnimationExtension_keyboardWillShow(_ notification: Notification) {
        guard let keyboardWillShowHideListenerSelf = self as? KeyboardWillShowHideListener,
            let keyboardAnimation = KeyboardAnimation(notification: notification) else {
            return
        }
        
        keyboardWillShowHideListenerSelf.keyboardWillShow(with: keyboardAnimation)
    }
    
    @objc
    private func keyboardAnimationExtension_keyboardWillHide(_ notification: Notification) {
        guard let keyboardWillShowHideListenerSelf = self as? KeyboardWillShowHideListener,
            let keyboardAnimation = KeyboardAnimation(notification: notification) else {
            return
        }
        
        keyboardWillShowHideListenerSelf.keyboardWillHide(with: keyboardAnimation)
    }
    
    @objc
    private func keyboardAnimationExtension_keyboardDidShow(_ notification: Notification) {
        guard let keyboardDidShowHideListenerSelf = self as? KeyboardDidShowHideListener,
            let keyboardAnimation = KeyboardAnimation(notification: notification) else {
                return
        }
        
        keyboardDidShowHideListenerSelf.keyboardDidShow(with: keyboardAnimation)
    }
    
    @objc
    private func keyboardAnimationExtension_keyboardDidHide(_ notification: Notification) {
        guard let keyboardDidShowHideListenerSelf = self as? KeyboardDidShowHideListener,
            let keyboardAnimation = KeyboardAnimation(notification: notification) else {
                return
        }
        
        keyboardDidShowHideListenerSelf.keyboardDidHide(with: keyboardAnimation)
    }

}
