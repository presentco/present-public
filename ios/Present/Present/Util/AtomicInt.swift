//
//  AtomicInt.swift
//  Present
//
//  Created by Patrick Niemeyer on 7/2/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation

public class AtomicInt
{
    private var value = 0
    private let lock = DispatchSemaphore(value: 1)
    
    public init(_ value: Int) {
        self.value = value
    }

    public func get() -> Int {
        lock.wait()
        defer { lock.signal() }
        return value
    }
    
    public func set(_ newValue: Int) {
        lock.wait()
        defer { lock.signal() }
        value = newValue
    }
    
    public func incrementAndGet() -> Int {
        lock.wait()
        defer { lock.signal() }
        value += 1
        return value
    }
    
    public func decrementAndGet() -> Int {
        lock.wait()
        defer { lock.signal() }
        value -= 1
        return value
    }
}

public class Sequence {
    private var value = AtomicInt(0)
    
    public func next() -> Int {
        return value.incrementAndGet()
    }
    public func current() -> Int {
        return value.get()
    }
}
