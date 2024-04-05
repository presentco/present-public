//
//  FullName.swift
//  Present
//
//  Created by Dan Federman on 4/11/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import PresentProtos

public class FullName
{
    // MARK: Private Static Properties
    
    private static let persistenceNameSeparator = "\t"
    
    // MARK: Public Properties
    
    public let givenName: String
    public let familyName: String
    
    // A supplied (fully rendered) full name, e.g. from the Contacts API
    private let _fullName: String?
    
    public var fullName: String
    {
        // Prefer the provided one if it exists
        if let name = _fullName {
            return name
        }
        
        // Compose one or fall back to components if needed
        if !givenName.isEmpty && !familyName.isEmpty {
            return "\(givenName) \(familyName)"
            
        } else if !givenName.isEmpty {
            return givenName
            
        } else {
            return familyName
        }
    }
    
    public var asData: Data {
        return [givenName, familyName].joined(separator: FullName.persistenceNameSeparator).data(using: .utf8)!
    }
    
    public var asUserNameProto: Present.UserName {
        return try! Present.UserName.getBuilder()
            .setFirst(givenName)
            .setLast(familyName)
            .build()
    }
    // MARK: Equatable
    
    public static func ==(lhs: FullName, rhs: FullName) -> Bool {
        return lhs.givenName == rhs.givenName
            && lhs.familyName == rhs.familyName
    }
    
    // MARK: Initialization
    
    public convenience init?(fullName: String) {
        guard !fullName.isEmpty else {
            return nil
        }
        
        let names = fullName.trimmingWhitespace().components(separatedBy: .whitespaces)
        let firstName = names[0]
        let lastName = names.dropFirst().joined(separator: " ")
        
        self.init(givenName: firstName, familyName: lastName)
    }
    
    /// fullName may be provided (e.g. a composite rendered by the Contacts API)
    /// If fullName is not provided we attempt to compose it ourselves from the parts.
    public init(givenName: String, familyName: String, fullName: String? = nil) {
        self.givenName = givenName
        self.familyName = familyName
        self._fullName = fullName
    }
    
    public init(userNameProto: Present.UserName) {
        givenName = userNameProto.first
        familyName = userNameProto.last
        _fullName = nil
    }
    
    public init?(data: Data) {
        guard let stringFromData = String(data: data, encoding: .utf8) else {
            return nil
        }
        
        let names = stringFromData.components(separatedBy: FullName.persistenceNameSeparator)
        guard names.count == 2 else {
            return nil
        }
        
        givenName = names[0]
        familyName = names[1]
        _fullName = nil
    }
    
    public var isEmpty: Bool {
        return givenName.trimmingWhitespace().isEmpty && familyName.trimmingWhitespace().isEmpty
    }
}

public class NoName: FullName
{
    public convenience init() {
        self.init(givenName: "", familyName: "")
    }
}

extension FullName : Comparable
{
    /// Returns a Boolean value indicating whether the value of the first
    /// argument is less than that of the second argument.
    ///
    /// This function is the only requirement of the `Comparable` protocol. The
    /// remainder of the relational operator functions are implemented by the
    /// standard library for any type that conforms to `Comparable`.
    ///
    /// - Parameters:
    ///   - lhs: A value to compare.
    ///   - rhs: Another value to compare.
    public static func <(lhs: FullName, rhs: FullName) -> Bool {
        return sortLessThanFullname(lhs:lhs, rhs:rhs)
    }
    
    private static func sortLessThanFullname(lhs: FullName, rhs: FullName) -> Bool {
        return lhs.fullName.lowercased() < rhs.fullName.lowercased()
    }

    // Do a "less than" sort by family name and then first name as a secondary sort
    private static func sortLessThanComponents(lhs: FullName, rhs: FullName) -> Bool {
        if lhs.familyName.lowercased() != rhs.familyName.lowercased() {
            return lhs.familyName.lowercased() < rhs.familyName.lowercased()
        } else {
            return lhs.givenName.lowercased() < rhs.givenName.lowercased()
        }
    }
}
