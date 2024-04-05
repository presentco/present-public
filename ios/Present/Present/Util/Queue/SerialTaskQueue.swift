//
//  SerialTaskQueue.swift
//  Present
//
//  Created by Patrick Niemeyer on 8/2/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation

/// A list of blocks to be executed serially on the calling thread when the run() method is invoked.
/// Once executed the blocks are removed and any blocks subsequently submitted will be executed
/// immediately on the calling thread.
public class SerialTaskQueue
{
    private var queue: ([()->Void])? = []
    
    public func enqueue(block: @escaping () -> Void) {
        if self.queue == nil {
            //log("serial task queue: \(ObjectIdentifier(self)), queue=\(String(describing: self.queue)) running block directly, thread: \(Thread.current)")
            block()
        } else {
            //log("serial task queue: \(ObjectIdentifier(self)), queue=\(String(describing: self.queue)) queueing block, thread: \(Thread.current)")
            self.queue?.append(block)
        }
    }
    
    public func run() {
        //log("serial task queue: \(ObjectIdentifier(self)) running, queue=\(String(describing: queue)), thread: \(Thread.current)")

        // Enqueued tasks may indirectly cause the queue array to be mutated, so we must iterate carefully
        while true {
            if (self.queue ?? []).isNotEmpty, let task = self.queue?.removeFirst() {
                task()
            } else {
                break
            }
        }
        self.queue = nil
    }
}
