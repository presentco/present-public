//
//  LaunchScreenViewController.swift
//  Present
//
//  Created by Dan Federman on 3/29/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation


public final class LaunchScreenViewController: UIViewController {
    
    // MARK: UIViewController
    
    // Note: This is relying on what seems to be an undocumented internal naming convention for
    // Note: selecting specific image sizes from the LaunchImage image set.
    // Note: The actual image names do not matter. The string is composed as:
    // Note:     LaunchImage-<iOS version string>-<height string>
    // Note: scale (e.g. @3x) and png extensions are not required but tolerated.
    public override func viewDidLoad()
    {
        super.viewDidLoad()
        
        view.addSubview(launchImageView)
        
        switch max(UIScreen.main.bounds.height, UIScreen.main.bounds.width) {
        case 568:
            launchImageView.image = UIImage(named: "LaunchImage-700-568h")
        case 667:
            launchImageView.image = UIImage(named: "LaunchImage-800-667h")
        case 736:
            launchImageView.image = UIImage(named: "LaunchImage-800-Portrait-736h")
        case 812:
            launchImageView.image = UIImage(named: "LaunchImage-1100-Portrait-2436h")
        default:
            logError("Could not find LaunchScreen image for screen with size \(UIScreen.main.bounds.height)")
            launchImageView.image = UIImage(named: "LaunchImage-800-Portrait-736h")
        }
    }

    public override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        logEvent(.app_view_splash_screen)
        logDebug("LaunchScreenViewController: didAppear")
    }

    public override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        
        launchImageView.sizeToFitSuperview()
    }
    
    public override var shouldAutorotate: Bool {
        return false
    }
    
    public override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
        return .portrait
    }
    
    public override var preferredStatusBarStyle: UIStatusBarStyle {
        return .default
    }
    
    public override var prefersStatusBarHidden: Bool {
        return true
    }
    
    // MARK: Private Properties
    
    private let launchImageView = UIImageView()
    
}
