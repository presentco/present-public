//
//  DeterminingWaitingIndicatingViewController.swift
//
//  Created by Patrick Niemeyer on 7/29/16.
//  Copyright © 2016 Present Company. All rights reserved.
//

import Foundation
import Relativity
import Then
import UIKit

public final class WaitingIndicatorViewController: UIViewController
{
    public let spinner = PresentLogoActivityIndicator()
    
    // MARK: UIViewController
    
    public override func viewDidLoad() {
        super.viewDidLoad()
        
        view.addSubview(contentView)
        contentView.constrainToFillSuperview()
        installChild(viewController: spinner, in: contentView) {
            $0.constrainToFillSuperview()
        }
    }
    
    public override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        spinner.startAnimating()
    }
    
    // MARK: Private Properties
    
    private let contentView = ContentView().then {
        $0.gradientBackgroundView.colors = Palette.onboardingBackgroundGradient
    }
    
    // MARK: ContentView
    
    private class ContentView: UIView
    {
        public let gradientBackgroundView = GradientView()

        public required init() {
            super.init(frame: .zero)
            addSubview(gradientBackgroundView)
            gradientBackgroundView.constrainToFillSuperview()
        }
        
        public required init?(coder aDecoder: NSCoder) {
            fatalError("init(coder:) has not been implemented")
        }
    }

    public override var shouldAutorotate: Bool { return false }
    
    public override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
        return .portrait
    }
    
    public override var preferredStatusBarStyle: UIStatusBarStyle {
        return .default
    }
}
