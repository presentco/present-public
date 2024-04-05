//
//  CreateGroupSearchResult.swift
//  Created by Patrick Niemeyer
//  Copyright © 2017 Present Company. All rights reserved.
//

import CoreLocation
import Foundation
import MapKit

public final class CreateGroupSearchResult {
    
    // MARK: Static Methods
    
    public static func currentLocation() -> CreateGroupSearchResult {
        return CreateGroupSearchResult(type: .currentLocation)
    }
    
    public static func customLocation() -> CreateGroupSearchResult {
        return CreateGroupSearchResult(type: .customLocation)
    }
    
    // MARK: Private Types
    
    public enum ResultType {
        case none
        case area
        case place
        case customLocation
        case currentLocation
        case previouslySelected
    }
    
    private enum Location {
        case none
        case unresolved(MKLocalSearchCompletion)
        case coordinate(CLLocationCoordinate2D)
    }
    
    // MARK: Public Properties
    
    public private(set) var type: ResultType = .none
    public private(set) var subtitle: String = ""
    public var title: String = ""
    
    // MARK: Private Properties
    
    private var location: Location = .none
    
    // MARK: Initializers
    
    private init(type: ResultType) {
        self.type = type
    }
    
    public init(placemark: CLPlacemark) {
        self.type = .place
        configureWith(placemark: placemark)
    }
    
    public init(searchCompletion: MKLocalSearchCompletion) {
        self.type = .place
        self.location = .unresolved(searchCompletion)
        self.title = searchCompletion.title
        self.subtitle = searchCompletion.subtitle
    }
    
    /// Creates a new instance of the receiver when the editing a group with a previously selected location.
    public init(previouslySelected location: CLLocationCoordinate2D, title: String) {
        self.type = .previouslySelected
        self.location = .coordinate(location)
        self.title = title
        self.subtitle = NSLocalizedString(
            "CreateGroupSearchResultPreviouslySelectedSubtitle",
            tableName: nil,
            bundle: .main,
            value: "Placed Earlier",
            comment: "The subtitle for the location of the group previously selected."
        )
    }
    
    // MARK: Public Properties
    
    public var locationCoordinate: CLLocationCoordinate2D? {
        if case .coordinate(let value) = location {
            return value
        } else {
            return nil
        }
    }
    
    // MARK: Public Methods
    
    /// Attempts to resolve the location of the receiver.
    public func resolveLocation(completionHandler: @escaping (CreateGroupSearchResult, CLLocationCoordinate2D?) -> Void)
    {
        switch location {
        case .none:
            completionHandler(self, nil)
            
        case let .coordinate(value):
            completionHandler(self, value)
            
        case let .unresolved(searchCompletion):
            let request = MKLocalSearchRequest(completion: searchCompletion)
            
            // Submit a request to resolve the location.
            MKLocalSearch(request: request).start { (response, error) in
                if let place = response?.mapItems.first?.placemark {
                    self.configureWith(placemark: place)
                }
                
                if let error = error {
                    logError("Local search failed due to \(error)")
                }
                
                completionHandler(self, self.locationCoordinate)
            }
        }
    }
    
    /// Updates the location, title and subtitle of the receiver based on data from the placemark.
    public func configureWith(placemark: CLPlacemark) {
        if let coordinate = placemark.location?.coordinate {
            location = .coordinate(coordinate)
        } else {
            location = .none
        }
        
        guard title.isEmpty else {
            // We already have a title. No need to set one with our local.
            return
        }
        
        // Extract the title and subtitle from the placemark if available.
        if let subLocality = placemark.subLocality {
            title = subLocality
            subtitle = placemark.locality ?? ""
        } else if let locality = placemark.locality {
            title = locality
            subtitle = ""
        } else {
            title = NSLocalizedString(
                "CreateGroupSearchResultUnknownLocationTitle",
                tableName: nil,
                bundle: .main,
                value: "Unknown location",
                comment: "Displayed in the search results when the title of the current placemark is unknown."
            )
            
            subtitle = ""
        }
    }
    
}
