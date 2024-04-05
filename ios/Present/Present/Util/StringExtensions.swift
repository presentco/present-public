import Foundation

public extension String
{
    /// Create an NSAttributedString from an image icon using an attachment
    // TODO: xoffset doesn't seem to work
    static func attributedStringFromImage(
            image: UIImage,
            xoffset: CGFloat = 0.0, yoffset: CGFloat = 0.0,
            width widthIn: CGFloat? = nil, height heightIn: CGFloat? = nil
    ) -> NSAttributedString {
        let imageAttachment =  NSTextAttachment()
        imageAttachment.image = image
        let width = widthIn ?? imageAttachment.image!.size.width
        let height = heightIn ?? imageAttachment.image!.size.height
        imageAttachment.bounds = CGRect(x: xoffset, y: yoffset, width: width, height: height)
        return NSAttributedString(attachment: imageAttachment)
    }
    
    static func attributedStringSpacerImage(
        width widthIn: CGFloat, height heightIn: CGFloat
        ) -> NSAttributedString {
        let imageAttachment =  NSTextAttachment()
        imageAttachment.image = nil
        let width = widthIn
        let height = heightIn
        imageAttachment.bounds = CGRect(x: 0, y: 0, width: width, height: height)
        return NSAttributedString(attachment: imageAttachment)
    }

    /**
        Truncates the string to 'length' number of characters & appends the trailing string if characters are trimmed
        Total length will be length + trailing.characters.count.
    */
    public func truncate(_ length: Int, trailing: String = "\u{2026}") -> String {
        if self.count > length {
            return self.substring( to: self.index(self.startIndex, offsetBy: length) ) + trailing
        } else {
            return self
        }
    }
    
    public func trimmingWhitespace() -> String {
        return trimmingCharacters(in: .whitespacesAndNewlines)
    }
    
    public func stringRange(for range: UITextRange, in textField: UITextField) -> Range<String.Index> {
        let lowerBound = index(startIndex, offsetBy: textField.offset(from: textField.beginningOfDocument, to: range.start))
        let upperBound = index(lowerBound, offsetBy: textField.offset(from: range.start, to: range.end))
        
        return Range(uncheckedBounds: (lower: lowerBound, upper: upperBound))
    }
    
    public func count(ofCharactersIn characterSet: CharacterSet, in range: Range<String.Index>) -> Int {
        var countOfCharacters = 0
        var currentRange = range
        while true {
            guard let rangeOfNextCharacter = rangeOfCharacter(from: characterSet, range: currentRange) else {
                break
            }
            
            countOfCharacters += 1
            guard rangeOfNextCharacter.upperBound < currentRange.upperBound else {
                break
            }
            
            currentRange = Range(uncheckedBounds: (lower: self.index(after: rangeOfNextCharacter.upperBound),
                                                   upper: currentRange.upperBound))
        }
        
        return countOfCharacters
    }
    
    public func removingCharacters(in set: CharacterSet) -> String {
        return components(separatedBy: set).joined()
    }
    
    /// Replace smart quotes with dumb quotes
    public func withDumbQuotes() -> String {
        return self
            .replacingOccurrences(of: "‘", with: "'")
            .replacingOccurrences(of: "’", with: "'")
            .replacingOccurrences(of: "“", with: "\"")
            .replacingOccurrences(of: "”", with: "\"")
    }
}


public extension NSString {
    
    public func count(ofCharactersIn characterSet: CharacterSet, in range: NSRange) -> Int {
        var countOfCharacters = 0
        var currentRange = range
        while true {
            let rangeOfNextCharacter = rangeOfCharacter(from: characterSet, range: currentRange)
            guard rangeOfNextCharacter.location != NSNotFound else {
                break
            }
            
            countOfCharacters += 1
            let rangeOfNextCharacterUpperBound = rangeOfNextCharacter.location + rangeOfNextCharacter.length
            guard rangeOfNextCharacterUpperBound < currentRange.location + currentRange.length else {
                break
            }
            
            currentRange = NSRange(location: rangeOfNextCharacterUpperBound, length: currentRange.location + currentRange.length - rangeOfNextCharacter.location - rangeOfNextCharacter.length)
        }
        
        return countOfCharacters
    }
    
}

extension Optional where Wrapped == String {
    var isEmpty: Bool {
        return self?.isEmpty ?? true
    }
    var isEmptyOrWhitespace: Bool {
        return self?.trimmingWhitespace().isEmpty ?? true
    }
}

// Localize?
extension String {
    public func pluralize(forNumber number: Int) -> String {
        return number != 1 ? self+"s" : self
    }
}

extension String {
    func dropTrailingSlash() -> String {
        return self.last == "/" ? String(self.dropLast()) : self
    }
}
