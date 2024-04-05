//
//  ConditionalViewController.swift
//  Present
//
//  Created by Patrick Niemeyer on 7/17/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation

/// A view controller that supports showing a placeholder explaining a condition
/// (e.g. required permission) until granted and then switching to produce and display
/// another, specified type of view controller.
public class ConditionalViewController<T: PresentViewController>: PresentViewController
{
    // The placeholder
    public var placeholderViewController: PlaceholderViewController?
    
    // The content view controller
    public var viewController: T?
    
    /// configuration queue for the placeholder view controller
    private let configurePlaceholderQueue = SerialTaskQueue()
    
    /// configuration queue for the content view controller
    private let configureViewControllerQueue = SerialTaskQueue()
    
    private var initViewControllerBlock: (()->T)?
    
    public var showPlaceholder: Bool? {
        didSet {
            guard let showPlaceholder = showPlaceholder else { return }
            if showPlaceholder {
                self.showPlaceholderViewcontroller()
            } else {
                self.showViewController()
            }
        }
    }
    
    /// Runs after the placeholder view controller's view has loaded
    @discardableResult
    public func configurePlaceholder(withBlock block: @escaping (PlaceholderViewController)->Void) -> Self {
        configurePlaceholderQueue.enqueue { [weak self] in
            guard let sself = self else { return }
            guard let placeholderViewController = sself.placeholderViewController else {
                fatalError("no view controller")
            }
            block(placeholderViewController)
        }
        return self
    }
    
    /// Runs after the content view controller's view has loaded
    @discardableResult
    public func configureViewController(withBlock block: @escaping (T)->Void) -> Self {
        configureViewControllerQueue.enqueue { [weak self] in
            guard let sself = self else { return }
            guard let viewController = sself.viewController else {
                fatalError("no view controller")
            }
            block(viewController)
        }
        return self
    }
    
    /// Runs after the content view controller's view has loaded
    @discardableResult
    public func initViewController(_ block: @escaping ()->T) -> Self {
        self.initViewControllerBlock = block
        return self
    }
    
    private func initPlaceholderViewController()
    {
        placeholderViewController = PlaceholderViewController()
        .configure { _ in
            self.configurePlaceholderQueue.run()
        }.then {
            self.placeholderViewController = $0 // Must be set before config runs
            install(viewController: $0)
        }
    }
    
    private func initViewController() {
        self.viewController = initViewControllerBlock?()
            .configure { _ in
                self.configureViewControllerQueue.run()
            }.then {
                self.viewController = $0 // Must be set before config runs
                install(viewController: $0)
            }
    }
    
    private func install(viewController: UIViewController)
    {
        installChild(viewController: viewController, in: view)
        viewController.view.constrainToFillSuperview()
    }
    
    private func showPlaceholderViewcontroller()
    {
        if let placeholderViewController = self.placeholderViewController {
            placeholderViewController.view.toFront()
        } else {
            initPlaceholderViewController()
        }
    }
    
    private func showViewController()
    {
        if let viewController = self.viewController {
            viewController.view.toFront()
        } else {
            initViewController()
        }
    }
}
