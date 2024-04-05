//
//  PresentViewController.swift
//  Present
//
//  Created by Patrick Niemeyer on 3/27/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import RxSwift

/// Support deferred configuration
public protocol QueuedConfigurable: class {
    var configureQueue: SerialTaskQueue { get }
    // Why does declaring the signature in the protocol break this?
    //func configure(_ block: @escaping (Self)->Void)
}

/// Support deferred configuration
public extension QueuedConfigurable {
    @discardableResult
    func configure(withBlock block: @escaping (Self)->Void) -> Self {
        //log("\(type(of: self)) configureQueue enqueued item")
        configureQueue.enqueue {
            [weak self] in if let sself = self { block(sself) }
        }
        return self
    }
}

public class PresentViewController: UIViewController, ApplicationServices, QueuedConfigurable
{
    deinit { log("viewcontroller deinit \(type(of: self))") }

    var disposal = DisposeBag()
    
    lazy var activityIndicator = PresentLogoActivityIndicator()
    
    /// Configuration to be run after the view is loaded.
    /// See QueuedConfigurable
    public lazy var configureQueue = SerialTaskQueue()
    
    public init() {
        super.init(nibName: nil, bundle: nil)
    }
    
    // Note: implementing this is necessary to expose it to subclasses
    override public init(nibName nibNameOrNil: String?, bundle nibBundleOrNil: Bundle?) {
        super.init(nibName: nibNameOrNil, bundle: nibBundleOrNil)
    }

    public required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
    
    public override func viewDidLoad() {
        //log("\(type(of: self)) configureQueue running")
        configureQueue.run()
    }
    
    /// Install the activity indicator in a specific view. If the indicator is started
    /// without specifying a few the main view will be used.
    public func installActivityIndicator(inView view: UIView) {
        if activityIndicator.parent != nil {
            logError("activity indicator already installed")
        }
        installChild(viewController: activityIndicator, in: view) {
            $0.sizeToFitSuperview()
        }
    }
    
    public func startActivity() {
        if activityIndicator.view.superview == nil {
            installActivityIndicator(inView: view)
        }
        activityIndicator.startAnimating()
    }
    public func stopActivity() {
        endActivity()
    }
    public func endActivity() {
        activityIndicator.stopAnimating()
    }
    
    ///
    ///          Title
    ///     This is my message
    ///  -----------------------
    ///           |OK|
    ///
    public func showAcknowledgeAlert(
        title: String, message: String, completion: (()->Void)? = nil)
    {
        UIAlertController.showAcknowledgeAlert(fromViewController: self, title: title, message: message, completion: completion)
    }
    
    ///
    ///          Title
    ///     This is my message
    ///  -----------------------
    ///     |"Cancel"| |OK|
    ///
    public func confirmDestructiveAction(
        title: String,
        message: String,
        cancelTitle: String = "Cancel",
        destroyTitle: String = "OK",
        onCancel: (()->Void)? = nil,
        onDestroy: (()->Void)? = nil)
    {
        UIAlertController.showDestructiveAlert(
            fromViewController: self, title: title, message: message,
            cancelTitle: cancelTitle, destroyTitle: destroyTitle,
            onCancel: onCancel,
            onDestroy: onDestroy
        )
    }
}

public extension PresentViewController
{
    public override var shouldAutorotate: Bool {
        return false
    }
    
    public override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
        return .portrait
    }
    
    public override var preferredStatusBarStyle: UIStatusBarStyle {
        return .default
    }
    
    override var prefersStatusBarHidden: Bool {
        return super.prefersStatusBarHidden
    }
}

