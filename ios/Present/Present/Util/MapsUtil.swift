//
//  MapsUtil.swift
//  Present
//
//  Created by Patrick Niemeyer on 8/1/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import CoreLocation
import PresentProtos
import MapKit

public final class MapsUtil
{
    // Aproximate distance per degree of latitude at 38 deg N.
    public static let approximateMetersPerDegreeLatitudeAt38DegN = 111000.0  // m
    
    public class func appleMapsUrl(forLocation location: SelectedLocation) -> URL? {
        return URL(string: appleMapsUrlString(forLocation: location))
    }
    
    public class func appleMapsUrlString(forLocation location: SelectedLocation) -> String {
        return appleMapsUrlString(forCoordinate: location.coordinate, withName: location.locationName)
    }
    
    public class func appleMapsUrlString(forCoordinate coordinate: CLLocationCoordinate2D, withName name: String) -> String
    {
        let encodedName = name.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        return "http://maps.apple.com/?ll=\(coordinate.latitude),\(coordinate.longitude)&q=\(encodedName)"
    }
}

// Some generic location utils as well as conversions between our proto coordinate type and CL Location types

public extension Present.Coordinates {
    
    public var as2DCoordinate: CLLocationCoordinate2D {
        return CLLocationCoordinate2DMake(latitude, longitude)
    }
    
    public var asLocation: CLLocation {
        return CLLocation(latitude: latitude, longitude: longitude)
    }
    
}

extension CLLocationCoordinate2D
{
    func offset(
        withLatMeters offsetLatMeters : CLLocationDistance,
        withLongMeters offsetLongMeters : CLLocationDistance
        )
        -> CLLocationCoordinate2D
    {
        var mapPoint = MKMapPointForCoordinate(self)
        
        let metersPerPoint = MKMetersPerMapPointAtLatitude(self.latitude)
        mapPoint.y += offsetLatMeters / metersPerPoint
        mapPoint.x += offsetLongMeters / metersPerPoint
        
        return MKCoordinateForMapPoint(mapPoint)
    }
    
    func distanceTo( _ coord : CLLocationCoordinate2D ) -> CLLocationDistance {
        return distanceBetween( coord, self )
    }
    
    /// Convert the Core Location coordinates (lat/long) to a proto coordinate (lat/long/accuracy).
    var asProtoCoordinate: Present.Coordinates {
        let coord = Present.Coordinates.getBuilder()
        
        coord.latitude = latitude
        coord.longitude = longitude
        coord.accuracy = 0
        
        return try! coord.build()
    }
    
}

extension CLLocationCoordinate2D {
    func isEqual(_ coord: CLLocationCoordinate2D) -> Bool {
        return (fabs(self.latitude - coord.latitude) < .ulpOfOne) && (fabs(self.longitude - coord.longitude) < .ulpOfOne)
    }
}
public func ==(lhs: CLLocationCoordinate2D, rhs: CLLocationCoordinate2D) -> Bool {
    return lhs.isEqual(rhs)
}
public func !=(lhs: CLLocationCoordinate2D, rhs: CLLocationCoordinate2D) -> Bool {
    return !lhs.isEqual(rhs)
}

extension CLLocation
{
    func distanceTo( _ coord : CLLocationCoordinate2D ) -> CLLocationDistance {
        return distanceBetween( self, coord )
    }
}

// Return the distance in meters between to CL coordinates
func distanceBetween( _ coord1 : CLLocationCoordinate2D, _ coord2 : CLLocationCoordinate2D ) -> CLLocationDistance
{
    let loc1 = CLLocation(latitude: coord1.latitude, longitude: coord1.longitude)
    return distanceBetween( loc1, coord2 )
}
// Return the distance in meters between a CL location and a CL Coordinate
func distanceBetween( _ loc1: CLLocation, _ coord2 : CLLocationCoordinate2D ) -> CLLocationDistance
{
    let loc2 = CLLocation(latitude: coord2.latitude, longitude: coord2.longitude)
    return distanceBetween( loc1, loc2 )
}
// Just for consistency
func distanceBetween( _ loc1: CLLocation, _ loc2: CLLocation ) -> CLLocationDistance {
    return loc1.distance(from: loc2)
}

extension MKMapView {
    func visibleAnnotations() -> [MKAnnotation] {
        return self.annotations(in: self.visibleMapRect).compactMap { $0 as? MKAnnotation }
    }
}
        
