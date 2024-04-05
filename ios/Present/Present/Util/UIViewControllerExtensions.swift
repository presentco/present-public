//
//  UIViewControllerExtensions.swift
//  Present
//
//  Created by Dan Federman on 2/13/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation


public extension UIViewController {
    
    /// Installs a child view controller into the specified subview of the receiver. For more information on installing child view controllers, [read this documentation](https://developer.apple.com/library/content/featuredarticles/ViewControllerPGforiPhoneOS/ImplementingaContainerViewController.html).
    /// - parameter viewController: The view controller to install.
    /// - parameter subview: The subview of the receiver in which to install the view controller. Can be the receiver's main view.
    /// - parameter layoutBlock: A block which sizes and optionally lays out the child view controller's view.
    /// - parameter childViewControllerView: The child view controller's view.
    public func installChild(viewController: UIViewController, in subview: UIView, layoutBlock: (_ childViewControllerView: UIView) -> Void = {_ in}) 
    {
        assert(subview == view || subview.isDescendant(of: view))
        
        addChildViewController(viewController)
        subview.addSubview(viewController.view)
        layoutBlock(viewController.view)
        viewController.didMove(toParentViewController: self)
    }
    
    public func uninstallChild(viewController: UIViewController) {
        viewController.willMove(toParentViewController: nil)
        viewController.view.removeFromSuperview()
        viewController.removeFromParentViewController()
    }
    
}

/*
public extension UIViewController {
    /// Load a view controller associated with this class from a XIB file, specifying an index into the xib.
    public func viewControllerFromNibForClass<T: UIViewController>(index : Int = 0) -> T? {
        let bundle = Bundle(for: type(of: self))
        let nib = UINib(nibName: String(describing: type(of: self)), bundle: bundle)
        return nib.instantiate(withOwner: self, options: nil)[index] as? T
    }
}
 */
