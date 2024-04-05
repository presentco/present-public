//
//  BlurUtil.swift
//  Present
//
//  Created by Patrick Niemeyer on 4/7/16.
//  Copyright © 2016 Present Company. All rights reserved.
//

import CoreGraphics
import UIKit


/**
    This is a Swift async wrapper for calling the UIImage+ImageEffects.h blur effect.
    The image is first scaled to screen size. (Note that this relies on our modified version of that util.)
    The blur operation is run on the default global dispatch queue.
 
    @see UIImage+ImageEffects.h
    @see Present-Bridging-Header.h
*/
func blurImageAsync(
    _ img : UIImage,
    blurRadius : CGFloat = 8,
    tintColor : UIColor? = nil,
    saturationDeltaFactor : CGFloat = 0.4,
    result: @escaping (UIImage) -> Void
) {
    DispatchQueue.global(qos: .default).async {
        let blurredImage = blurImage(img, blurRadius: blurRadius, tintColor: tintColor, saturationDeltaFactor: saturationDeltaFactor )
        result(blurredImage)
    }
}

func blurImage(
    _ img : UIImage,
    blurRadius : CGFloat = 8,
    tintColor : UIColor? = nil,
    saturationDeltaFactor : CGFloat = 0.4
) -> UIImage
{
    let originalImage = img.scaleProportional( to: UIScreen.main.bounds.size )
    let blurredImage = originalImage?.applyBlur(
        withRadius: blurRadius, tintColor: tintColor, saturationDeltaFactor: saturationDeltaFactor, maskImage: nil)
    return blurredImage!
}
