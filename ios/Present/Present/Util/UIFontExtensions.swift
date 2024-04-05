//
//  UIFontExtensions.swift
//  Present
//
//  Created by Dan Federman on 2/14/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import Relativity


public extension UIFont {
    
    // MARK: Weight
    
    public enum PresentWeight {
        case light
        case regular
        case semibold
        case medium
        case bold
        case heavy
    }
    
    // MARK: Public Class Methods
    
    public class func presentFont(ofSize fontSize: CGFloat, weight: PresentWeight) -> UIFont {
        let presentFontName = ".SFUIDisplay"
        switch weight {
        case .light:
            return UIFont(name: "\(presentFontName)-Light", size: fontSize)!
        case .regular:
            return UIFont(name: presentFontName, size: fontSize)!
        case .semibold:
            return UIFont(name: "\(presentFontName)-Semibold", size: fontSize)!
        case .medium:
            return UIFont(name: "\(presentFontName)-Medium", size: fontSize)!
        case .bold:
            return UIFont(name: "\(presentFontName)-Bold", size: fontSize)!
        case .heavy:
            return UIFont(name: "\(presentFontName)-Heavy", size: fontSize)!
        }
    }
    
    /// Returns Present's font scaled to the screen size of `view`. Allows for easy scaling of fonts down or up from single-screen mocks.
    public class func presentFont(ofSize fontSize: CGFloat, weight: PresentWeight, on screenSize: ScreenSize = .fivePointFiveInch, in view: UIView) -> UIFont {
        let pixelRounder: PixelRounder
        if view.window == nil {
            pixelRounder = PixelRounder()
        } else {
            pixelRounder = PixelRounder(for: view)
        }
        
        return presentFont(ofSize: pixelRounder.convert(fontSize, to: ScreenSize(view: view), from: screenSize), weight: weight)
    }
}
