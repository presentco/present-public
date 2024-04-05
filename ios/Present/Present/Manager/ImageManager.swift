//
//  ImageManager.swift
//  Present
//
//  Created by Patrick Niemeyer on 7/13/16.
//  Copyright © 2016 Present Company. All rights reserved.
//

import Foundation
import JSQMessages
import SDWebImage
import Then
import RxSwift

public typealias CacheKey = String

public enum ImageManagerError : Error {
    case failed
}

public final class ImageManager: Then
{
    static var shared = ImageManager().then { _ in
        SDImageCache.shared().shouldDecompressImages = false
    }
    static let logImageLoading = true
    
    // Image upload params
    static let uploadImageCompressionQuality = 1.0 // max quality, large file
    static let uploadImageMaxDimension: CGFloat = 1024 // Aspect fit in px
    static let uploadUserAvatarPhotoDimension : CGFloat = 1024 // Aspect fit in px.
    
    /// The image retrieved for the specified URL.
    /// The URL is returned to help in discarding stale image requests
    /// (e.g. for loading images in a reusable table cell)
    public struct UrlImage {
        let url: String
        let image: UIImage
    }
    
    public func getImage(atURL url: URL)->Single<UrlImage> {
        let urlString = url.absoluteString
        return getImage(atURLString: urlString)
    }
    
    public func getImage(atURLString urlString: String)->Single<UrlImage> {
        return Single.create { observer in
            //mainAfter(1.0) { // TESTING SLOW CONNECTION
            self.getImage(
                atURLString: urlString,
                errorHandler: {
                    observer(.error(ImageManagerError.failed))
            },
                successHandler: { image in
                    observer(.success(UrlImage(url: urlString, image: image)))
            }
            )
            //}
            return Disposables.create()
        }
    }

    func getImage(atURL url: URL, errorHandler: @escaping () -> Void = {}, successHandler: @escaping (UIImage) -> Void) {
        let urlString = url.absoluteString
        getImage(atURLString: urlString, errorHandler: errorHandler, successHandler: successHandler)
    }
    
    func getImage(atURLString urlString: String, errorHandler: @escaping () -> Void = {}, successHandler: @escaping (UIImage) -> Void) {
        let url = URL( string: urlString )
        SDWebImageManager.shared().downloadImage(
            with: url,
            // Without .RetryFailed SDWebImage will stop trying to reload missing images.
            options: [.retryFailed],
            progress: nil,
            completed: { (image, error, cacheType, finished/*for progressive download*/, url) in
                if ImageManager.logImageLoading && cacheType == .none {
                    if let error = error {
                        logError("Failed to load image from network: \(error) : \(String(describing: url))")
                    }
                }
                
                if let image = image, error == nil {
                    successHandler(image)
                    
                } else {
                    errorHandler()
                }
        })
    }
    
    func hasImage( _ urlString : String, complete: @escaping (_ imageIsInCache:Bool) -> Void )
    {
        SDWebImageManager.shared().cachedImageExists(
            for: URL(string: urlString), completion: complete )
    }
    
    func getStoredImage( forCacheKey cacheKey : CacheKey ) -> UIImage? {
        // imageFromDiskCacheForKey: "Query the disk cache synchronously after checking the memory cache."
        return SDImageCache.shared().imageFromDiskCache( forKey: cacheKey )
    }
    
    func getStoredImageAsync( forCacheKey cacheKey : CacheKey, complete: @escaping (UIImage?) -> Void ) {
        SDImageCache.shared().queryDiskCache(
            forKey: cacheKey, done: { image, cacheType in complete(image) } )
    }
    
    /**
        Pre-cache a local image for the given cache key.
    */
    func cacheImage( _ image : UIImage, forCacheKey cacheKey : CacheKey ) {
        SDImageCache.shared().store(image, forKey: cacheKey )
    }
    
    func cacheImage( _ data : Data, forCacheKey cacheKey : CacheKey ) {
        SDImageCache.shared().storeImageData(toDisk: data, forKey: cacheKey)
    }
    
    /**
        Pre-cache a local image that we are uploading to be available at the specified URL.
        Subsequent gets on the image by URL should retrieve it from the cache until expiration.
        i.e. Cache the image so the we don't immediately download what we just uploaded.
    */
    func cacheImage( _ image : UIImage, forUrl urlString : String ) {
        if let url = URL(string: urlString) {
            SDWebImageManager.shared().saveImage(toCache: image, for: url)
        }
    }
    
    /// Clear all image caching on disk and memory
    func clearCache() {
        logDebug("ImageManager: Clearing disk and memory caches.")
        SDImageCache.shared().clearDisk()
        SDImageCache.shared().clearMemory()
    }
    
    /// Clear the in-memory cache only
    func clearMemoryCache() {
        logDebug("ImageManager: Clearing memory cache.")
        SDImageCache.shared().clearMemory()
    }
    
    /// Clear a single image from the cache
    func clearImageFromCache( _ urlString : String ) {
        SDImageCache.shared().removeImage(forKey: urlString, fromDisk: true)
    }

    /// Render the image to a JPG at the specified max size and quality
    class func getJpgPhotoBytesForUpload(
        _ imageIn : UIImage, scaleMaxDimension: CGFloat = ImageManager.uploadImageMaxDimension) -> Data
    {
        let image = imageIn.scaleAspectFitIfNeeded(scaleMaxDimension)
        guard let data = UIImageJPEGRepresentation(image, CGFloat(uploadImageCompressionQuality)) else {
            logError("Couldn't render image to bytes.")
            return Data()
        }
        return data
    }
}
