//
//  NotificationSettingsViewController.swift
//  Present
//
//  Created by Dan Federman on 7/24/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import PresentProtos
import Relativity
import Then
import UIKit


/// The notification settings screen where users can select which notifications they receive
/// Currently accessible through the user profile screen.
public final class NotificationSettingsViewController: UIViewController {
    
    // MARK: Initialization
    
    public required init(userManager: UserManager, screenPresenter: ScreenPresenter) {
        self.userManager = userManager
        self.screenPresenter = screenPresenter
        
        super.init(nibName: nil, bundle: nil)
    }
    
    public required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    // MARK: UIViewController
    
    public override func viewDidLoad() {
        super.viewDidLoad()
        
        view.backgroundColor = .white
        view.addSubview(contentView)
        
        navigationBar.title.text = NSLocalizedString(
            "NotificationSettingsViewControllerTitle",
            tableName: nil,
            bundle: .main,
            value: "Notification Settings",
            comment: "Title for view controller showing notification settings."
        )
        navigationBar.title.apply(theme: UILabel.Theme(textColor: Palette.blackTextColor, font: UIFont.presentFont(ofSize: 18.0, weight: .medium), textAlignment: .center, numberOfLines: 1))
        navigationBar.backButton.addTarget(self, action: #selector(goBack), for: .touchUpInside)
        navigationBar.backButton.setImage(#imageLiteral(resourceName: "LeftChevronGlyph"), for: .normal)
        view.addSubview(navigationBar)
        

        userManager.whenUserProfileAvailable { [weak self] userProfile in
            self?.apply(viewModel: userProfile)
        }
    }

    public override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        view.setNeedsLayout() // iOS 11 safe area
    }

    public override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        
        navigationBar.layoutInSuperview()
        if #available(iOS 11.0, *) {
            // hack, get this into the layout in superview (use additional safe area prop?)
            navigationBar.top --> .top + view.safeAreaInsets.top.verticalOffset //+ 20.verticalOffset
        }

        contentView.bounds.size = CGSize(width: view.bounds.width, height: (navigationBar.bottom |--| view.bottom).height)
        contentView.bottom --> .bottom
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
    
    // MARK: Private Properties
    
    private let userManager: UserManager
    private let screenPresenter: ScreenPresenter
    
