//
//  GatedViewController.swift
//  Present
//
//  Created by Patrick Niemeyer on 5/2/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import RxSwift

/// Deprecated: Migrate to ConditionalViewController
/// A view controller that supports shows a placeholder explaining required permissions until they are granted.
public class GatedViewController: PresentViewController
{
    public var placeholderViewController: PlaceholderViewController?
    
    /// configuration queue for the placeholder view controller
    private var configurePlaceholderQueue = SerialTaskQueue()

    // TODO: pull child usages up here and encapsulate
    /// configuration queue for the produced view controller
    let configureViewControllerQueue = SerialTaskQueue()

    /// Runs after the placeholder view controller's view has loaded
    @discardableResult
    func configurePlaceholder(withBlock block: @escaping (PlaceholderViewController)->Void) -> Self {
        configurePlaceholderQueue.enqueue {
            if let placeholderViewController = self.placeholderViewController {
                block(placeholderViewController)
            }
        }
        return self
    }
    
    override public func viewDidLoad() {
        super.viewDidLoad()
        self.view.backgroundColor = .white
    }
    
    func initPlaceholderViewController() {
        placeholderViewController = PlaceholderViewController()
        .configure { _ in
            self.configurePlaceholderQueue.run()
        }.then {
            self.placeholderViewController = $0 // Must be set before config
            install(viewController: $0)
        }
    }
    
    func install(viewController: UIViewController)
    {
        installChild(viewController: viewController, in: view)
        viewController.view.constrainToFillSuperviewSafeArea()
        //viewController.view.setNeedsUpdateConstraints()
        //viewController.view.setNeedsLayout()
    }
    
    func showPlaceholderViewcontroller()
    {
        if let placeholderViewController = self.placeholderViewController {
            placeholderViewController.view.toFront()
        } else {
            initPlaceholderViewController()
        }
    }
}
