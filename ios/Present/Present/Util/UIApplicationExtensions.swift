//
//  UIApplicationExtensions.swift
//  Present
//
//  Created by Patrick Niemeyer on 11/30/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation


public extension UIApplication
{
    public class var activeInForeground : Bool {
        switch(UIApplication.shared.applicationState) {
            case .active:
                return true
            case .background, .inactive:
                return false
        }
    }
}
