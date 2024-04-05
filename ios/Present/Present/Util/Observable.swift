//
//  Observable.swift
//  Present
//
//  Created by Dan Federman on 1/9/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation

// TODO: We need to prevent adding duplicates to the observer list as well as handle de-registration
// TODO: and clearing dead observers from the queue.
/// An Observable holds a value, a set of observers, and a transformation block that applies the value.
public final class LegacyObservable<ValueType, ObserverType>
{
    // MARK: Private Properties
    
    // Internal synchronization
    private let workQueue = DispatchQueue(label: "Observable Work Queue")

    // The transformation to apply the value to the observer
    private let applyBlock: (ValueType, ObserverType) -> Void

    // The observer array
    private var observers = [Observer]()

    // The value, internally nil if not yet set. (The observable ValueType is non-nil).
    private var value: ValueType? {
        didSet {
            //logDebug("Observable: setting value for observable: \(ObserverType.self) to \(value)")
            
            // The value cannot return to nil (unset).
            guard let value = value else { return }
            
            observers.forEach { observerStore in
                guard let observer = observerStore.observer else {
                    //logDebug("Observable: for type \(ObserverType.self) listener defunct")
                    // TODO: We need to remove deallocated observers
                    return;
                }

                /*
                // TEST: REMOVE
                if ObserverType.self == JoinedGroupsUnreadCountObserver.self,
                    String.init(describing: observer).contains("ApplicationIconBadgeManager")
                {
                } else {
                    logDebug("Skipping observer \(observer) for \(ObserverType.self)")
                    logDebug("type = \(String.init(describing: observer.self))")
                    return;
                }*/
                
                observerStore.queue.async {
                    //logDebug("Observable: notifying: \(observer)")
                    self.applyBlock(value, observer)
                }
            }
        }
    }

    // MARK: Initialization
    
    /// Initializes an observable.
    /// - parameter observationBlock: A block that passes the supplied `value` into the supplied `observer`'s observation method.
    public required init(applyBlock: @escaping (ValueType, ObserverType) -> Void) {
        self.applyBlock = applyBlock
    }
    
    // MARK: Public Methods
    
    /// Sets the value to passed into registered observers.
    /// Observers registered after this value is set will receive this value after registering.
    /// The same value may be set more than once: The client should guard changes if required.
    public func set(value: ValueType) {
        workQueue.async {
            self.value = value
        }
    }
    
    /// Registers an observer. If a `value` has already been set the observer will receive an immediate callback.
    /// @return a deregistration block
    public func register(observer: ObserverType, on callbackQueue: DispatchQueue = .main) -> (()->Void)
    {
        let observerHolder = Observer(observer: observer, queue: callbackQueue)
        workQueue.async {
            // If we already have a value. Notify the observer.
            if let value = self.value {
                callbackQueue.async {
                    self.applyBlock(value, observer)
                }
            }
            // TODO: We need to prevent adding duplicates to the observer list.
            // TODO: It's not as easy as checking the === identity of the stored observers because there seems to be
            // TODO: no way to constrain our ObserverType to be a reference type:
            // TODO: https://bugs.swift.org/browse/SR-6039
            // Add to the observers list for future updates
            self.observers.append(observerHolder)
        }
        let deregistrationBlock : (()->Void) = { [weak self] in
            logDebug("de-registering listener: \(type(of: observer))")
            guard let sself = self else { return }
            sself.workQueue.async {
                sself.observers = sself.observers.filter { $0 !== observerHolder }
            }
        }
        return deregistrationBlock
    }

    // TODO: Why do we need this?
    /// Copies observers from the provided queue into the receiver's queue.
    public func copyObservers(from otherQueue: LegacyObservable<ValueType, ObserverType>) {
        workQueue.async {
            let otherObservers = otherQueue.workQueue.sync { otherQueue.observers }
            self.observers.append(contentsOf: otherObservers)
        }
    }

    // MARK: ObserverStore

    // Hold the observer and observer's callback queue
    private final class Observer
    {
        public let queue: DispatchQueue

        private let weakObserver: WeakWrapper<ObserverType>

        // MARK: Initialization

        public init(observer: ObserverType, queue: DispatchQueue) {
            weakObserver = WeakWrapper(value: observer)
            self.queue = queue
        }
        
        // MARK: Public Properties

        public var observer: ObserverType? {
            return weakObserver.value
        }
        
        // MARK: WeakWrapper

        // Since we want our Observable's ObserverType to be capable of being a protocol,
        // we can't enforce that the ObserverType is a class, so we can't enforce that the
        // observer is capable of being stored weakly. Use this Weakable class to work around
        // our inability to directly store our observer weakly. If our ObserverType instance is
        // an object we will store it weakly to prevent retain cycles.
        // For further reading regarding generics and weak storage, check out https://bugs.swift.org/browse/SR-55
        private final class WeakWrapper<ObserverType>
        {
            // MARK: Private Properties

            private weak var object: AnyObject?

            // MARK: Initialization

            public init(value: ObserverType) {
                object = value as AnyObject
            }
            
            // MARK: Public Properties

            public var value: ObserverType? {
                return object as? ObserverType
            }

        }
    }
}
