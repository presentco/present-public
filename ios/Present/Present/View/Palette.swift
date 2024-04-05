//
//  Palette.swift
//  Present
//
//  Created by Dan Federman on 1/12/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation


public struct Palette {

    public static let defaultHighlightedButtonTitleAlpha: CGFloat = 0.5
    public static let defaultDisabledButtonTitleAlpha: CGFloat = 0.2
    public static let defaultLabelPlaceholderTextAlpha: CGFloat = 0.5
    
    public static let blackTextColor = UIColor(red: 0x03, green: 0x03, blue: 0x03)
    public static let lightGrayTextColor = UIColor(red: 0xA4, green: 0xAA, blue: 0xB3, alpha: 1)
    public static let unreadPurpleColor = UIColor(red: 157, green: 103, blue: 234, alpha: 1)
    public static let whiteToWhiteSeparatorColor = UIColor(red: 242, green: 242, blue: 242, alpha: 1.0)

    public static let cursorColor = UIColor(hex: 0xd0_b0_ed)

    public static let defaultNavigationButtonTitleColor = UIColor.white
    public static let defaultNavigationButtonBackgroundColor = UIColor(red: 0x78, green: 0x3D, blue: 0xE3)
    
    public static let onboardingBackgroundGradient = [UIColor.white, UIColor.white]
    public static let onboardingNavigationButtonTitleColor = UIColor.white
    public static let onboardingNavigationButtonBackgroundColor = UIColor(red: 0x53, green: 0x37, blue: 0xC2)
    
    public static let joinToCommentButtonTitleColor = UIColor.white
    public static let joinToCommentButtonBackgroundColor = UIColor(red: 0x78, green: 0x3D, blue: 0xE3)
    
    public static let incomingBalloonColor = UIColor(red: 0xF3, green: 0xF4, blue: 0xF7)
    public static let outgoingBalloonColor = UIColor(red: 0xED, green: 0xEE, blue: 0xFF)
    public static let messageTextColor = UIColor(red: 0x4A, green: 0x4A, blue: 0x4A)
    
    public static let darkHeaderBackgroundColor = UIColor(hex: 0x3F_3F_3F)
    public static let darkHeaderTextColor = UIColor.white
    
    public static let avatarLoadingColor = UIColor.lightGray
    
    public static let createGroupPanelTextColor = blackTextColor
    public static let createGroupPanelLabelColor = blackTextColor.withAlphaComponent(0.54)
    public static let createGroupPanelDividerColor = UIColor(hex: 0xb6_b9_bf)
    public static let createGroupCursorColor = Palette.cursorColor
    public static let createGroupTableTextColor = UIColor(hex: 0x9b_9b_9b)
    public static let createGroupTableIconColor = UIColor(hex: 0x9e_9e_9e)

    //public static let presentPurple = UIColor(hex:0x81_36_EC)
    public static let presentPurple = UIColor(hex:0x71_41_DB)
    public static let presentPurple2 = UIColor(hex:0x72_46_C5)
}

extension UIColor {
    struct Present {
        struct Purple {
            static let Main = UIColor(red: 106, green: 69, blue: 199)
        }
        
        struct Line {
            static let Gray = UIColor(red: 242, green: 242, blue: 242)
        }
        
        struct Button {
            static let Enabled = UIColor(red: 106, green: 69, blue: 199)
            static let Disabled = UIColor(red: 223, green: 223, blue: 223)
        }
        
        struct Text {
            static let Error = UIColor(red: 223, green: 57, blue: 157)
            static let LightGray = UIColor(red: 126, green: 126, blue: 126)
        }
    }
}
