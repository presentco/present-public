//
//  BundleExtensions.swift
//  Present
//
//  Created by Dan Federman on 3/30/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation


public extension Bundle {
    
    // MARK: Private Static Properties
    
    private static var computedApplicationVersion: String?
    
    // MARK: Public Properties
    
    public var applicationVersion: String {
        if let computedApplicationVersion = Bundle.computedApplicationVersion {
            return computedApplicationVersion
            
        } else {
            let computedApplicationVersion: String = {
                if let version = infoDictionary?["CFBundleShortVersionString"] as? String {
                    if let build = infoDictionary?["CFBundleVersion"] as? String {
                        return version + "b" + build
                        
                    } else {
                        return version
                    }
                    
                } else {
                    logError("Could not determine application version")
                    return "UNKNOWN"
                }
            }()
            
            Bundle.computedApplicationVersion = computedApplicationVersion
            
            return computedApplicationVersion
        }
    }
    
}
