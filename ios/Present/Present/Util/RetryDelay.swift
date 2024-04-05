//
//  RetryDelay.swift
//  Present
//
//  Created by Patrick Niemeyer on 10/1/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation

// Retry delay with exponential backoff
public class RetryDelay
{
    private let initialDelayMillis: Int
    private let maxDelayMillis: Int
    private var _nextDelayMillis: Int = 1000 // value never used
    
    public func nextDelayMillis() -> Int {
        _nextDelayMillis = min(maxDelayMillis, _nextDelayMillis * 2)
        return _nextDelayMillis
    }
    
    public func reset() {
        _nextDelayMillis = initialDelayMillis
    }
    
    init(initialDelayMillis: Int, maxDelayMillis: Int) {
        self.initialDelayMillis = initialDelayMillis
        self.maxDelayMillis = maxDelayMillis
        reset()
    }
}
