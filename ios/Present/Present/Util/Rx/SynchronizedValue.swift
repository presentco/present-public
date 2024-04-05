//
//  SynchronizedValue.swift
//  Present
//
//  Created by Patrick Niemeyer on 8/2/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation

/// Value holder with setter and getter synchronized by NSRecursiveLock
public class SynchronizedValue<E>
{
    private var _value: E?
    private var lock = NSRecursiveLock()
    
    // Synchronized copy of the current value, initially nil
    public var value: E? {
        get {
            lock.lock(); defer { lock.unlock() }
            return _value
        }
        set(newValue) {
            lock.lock(); defer { lock.unlock() }
            _value = newValue
        }
    }
    
    public init(with initialValue: E? = nil) {
        value = initialValue
    }
}

