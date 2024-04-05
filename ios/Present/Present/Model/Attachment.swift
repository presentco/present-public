//
//  Attachment.swift
//  Present
//
//  Created by Dan Federman on 3/20/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import JSQMessages
import PresentProtos


public enum Attachment {
    case remoteImage(source: URL)
    case remoteMovie(source: URL, thumbnailSource: URL)
    case localImage(image: UIImage)
    case localMovie(source: URL, thumbnail: UIImage)
    
    // MARK: Initialization
    
    public init?(proto: Present.ContentResponse) {
        guard proto.hasContent, let source = URL(string: proto.content) else {
            return nil
        }
        
        switch proto.contentType {
        case .jpeg:
            self = .remoteImage(source: source)
        case .mp4:
            guard proto.hasContentThumbnail, let thumbnailURL = URL(string: proto.contentThumbnail) else {
                return nil
            }
            
            self = .remoteMovie(source: source, thumbnailSource: thumbnailURL)
        }
    }
    
    // MARK: Public Properties
    
    public var isPhoto: Bool {
        return asPhoto != nil
    }
    
    public var asPhoto: Photo? {
        switch self {
        case let .remoteImage(source):
            return .remoteImage(source: source)
            
        case let .localImage(image):
            return .localImage(image: image)
            
        case .remoteMovie, .localMovie:
            return nil
        }
    }
    
    public var isMovie: Bool {
        return asMovie != nil
    }
    
    public var asMovie: Movie? {
        switch self {
        case let .remoteMovie(source, thumbnailSource):
            return .remoteMovie(source: source, thumbnailSource: thumbnailSource)
            
        case let .localMovie(source, thumbnail):
            return .localMovie(source: source, thumbnail: thumbnail)

        case .remoteImage, .localImage:
            return nil
        }
    }
    
    public var isUploadable: Bool {
        return asUploadable != nil
    }
    
    public var asUploadable: Uploadable? {
        switch self {
        case let .localImage(image):
            return .localImage(image: image)
            
        case let .localMovie(source, thumbnail):
            return .localMovie(source: source, thumbnail: thumbnail)
            
        case .remoteImage, .remoteMovie:
            return nil
        }
    }
    
    public var summaryText: String {
        switch self {
        case .remoteImage, .localImage:
            return NSLocalizedString("AttachmentImageSummaryText",
                              tableName: nil, bundle: .main,
                              value: "Attachment: Image",
                              comment: "Summary text for a message's image attachment.")
        case .remoteMovie, .localMovie:
            return NSLocalizedString("AttachmentVideoSummaryText",
                                     tableName: nil, bundle: .main,
                                     value: "Attachment: Video",
                                     comment: "Summary text for a message's video attachment.")
        }
    }
    
    // MARK: Public Methods
    
    public func asMediaItem(outgoing: Bool) -> JSQMediaItem {
        switch self {
        case let .remoteImage(source):
            return PhotoMessageModel(attachment: .remoteImage(source: source), outgoing: outgoing)
            
        case let .localImage(image):
            return PhotoMessageModel(attachment: .localImage(image: image), outgoing: outgoing)
            
        case let .remoteMovie(source, thumbnailSource):
            return VideoMessageModel(attachment: .remoteMovie(source: source, thumbnailSource: thumbnailSource), outgoing: outgoing)
            
        case let .localMovie(source, thumbnail):
            return VideoMessageModel(attachment: .localMovie(source: source, thumbnail: thumbnail), outgoing: outgoing)
        }
    }
    
    // MARK: Public Structs
    
    public enum Photo: Equatable {
        case remoteImage(source: URL)
        case localImage(image: UIImage)
        
        // MARK: Equatable
        
        public static func ==(lhs: Photo, rhs: Photo) -> Bool {
            switch lhs {
            case let .remoteImage(lhsSource):
                guard case let .remoteImage(rhsSource) = rhs else {
                    return false
                }
                
                return lhsSource == rhsSource
                
            case let .localImage(lhsImage):
                guard case let .localImage(rhsImage) = rhs else {
                    return false
                }
                
                return lhsImage == rhsImage
            }
        }
    }
    
    public enum Movie: Equatable {
        case remoteMovie(source: URL, thumbnailSource: URL)
        case localMovie(source: URL, thumbnail: UIImage)
        
        // MARK: Equatable
        
