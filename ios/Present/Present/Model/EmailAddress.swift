//
//  EmailAddress.swift
//  Present
//
//  Created by Dan Federman on 1/2/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation


public struct EmailAddress {
    
    // MARK: Private Static Properties
    
    /// The regular expression used to define a valid email address. Expression taken from http://www.cocoawithlove.com/2009/06/verifying-that-string-is-email-address.html
    private let emailAddressPredicate = NSPredicate(format: "SELF MATCHES %@",
                                                    "(?:[a-z0-9!#$%\\&'*+/=?\\^_`{|}~-]+(?:\\.[a-z0-9!#$%\\&'*+/=?\\^_`"
                                                        + "{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x"
                                                        + "5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:"
                                                        + "(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*"
                                                        + "[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}"
                                                        + "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:["
                                                        + "\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\["
                                                        + "\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])")
    
    // MARK: Initialization
    
    public init?(normalizedString: String) {
        guard emailAddressPredicate.evaluate(with: normalizedString) else {
            return nil
        }
        
        self.normalizedString = normalizedString
    }
    
    // MARK: Alias
    
    public let normalizedString: String
    
}
