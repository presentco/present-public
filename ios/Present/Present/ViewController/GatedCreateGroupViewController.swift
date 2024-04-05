//
//  GatedCreateGroupViewController.swift
//  Present
//
//  Created by Patrick Niemeyer on 5/2/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import RxSwift

/// Guards for the necessary signup status and permissions, showing either
/// the view controller or the appropriate signup placeholder screen as needed.
public class GatedCreateGroupViewController: GatedViewController 
{
    // One of these is shown
    public var createGroupViewController: CreateCircleViewController?
    
    /// The latest decision on whether to show create group or the placeholder
    /// Note: Set on the main thead.
    public var shouldProceedToCreateGroup: Bool = false

    override public func viewDidLoad()
    {
        super.viewDidLoad()
        
        configurePlaceholder {
            $0.titleText.text = "Create A Circle"
            $0.subtitleText.text = "A circle is a location-based chat to chat with friends and people nearby."
            $0.titleImage.image = #imageLiteral(resourceName: "CreateCircleImage")
            // The "get started" button
            $0.buttonEventMapping[$0.verifyPhoneNumberButton] = .create_placeholder_tap_get_started
        }
        
        Observable.combineLatest(
            userManager.authorizationStatus.observable,
            locationManager.locationAuthorized.observable
        ).onNext
            { authStatus, locationAuthorized in
                let userAuthorized = self.userManager.authorizedToProceedToApp
                
                log("auth status = \(authStatus)")
                if userAuthorized && locationAuthorized {
                    self.shouldProceedToCreateGroup = true
                    self.showCreateGroupViewController()
                } else {
                    self.shouldProceedToCreateGroup = false
                    self.showPlaceholderViewcontroller()
                }
                
                self.placeholderViewController?.verifyPhoneNumberButton.isShown = !userAuthorized
                self.placeholderViewController?.allowLocationAccessButton.isShown = userAuthorized && !locationAuthorized
                
            }.disposed(by: disposal)
    }

    public override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        logEvent(.create_placeholder_view)
    }

    private func showCreateGroupViewController()
    {
        if let createGroupViewController = self.createGroupViewController {
            createGroupViewController.view.toFront()
        } else {
            self.createGroupViewController = CreateCircleViewController()
                .configure { _ in
                    self.configureViewControllerQueue.run()
                }.then {
                    self.createGroupViewController = $0 // Must be set before config
                    install(viewController: $0)
                }
        }
    }
    
    @discardableResult
    public func configure(withBlock block: @escaping ((CreateCircleViewController)->Void)) -> Self {
        configureViewControllerQueue.enqueue {
            self.createGroupViewController?.configure(withBlock: block)
        }
        return self
    }

}

