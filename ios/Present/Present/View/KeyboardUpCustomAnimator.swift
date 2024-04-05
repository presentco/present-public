//
//  KeyboardUpCustomAnimator.swift
//  Present
//
//  Created by Dan Federman on 4/12/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import Relativity


/// A protocol declaring that the conforming view controller wants the keyboard up always.
public protocol RequiresKeyboardUpDuringTransition: class {
    
    /// The text field that has first responder when the view appears.
    var firstInputTextView: TextInputView { get }
    
    /// The text field that has first responder when the view disappears.
    var lastInputTextView: TextInputView { get }
    
    /// The button that sits on top of the keyboard.
    var keyboardToppingButton: UIButton? { get }
    
    /// The content inset cooresponding to the current keyboard height (height is zero if keyboard is not showing).
    var bottomContentInset: CGFloat { get set }
}


public class KeyboardUpCustomAnimator {
    
    // MARK: Initialization
    
    public required init(old: RequiresKeyboardUpDuringTransition, new: RequiresKeyboardUpDuringTransition) {
        self.old = old
        self.new = new
        
        keyboardToppingButton = KeyboardToppingButton(old: old, new: new)
    }
    
    public convenience init?(with transitionContext: UIViewControllerContextTransitioning) {
        guard let fromVC = transitionContext.viewController(forKey: .from) as? RequiresKeyboardUpDuringTransition,
            let toVC = transitionContext.viewController(forKey: .to) as? RequiresKeyboardUpDuringTransition else {
            return nil
        }
        
        self.init(old: fromVC, new: toVC)
    }
    
    // MARK: Public Methods
    
    public func setup(transitionContext: UIViewControllerContextTransitioning) {
        if let keyboardToppingButton = keyboardToppingButton {
            transitionContext.containerView.addSubview(keyboardToppingButton.newButtonSnapshot)
            keyboardToppingButton.newButtonSnapshot.middle --> keyboardToppingButton.oldButton.middle
            
            transitionContext.containerView.addSubview(keyboardToppingButton.oldButtonSnapshot)
            keyboardToppingButton.oldButtonSnapshot.middle --> keyboardToppingButton.oldButton.middle
        }
        
        new.bottomContentInset = old.bottomContentInset
        
        transitionContext.containerView.addSubview(dummyTextView)
        dummyTextView.copyAttributes(from: old.lastInputTextView)
        dummyTextView.becomeFirstResponder()
    }
    
    public func animate(using transitionContext: UIViewControllerContextTransitioning) {
        keyboardToppingButton?.oldButtonSnapshot.alpha = 0.0
    }
    
    public func cleanup() {
        dummyTextView.allowResignFirstResponder = true
        new.firstInputTextView.becomeFirstResponder()
        
        keyboardToppingButton?.newButtonSnapshot.removeFromSuperview()
        keyboardToppingButton?.oldButtonSnapshot.removeFromSuperview()
    }
    
    // MARK: Private Properties
    
    private let old: RequiresKeyboardUpDuringTransition
    private let new: RequiresKeyboardUpDuringTransition
    private let dummyTextView = FirstResponderStealingTextView()
    private let keyboardToppingButton: KeyboardToppingButton?
    
    // MARK: Private Class
    
    private class KeyboardToppingButton {
        
        // MARK: Initialization
        
        init?(old: RequiresKeyboardUpDuringTransition, new: RequiresKeyboardUpDuringTransition) {
            guard let oldButton = old.keyboardToppingButton,
                let newButton = new.keyboardToppingButton,
                oldButton.bounds == newButton.bounds,
                let oldButtonSnapshot = oldButton.snapshotView(afterScreenUpdates: false),
                let newButtonSnapshot = newButton.snapshotView(afterScreenUpdates: true) else {
                    return nil
            }
            
            self.oldButton = oldButton
            self.newButton = newButton
            self.oldButtonSnapshot = oldButtonSnapshot
            self.newButtonSnapshot = newButtonSnapshot
        }
        
        // MARK: Public Properties
        
        public let oldButton: UIButton
        public let newButton: UIButton
        public let oldButtonSnapshot: UIView
        public let newButtonSnapshot: UIView
    }
}

private class FirstResponderStealingTextView: UITextView {
    
    // MARK: Public Properties
    
    public var allowResignFirstResponder = false
    
    // MARK: UIView
    
    public override var canResignFirstResponder: Bool {
        return allowResignFirstResponder
    }
    
    @objc
    public override func resignFirstResponder() -> Bool {
        let _ = super.resignFirstResponder()
        return allowResignFirstResponder
    }
    
}
