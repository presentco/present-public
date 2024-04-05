//
//  GatedProfileViewController.swift
//  Present
//
//  Created by Patrick Niemeyer on 5/1/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation

/// Guards the profile view for the necessary signup status and permissions, showing either
/// the ProfileViewController or the appropriate signup screen as needed.
public class GatedProfileViewController: GatedViewController, ProfileViewControllerConfig
{
    // One of these is shown
    public var profileViewController: ProfileViewController?
    
    // TODO: Factor this out into parent if we add more of these
    public var showPlaceholder: Bool? {
        didSet {
            guard let showPlaceholder = showPlaceholder else { return }
            if showPlaceholder {
                self.showPlaceholderViewcontroller()
            } else {
                self.showProfileViewController()
            }
            logView()
        }
    }

    override public func viewDidLoad()
    {
        super.viewDidLoad()
        userManager.authorizationStatus.observable.onNext { status in
            logn("auth status = \(status)")
            if self.userManager.authorizedToProceedToApp {
                self.showPlaceholder = false
            } else {
                self.showPlaceholder = true
            }
        }.disposed(by: disposal)
    }
    
    public override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        logView()
    }
    
    private func logView() {
        if let showPlaceholder = showPlaceholder {
            if showPlaceholder {
                logEvent(.profile_placeholder_view)
            } else {
                logEvent(.profile_view)
            }
        }
    }

    private func showProfileViewController()
    {
        if let profileViewController = self.profileViewController {
            profileViewController.view.toFront()
        } else {
            profileViewController = ProfileViewController()
                .configure { _ in
                    self.configureViewControllerQueue.run()
                }.then {
                    self.profileViewController = $0 // Must be set before config
                    install(viewController: $0)
                }
        }
    }

    // MARK: ProfileViewControllerConfig
    
    public func apply(person: Person) {
        configureViewControllerQueue.enqueue {
            self.profileViewController?.apply(person: person)
            self.profileViewController?.view.isHidden = false
        }
    }
    public func setBackButtonVisible(_ visible: Bool) {
        configureViewControllerQueue.enqueue {
            self.profileViewController?.setBackButtonVisible(visible)
        }
    }
}

