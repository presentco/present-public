//
//  ActivityIndicatorOverlayViewController.swift
//  Present
//
//  Created by Martin Mroz on 5/4/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import Relativity
import Then
import UIKit


public final class ActivityIndicatorOverlayViewController : UIViewController {
    
    // MARK: Public Properties
    
    /// Tint color of the activity indicator.
    public var color: UIColor? {
        get {
            return activityIndicator.color
        }
        set(newValue) {
            activityIndicator.color = newValue
        }
    }
    
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
        activityIndicator.startAnimating()
    }
    
    public func stopAnimating() {
        activityIndicator.stopAnimating()
    }
    
    // MARK: UIViewController
    
    public override func viewDidLoad() {
        super.viewDidLoad()
        
        view.addSubview(activityIndicator)
        
        updateBlockingState()
    }
    
    public override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        
        activityIndicator.bounds.size = CGSize(width: 50, height: 50)
        // Transform the spinner to make it larger.
        activityIndicator.transform = CGAffineTransform(scaleX: 2.0, y: 2.0)
        activityIndicator.middle --> view.middle
    }
    
    // MARK: Private Properties
    
    private var activityIndicator = UIActivityIndicatorView(activityIndicatorStyle: .gray)
    
    // MARK: Private Methods
    
    private func updateBlockingState() {
        guard isViewLoaded else {
            return
        }
        view.isUserInteractionEnabled = isBlocking
    }
    
}
