//
//  ArrayExtensions.swift
//  Present
//
//  Created by Dan Federman on 1/20/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation


public extension Array {
    
    // DEPRECATED: Use the built-in dictionary init
    /// Returns a dictionary containing the results of mapping the sequence to key:value pairs using the provided closure.
    ///
    /// In this example, `mapToDictionary` is used to convert an array of strings
    /// to a dictionary of letters mapped to an array of words that
    /// have that letter as their first letter.
    ///
    ///      let cast = ["Vivien", "Marlon", "Kim", "Karl"]
    ///      cast.mapToDictionary { (element, dictionary) -> [String : [String]] in
    ///          let firstLetterCharacter = element.substring(to: element.index(element.startIndex, offsetBy: 1))
    ///          if var namesWithFirstLetter = dictionary[firstLetterCharacter] {
    ///              namesWithFirstLetter.append(element)
    ///              return [firstLetterCharacter : namesWithFirstLetter]
    ///          } else {
    ///              return [firstLetterCharacter : [element]]
    ///          }
    ///      }
    ///
    /// - parameter transform: A mapping closure that returns a dictionary for a given element and dictionary that will be entered into the resulting dictionary.
    /// - parameter element: The element to be mapped into the dictionary.
    /// - parameter dictionary: The cumulative result of prior transforms.
    /// - returns: A dictionary containing the transformed key:value pairs.
    public func mapToDictionary<K,V>(_ transform: (_ element: Element, _ dictionary: [K : V]) throws -> [K : V]) rethrows -> [K : V] {
        var dictionary = [K : V]()
        for element in self {
            let entry = try transform(element, dictionary)
            for (key, value) in entry {
                dictionary[key] = value
            }
        }
        
        return dictionary
    }
    
    // DEPRECATED: Use the built-in dictionary init
    /// Returns a dictionary containing the results of mapping the sequence to key:value pairs using the provided closure.
    ///
    /// In this example, `mapToDictionary` is used to convert an array of strings
    /// to a dictionary of letters mapped to an array of words that
    /// have that letter as their first letter.
    ///
    ///      let cast = ["Vivien", "Marlon", "Kim", "Karl"]
    ///      cast.mapToDictionary { [$0.substring(to: $0.index($0.startIndex, offsetBy: 1)) : [$0]] }
    ///
    /// - parameter transform: A mapping closure that returns a dictionary for a given element that will be entered into the resulting dictionary.
    /// - parameter element: The element to be mapped into the dictionary.
    /// - returns: A dictionary containing the transformed key:value pairs.
    public func mapToDictionary<K,V>(_ transform: (_ element: Element) throws -> [K : [V]]) rethrows -> [K : [V]] {
        var dictionary = [K : [V]]()
        for element in self {
            let entry = try transform(element)
            for (key, value) in entry {
                if var values = dictionary[key] {
                    values.append(contentsOf: value)
                    dictionary[key] = values
                } else {
                    dictionary[key] = value
                }
            }
        }
        
        return dictionary
    }
    
    /// Returns a dictionary containing the results of mapping the sequence to key:value pairs using the provided closure.
    ///
    /// In this example, `mapToDictionary` is used to convert an array of strings
    /// to a dictionary of letters mapped to the word that has that
    /// letter as its first letter.
    ///
    ///      let cast = ["Vivien", "Marlon", "Kim"]
    ///      cast.mapToDictionary { [$0.substring(to: $0.index($0.startIndex, offsetBy: 1)) : $0] }
    ///
    /// - parameter transform: A mapping closure that returns a dictionary for a given element that will be entered into the resulting dictionary.
    /// - parameter element: The element to be mapped into the dictionary.
    /// - returns: A dictionary containing the transformed key:value pairs.
    public func mapToDictionary<K,V>(_ transform: (_ element: Element) throws -> [K : V]) rethrows -> [K : V] {
        return try mapToDictionary { element, _ in try transform(element) }
    }
}

extension Array {
    public var isNotEmpty: Bool { return !self.isEmpty }
}

extension Array {
    func appending(_ element: Element) -> Array {
        var ar = Array(self)
        ar.append(element)
        return ar
    }
}