        public static func ==(lhs: Movie, rhs: Movie) -> Bool {
            switch lhs {
            case let .remoteMovie(lhsSource, lhsThumbnailSource):
                guard case let .remoteMovie(rhsSource, rhsThumbnailSource) = rhs else {
                    return false
                }
                
                return lhsSource == rhsSource && lhsThumbnailSource == rhsThumbnailSource
                
            case let .localMovie(lhsSource, lhsThumbnail):
                guard case let .localMovie(rhsSource, rhsThumbnail) = rhs else {
                    return false
                }
                
                return lhsSource == rhsSource && lhsThumbnail == rhsThumbnail
            }
        }
        
        // MARK: Public Properties
        
        public var source: URL {
            switch self {
            case let .remoteMovie(source, _):
                return source
                
            case let .localMovie(source, _):
                return source
            }
        }
    }
    
    public enum Uploadable: Equatable {
        case localImage(image: UIImage)
        case localMovie(source: URL, thumbnail: UIImage)
        
        // MARK: Equatable
        
        public static func ==(lhs: Uploadable, rhs: Uploadable) -> Bool {
            switch lhs {
            case let .localImage(lhsImage):
                guard case let .localImage(rhsImage) = rhs else {
                    return false
                }
                
                return lhsImage == rhsImage
                
            case let .localMovie(lhsSource, lhsThumbnail):
                guard case let .localMovie(rhsSource, rhsThumbnail) = rhs else {
                    return false
                }
                
                return lhsSource == rhsSource && lhsThumbnail == rhsThumbnail
            }
        }
        
        // MARK: Public Properties
        
        public var asAttachment: Attachment {
            switch self {
            case let .localImage(image):
                return .localImage(image: image)
                
            case let .localMovie(source, thumbnail):
                return .localMovie(source: source, thumbnail: thumbnail)
            }
        }
    }
}


// MARK: - PersistableAttachment


public final class PersistableAttachment: NSObject, NSSecureCoding {
    
    // MARK: NSSecureCoding
    
    public static var supportsSecureCoding: Bool {
        return true
    }
    
    // MARK: Private Static Properties
    
    private static let sourceURLKey = "sourceURL"
    private static let thumbnailURLKey = "thumbnailURL"
    private static let imageKey = "image"
    
    // MARK: Initialization
    
    public required init(attachment: Attachment) {
        self.attachment = attachment
        
        super.init()
    }
    
    public required init?(coder aDecoder: NSCoder) {
        if let sourceURL = aDecoder.decodeObject(of: NSURL.self, forKey: PersistableAttachment.sourceURLKey),
            let thumbnailURL = aDecoder.decodeObject(of: NSURL.self, forKey: PersistableAttachment.thumbnailURLKey) {
            attachment = .remoteMovie(source: sourceURL as URL, thumbnailSource: thumbnailURL as URL)
            
        } else if let sourceURL = aDecoder.decodeObject(of: NSURL.self, forKey: PersistableAttachment.sourceURLKey),
            let image = aDecoder.decodeObject(of: UIImage.self, forKey: PersistableAttachment.imageKey) {
            attachment = .localMovie(source: sourceURL as URL, thumbnail: image)
            
        } else if let sourceURL = aDecoder.decodeObject(of: NSURL.self, forKey: PersistableAttachment.sourceURLKey) {
            attachment = .remoteImage(source: sourceURL as URL)
            
        } else if let image = aDecoder.decodeObject(of: UIImage.self, forKey: PersistableAttachment.imageKey) {
            attachment = .localImage(image: image)
            
        } else {
            return nil
        }
    }
    
    public required init?(from data: Data) {
        guard let unarchivedSelf = NSKeyedUnarchiver.unarchiveObject(with: data) as? PersistableAttachment else {
            return nil
        }
        
        attachment = unarchivedSelf.attachment
    }
    
    // MARK: NSCoding
    
    public func encode(with aCoder: NSCoder) {
        switch attachment {
        case let .remoteImage(source):
            aCoder.encode(source, forKey: PersistableAttachment.sourceURLKey)
            
        case let .localImage(image):
            aCoder.encode(image, forKey: PersistableAttachment.imageKey)
            
        case let .remoteMovie(source, thumbnailSource):
            aCoder.encode(source, forKey: PersistableAttachment.sourceURLKey)
            aCoder.encode(thumbnailSource, forKey: PersistableAttachment.thumbnailURLKey)
            
        case let .localMovie(source, thumbnail):
            aCoder.encode(source, forKey: PersistableAttachment.sourceURLKey)
            aCoder.encode(thumbnail, forKey: PersistableAttachment.imageKey)
        }
    }
    
    // MARK: Public Properties
    
    public let attachment: Attachment
    
    public var asData: Data {
        return NSKeyedArchiver.archivedData(withRootObject: self)
    }
    
}
