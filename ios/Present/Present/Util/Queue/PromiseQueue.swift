//
//  PromiseQueue.swift
//  Present
//
//  Created by Pat Niemeyer
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation

/// A queue that holds blocks in a suspended state until its fulfill() mehod is called.
public final class PromiseQueue : SuspendableQueue
{
    public func fulfill() {
        resume()
    }
    
    public var isFullfilled: Bool {
        return !isSuspended
    }
}

/// A suspendable queue accepting blocks with an optionally specified callback queue.
/// The queue is initialized in a suspended state.
// Note: This could be done directly on DispatchQueue but it's nice to have the initializer perform
// Note: the suspend and to remove the suspend / resume balancing requirement.
// Note: See also the DispatchQueue initiallyInactive attribute.
public class SuspendableQueue
{
    // MARK: Private Properties
    
    deinit {
        if isSuspended {
            fatalError("Crashing iOS bug: We can't deinit a suspended queue. Arrange to resume before deallocating.")
        }
    }

    private let workQueue = DispatchQueue(label: "Work Queue")
    var isSuspended: Bool = true

    // MARK: Public Methods

    public init() {
        workQueue.suspend()
    }
    
    /// Queues the block to be executed on the specified queue when this queue is resumed.
    /// By default blocks are executed on the main queue.
    public func enqueue(for queue: DispatchQueue = .main, block: @escaping () -> Void) {
        workQueue.async {
            queue.async(execute: block)
        }
    }

    public func suspend() {
        guard !isSuspended else { return }
        workQueue.suspend()
        isSuspended = true
    }

    public func resume() {
        guard isSuspended else { return }
        workQueue.resume()
        isSuspended = false
    }

}
