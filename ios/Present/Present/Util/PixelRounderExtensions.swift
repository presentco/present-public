//
//  PixelRounderExtensions.swift
//  Present
//
//  Created by Dan Federman on 4/6/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import Relativity


public extension Relativity.PixelRounder {
    
    // MARK: Public Properties
    
    public var pixelHeight: CGFloat {
        return ceilToPixel(1e-10)
    }
    
    // MARK: Public Methods
    
    /// Returns the input, converted to the desired screen size, rounded to the pixel.
    public func convert(_ input: CGFloat, to currentScreenSize: ScreenSize, from designedScreen: ScreenSize = .fivePointFiveInch) -> CGFloat {
        return roundToPixel(currentScreenSize.convert(input, from: designedScreen))
    }
    
    /// Returns the input, converted to the desired screen size, rounded to the pixel.
    public func convert(_ input: CGFloat, to view: UIView, from designedScreen: ScreenSize = .fivePointFiveInch) -> CGFloat {
        return roundToPixel(ScreenSize(view: view).convert(input, from: designedScreen))
    }
    
}
