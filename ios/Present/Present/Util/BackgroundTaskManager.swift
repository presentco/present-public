//
//  BackgroundTaskManager.swift
//  Present
//
//  Created by Dan Federman on 4/7/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation


public protocol BackgroundTaskManager: class {
    
    func beginBackgroundTask(expirationHandler handler: (() -> Void)?) -> UIBackgroundTaskIdentifier
    func endBackgroundTask(_ identifier: UIBackgroundTaskIdentifier)
    
}


extension UIApplication: BackgroundTaskManager {
    
    // UIApplication already conforms.
    
}
