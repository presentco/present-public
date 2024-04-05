//
//  UserNotificationPoster.swift
//  Present
//
//  Created by Dan Federman on 4/6/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import UserNotifications


public protocol UserNotificationPoster {
    
    func add(_ request: UNNotificationRequest, withCompletionHandler completionHandler: ((Error?) -> Swift.Void)?)
    
}


extension UNUserNotificationCenter: UserNotificationPoster {
    
    // UNUserNotificationCenter already conforms.
    
}
