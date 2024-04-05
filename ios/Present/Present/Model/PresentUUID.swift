//
//  PresentUUID.swift
//  Present
//
//  Created by Dan Federman on 7/26/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation


public struct PresentUUID: CustomStringConvertible, Hashable, Equatable {
    
    // MARK: Equatable
    
    public static func ==(lhs: PresentUUID, rhs: PresentUUID) -> Bool {
        return lhs.uuidString == rhs.uuidString
    }
    
    // MARK: Initialization
    
    public init() {
        // Create a UUID with a string that is to spec. UUID spec requires lowercased characters.
        uuidString = UUID().uuidString.lowercased()
    }

    public init(_ uuidString: String) {
        // Trust the UUID string that is passed in.
        self.uuidString = uuidString
    }

    public init(uuidString: String) {
        // Trust the UUID string that is passed in.
        self.uuidString = uuidString
    }
    
    public init(uuidString: NSString) {
        // Trust the UUID string that is passed in.
        self.uuidString = String(uuidString)
    }
    
    // MARK: Public Properties
    
    public let uuidString: String
    
    // MARK: CustomStringConvertible
    
    public var description: String {
        return uuidString
    }
    
    // MARK: Hashable
    
    public var hashValue: Int {
        return uuidString.hashValue
    }
    
}
