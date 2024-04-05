//
//  DispatchQueueExtensions.swift
//  Present
//
//  Created by Patrick Niemeyer on 10/1/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation

/// Execute the block asynchronously on the main queue
public func main(execute work: @escaping @convention(block) () -> Swift.Void) {
    DispatchQueue.main.async(execute: work)
}
/// Execute the block asynchronously on the main queue after the specified time.
public func mainAfter(_ seconds: Float, execute work: @escaping @convention(block) () -> Swift.Void) {
    mainAfter(milliseconds: Int(seconds*1000), execute: work)
}
/// Execute the block asynchronously on the main queue after the specified time.
public func mainAfter(seconds: Int, execute work: @escaping @convention(block) () -> Swift.Void) {
    DispatchQueue.main.asyncAfter(seconds: seconds, execute: work)
}
/// Execute the block asynchronously on the main queue after the specified time.
public func mainAfter(milliseconds: Int, execute work: @escaping @convention(block) () -> Swift.Void) {
    DispatchQueue.main.asyncAfter(milliseconds: milliseconds, execute: work)
}

extension DispatchQueue {
    
    public func asyncAfter(seconds: Int, execute work: @escaping @convention(block) () -> Swift.Void) {
        self.asyncAfter(deadline: .now() + DispatchTimeInterval.seconds(seconds), execute: work)
    }
    
    public func asyncAfter(milliseconds: Int, execute work: @escaping @convention(block) () -> Swift.Void) {
        self.asyncAfter(deadline: .now() + DispatchTimeInterval.milliseconds(milliseconds), execute: work)
    }

    public func asyncAfter(_ retryDelay: RetryDelay, execute work: @escaping @convention(block) () -> Swift.Void) {
        self.asyncAfter(milliseconds: retryDelay.nextDelayMillis(), execute: work)
    }
}
