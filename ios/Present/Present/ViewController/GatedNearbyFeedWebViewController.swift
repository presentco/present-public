//
//  GatedNearbyFeedWebViewController.swift
//  Present
//
//  Created by Patrick Niemeyer on 5/9/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation

// The web view showing the nearby tab of the feed view on the home screen
class GatedNearbyFeedWebViewController : GatedFeedContentViewController
{
    // Override viewDidLoad to specify the location observation logic.  Factor out?
    override public func viewDidLoad()
    {
        super.viewDidLoad()
        configurePlaceholder {
            $0.titleText.text = "Welcome to Present!"
            $0.subtitleText.text = "Enable location to find things to do nearby. We never share your location."
            $0.titleImage.image = #imageLiteral(resourceName: "DiscoverImage")
            $0.allowLocationAccessButton.isHidden = false
        }
        configure { _ in
            // configure the FeedContentViewController
        }
        
        locationManager.locationAuthorized.observable.onNext { authorized in
            log("NearbyFeedWebViewController: location auth status = \(authorized)")
            if authorized {
                self.showFeedContentViewController()
            } else {
                self.showPlaceholderViewcontroller()
            }
            }.disposed(by: disposal)
    }
    
}

