//
//  ActualizableQueue.swift
//  Present
//
//  Created by Dan Federman on 1/20/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation


public final class OptimisticTransform<T>: Equatable {
    
    // MARK: Equatable
    
    public static func ==(lhs: OptimisticTransform, rhs: OptimisticTransform) -> Bool {
        return lhs.uuid == rhs.uuid
    }
    
    // MARK: Initialization
    
    /// Creates an optimistic transform used to optimistically apply a user-generated modification to a model object prior to receiving a server response.
    /// - parameter identifier: A identifier used to determine which value should be transformed. Returning nil from the transform will cause the item with that identifier to be removed.
    /// - parameter transform: A block which transforms a value.
    public init(identifier: String, transform: @escaping (T) -> T?) {
        self.transform = .modify(identifier: identifier, transform)
    }
    
    /// Creates an optimistic transform used to optimistically insert a user-generated model object prior to receiving a server response.
    /// - parameter value: An object to be inserted into the model.
    /// - parameter identifier: A identifier for the object to be inserted.
    public init(insert value: T, withIdentifier identifier: String) {
        self.transform = .insert(identifier: identifier, value)
    }
    
    /// Creates an optimistic transform used to optimistically remove a model object prior to receiving a server response.
    /// - parameter identifier: A identifier for the object to be removed.
    public init(removeObjectWithIdentifier identifier: String) {
        self.transform = .remove(identifier: identifier)
    }
    
    // MARK: Public Properties
    
    /// True once the app has confirmation the server is aware of the actualizable change.
    public var isActualized: Bool = false
    
    // MARK: Public Methods
    
    public func applyTransform(toIdentifierToValueMap identifierToValueMap: [String : T]) -> [String : T] {
        var identifierToTransformedValueMap = identifierToValueMap
        switch transform {
        case let .modify(identifier, modification):
            if let value = identifierToTransformedValueMap[identifier] {
                identifierToTransformedValueMap[identifier] = modification(value)
            }
            
        case let .insert(identifier, value):
            identifierToTransformedValueMap[identifier] = value
            
        case let .remove(identifier):
            identifierToTransformedValueMap[identifier] = nil
        }
        
        return identifierToTransformedValueMap
    }
    
    // MARK: Private Properties
    
    private let transform: Transform<T>
    private let uuid = PresentUUID()
    
}


public final class Synchronization: Equatable {
    
    // MARK: Equatable
    
    public static func ==(lhs: Synchronization, rhs: Synchronization) -> Bool {
        return lhs.uuid == rhs.uuid
    }

    // MARK: Public Properties
    
    /// True once the app has confirmation the server is aware of the actualizable change.
    public var isActualized: Bool = false
    
    // MARK: Private Properties
    
    private let uuid = PresentUUID()
}


public final class ActualizableQueue<T> {
    
    // MARK: Public Methods
    
    /// Appends a transform to the queue.
    public func append(transform: OptimisticTransform<T>) {
        actualizableItems.append(.transform(transform))
    }
    
    /// Appends a synchronization marker to the queue.
    public func append(synchronization: Synchronization) {
        actualizableItems.append(.synchronization(synchronization))
    }
    
    /// Call when a transform is no longer optimistic.
    public func actualize(transform: OptimisticTransform<T>) {
        transform.isActualized = true
    }
    
    /// Call when a synchronization has been completed.
    public func actualize(synchronization: Synchronization) {
        synchronization.isActualized = true
        
        pruneActualizedTransforms(priorTo: synchronization)
    }
    
    /// Apply optimistic transforms to values in the provided map and return the a map with the transformed value.
    public func applyTransforms(toIdentifierToValueMap identifierToValueMap: [String : T]) -> [String : T] {
        var identifierToTransformedValueMap = identifierToValueMap
        for actualizableItem in actualizableItems {
            switch actualizableItem {
            case let .transform(optimisticTransform):
                identifierToTransformedValueMap = optimisticTransform.applyTransform(toIdentifierToValueMap: identifierToTransformedValueMap)
                
            case .synchronization:
                // No transform to apply here.
                break
                
            }
        }
        return identifierToTransformedValueMap
    }
    
    #if UNIT_TEST
    
    // MARK: Testing Methods
    
    /// Returns true if the private queue has the same transforms in the same order as the passed in array.
    public func queueHas(orderedTransforms: [OptimisticTransform<T>]) -> Bool {
        let myOrderedTransforms: [OptimisticTransform<T>] = actualizableItems.flatMap {
            if case let .transform(transform) = $0 {
                return transform
            } else {
                return nil
            }
        }
        
        return myOrderedTransforms == orderedTransforms
    }
    
    #endif
    
    // MARK: Private Properties
    
    private var actualizableItems = [ActualizableItem<T>]()
    
    // MARK: Private Methods
    
    /// Prunes the first N sequential actualized transforms.
    private func pruneActualizedTransforms(priorTo actualizedSynchronization: Synchronization) {
        var indexOfLastActualizedTransformPriorToMarker: Int? = nil
        findIndexToRemoveUntil: for (index, actualizableItem) in actualizableItems.enumerated() {
            guard actualizableItem.isActualized else {
                break
            }
            
            switch actualizableItem {
            case let .synchronization(synchronization):
                guard synchronization != actualizedSynchronization else {
                    break findIndexToRemoveUntil
                }
                
            case .transform:
                indexOfLastActualizedTransformPriorToMarker = index
            }
        }
        
        if let indexOfLastActualizedTransform = indexOfLastActualizedTransformPriorToMarker {
            actualizableItems.removeSubrange(0...indexOfLastActualizedTransform)
        }
    }
    
}


// MARK: – Private Enums


private enum ActualizableItem<T> {
    
    case transform(OptimisticTransform<T>)
    case synchronization(Synchronization)
    
    public var isActualized: Bool {
        switch self {
        case let .transform(optimisticTransform):
            return optimisticTransform.isActualized
            
        case let .synchronization(synchronization):
            return synchronization.isActualized
        }
    }
}


private enum Transform<T> {
    case modify(identifier: String, (T) -> T?)
    case insert(identifier: String, T)
    case remove(identifier: String)
}
