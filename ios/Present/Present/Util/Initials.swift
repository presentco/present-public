//
//  Initials.swift
//  Present
//
//  Created by Patrick Niemeyer on 7/15/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation

public class Initials
{
    public static func generateInitialsImage(name: FullName, height: CGFloat) -> UIImage?
    {
        let label = UILabel()
        label.frame.size = CGSize(width: height, height: height)
        label.textColor = UIColor.white
        label.backgroundColor = Palette.presentPurple2
        let first = name.givenName.first ?? " "
        let last = name.familyName.first ?? " "
        let initials = String(first) + String(last)
        label.font = UIFont.presentFont(ofSize: 18, weight: .medium)
        label.text = initials
        label.textAlignment = NSTextAlignment.center
        label.layer.cornerRadius = height / 2.0
        
        UIGraphicsBeginImageContextWithOptions(label.bounds.size, true, UIScreen.main.scale)
        label.layer.render(in: UIGraphicsGetCurrentContext()!)
        let image = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        return image
    }
}
