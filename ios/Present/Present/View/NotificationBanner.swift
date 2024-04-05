//
//  NotificationBanner.swift
//  Present
//
//  Created by Pegah Keshavarz on 1/10/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import PresentProtos

public protocol NotificationBannerDelegate: class {
    func enableNotification()
    func onClickNoButton()
}

public class NotificationBanner : UIView
{
    // TODO: spelling
    @IBOutlet weak var firtTitle: UILabel!
    @IBOutlet weak var secondTitle: UILabel!
    
    // TODO: spelling
    @IBOutlet weak var seperator: UILabel!{
        didSet{
            seperator.backgroundColor = Palette.whiteToWhiteSeparatorColor
        }
    }
    @IBOutlet weak var noButton: UIButton!{
        didSet{
             noButton.roundedCorners = 15
             noButton.layer.borderWidth = 2
             noButton.layer.borderColor = #colorLiteral(red: 0.4235294118, green: 0.2470588235, blue: 0.8, alpha: 1)
             noButton.addTarget(self, action: #selector(onClickNoButton), for: .touchUpInside)
        }
    }

    @IBOutlet weak var yesButton: UIButton!{
        didSet{
            yesButton.roundedCorners = 15
            yesButton.addTarget(self, action: #selector(enableNotification), for: .touchUpInside)
        }
    }
    
  
    override init(frame: CGRect) {
        super.init(frame: frame)
        initView()
    }
    
    public weak var delegate: NotificationBannerDelegate?
    
    required public init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
    
    private func initView() {
        let view = viewFromNibForClass()
        view.frame = bounds
        addSubview(view)
        
    }
    

    @IBAction func enableNotification(_ sender: Any) {
         delegate?.enableNotification()
    }


    @IBAction func onClickNoButton(_ sender: Any) {
        delegate?.onClickNoButton()
    }
}

// MARK: Logic related to showing the notification banner

extension NotificationBanner
{
    /// The block receives true if the notification banner should appear, taking into account:
    ///   - The current status of the user's iOS notification permissions.
    ///   - The user's current notification preference settings.
    ///   - The user's last banner viewed time after a dismissal.
    public class func getStatus(
        userManager: UserManager, notificationAuthorizationManager: NotificationAuthorizationManager,
        block: @escaping (Bool)->Void)
    {
        userManager.whenUserProfileAvailable { userProfile in
            notificationAuthorizationManager.retrieveStatus { iOSStatus in
                
                var iosNotificationsEnabled: Bool
                switch(iOSStatus) {
                case .authorized:
                    iosNotificationsEnabled = true
                case .denied, .partial, .unrequested:
                    iosNotificationsEnabled = false
                }

                // If we don't have iOS notifs on or the user has any notif preference turned off
                let needNotifs = !iosNotificationsEnabled ||
                    !userProfile.notificationSettings.userCommentsOnJoinedGroup ||
                    !userProfile.notificationSettings.userJoinsOwnedGroup
                //logDebug("NotificationBanner: needNotifs = \(needNotifs)")

                // Get the (singleton) view state to see when the user last saw the banner
                getViewState { viewState in
                    // Admins see it every minute for testing
                    let showInterval = (UserManager.shared.userIsAdmin ? TimeInterval.minutes(1) : TimeInterval.hours(24)).milliseconds
                    let since = Date.systemTimeInMilliseconds - (viewState.lastViewedDate ?? 0)

                    let shouldShow = needNotifs && since > showInterval
                    //logDebug("NotificationBanner: shouldShow = \(shouldShow)")
                    block(shouldShow)
                }
            }
        }
    }
    
    public class func getViewState(block: @escaping (NotificationBannerViewState)->Void) {
        ViewStateManager.shared.viewState(forInstance: "notificationBanner") { (viewState:NotificationBannerViewState) in
            block(viewState)
        }
    }
}

/// The view state stored for the notification banner
public class NotificationBannerViewState : ViewState, JsonConvertible {
    public var lastViewedDate: UInt64? = nil { didSet { save() } }
}
