//
//  ScreenTransitionAnimator.swift
//  Present
//
//  Created by Dan Federman on 3/8/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import UIKit


public protocol ScreenTransitionAnimatorObserver: class {
    
    /// Called when a transition has finished.
    /// - parameter screenTransitionAnimator: The animator whose transition has finished.
    /// - parameter transitionDidComplete: Indicates whether the transition completed. If true, the new screen is being presented. If false, the old screen is still presented.
    func transitionDidFinish(for screenTransitionAnimator: ScreenTransitionAnimator, transitionDidComplete: Bool)
    
}


// MARK: – ScreenTransitionAnimator


public final class ScreenTransitionAnimator: NSObject, UIViewControllerAnimatedTransitioning {
    
    // MARK: TransitionStyle
    
    public enum TransitionStyle: CustomStringConvertible {
        
        case none
        case fade
        case slideInHorizontal
        case slideOutHorizontal
        case slideInVertical
        case slideOutVertical
        
        // MARK: Public Properties
        
        public var reversed: TransitionStyle {
            switch self {
            case .none:
                return .none
            case .fade:
                return .fade
                
            case .slideInHorizontal:
                return .slideOutHorizontal
            case .slideOutHorizontal:
                return .slideInHorizontal
                
            case .slideInVertical:
                return .slideOutVertical
            case .slideOutVertical:
                return .slideInVertical
            }
        }
        
        // MARK: CustomStringConvertible
        
        public var description: String {
            switch self {
            case .none:
                return "none"
            case .fade:
                return "fade"
            case .slideInHorizontal:
                return "slideInHorizontal"
            case .slideOutHorizontal:
                return "slideOutHorizontal"
            case .slideInVertical:
                return "slideInVertical"
            case .slideOutVertical:
                return "slideOutVertical"
            }
        }
        
        // MARK: Public Methods
        
        public func animate(withDuration animationDuration: TimeInterval, animations: @escaping () -> Void, completion: @escaping (Bool) -> Void) {
            switch self {
            case .none:
                // Ignore duration here.
                animations()
                completion(true)
                
            case .fade:
                UIView.animate(withDuration: animationDuration, animations: animations, completion: completion)
                
            case .slideInHorizontal:
                UIView.animate(withDuration: animationDuration, delay: 0.0, options: .curveEaseIn, animations: animations, completion: completion)
                
            case .slideOutHorizontal:
                UIView.animate(withDuration: animationDuration, delay: 0.0, options: .curveEaseOut, animations: animations, completion: completion)
                
            case .slideInVertical:
                // Use easeOut animation curve to ensure that bounce is prominent.
                UIView.animate(withDuration: animationDuration, delay: 0.0, usingSpringWithDamping: 0.72, initialSpringVelocity: 0.0, options: .curveEaseOut, animations: animations, completion: completion)
                
            case .slideOutVertical:
                UIView.animate(withDuration: animationDuration, delay: 0.0, usingSpringWithDamping: 0.8, initialSpringVelocity: 0.0, options: .curveLinear, animations: animations, completion: completion)
            }
        }
        
        public func newViewEndingAlpha(with transitionContext: UIViewControllerContextTransitioning) -> CGFloat {
            return 1.0
        }
        
        public func oldViewStartingTransform(with transitionContext: UIViewControllerContextTransitioning) -> CGAffineTransform {
            return .identity
        }
        
        public func oldViewEndingTransform(with transitionContext: UIViewControllerContextTransitioning) -> CGAffineTransform {
            switch self {
            case .none, .fade, .slideInVertical:
                return .identity
                
            case .slideInHorizontal:
                return CGAffineTransform(translationX: -transitionContext.containerView.bounds.width, y: 0.0)
                
            case .slideOutHorizontal:
                return CGAffineTransform(translationX: transitionContext.containerView.bounds.width, y: 0.0)
                
            case .slideOutVertical:
                // Increase veritical transform by a small percentage to ensure the slide out spring doesn't push the dismissed VC back onto the bottom of the screen.
                return CGAffineTransform(translationX: 0.0, y: transitionContext.containerView.bounds.height * 1.02)
            }
        }
        
        public func newViewStartingTransform(with transitionContext: UIViewControllerContextTransitioning) -> CGAffineTransform {
            return reversed.oldViewEndingTransform(with: transitionContext)
        }
        
        public func newViewEndingTransform(with transitionContext: UIViewControllerContextTransitioning) -> CGAffineTransform {
            return reversed.oldViewStartingTransform(with: transitionContext)
        }
        
        public func newViewStartingAlpha(with transitionContext: UIViewControllerContextTransitioning) -> CGFloat {
            switch self {
            case .fade:
                // Hide the new view.
                return 0.0
                
            case .none, .slideInHorizontal, .slideOutHorizontal, .slideInVertical, .slideOutVertical:
                return 1.0
            }
        }
    }
    
