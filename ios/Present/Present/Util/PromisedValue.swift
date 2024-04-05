//
//  PromisedValue.swift
//  Present
//
//  Created by Dan Federman on 6/27/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation

//
// TODO: Replace this with a shared, replayed, take(1) RxSwift PublishSubject.
// TODO: We'll need to make a wrapper that performs our fulfill() on the underlying subject.
//
/// Accept blocks to be executed and removed upon fulfillment of the value.
/// Blocks added after the value is present will be executed and removed immediately.
public final class PromisedValue<T>
{
    // Optional init block to be triggered by the first call to then()
    let initBlock: ((PromisedValue<T>)->Void)?
    
    // MARK: Initialization
    
    /// @param lazilyInitializedWith a block invoked to initialize the value lazily
    ///   on first invocation of the then() clause.
    public init(lazilyInitializedWith initBlock: ((PromisedValue<T>)->Void)? = nil) {
        self.initBlock = initBlock
    }

    // MARK: Public Methods
    
    /// Executes and then removes all queued blocks. Blocks queued after calling this method will be immediately executed until queue is resumed.
    public func fulfill(with value: T) {
        //logDebug("Fulfill promise with value: \(type(of:value))")
        workQueue.async {
            self.value = value
            self.flush_inWorkQueue()
        }
    }
    
    /// Clears the stored promised value, preventing blocks queued after calling this method from being executed until the promise is fulfilled again.
    public func resetValue() {
        workQueue.async {
            self.value = nil
        }
    }
    
    /// Queues the block to be executed on the provided queue with the promised value.
    public func then(on queue: DispatchQueue = .main, block: @escaping (T) -> Void) {
        workQueue.async {
            // If we have a value, dispatch it immediately
            if let value = self.value {
                queue.async {
                    block(value)
                }
            } else {
                // Queue up the blocks for later dispatch
                self.blocksToDispatch.append(DispatchBlock(queue: queue, block: block))
                
                // If we have an init block trigger it now
                if let initBlock = self.initBlock {
                    initBlock(self)
                }
            }
        }
    }
    
    // MARK: Private Structs
    
    private struct DispatchBlock {
        let queue: DispatchQueue
        let block: (T) -> Void
    }
    
    // MARK: Private Methods
    
    private func flush_inWorkQueue() {
        guard let value = value else {
            return
        }
        
        for blockToDispatch in blocksToDispatch {
            blockToDispatch.queue.async {
                blockToDispatch.block(value)
            }
        }
        blocksToDispatch.removeAll()
    }
    
    // MARK: Private Properties
    
    private let workQueue = DispatchQueue(label: "Promised Value Work Queue")
    private var blocksToDispatch = [DispatchBlock]()
    private var value: T?
    
}
