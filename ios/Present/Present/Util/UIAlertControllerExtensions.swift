//
//  UIAlertControllerExtensions.swift
//  Present
//
//  Created by Dan Federman on 3/10/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation


public extension UIAlertController {
    
    // MARK: Public Methods
    
    // MARK: Builder
    
    @discardableResult
    // Alert style may .actionSheet or .alert for pop up
    public func withAction(title: String, style: UIAlertActionStyle = .default, handler: ((UIAlertAction) -> Void)? = nil) -> UIAlertController {
        let action = UIAlertAction(title: title, style: style, handler: handler)
        addAction(action)
        return self
    }
    // Action with cancel style
    @discardableResult
    public func withCancel(title: String = "Cancel", handler: ((UIAlertAction) -> Void)? = nil) -> UIAlertController {
        return withAction(title: title, style: .cancel, handler: handler)
    }
    // Destructive style indicates data may be changed
    @discardableResult
    public func withDestructive(title: String, handler: ((UIAlertAction) -> Void)? = nil) -> UIAlertController {
        return withAction(title: title, style: .destructive, handler: handler)
    }
    
    // MARK: Standard Helpers
    
    ///
    ///          Title
    ///     This is my message
    ///  -----------------------
    ///           |OK|
    ///
    public class func showAcknowledgeAlert(
        fromViewController viewController: UIViewController,
        title: String, message: String,
        completion: (()->Void)? = nil)
    {
        UIAlertController(title: title, message: message, preferredStyle: UIAlertControllerStyle.alert)
            .withAction(title: "OK", style: .default, handler: { alert in completion?() })
        .do {
            viewController.present($0, animated: true, completion: nil)
        }
        
    }
    
    ///
    ///          Title
    ///     This is my message
    ///  -----------------------
    ///     |"Cancel"| |OK|
    ///
    public class func showDestructiveAlert(
        fromViewController viewController: UIViewController,
        title: String,
        message: String,
        cancelTitle: String = "Cancel",
        destroyTitle: String = "OK",
        onCancel: (()->Void)? = nil,
        onDestroy: (()->Void)? = nil
        )
    {
        UIAlertController(
            title: title, message: message, preferredStyle: .actionSheet)
            .withCancel(title: cancelTitle) { _ in onCancel?() }
            .withDestructive(title: destroyTitle) { _ in onDestroy?() }
            .do {
                viewController.present($0, animated: true, completion: nil)
            }
    }
    
    // MARK: Legacy
    
    // TODO: Migrate away from this to the builder pattern below
    @discardableResult
    public func addAction(withTitle title: String, style: UIAlertActionStyle = .default, handler: ((UIAlertAction) -> Void)? = nil) -> UIAlertAction {
        let action = UIAlertAction(title: title, style: style, handler: handler)
        addAction(action)
        
        return action
    }
    
    // TODO: Migrate away from this to the builder pattern below
    // TODO: Working around a compiler issue with naming in extension
    @discardableResult
    public func addActionWithTitle(title: String, style: UIAlertActionStyle = .default, handler: ((UIAlertAction) -> Void)? = nil) -> UIAlertAction {
        let action = UIAlertAction(title: title, style: style, handler: handler)
        addAction(action)
        
        return action
    }
    
}
