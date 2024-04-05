//
//  TimeIntervalExtensions.swift
//  Present
//
//  Created by Dan Federman on 2/10/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation


public extension TimeInterval {
    
    // MARK: Public Static Properties
    
    public static let second = TimeInterval(1.0)

    /// The number of seconds per minute.
    public static let minute = TimeInterval(second*60.0)
    
    /// The number of seconds per hour.
    public static let hour = TimeInterval(minute*60.0)
    
    public static func seconds(_ num: Int) -> TimeInterval {
        return second * Double(num)
    }
    
    public static func minutes(_ num: Int) -> TimeInterval {
        return minute * Double(num)
    }
    
    public static func hours(_ num: Int) -> TimeInterval {
        return hour * Double(num)
    }
    
    public var milliseconds: UInt64 {
        get { return UInt64(self * 1000.0) }
    }
    public var seconds: Double {
        get { return self }
    }
    public var minutes: Double {
        get { return self / TimeInterval.minute }
    }
    public var hours: Double {
        get { return self / TimeInterval.hour }
    }
}
