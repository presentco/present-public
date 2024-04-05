//
//  ShadowUtil.swift
//  Present
//
//  Created by Patrick Niemeyer on 4/6/16.
//  Copyright © 2016 Present Company. All rights reserved.
//

import CoreGraphics
import UIKit


/**
    Create a (hopefully efficiently rendered) shadow using the CALayer shadow path facility.
    Note that you probably want to set up shadows in viewWillAppear or viewDidLayoutSubviews after
    autolayout has determined sizes.
 */
class ShadowUtil
{
    class func setShadow(
        _ view : UIView,
        shadowXOffset : CGFloat,
        shadowYOffset : CGFloat,
        shadowOpacity : Float,
        shadowRadius : CGFloat,
        setShadowPath : Bool
    )
    {
        view.clipsToBounds = false
        
        let layer = view.layer
        let bounds = view.bounds
        
        layer.shadowColor = UIColor.black.cgColor
        layer.shadowOpacity = shadowOpacity
        layer.shadowRadius = shadowRadius
        layer.shadowOffset = CGSize(width: shadowXOffset,height: shadowYOffset)
        
        // Setting the shadow path may be important for performance.
        if setShadowPath
        {
            let rect = bounds.offsetBy(dx: shadowXOffset, dy: shadowYOffset)
            if view.roundedCorners > 0 {
                layer.shadowPath =
                    UIBezierPath(roundedRect: rect, cornerRadius: view.roundedCorners).cgPath
            } else {
                layer.shadowPath = UIBezierPath(rect: rect).cgPath
            }
        }
        
        // Note: I'm not certain that this is improving performance, but we currently have no dynamic 
        // effects during scrolling so we can allow it.
        layer.shouldRasterize = true
        layer.rasterizationScale = UIScreen.main.scale
    }
}
