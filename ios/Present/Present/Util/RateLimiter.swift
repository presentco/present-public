//
//  RateLimiter.swift
//  Present
//
//  Created by Dan Federman on 3/23/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation


/// A queue used to store blocks that will be executed in parallel up to the maximum concurrent operation count.
public final class RateLimiter {
    
    // MARK: Initialization
    
    public init(maxConcurrentOperationCount: Int = 1) {
        if maxConcurrentOperationCount <= 0 {
            logError("A serialized queue must be created with at least 1 unit of work capacity, defaulting to 1.")
            self.remainingWorkCapacity = 1
            
        } else {
            self.remainingWorkCapacity = maxConcurrentOperationCount
        }
    }
    
    // MARK: Public Methods
    
    /// Adds the block to be executed on the provided dispatch queue on a later dequeue() call.
    /// - parameter queue: The queue to execute the block on.
    /// - parameter block: The task to be executed serially.
    /// - parameter blockCompletionHandler: The block's async completion handler.
    ///   The completion handler _must_ be called when the block has completed its work, otherwise a
    ///   starvation condition may ensue.
    public func add(on queue: DispatchQueue = .main, block: @escaping (_ blockCompletionHandler: @escaping () -> Void) -> Void) {
        synchronizationQueue.async {
            self.work.append(Work(queue: queue, block: block))
            
            self.scheduleNextWorkUnitIfPossible()
        }
    }
    
    // MARK: Private Properties
    
    /// Dispatch queue used to sychronize access to local attributes.
    private let synchronizationQueue = DispatchQueue(label: "RateLimiter Work Queue")
    
    /// Array of work units to be performed.
    private var work = [Work]()
    
    /// Amount of execution resources available.
    private var remainingWorkCapacity: Int
    
    // MARK: Private Methods
    
    private func scheduleNextWorkUnitIfPossible() {
        guard remainingWorkCapacity > 0, work.count > 0 else {
            return
        }
        
        remainingWorkCapacity -= 1
        
        let dequeuedBlock = work.removeFirst()
        dequeuedBlock.queue.async {
            dequeuedBlock.block({
                self.synchronizationQueue.async {
                    self.remainingWorkCapacity += 1
                    self.scheduleNextWorkUnitIfPossible()
                }
            })
        }
    }
    
    // MARK: Private Structs
    
    private struct Work {
        let queue: DispatchQueue
        let block: (@escaping () -> Void) -> Void
    }
    
}
