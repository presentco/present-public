//
//  FloatingPointArithmeticCapable.swift
//  Present
//
//  Created by Dan Federman on 1/1/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import CoreGraphics
import Foundation


// MARK: – FloatingPointArithmeticCapable


public protocol FloatingPointArithmeticCapable: Comparable {
    static func +(lhs: Self, rhs: Self) -> Self
    static func -(lhs: Self, rhs: Self) -> Self
    static func *(lhs: Self, rhs: Self) -> Self
    static func /(lhs: Self, rhs: Self) -> Self
    //static func %(lhs: Self, rhs: Self) -> Self
    
    init(_ : Int)
}


// MARK: – FloatingPointArithmeticCapable Extension


public extension FloatingPointArithmeticCapable {
    
    // MARK: Public Static Methods
    
    public static var normalizedMinimumValue: Self {
        return Self(0)
    }
    
    public static var normalizedMaximumValue: Self {
        return Self(1)
    }
    
    // MARK: Public Methods
    
    public func clamped(to range: ClosedRange<Self>) -> Self {
        return min(max(range.lowerBound, self), range.upperBound)
    }
    
    public func clamped(from lowerBound: Self, to upperBound: Self) -> Self {
        return clamped(to: ClosedRange(uncheckedBounds: (lower: lowerBound, upper: upperBound)))
    }
    
}


// MARK: – FloatingPointArithmeticCapable Conforming Extensions


extension CGFloat: FloatingPointArithmeticCapable {}
extension Float: FloatingPointArithmeticCapable {}
extension Double: FloatingPointArithmeticCapable {}
