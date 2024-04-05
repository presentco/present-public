//
//  DateExtensions.swift
//  Present
//
//  Created by Dan Federman on 2/10/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation


/// The number of seconds per minute.
public let SEC_PER_MIN: UInt64 = 60
/// The number of minutes per hour.
public let MIN_PER_HOUR: UInt64 = 60
/// The number of seconds per hour.
public let SEC_PER_HOUR: UInt64 = SEC_PER_MIN * MIN_PER_HOUR

/// Number of milliseconds per second.
public let MSEC_PER_SEC: UInt64 = 1000
/// The number of milliseconds per minute.
public let MSEC_PER_MIN : UInt64 = MSEC_PER_SEC * SEC_PER_MIN


public extension Date {
    
    // MARK: Public Static Properties
    
    //// The Unix Epoch time stamp in milliseconds, matching Java's System.currentTimeMillis().
    public static var systemTimeInMilliseconds: UInt64 {
        return Date().millisecondsSince1970
    }
    
    //// The Unix Epoch time stamp in milliseconds, matching Java's System.currentTimeMillis().
    public static var currentTimeMillis: UInt64 {
        return systemTimeInMilliseconds
    }
    
    /// A date formatter that interacts with dates with a short month followed by day and year. e.g. "Sept 1, 2016".
    public static let shortMonthFullDayYearDateFormatter: DateFormatter = {
        let dateFormatter = DateFormatter()
        dateFormatter.dateStyle = .medium
        dateFormatter.timeStyle = .none
        
        return dateFormatter
    }()
    
    /// A date formatter that interacts with dates with a short month followed by day. e.g. "Sept 1".
    public static let noYearDateFormatter: DateFormatter = {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "MMM dd"
        
        return dateFormatter
    }()
    
    /// A date formatter that interacts with dates represented by relative text. e.g. "Yesterday", "Today", and "Tomorrow".
    public static let relativeDayDateFormatter: DateFormatter = {
        let dateFormatter = DateFormatter()
        dateFormatter.doesRelativeDateFormatting = true
        dateFormatter.dateStyle = .short
        
        return dateFormatter
    }()
    
    /// A date formatter that interacts with dates represented by a day of the week. e.g. "Monday".
    public static let dayOfWeekDateFormatter: DateFormatter = {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "EEEE"
        
        return dateFormatter
    }()
    
    /// A date formatter that interacts with dates represented by a time of day. e.g. "2:16 PM".
    public static let hourMinuteTimeFormatter: DateFormatter = {
        let dateFormatter = DateFormatter()
        dateFormatter.timeStyle = .short
        
        return dateFormatter
    }()
    
    public static let nowText = NSLocalizedString("DateRepresentingCurrentTime",
                                                  tableName: nil, bundle: .main,
                                                  value: "Now",
                                                  comment: "Accessory text describing that an event took place now (or very very recently)")
    
    // MARK: Public Static Methods
    
    /// Returns the number of hours represented by the input time interval as a string.
    public static func hoursText(for timeInterval: TimeInterval) -> String {
        let hours = Int(timeInterval / TimeInterval.hour)
        
        let formatString = NSLocalizedString("TimeDeltaInHoursFormat",
                                             tableName: nil, bundle: .main,
                                             value: "%dhr",
                                             comment: "An abbreviated string representing time in hours. arg0: an integer representing a number of hours (always less than 24).")
        return String(format: formatString, hours)
    }
    
    /// Returns the number of minutes represented by the input time interval as a string.
    public static func minutesText(for timeInterval: TimeInterval) -> String {
        let minutes = Int(timeInterval / TimeInterval.minute)
        
        let formatString = NSLocalizedString("TimeDeltaInMinutesFormat",
                                             tableName: nil, bundle: .main,
                                             value: "%d min",
                                             comment: "An abbreviated string representing time in min. arg0: an integer representing a number of minutes (always less than 60).")
        return String(format: formatString, minutes)
    }
    
    /// Returns the number of seconds represented by the input time interval as a string.
    public static func secondsText(for timeInterval: TimeInterval) -> String {
        let seconds = Int(timeInterval)
        
        let formatString = NSLocalizedString("TimeDeltaInSecondsFormat",
                                             tableName: nil, bundle: .main,
                                             value: "%ds",
                                             comment: "An abbreviated string representing time in seconds. arg0: an integer representing a number of seconds (always less than 60).")
        return String(format: formatString, seconds)
    }
    
    // MARK: Initialization
    
    //// Create a Date from a Unix Epoch time stamp in milliseconds, matching Java's System.currentTimeMillis().
    public init(millisecondsSince1970: UInt64) {
        self.init(timeIntervalSince1970: Double(millisecondsSince1970 / MSEC_PER_SEC))
    }
    
    // MARK: Public Properties
    
    /// A Unix Epoch time stamp in milliseconds.
    public var millisecondsSince1970: UInt64 {
        return UInt64(timeIntervalSince1970 * TimeInterval(MSEC_PER_SEC))
    }
    
    // MARK: Public Methods
    
    /// Returns a string representing the earlierDate relative to self.
    /// - parameter earlierDate: The date to which to compare self.
    /// - parameter nowTimeSpan: The number of seconds prior to self that will be considered to be "now".
    /// - parameter maxGranularity: The maximum granularity of the relative time.
    /// - parameter calendar: The calendar to use for calculating date components.
    /// - returns: The date delta represented as a string. Examples: "5s", "49 min", "3 hrs", "Yesterday", "Monday", "Jan 21", and "Dec 1, 2014".
    public func relativeTime(since earlierDate: Date, nowTimeSpan: TimeInterval = 0.0, maxGranularity: RelativeTimeGranularity = .second, calendar: Calendar = .current) -> String {
        let relaventDateComponents: Set<Calendar.Component> = {
            let relaventDateComponents: [Calendar.Component] = [.year, .month, .day, .hour, .minute]
            return Set(relaventDateComponents)
        }()
        
        let interval = timeIntervalSince(earlierDate)
        guard interval > nowTimeSpan else {
            return Date.nowText
        }
        
        let differenceDateComponents = calendar.dateComponents(relaventDateComponents, from: earlierDate, to: self)
        
        guard
            let yearDelta = differenceDateComponents.year,
            let monthDelta = differenceDateComponents.month,
            let dayDelta = differenceDateComponents.day,
            let hourDelta = differenceDateComponents.hour,
            let minuteDelta = differenceDateComponents.minute
            else {
            return ""
        }
        
        if yearDelta > 0 {
            return Date.shortMonthFullDayYearDateFormatter.string(from: earlierDate)
            
        } else if monthDelta > 0 {
            return Date.noYearDateFormatter.string(from: earlierDate)
            
        } else if dayDelta > 0 {
            if dayDelta > 6 {
                return Date.noYearDateFormatter.string(from: earlierDate)
            } else if dayDelta < 1 {
                return Date.relativeDayDateFormatter.string(from: earlierDate)
            } else {
                return Date.dayOfWeekDateFormatter.string(from: earlierDate)
            }
            
        } else {
            switch maxGranularity {
            case .day:
                return Date.hourMinuteTimeFormatter.string(from: earlierDate)
                
            case .second:
                if hourDelta > 0 {
                    return Date.hoursText(for: interval)
                    
                } else if minuteDelta > 0 {
                    return Date.minutesText(for: interval)
                    
                } else {
                    return Date.secondsText(for: interval)
                }
            }
        }
    }
    
    // MARK: Public Enums
    
    public enum RelativeTimeGranularity {
        case day
        case second
    }
}
