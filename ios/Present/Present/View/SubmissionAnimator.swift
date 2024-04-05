//
//  SubmissionAnimator.swift
//  Present
//
//  Created by Dan Federman on 4/13/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation


public protocol AnimatedSubmissionCapable {
    
    var bounds: CGRect { get }
    var headerLabel: UILabel { get }
    var submissionViewsToAnimate: [UIView] { get }
    var spinner: UIActivityIndicatorView { get }
    var navigationButton: UIButton { get }
    
    func layoutIfVisible()
    
}


public final class SubmissionAnimator {
    
    // MARK: Private Static Properties
    
    private static let animationTime: TimeInterval = 0.35
    private static let minimumTimeAfterSubmissionAnimationStartBeforeTriggerErrorAnimation: DispatchTimeInterval = .seconds(1)
    
    // MARK: Public Properties
    
    public var animatedSubmissionCapable: AnimatedSubmissionCapable? {
        didSet {
            if let animatedSubmissionCapable = animatedSubmissionCapable {
                navigationButtonWasEnabled = animatedSubmissionCapable.navigationButton.isEnabled
            }
        }
    }
    
    // MARK: Public Methods
    
    public func animateUserInputSubmission(spinnerAnimatesFromRight: Bool = true) {
        guard let animatedSubmissionCapable = animatedSubmissionCapable else {
            return
        }
        
        animatedSubmissionCapable.spinner.transform = spinnerAnimatesFromRight ? rightTransform(withBounds: animatedSubmissionCapable.bounds) : leftTransform(withBounds: animatedSubmissionCapable.bounds)
        animatedSubmissionCapable.spinner.startAnimating()
        
        navigationButtonWasEnabled = animatedSubmissionCapable.navigationButton.isEnabled
        animatedSubmissionCapable.navigationButton.isEnabled = false
        let submissionViewsTransform = spinnerAnimatesFromRight ? leftTransform(withBounds: animatedSubmissionCapable.bounds) : rightTransform(withBounds: animatedSubmissionCapable.bounds)
        userInputSubmissionAnimationStart = DispatchTime.now()
        UIView.animate(withDuration: SubmissionAnimator.animationTime, delay: 0.0, usingSpringWithDamping: 0.75, initialSpringVelocity: 0.0, options: [.curveEaseOut], animations: {
            animatedSubmissionCapable.submissionViewsToAnimate.forEach { $0.transform = submissionViewsTransform }
            
            animatedSubmissionCapable.spinner.transform = .identity
            
        }, completion: nil)
    }
    
    public func animateUserInputSubmissionError(withErrorText errorText: String, completion: @escaping () -> Void = {}) {
        guard let animatedSubmissionCapable = animatedSubmissionCapable else {
            return
        }
        
        func animateUserInputSubmissionError() {
            animatedSubmissionCapable.headerLabel.text = errorText
            
            // Make sure the header label is properly laid out with the new text.
            animatedSubmissionCapable.layoutIfVisible()
            
            showUserInputViews(animated: true, completion: completion)
        }
        
        if let animationStart = userInputSubmissionAnimationStart {
            // Make sure our input submission timing stays up for a minimum amount of time
            DispatchQueue.main.asyncAfter(deadline: animationStart + SubmissionAnimator.minimumTimeAfterSubmissionAnimationStartBeforeTriggerErrorAnimation) {
                animateUserInputSubmissionError()
            }
        } else {
            logError("Animating user input submission error but we aren't in the middle of a submission animation")
            animateUserInputSubmissionError()
        }
    }
    
    public func showUserInputViews(animated: Bool, completion: @escaping () -> Void = {}) {
        guard let animatedSubmissionCapable = animatedSubmissionCapable else {
            return
        }
        
        UIView.animate(withDuration: animated ? SubmissionAnimator.animationTime : 0.0, delay: 0.0, usingSpringWithDamping: 0.75, initialSpringVelocity: 0.0, options: [.curveEaseIn], animations: {
            animatedSubmissionCapable.submissionViewsToAnimate.forEach { $0.transform = .identity }
            
            animatedSubmissionCapable.spinner.transform = self.rightTransform(withBounds: animatedSubmissionCapable.bounds)
            
        }, completion: { _ in
            animatedSubmissionCapable.navigationButton.isEnabled = self.navigationButtonWasEnabled
            animatedSubmissionCapable.spinner.stopAnimating()
            
            self.userInputSubmissionAnimationStart = nil
            
            completion()
        })
    }
    
    // MARK: Private Properties
    
    private var userInputSubmissionAnimationStart: DispatchTime?
    private var navigationButtonWasEnabled: Bool = false
    
    // MARK: Private Methods
    
    private func leftTransform(withBounds bounds: CGRect) -> CGAffineTransform {
        return CGAffineTransform(translationX: -bounds.width, y: 0.0)
    }
    
    private func rightTransform(withBounds bounds: CGRect) -> CGAffineTransform {
        return CGAffineTransform(translationX: bounds.width, y: 0.0)
    }
    
}
