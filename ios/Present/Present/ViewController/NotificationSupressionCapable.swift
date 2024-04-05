//
//  NotificationSupressionCapable.swift
//  Present
//
//  Created by Dan Federman on 4/3/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation


public protocol NotificationSupressionCapable {
    
    func shouldSuppress(notification: RemoteNotification) -> Bool
    
}
