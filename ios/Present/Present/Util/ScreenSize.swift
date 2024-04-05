//
//  ScreenSize.swift
//  Present
//
//  Created by Dan Federman on 4/6/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation


public enum ScreenSize {
    
    case fourInch
    case fourPointSevenInch
    case fivePointFiveInch
    
    // MARK: Public Static Methods
    
    public static func convert(_ input: CGFloat, to currentScreenSize: ScreenSize, from designedScreen: ScreenSize = .fivePointFiveInch) -> CGFloat {
        let scale = currentScreenSize.screenWidth / designedScreen.screenWidth
        return input * scale
    }
    
    // MARK: Initialization
    
    public init(view: UIView) {
        let screenWidth = view.window?.bounds.width ?? UIScreen.main.bounds.width
        
        switch screenWidth {
        case ScreenSize.fourInch.screenWidth:
            self = .fourInch
        case ScreenSize.fourPointSevenInch.screenWidth:
            self = .fourPointSevenInch
        case ScreenSize.fivePointFiveInch.screenWidth:
            self = .fivePointFiveInch
        default:
            logError("Could not find correct font for screen with width \(screenWidth)")
            self = .fourPointSevenInch
        }
    }
    
    // MARK: Public Variables
    
    public var screenWidth: CGFloat {
        switch self {
        case .fourInch:
            return 320
        case .fourPointSevenInch:
            return 375
        case .fivePointFiveInch:
            return 414
        }
    }
    
    public var underlineThickness: CGFloat {
        switch self {
        case .fourInch:
            return 1.5
        case .fourPointSevenInch:
            return 2.0
        case .fivePointFiveInch:
            return 3.0
        }
    }
    
    // MARK: Public Methods
    
    public func convert(_ input: CGFloat, from designedScreen: ScreenSize = .fivePointFiveInch) -> CGFloat {
        return ScreenSize.convert(input, to: self, from: designedScreen)
    }
}