    private let navigationBar = FauxNavigationBar()
    private let contentView = ContentView().then {
        $0.notificationSettingsHeader.text = NSLocalizedString(
            "ProfileViewControllerNotificationSettingsHeader",
            tableName: nil,
            bundle: .main,
            value: "Notify me when…",
            comment: "Header text above rows that let you edit push notification settings."
        )
        $0.ownedCircleJoinedNotificationSettingsRow.label.text = NSLocalizedString(
            "ProfileViewControllerNotificationForUserJoinedOwnedCircleSettingsText",
            tableName: nil,
            bundle: .main,
            value: "someone joins a circle I created",
            comment: "Accessory text for a toggle that allows changing whether notifications are sent when someone joins a circle you own."
        )
        $0.joinedGroupNotificationSettingRow.label.text = NSLocalizedString(
            "ProfileViewControllerNotificationForJoinedCirclesSettingsText",
            tableName: nil,
            bundle: .main,
            value: "someone adds to a circle I have joined",
            comment: "Accessory text for a toggle that allows changing whether notifications are sent when someone messages a group the user has joined."
        )
        
        $0.notificationSettingsHeader.apply(theme: UILabel.Theme(textColor: Palette.blackTextColor,
                                                                 font: .presentFont(ofSize: 18.0, weight: .semibold),
                                                                 numberOfLines: 1))
        
        func theme(settingsRow: ContentView.SettingsRowView) {
            settingsRow.backgroundColor = .white
            settingsRow.label.apply(theme: UILabel.Theme(textColor: Palette.blackTextColor,
                                                         font: .presentFont(ofSize: 16.0, weight: .regular)))
            let onTintColor = UIColor(red: 0x30, green: 0x23, blue: 0xAE)
            settingsRow.toggle.onTintColor = onTintColor
            settingsRow.toggle.tintColor = onTintColor.withAlphaComponent(0.5)
        }
        
        theme(settingsRow: $0.joinedGroupNotificationSettingRow)
        theme(settingsRow: $0.ownedCircleJoinedNotificationSettingsRow)
        
        $0.ownedCircleJoinedNotificationSettingsRow.toggle.addTarget(self, action: #selector(didToggleOwnedCircleJoinedNotificationSettings), for: .valueChanged)
        $0.joinedGroupNotificationSettingRow.toggle.addTarget(self, action: #selector(didToggleJoinedGroupNotificationSettings), for: .valueChanged)
        
        $0.addKeyboardShowHideObservers()
    }
    
    // MARK: Private Methods
    
    @objc
    private func didToggleJoinedGroupNotificationSettings() {
        userManager.setNotificationsEnabled(forJoinedGroups: contentView.joinedGroupNotificationSettingRow.toggle.isOn)
    }
    
    @objc
    private func didToggleOwnedCircleJoinedNotificationSettings() {
        userManager.setNotificationsEnabled(forUsersJoiningOwnedGroups: contentView.ownedCircleJoinedNotificationSettingsRow.toggle.isOn)
    }
    
    @objc
    private func goBack() {
        screenPresenter.goBack()
    }
    
    // Set the profile data from the user manager. Invoked on available.
    private func apply(viewModel profile: Present.UserProfile)  {
        contentView.joinedGroupNotificationSettingRow.toggle.isOn = profile.notificationSettings.userCommentsOnJoinedGroup
        contentView.ownedCircleJoinedNotificationSettingsRow.toggle.isOn = profile.notificationSettings.userJoinsOwnedGroup
        
        contentView.layoutIfVisible()
    }
    
    // MARK: ContentView
    
    private class ContentView: UIView {
        
        // MARK: Initialization
        
        public required init() {
            super.init(frame: .zero)
            
            addSubview(notificationSettingsHeader)
            addSubview(ownedCircleJoinedNotificationSettingsRow)
            addSubview(joinedGroupNotificationSettingRow)
        }
        
        public required init?(coder aDecoder: NSCoder) {
            fatalError("init(coder:) has not been implemented")
        }
        
        // MARK: Public Properties
        
        public let notificationSettingsHeader = UILabel()
        public let ownedCircleJoinedNotificationSettingsRow = SettingsRowView()
        public let joinedGroupNotificationSettingRow = SettingsRowView()
        
        // MARK: UIVIew
        
        public override func layoutSubviews() {
            super.layoutSubviews()
            
            notificationSettingsHeader.sizeToFit(fixedWidth: bounds.width - 56.0)
            
            let settingsRows = [ownedCircleJoinedNotificationSettingsRow, joinedGroupNotificationSettingRow]
            var maxSettingsRowTextHeight: CGFloat = 0.0
            settingsRows.forEach {
                $0.bounds.size.width = bounds.width
                $0.layoutIfVisible()
                
                let textHeight = $0.label.bounds.height
                maxSettingsRowTextHeight = max(textHeight, maxSettingsRowTextHeight)
            }
            
            // Make sure that the tallest row has at least 11pts of padding between the text and both the top and bottom of the cell.
            let maxSettingsRowHeight = maxSettingsRowTextHeight + 22.0
            settingsRows.forEach {
                $0.bounds.size.height = maxSettingsRowHeight
            }
            
            distributeSubviewsVertically {
                25.0 <> notificationSettingsHeader <> 13 <> ownedCircleJoinedNotificationSettingsRow <> 1 <> joinedGroupNotificationSettingRow <> ~1~
            }
        }
        
        // MARK: Public Class
        
        public final class SettingsRowView: UIView {
            
            // MARK: Initialization
            
            public required init() {
                super.init(frame: .zero)
                
                addSubview(toggle)
                addSubview(label)
            }
            
            public required init?(coder aDecoder: NSCoder) {
                fatalError("init(coder:) has not been implemented")
            }
            
            // MARK: Public Properties
            
            public let toggle = UISwitch()
            public let label = UILabel()
            
            // MARK: UIVIew
            
            public override func layoutSubviews() {
                super.layoutSubviews()
                
                toggle.sizeToFit()
                left + LayoutConstants.margins.left.horizontalOffset <-- toggle.left
                
                let pixelRounder = PixelRounder(for: self)
                label.sizeToFit(fixedWidth: (toggle.right |--| right).width - LayoutConstants.margins.right - pixelRounder.convert(LayoutConstants.labelToggleMargin, to: self))
                label.right + LayoutConstants.margins.right.horizontalOffset --> right
            }
            
            // MARK: LayoutConstants
            
            private struct LayoutConstants {
                static let margins = UIEdgeInsets(top: 0.0, left: 19.0, bottom: 0.0, right: 19.0)
                static let labelToggleMargin: CGFloat = 12.0
            }
        }
    }
}
