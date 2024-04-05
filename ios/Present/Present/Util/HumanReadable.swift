//
//  HumanReadable.swift
//  Present
//
//  Created by Patrick Niemeyer on 1/10/16.
//  Copyright © 2016 Present Company. All rights reserved.
//

import Foundation

class HumanReadable
{
    static let HereDistance = 100.0 // meters
    
    /**
     "thirty-six seconds""
     "ten hours"
     "four days, four hours"
     */
    class func timeInterval( _ millis : UInt64 ) -> String
    {
        let formatter = DateComponentsFormatter()
        formatter.unitsStyle = .spellOut
        return formatter.string( from: TimeInterval(millis / 1000) )!
    }
    
    /**
     "four days"
     */
    class func courseTimeInterval( _ millis : UInt64 ) -> String
    {
        let str = timeInterval(millis)
        if let i = str.range(of: ",") {
            return str.substring(to: i.lowerBound)
        } else {
            return str
        }
    }
    
    /**
     42m
     7km
     */
    class func distance( _ meters: Double ) -> String
    {
        if ( meters < HumanReadable.HereDistance ) {
            return "Here"
        }
        let im = Int(meters)
        if ( im < 1000 ) {
            return "\(im)m"
        }
        return "\(im/1000)km"
    }
    
    /**
     */
    class func distanceShortMiles( _ meters: Double ) -> String
    {
        if ( meters < HumanReadable.HereDistance ) {
            return "Here"
            //return "0.0mi"
        }
        let metersToMiles = 0.000621371
        let miles = meters * metersToMiles
        return String(format: "%0.1fmi", miles)
    }
    
}