    // MARK: Initialization
    
    public init(transitionStyle: TransitionStyle) {
        self.transitionStyle = transitionStyle
    }
    
    // MARK: Public Properties
    
    public let transitionStyle: TransitionStyle
    public weak var observer: ScreenTransitionAnimatorObserver?
    
    // MARK: CustomStringConvertible
    
    public override var description: String {
        return "\(super.description) \(transitionStyle)"
    }
    
    // MARK: UIViewControllerAnimatedTransitioning
    
    public func transitionDuration(using transitionContext: UIViewControllerContextTransitioning?) -> TimeInterval {
        switch transitionStyle {
        case .none:
            return 0.0
            
        case .fade, .slideInHorizontal, .slideOutHorizontal:
            return 0.2

        case .slideInVertical, .slideOutVertical:
            return 0.3666
        }
    }
    
    public func animateTransition(using transitionContext: UIViewControllerContextTransitioning) {
        guard let oldView = transitionContext.view(forKey: .from),
            let newView = transitionContext.view(forKey: .to) else {
                logError("Missing from \(String(describing: transitionContext.view(forKey: .from))) or to \(String(describing: transitionContext.view(forKey: .to))) view")
                return
        }
        
        if let fromVC = transitionContext.viewController(forKey: .from),
            let toVC = transitionContext.viewController(forKey: .to) {
            logEvent(type: .view, "Presenting \(String(describing: type(of: toVC))), dismissing \(String(describing: type(of: fromVC)))")
            
        } else if let toVC = transitionContext.viewController(forKey: .to) {
            logEvent(type: .view, "Presenting \(String(describing: type(of: toVC)))")
            
        } else if let fromVC = transitionContext.viewController(forKey: .from) {
            logEvent(type: .view, "Dismissing \(String(describing: type(of: fromVC)))")
        }
        
        let animationDuration = transitionDuration(using: transitionContext)
        
        let oldViewCouldReceiveUserInteraction = oldView.isUserInteractionEnabled
        let newViewCouldReceiveUserInteraction = newView.isUserInteractionEnabled
        
        // Ignore interaction events during the transition.
        oldView.isUserInteractionEnabled = false
        newView.isUserInteractionEnabled = false
        
        // Interactive push/pops can leave these views with the wrong starting coordinates.
        // Ensure the views we are working have a proper starting position & transform.
        oldView.transform = .identity
        newView.transform = .identity
        oldView.frame = transitionContext.containerView.bounds
        newView.frame = transitionContext.containerView.bounds
        
        oldView.transform = transitionStyle.oldViewStartingTransform(with: transitionContext)
        newView.transform = transitionStyle.newViewStartingTransform(with: transitionContext)
        newView.alpha = transitionStyle.newViewStartingAlpha(with: transitionContext)
        
        switch transitionStyle {
        case .none:
            // Add only the new view to the container.
            transitionContext.containerView.addSubview(newView)
            
        case .fade, .slideInHorizontal, .slideInVertical:
            // Add the old view to the container first so it can be covered by the new view.
            transitionContext.containerView.addSubview(oldView)
            // Add the new view to the container second so it can be covered by the old view.
            transitionContext.containerView.addSubview(newView)
            
        case .slideOutHorizontal, .slideOutVertical:
            // Add the new view to the container first so it is on the bottom.
            transitionContext.containerView.addSubview(newView)
            // Add the old view to the container second so it slides off of the top of the new view.
            transitionContext.containerView.addSubview(oldView)
        }
        
        newView.layoutIfVisible()
        
        let keyboardUpCustomAnimator = KeyboardUpCustomAnimator(with: transitionContext)
        keyboardUpCustomAnimator?.setup(transitionContext: transitionContext)
        
        transitionStyle.animate(withDuration: animationDuration, animations: {
            oldView.transform = self.transitionStyle.oldViewEndingTransform(with: transitionContext)
            newView.transform = self.transitionStyle.newViewEndingTransform(with: transitionContext)
            newView.alpha = self.transitionStyle.newViewEndingAlpha(with: transitionContext)
            keyboardUpCustomAnimator?.animate(using: transitionContext)

        }, completion: { didComplete in
            // Now that the transition is complete, restore the views' ability to receive interaction events.
            oldView.isUserInteractionEnabled = oldViewCouldReceiveUserInteraction
            newView.isUserInteractionEnabled = newViewCouldReceiveUserInteraction

            // Call our observer first, so that our currentScreen is up to date before the transition completion handler is called.
            // Note: This is where the navigation stack is modified.
            self.observer?.transitionDidFinish(for: self, transitionDidComplete: true)
            
            // Now that our observer's internal state is set up, tell the transition context that we've completed.
            transitionContext.completeTransition(true)
            
            // Cleanup our custom animator after the containerView has been cleaned up to avoid screwing with the first responder chain.
            keyboardUpCustomAnimator?.cleanup()
        })
    }
}
