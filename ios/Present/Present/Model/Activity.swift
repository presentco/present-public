//
//  Activity.swift
//  Present
//
//  Created by Dan Federman on 7/6/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation

public struct Activity: Comparable
{
    // MARK: Public Properties
    
    public let activityToken: PresentUUID
    public let date: Date
    public let photo: URL?
    public let summary: String
    
    public let target: Presentable
    
    // MARK: Comparable
    
    public static func <(lhs: Activity, rhs: Activity) -> Bool {
        guard lhs.date.timeIntervalSinceReferenceDate != rhs.date.timeIntervalSinceReferenceDate else {
            return lhs.activityToken.uuidString < rhs.activityToken.uuidString
        }
        
        return lhs.date.timeIntervalSinceReferenceDate < rhs.date.timeIntervalSinceReferenceDate
    }
    
    public static func <=(lhs: Activity, rhs: Activity) -> Bool {
        return lhs.date.timeIntervalSinceReferenceDate <= rhs.date.timeIntervalSinceReferenceDate
    }
    
    public static func >=(lhs: Activity, rhs: Activity) -> Bool {
        return lhs.date.timeIntervalSinceReferenceDate >= rhs.date.timeIntervalSinceReferenceDate
    }
    
    public static func >(lhs: Activity, rhs: Activity) -> Bool {
        guard lhs.date.timeIntervalSinceReferenceDate != rhs.date.timeIntervalSinceReferenceDate else {
            return lhs.activityToken.uuidString > rhs.activityToken.uuidString
        }
        
        return lhs.date.timeIntervalSinceReferenceDate > rhs.date.timeIntervalSinceReferenceDate
    }
    
    // MARK: Equatable
    
    public static func ==(lhs: Activity, rhs: Activity) -> Bool {
        return lhs.activityToken == rhs.activityToken
    }
    
}
