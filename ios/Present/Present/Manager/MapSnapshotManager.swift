//
//  MapSnapshotUtil.swift
//  Present
//
//  Created by Patrick Niemeyer on 1/25/16.
//  Copyright © 2016 Present Company. All rights reserved.
//

import Foundation
import MapKit


public final class MapSnapshotManager {
    
    // MARK: Public Static Properties
    
    /// Shared instance of the map snapshot manager.
    public static let shared = MapSnapshotManager()
    
    // TODO:dan: Obtain a pre-blurred image instead of computing this at runtime.
    /// Blurred version of a placeholder map.
    public static let loadingMapImage = blurImage(#imageLiteral(resourceName: "map2"), blurRadius: 8.0, saturationDeltaFactor: 0.4)
    
    // MARK: Private Static Properties
    
    /// Current version of the map layout. Must be incremented when the map layout changes.
    private static let mapVersion = 12
    
    // MARK: Public Methods
    
    /// Get a map snapshot for the region centered on the bubble location.
    /// - parameter center: The center of the map location.
    /// - parameter size: The size in points of the snapshot.
    /// - parameter name: a name used for logging and debugging purposes.
    /// - parameter completionHandler: A block to invoke when the map snapshot has been rendered. Takes the rendered map as an argument.
    /// - parameter id: An id associated with the request (e.g. may be used to discard stale results)
    public func mapSnapshot(at center: CLLocationCoordinate2D, size: CGSize, name: String, id: String, completionHandler: @escaping (UIImage, String) -> Void)
    {
        synchronizationQueue.async {
            let mapToGenerate = MapToGenerate(center: center, width: size.width, height: size.height, name: name)
            ImageManager.shared.getStoredImageAsync(forCacheKey: mapToGenerate.cacheKey, complete: { image in
                if let image = image {
                    //logDebug("MapSnapshotManager: found cached image for: \(name)");
                    DispatchQueue.main.async {
                        completionHandler(image, id)
                    }
                } else {
                    //logDebug("MapSnapshotManager: enqueue map generation for: \(name)");
                    self.enqueue_inSynchronizationQueue(mapToGenerate: mapToGenerate, completionHandler: { image in completionHandler(image,id) } )
                }
            })
        }
    }
    
    /// Get a map snapshot for the region centered on the bubble location.
    /// - parameter center: The center of the map location.
    /// - parameter size: The size in points of the snapshot.
    /// - parameter name: a name used for logging and debugging purposes.
    /// - parameter completionHandler: A block to invoke when the map snapshot has been rendered. Takes the rendered map as an argument.
    public func mapSnapshot(at center: CLLocationCoordinate2D, size: CGSize, name: String, completionHandler: @escaping (UIImage) -> Void) {
        synchronizationQueue.async {
            let mapToGenerate = MapToGenerate(center: center, width: size.width, height: size.height, name: name)
            ImageManager.shared.getStoredImageAsync(forCacheKey: mapToGenerate.cacheKey, complete: { image in
                if let image = image {
                    //logDebug("MapSnapshotManager: found cached image for: \(name)");
                    DispatchQueue.main.async {
                        completionHandler(image)
                    }
                } else {
                    //logDebug("MapSnapshotManager: enqueue map generation for: \(name)");
                    self.enqueue_inSynchronizationQueue(mapToGenerate: mapToGenerate, completionHandler: completionHandler)
                }
            })
        }
    }
    
    // MARK: Private Properties
    
    /// Used to synchronize access to shared variables.
    private let synchronizationQueue = DispatchQueue(label: "Map Snapshot Manager Synchronization Queue")
    
    /// Used for render operations.
    private let rateLimiter = RateLimiter(maxConcurrentOperationCount: 3)
    
    // MARK: Private Properties - Access with Synchronization Queue
    
    /// List of completion handlers for oustanding requests.
    private var requestToCompletionHandlersMap = [MapToGenerate : [(UIImage) -> Void]]()
    
    // MARK: Private Methods
    
    /// Enqueues a map generation request. Thread-safe.
    private func enqueue_inSynchronizationQueue(mapToGenerate: MapToGenerate, completionHandler: @escaping (UIImage) -> Void) {
        if let completionHandlers = requestToCompletionHandlersMap[mapToGenerate] {
            requestToCompletionHandlersMap[mapToGenerate] = completionHandlers + [completionHandler]
            return
        } else {
            requestToCompletionHandlersMap[mapToGenerate] = [completionHandler]
        }
        
        // Enqueue the render request via the rate limiter.
        rateLimiter.add { (completionHandler) in
            self.renderMapSnapshot(for: mapToGenerate, completionHandler: completionHandler)
        }
    }

    /// Executes completion handlers for a map generation request. Thread-safe.
    private func complete_inSynchronizationQueue(generationOf map: MapToGenerate, snapshot: MKMapSnapshot)
    {
        for completionHandler in requestToCompletionHandlersMap[map] ?? []
        {
            DispatchQueue.main.async {
                
                UIGraphicsBeginImageContextWithOptions(snapshot.image.size, true, snapshot.image.scale)
                
                // Draw the map
                snapshot.image.draw(at: CGPoint(x:0, y:0))
                
                // Draw the pin annotation with the pin pointing to the center of the map
                // TODO: Removing this because the pins on the circle lists are
                // TODO: too large. Need to review what kind of pins at what size
                // TODO: are needed, add options to this util and restore.
                let annotationView = MKAnnotationView()
                //let annotationImage = #imageLiteral(resourceName: "locationPinPurple.pdf")
                let annotationImage = #imageLiteral(resourceName: "small-pin")
                annotationView.image = annotationImage
                // scale the image to 14pt high
                let height: CGFloat = 14
                let width: CGFloat = annotationView.image!.size.width * height / annotationView.image!.size.height
                annotationView.drawHierarchy(in: CGRect(
                    x:snapshot.point(for: map.center).x-width/2.0, // center of the pin
                    y:snapshot.point(for: map.center).y-height/2,
                    width:width,
                    height:height),
                 afterScreenUpdates: true)

                guard let image = UIGraphicsGetImageFromCurrentImageContext() else {
                    UIGraphicsEndImageContext()
                    return
                }
                
                UIGraphicsEndImageContext()
                
                // Create a PNG representation of the map for our cache.
                guard let pngData = UIImagePNGRepresentation(image) else {
                    logError("Map snapshot failed to convert to png")
                    self.purge_inSynchronizationQueue(generationOf: map)
                    return
                }
                ImageManager.shared.cacheImage(pngData, forCacheKey: map.cacheKey)
                    
                completionHandler(image)
            }
        }
        
        requestToCompletionHandlersMap[map] = nil
    }
    
    /// Purge in-flight requests without calling completion handlers in the event an error occurs during generation.
    private func purge_inSynchronizationQueue(generationOf map: MapToGenerate) {
        requestToCompletionHandlersMap[map] = nil
    }
    
    /// Invoked by the rate limiter on the main queue to kick off a map generation request.
    private func renderMapSnapshot(for map: MapToGenerate, completionHandler: @escaping () -> Void)
    {
        let regionWidth = map.radius
        let regionHeight = regionWidth * Double(map.height / map.width)
        let region = MKCoordinateRegionMakeWithDistance(map.center, regionHeight, regionWidth)
        
        let options = MKMapSnapshotOptions()
        options.region = region
        options.size = CGSize(width: map.width, height: map.height)
        options.mapType = .standard
        options.showsPointsOfInterest = true
        
        // The snapshot completion handler will cause locking on the completion queue.
        // To avoid locking the main thread, use a global concurrent queue for our completion handler.
        //logDebug("MapSnapshotManager: start render for: \(map.name)")
        MKMapSnapshotter(options: options).start(with: synchronizationQueue) { snapshot, error in
            //logDebug("MapSnapshotManager: finished render for: \(map.name)")
            defer {
                completionHandler()
            }
            
            guard error == nil else {
                logError("Map snapshot failed due to \(error!).")
                self.purge_inSynchronizationQueue(generationOf: map)
                return
            }
            
            guard let snapshot = snapshot else {
                logError("Map snapshot failed. No error was given.")
                self.purge_inSynchronizationQueue(generationOf: map)
                return
            }
            
            
            // Hop onto our work queue to access our completion handlers.
            self.complete_inSynchronizationQueue(generationOf: map, snapshot: snapshot)
        }
    }
    
    // MARK: MapToGenerate
    
    private struct MapToGenerate: Hashable
    {
        // MARK: Public Properties

        /// The center coordinate for the map.
        public let center: CLLocationCoordinate2D
        /// The minimum radius from the center location to show, in meters.
        //public let radius: Double = 325.0
        public let radius: Double = 1000.0
        /// The width of the desired map in points.
        public let width: CGFloat
        /// The height of the desired map in points.
        public let height: CGFloat
        /// A name for logging and debugging purposes
        public let name: String

        public var cacheKey: String {
            // Width and height are rounded to avoid floating point errors.
            return "map:\(MapSnapshotManager.mapVersion):\(center.longitude),\(center.latitude):\(ceil(width)):\(ceil(height))"
        }

        // MARK: Equatable
        
        public static func ==(lhs: MapToGenerate, rhs: MapToGenerate) -> Bool {
            // Use cache keys as an approximation of equality.
            return lhs.cacheKey == rhs.cacheKey
        }
        
        // MARK: Hashable
        
        public var hashValue: Int {
            return cacheKey.hashValue
        }
    }
}
