//
//  UIColorExtensions.swift
//  Present
//
//  Created by Patrick Niemeyer on 9/12/16.
//  Copyright © 2016 Present Company. All rights reserved.
//

import CoreGraphics
import UIKit


public extension UIColor {
    
    // MARK: Public Methods
    
    @nonobjc
    public convenience init(red: Int, green: Int, blue: Int, alpha: CGFloat = 1.0) {
        self.init(red: CGFloat(red) / 255.0, green: CGFloat(green) / 255.0, blue: CGFloat(blue) / 255.0, alpha: alpha)
    }

    public class func hex(_ value: Int) -> UIColor {
        return UIColor(hex: value)
    }
    
    /**
        Take an RGB encoded int value, e.g. "0xAA_BB_CC"
    */
    public convenience init(hex: Int, alpha: CGFloat = 1.0) {
        let r = hex >> 16
        let g = (hex >> 8) & 0xff
        let b = hex & 0xff
        self.init(red: r, green: g, blue: b, alpha: alpha)
    }
    
    /// e.g. "rgb(249, 249, 249)"
    public convenience init?(rgbString: String)
    {
        var rgb = rgbString.dropFirst(4).dropLast().split(separator: ",")
        func toInt(_ s:Substring) -> Int? {
            return Int(s.trimmingCharacters(in: CharacterSet.whitespaces))
        }
        guard rgb.count == 3,
            let red = toInt(rgb[0]), let green = toInt(rgb[1]), let blue = toInt(rgb[2])
        else { return nil }
        self.init(red: red, green: green, blue: blue)
    }
    
    func interpolateRGBColorTo(end: UIColor, fraction: CGFloat) -> UIColor? {
        var f = max(0, fraction)
        f = min(1, f)
        
        guard let c1 = self.cgColor.components, let c2 = end.cgColor.components else { return nil }
        
        // TODO: If this blows up it's because we used a built in UIColor that was not
        // TODO: constructed from color components? e.g. .white vs UIColor(hex: 0xFFFFFF)
        // TODO: What is going on here?
        let r: CGFloat = CGFloat(c1[0] + (c2[0] - c1[0]) * f)
        let g: CGFloat = CGFloat(c1[1] + (c2[1] - c1[1]) * f)
        let b: CGFloat = CGFloat(c1[2] + (c2[2] - c1[2]) * f)
        //let a: CGFloat = CGFloat(c1[3] + (c2[3] - c1[3]) * f)
        
        return UIColor(red: r, green: g, blue: b, alpha: 1.0)
    }
}
