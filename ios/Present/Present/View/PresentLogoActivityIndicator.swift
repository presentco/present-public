//
//  PurpleActivityIndicatorOverlayViewController.swift
//  Present
//
//  Created by Dan Federman on 8/3/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import Relativity
import Then
import UIKit

public final class PresentLogoActivityIndicator: UIViewController, CAAnimationDelegate
{
    public var name: String = "activity indicator"
    
    // MARK: Private Properties
    var logoSize: CGFloat = 48.0 {
        didSet {
            backSize = logoSize * 1.5
        }
    }
    var backSize: CGFloat = 48.0 * 1.5

    private let activityIndicatorBack = UIView()
        .then {
            $0.isHidden = true
        }
    private let activityIndicator = UIImageView(image: #imageLiteral(resourceName: "present-logo-purple"))
        .then {
            $0.isHidden = true
        }

    private let workQueue = DispatchQueue(label: "activity indicator")
    private var pendingStart: DispatchWorkItem?
    let startDelayMs = 300
    
    // TODO: Implement min display time counterpart to pending start
    private var startTime = Date()
    //private var pendingStop: DispatchWorkItem?
    let minDisplayPeriodMs = 500

    private var animation = CABasicAnimation(keyPath: "transform.rotation.z").then {
        $0.toValue = CGFloat.pi //* 2.0
        $0.duration = 1.0
        $0.repeatCount = Float.infinity
    }

    /// Indicate if the view controller is visible.
    /// This is used to govern renewal of the CA animation.
    private var isVisible = false {
        didSet {
            main {
                //log("\(self.name) isVisible updateAnimation")
                self.updateAnimation()
            }
        }
    }
    private var isAnimating = false {
        didSet {
            main {
                //log("\(self.name) isAnimating updateAnimation")
                self.updateAnimation()
            }
        }
    }

    private var shouldShowActivityIndicator: Bool {
        //log("show activity indicator, isAnimating = \(isAnimating), isVisible=\(isVisible)")
        return isAnimating && isVisible
    }


    // MARK: Private Static Properties

    private static let animationKey = "rotationAnimation"
    
    // MARK: Public Properties
    
    /// When `true`, precludes interaction with any views below the overlay in
    /// any region, including the part not directly under the activity indicator
    /// itself. Defaults to `false`.
    public var isBlocking: Bool = false {
        didSet {
            updateBlockingState()
        }
    }
    
    // MARK: Public Methods
    
    public func startAnimating() {
        scheduleStart()
    }
    
    public func stopAnimating() {
        scheduleStop()
    }
    
    /// Schedule animating the spinner after a brief delay to allow for a fast cancel.
    // Note: We should rewrite this to avoid having to cancel items.
    private func scheduleStart()
    {
        workQueue.async {
            // If already animating ignore
            if self.isAnimating { return }
            
            // pendingStart is our lock, only allow one
            if self.pendingStart != nil { return }
            
            // Schedule a start after short delay
            let pendingStartLocal = DispatchWorkItem {
                self.isAnimating = true
                self.startTime = Date()
                self.pendingStart = nil // clear the pending start
                
            }
            self.pendingStart = pendingStartLocal
            
            //log("\(self.name) scheduled pending start")
            self.workQueue.asyncAfter(milliseconds: self.startDelayMs) {
                self.workQueue.async(execute: pendingStartLocal)
            }
        }
    }
    
    // TODO: Implement min display time counterpart to pending start
    private func scheduleStop() {
        workQueue.async {
            // Cancel any pending start
            if let pendingStart = self.pendingStart {
                //log("\(self.name) cancelled pending start")
                pendingStart.cancel()
                self.pendingStart = nil
            }
            if !self.isAnimating { return }
            // stop animating
            let wasShortDisplay = Date().timeIntervalSince(self.startTime).milliseconds < self.minDisplayPeriodMs
            //log("\(self.name) stop executing, wasShort=\(wasShortDisplay)")
            self.isAnimating = false
        }
    }
    
    public func animationDidStart(_ anim: CAAnimation) {
        //log("\(self.name) animation did start")
    }
    // MARK: CAAnimationDelegate
    public func animationDidStop(_ anim: CAAnimation, finished flag: Bool)
    {
        //log("\(self.name) animation did stop ")
        guard shouldShowActivityIndicator else { return }
        
        //log("\(self.name) adding animation")
        // Animations are stopped when the app is backgrounded. Make sure they get re-added when the app comes into the foreground.
        activityIndicator.layer.add(animation, forKey: PresentLogoActivityIndicator.animationKey)
    }
    
    // MARK: UIViewController
    
    public override func viewDidLoad() {
        super.viewDidLoad()

        activityIndicatorBack.backgroundColor = .white
        activityIndicatorBack.alpha = 0.7
        view.addSubview(activityIndicatorBack)
        view.addSubview(activityIndicator)

        updateBlockingState()
    }
    
    public override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        
        activityIndicatorBack.roundedCorners = backSize / CGFloat(2.0)
        activityIndicator.bounds = CGRect(x:0, y:0, width:logoSize, height:logoSize)
        activityIndicator.middle --> view.middle
        activityIndicatorBack.bounds = CGRect(x:0, y:0, width:backSize, height:backSize)
        activityIndicatorBack.middle --> view.middle
    }
    
    public override func viewDidAppear(_ animated: Bool) {
        //log("view did appear")
        super.viewDidAppear(animated)
        isVisible = true
    }
    
    public override func viewDidDisappear(_ animated: Bool) {
        //log("view did dissappear ")
        isVisible = false
    }
    
    // MARK: Private Methods
    
    private func updateAnimation()
    {
        if shouldShowActivityIndicator {
            //log("\(self.name) add animation, isAnimating=\(activityIndicator.isAnimating), delegate=\(String(describing: animation.delegate))")
            guard animation.delegate == nil else {
                //log("\(self.name): animation delegate is not nil")
                return
            }
            animation.delegate = self
            activityIndicator.isHidden = false
            activityIndicatorBack.isHidden = false
            activityIndicator.layer.add(animation, forKey: PresentLogoActivityIndicator.animationKey)
            
        } else {
            //log("\(self.name) remove animation")
            animation.delegate = nil
            activityIndicator.layer.removeAnimation(forKey: PresentLogoActivityIndicator.animationKey)
            activityIndicator.isHidden = true
            activityIndicatorBack.isHidden = true
        }
    }
    
    private func updateBlockingState() {
        guard isViewLoaded else {
            return
        }
        
        view.isUserInteractionEnabled = isBlocking
    }
    
}
