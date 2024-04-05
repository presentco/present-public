//
//  PhoneNumber.swift
//  Present
//
//  Created by Dan Federman on 1/2/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation


public struct PhoneNumber: Equatable, Hashable
{
    // MARK: Private Static Properties
    
    private static let countryCodePrefix = "+"
    private static let northAmericanCode = "1"
    
    private static let disallowedNormalizedPhoneNumberCharacterSet: CharacterSet = { CharacterSet(charactersIn: "0123456789").inverted }()
    
    /// The format for North American phone numbers. Based on https://en.wikipedia.org/wiki/North_American_Numbering_Plan#Modern_plan
    private static let phoneNumberFormat = NSPredicate(format: "SELF MATCHES %@",
                                                       "1?[2-9]\\d{2}[2-9]\\d{2}\\d{4}")
    /// The format for North American phone numbers used to interact with the server.
    private static let normalizedPhoneNumberFormat = NSPredicate(format: "SELF MATCHES %@",
                                                                 "\\+1[2-9]\\d{2}[2-9]\\d{2}\\d{4}")
    
    // North American reserved phone numbers are in the range 555-0100 to 555-0199
    private static let fakePhoneNumberFormat = NSPredicate(format: "SELF MATCHES %@", "(\\+1)?\\d{3}555\\d{4}")
    
    // MARK: Private State
    
    /// A normalized phone number has no spaces, begins with a '+' followed by the country code, area code, and remaining digits.
    /// Example: North American phone number 415-123-4567 is normalized to '+14151234567'.
    public let normalizedString: String
    
    /// The server no longer expects the leading "+"
    public var serverString: String {
        return String(normalizedString.dropFirst())
    }
    
    /// For display to the customer.
    /// A display string for a phone number. Format is: '415-123-4567'.
    public let displayString: String
    
    // Optional label associated with this phone, e.g.: Home, Work, Other...
    public let label: String?
    
    // MARK: Initialization
    
    /// Initializes a PhoneNumber with a string representing the phone number.
    /// - parameter string: A string representing a phone number. Can have any formatting.
    public init?(string: String, label: String? = nil) {
        let strippedString = string.components(separatedBy: PhoneNumber.disallowedNormalizedPhoneNumberCharacterSet).joined()
        guard PhoneNumber.phoneNumberFormat.evaluate(with: strippedString) else {
            return nil
        }
        
        let normalizedString: String
        if strippedString.first == Character(PhoneNumber.northAmericanCode) {
            normalizedString = PhoneNumber.countryCodePrefix + strippedString
        } else {
            normalizedString = "\(PhoneNumber.countryCodePrefix)\(PhoneNumber.northAmericanCode)" + strippedString
        }
        
        self.normalizedString = normalizedString
        self.displayString = PhoneNumber.displayString(forNormalizedString: normalizedString)
        self.label = label
    }
    
    /// Initializes a PhoneNumber with a normalized string representing the phone number.
    /// - parameter normalizedString: A phone number string with no formatting, leading + and country code. Example: '+14152134567'.
    public init?(normalizedString: String, label: String? = nil) {
        guard PhoneNumber.normalizedPhoneNumberFormat.evaluate(with: normalizedString) else {
            return nil
        }
        
        self.normalizedString = normalizedString
        self.displayString = PhoneNumber.displayString(forNormalizedString: normalizedString)
        self.label = label
    }
    
