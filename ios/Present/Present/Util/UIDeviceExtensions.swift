//
//  UIDeviceExtensions.swift
//  Present
//
//  Created by Patrick Niemeyer on 7/25/16.
//

import Foundation

public extension UIDevice {
    
    // Based on: http://stackoverflow.com/a/35618585/74975
    var isSimulator: Bool {
        #if arch(i386) || arch(x86_64)
            return true
        #else
            return false
        #endif
    }
    
}
