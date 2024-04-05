//
//  Normalized.swift
//  Present
//
//  Created by Dan Federman on 12/22/16.
//  Copyright © 2016 Present Company. All rights reserved.
//

import CoreGraphics
import Foundation


public struct Normalized<T>: Equatable where T: FloatingPointArithmeticCapable {
    
    // MARK: – Equatable
    
    public static func ==(lhs: Normalized<T>, rhs: Normalized<T>) -> Bool {
        return lhs.value == rhs.value
            && lhs.rawRange == rhs.rawRange
            && lhs.rawValue == rhs.rawValue
    }
    
    // MARK: – Public Properties
    
    /// The value normalized from normalizedMinimumValue...normalizedMaximumValue.
    public let value: T
    
    public var inverted: Normalized<T> {
        return Normalized(value: rawUpperBound - rawValue + rawLowerBound, over: rawLowerBound...rawUpperBound)
    }
    
    /// True if the value equals the normalizedMinimumValue.
    public var isMinValue: Bool {
        return value == T.normalizedMinimumValue
    }
    
    /// True if the value equals the normalizedMaximumValue.
    public var isMaxValue: Bool {
        return value == T.normalizedMaximumValue
    }
    
    /// The original range.
    public let rawRange: ClosedRange<T>
    /// The original value.
    public let rawValue: T
    
    /// The lower bound of the raw value.
    public var rawLowerBound: T {
        return rawRange.lowerBound
    }
    /// The upper bound of the raw value.
    public var rawUpperBound: T {
        return rawRange.upperBound
    }
    
    // MARK: - Initialization
    
    public init(value: T, over range: ClosedRange<T> = ClosedRange(uncheckedBounds: (lower: T.normalizedMinimumValue, upper: T.normalizedMaximumValue)), clamped: Bool = false) {
        func normalized(unclampedValue value: T, over range: ClosedRange<T>) -> T {
            return (value - range.lowerBound) / (range.upperBound - range.lowerBound)
        }
        
        assert(range.lowerBound <= range.upperBound)
        
        self.value = normalized(unclampedValue: clamped ? value.clamped(to: range) : value, over: range)
        rawRange = range
        rawValue = value
    }
    
    public init(value: T, from lowerBound: T, to upperBound: T) {
        self = Normalized(value: value, over: ClosedRange(uncheckedBounds: (lower: lowerBound, upper: upperBound)))
    }
    
    public init(normalizedValue: T, over range: ClosedRange<T> = ClosedRange(uncheckedBounds: (lower: T.normalizedMinimumValue, upper: T.normalizedMaximumValue))) {
        assert(range.lowerBound <= range.upperBound)
        
        value = normalizedValue
        rawRange = range
        rawValue = range.lowerBound + normalizedValue * (range.upperBound - range.lowerBound)
    }
    
    public init(normalizedValue: T, from lowerBound: T, to upperBound: T) {
        self = Normalized(normalizedValue: normalizedValue, over: ClosedRange(uncheckedBounds: (lower: lowerBound, upper: upperBound)))
    }
}
