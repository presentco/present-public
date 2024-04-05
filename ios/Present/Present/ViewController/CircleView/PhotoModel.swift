//
//  PhotoModel.swift
//  Created by Patrick Niemeyer on 8/25/16.
//

import Foundation
import NYTPhotoViewer

/**
    Our implementation of the NYTPhoto data source for an image.
 */
public class PhotoModel: NSObject, NYTPhoto {
    
    // MARK: Equatable
    
    public static func ==(lhs: PhotoModel, rhs: PhotoModel) -> Bool {
        return lhs.image == rhs.image
            && lhs.uploader == rhs.uploader
    }
    
    // MARK: Initialization
    
    public init(photoAttachment: Attachment.Photo, uploader: Person) {
        switch photoAttachment {
        case let .localImage(localImage):
            image = localImage
            
        case .remoteImage:
            image = nil
        }
        
        imageData = nil
        attributedCaptionTitle = NSAttributedString(string: uploader.name)
        
        self.photoAttachment = photoAttachment
        self.uploader = uploader
        
        super.init()
    }
    
    // MARK: Public Properties
    
    public var mediaView: UIView?
    
    // MARK: NYTPhoto
    
    public var image: UIImage?
    public var imageData: Data? = nil // ImageData may be used to support animated GIFs
    public var placeholderImage: UIImage? = nil
    
    // Image caption
    public let attributedCaptionTitle: NSAttributedString?
    // Second line of caption
    public let attributedCaptionSummary: NSAttributedString? = nil
    // Third line of caption
    public let attributedCaptionCredit: NSAttributedString? = nil

    // MARK: Public Methods
    
    /// Async check cache for image and begin retrieval now if true.
    public func load(onDiskImageInto photosViewController: NYTPhotosViewController) {
        switch photoAttachment {
        case .localImage:
            load(into: photosViewController)
            
        case let .remoteImage(source):
            ImageManager.shared.hasImage(source.absoluteString) { inCache in
                if inCache {
                    self.load(into: photosViewController)
                }
            }
        }
    }
    
    /// Sets the image on the argument, downloading the image from the network if necessary.
    public func load(into photosViewController: NYTPhotosViewController) {
        switch photoAttachment {
        case .localImage:
            photosViewController.updateImage(for: self)
            
        case let .remoteImage(source):
            ImageManager.shared.getImage(atURL: source, successHandler: { image in
                self.image = image
                photosViewController.updateImage(for: self)
            })
        }
    }
    
    // MARK: Private Properties
    
    private let photoAttachment: Attachment.Photo
    private let uploader: Person
    
}
