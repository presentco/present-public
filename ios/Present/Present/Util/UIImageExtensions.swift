//
//  UIImageExtensions.swift
//  Present
//
//  Created by Patrick Niemeyer on 8/23/16.
//  Copyright © 2016 Present Company. All rights reserved.
//

import CoreGraphics
import Foundation
import UIKit


public extension UIImage {
    
    /// Colors the receiving glyph.
    /// - parameter glyphColor: The color to paint the glyph.
    /// - returns: The receiver with non-alpha components colored with the glyphColor.
    public func glyph(with glyphColor: UIColor) -> UIImage {
        UIGraphicsBeginImageContext(size)
        defer {
            UIGraphicsEndImageContext()
        }
        
        guard let context = UIGraphicsGetCurrentContext() else {
            logError("Could not get context")
            return self
        }
        
        let imageRect = CGRect(origin: .zero, size: size)
        
        context.setFillColor(glyphColor.cgColor)
        context.fill(imageRect)
        
        draw(in: imageRect, blendMode: .destinationIn, alpha: 1.0)
        
        guard let coloredImage = UIGraphicsGetImageFromCurrentImageContext() else {
            logError("Could not get colored image")
            return self
        }
        
        return coloredImage
    }
    
    // TODO: why can't objc overload these names?
    public func scaleAspectFitSizeIfNeeded( _ size : CGSize ) -> UIImage {
        return scaleAspectFitIfNeeded( min(size.width, size.height) )
    }
    
    /// Scales if needed, else returns the original
    public func scaleAspectFitIfNeeded( _ maxDim : CGFloat ) -> UIImage
    {
        let width = self.size.width * self.scale // UIImage size is reported in points
        let height = self.size.height * self.scale
        let maxwh = max(width,height)
        if maxwh <= maxDim { return self }
        let scale = maxDim / maxwh
        return self.scale(to: CGSize(width: width*scale, height: height*scale))
    }
    
    
    /*
    public func roundImage(radius: CGFloat) -> UIImage {
        let imageView: UIImageView = UIImageView(image: self)
        let layer = imageView.layer
        layer.masksToBounds = true
        layer.cornerRadius = radius
        UIGraphicsBeginImageContext(imageView.bounds.size)
        layer.render(in: UIGraphicsGetCurrentContext()!)
        let roundedImage = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        return roundedImage ?? UIImage()
    }*/
}
