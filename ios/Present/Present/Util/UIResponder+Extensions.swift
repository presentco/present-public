//
//  UIResponder+Extensions.swift
//  Present
//
//  Created by Patrick Niemeyer on 7/19/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation

public extension UIResponder
{
    /// This is useful for cases where we do not have easy access to a parent
    /// view controller that may be showing the keyboard, but we wish to
    /// dismiss any editing in progress
    public class func resignAnyResponders() {
        UIApplication.shared
            .sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
    }
}
