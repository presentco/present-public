//
//  NonEmpty.swift
//  Present
//
//  Created by Dan Federman on 1/20/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation


public protocol OrderedContentContainable {
    
    associatedtype ContentType
    
    var isEmpty: Bool { get }
    var first: ContentType? { get }
    var last: ContentType? { get }
}


// MARK: - NonEmpty<T>


public struct NonEmpty<T> where T: OrderedContentContainable {
    
    // MARK: Initialization
    
    public init?(value: T) {
        guard !value.isEmpty else {
            return nil
        }
        
        self.value = value
    }
    
    // MARK: Public Properties
    
    public let value: T
    
    public var first: T.ContentType {
        // We know we have a first value because value is not empty.
        return value.first!
    }
    
    public var last: T.ContentType {
        // We know we have a last value because value is not empty.
        return value.last!
    }
    
}


// MARK: - Extensions


extension Array: OrderedContentContainable {
    
    public typealias ContentType = Element
    
}