    // MARK: Public Static Methods
    
/*
    public static func transformFor(proposedReplacement string: String, forCharactersIn range: NSRange, in textInputView: UITextInput) -> TextFieldTransform
    {
        guard let rangeOfText = textInputView.textRange(from: textInputView.beginningOfDocument, to: textInputView.endOfDocument),
            let text = textInputView.text(in: rangeOfText) else {
            return .proposedReplacementDisallowed
        }
        
        let modifiedText = (text as NSString).replacingCharacters(in: range, with: string) as String
        
        guard let normalizedOriginalString = normalizedString(forPartialPhoneNumber: text),
            let normalizedModifiedString = normalizedString(forPartialPhoneNumber: modifiedText),
            !normalizedOriginalString.isEmpty || !normalizedModifiedString.isEmpty else {
                return .proposedReplacementDisallowed
        }
        
        /// Formats a potentially partial phone number string.
        /// - parameter normalizedString: A potentially partial phone number with no formatting, and optional leading country code. Examples: '1415', '41521', '14152134567', '4152134567'.
        /// - returns: A partial (or complete) phone number formatted for display. The optional +1 is retained if the input has it. Example outputs: '+1-415', '415-21', '+1-415-213-4567', '415-213-4567'.
        
        func partialDisplayString(forNormalizedString normalizedString: String) -> String {
            // Since we're an internal method there is no need to sanity check that we're being given a normalized string.
            var formattedString = ""
            let indexOfFirstNonCountryCodeCharacter: String.Index
            let northAmericanCountryCode = "\(countryCodePrefix)\(northAmericanCode)"
            if normalizedString.hasPrefix(northAmericanCountryCode) {
                // Hardcode the North American country code.
                formattedString += "\(northAmericanCountryCode)-"
                indexOfFirstNonCountryCodeCharacter = normalizedString.index(normalizedString.startIndex, offsetBy: northAmericanCountryCode.count)
            } else {
                indexOfFirstNonCountryCodeCharacter = normalizedString.startIndex
            }
            
            guard normalizedString.endIndex > indexOfFirstNonCountryCodeCharacter else {
                return formattedString
            }
            
            let nonCountryCodeCharacters = normalizedString[Range(indexOfFirstNonCountryCodeCharacter..<normalizedString.endIndex)]
            for (characterIndex, character) in nonCountryCodeCharacters.enumerated() {
                switch characterIndex {
                case 3, 6:
                    formattedString += "-\(character)"
                default:
                    formattedString += "\(character)"
                }
            }
            
            return formattedString
        }
        
        let formattedModifiedString = partialDisplayString(forNormalizedString: normalizedModifiedString)
        guard formattedModifiedString != modifiedText else {
            return .proposedReplacementAllowed
        }
        
        let selectionStartIndexOffset = range.location + range.length
        let charactersRemovedByNormalizationPriorToSelectionStartInOriginalString = text.count(ofCharactersIn: disallowedNormalizedPhoneNumberCharacterSet, in: NSRange(location: 0, length: selectionStartIndexOffset)) - (normalizedOriginalString.hasPrefix(countryCodePrefix) ? 1 : 0)
        let selectionStartIndexInOriginalNormalizedString = selectionStartIndexOffset - charactersRemovedByNormalizationPriorToSelectionStartInOriginalString
        let selectionStartIndexInModifiedNormalizedString = selectionStartIndexInOriginalNormalizedString + normalizedModifiedString.count - normalizedOriginalString.count
        
        let normalizedModifiedStringPriorToSelectionRange = normalizedModifiedString[Range(normalizedModifiedString.startIndex..<normalizedModifiedString.index(normalizedModifiedString.startIndex, offsetBy: selectionStartIndexInModifiedNormalizedString))]
        let formattedModifiedStringPriorToSelectionRange = partialDisplayString(forNormalizedString: String(normalizedModifiedStringPriorToSelectionRange))
        
        print("\(formattedModifiedStringPriorToSelectionRange) - \(selectionStartIndexOffset)")
        
        return .reformat(text: formattedModifiedString, selectedStartOffset: formattedModifiedStringPriorToSelectionRange.count - selectionStartIndexOffset)
    }
*/
    
    // MARK: Private Static Methods
    
    private static func displayString(forNormalizedString normalizedString: String) -> String {
        var displayString = normalizedString.replacingOccurrences(of: "\(countryCodePrefix)\(northAmericanCode)", with: "")
        
        let rangeOfAreaCode = Range(uncheckedBounds: (lower: displayString.startIndex,
                                                      upper: displayString.index(displayString.startIndex, offsetBy: 3)))
        let rangeOf3DigitsPostAreaCodeCode = Range(uncheckedBounds: (lower: rangeOfAreaCode.upperBound,
                                                                     upper: displayString.index(rangeOfAreaCode.upperBound, offsetBy: 3)))
        
        displayString.replaceSubrange(rangeOf3DigitsPostAreaCodeCode, with: "-" + displayString.substring(with: rangeOf3DigitsPostAreaCodeCode) + "-")
        
        return displayString
    }
    
    /// - returns: A partial (or complete) normalized. The optional +1 is retained if the input has it. Example outputs: '+141521', '41521', '+1-415-213-4567', '415-213-4567'.
    private static func normalizedString(forPartialPhoneNumber string: String) -> String? {
        let strippedString = string.removingCharacters(in: PhoneNumber.disallowedNormalizedPhoneNumberCharacterSet)
        
        guard strippedString.count <= (strippedString.hasPrefix(PhoneNumber.northAmericanCode) ? 11 : 10) else {
            // Our stripped string is too long.
            return nil
        }
        
        let normalizedString: String
        if strippedString.first == Character(PhoneNumber.northAmericanCode) {
            normalizedString = PhoneNumber.countryCodePrefix + strippedString
        } else {
            normalizedString = strippedString
        }
        
        return normalizedString
    }
    
    
    // MARK: Public Methods
    
    /// True if a phone number is a reserved for testing use only.
    public func isFake() -> Bool {
        return PhoneNumber.fakePhoneNumberFormat.evaluate(with: normalizedString)
    }
    
    public func isTollFree() -> Bool {
       let result =
            ["+1800", "+1888", "+1877", "+1866", "+1855", "+1844", "+1833"]
            .contains(normalizedString.prefix(5))
        //log("number: \(normalizedString), isTollFree: \(result)")
        return result
    }
    
    public var hashValue: Int {
        return normalizedString.hashValue
    }
}
