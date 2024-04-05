//
//  LocationProvider.swift
//
//  Created by Patrick Niemeyer on 1/8/16.
//  Copyright © 2016 Present Company. All rights reserved.
//

import CoreLocation
import Foundation
import PresentProtos
import MapKit
//import RxCocoa

public final class LocationManager: NSObject, CLLocationManagerDelegate, LocationProvider
{
    // MARK: Private Properties
    
    private let coreLocationManager = CLLocationManager()

    // TODO: Switch to Rx
    private let locationPromise = PromisedValue<CLLocation>()
    
    // TODO: Switch to Rx
    private var locationPermissionChangedPromise = PromisedValue<Bool>()
    
    /*
    lazy var authorized: Driver<Bool> = coreLocationManager.rx_didChangeAuthorizationStatus
        .startWith(CLLocationManager.authorizationStatus())
        .asDriver(onErrorJustReturn: CLAuthorizationStatus.NotDetermined)
        .map {
            switch $0 {
            case .AuthorizedAlways:
                return true
            default:
                return false
            }
    }*/

    public lazy var locationAuthorized = ObservableValue<Bool>().initialValue(self.shouldPromptForLocationPermission)
    // TESTING:
    //public lazy var locationAuthorized = FixedObservableValue.create(with: false)

    private var hasRequestedRoughLocation = false
    private var hasRequestedPreciseLocation = false

    public var shouldPromptForLocationPermission: Bool {
        switch CLLocationManager.authorizationStatus() {
        case .restricted, .denied, .notDetermined:
            return true
            
        case .authorizedAlways, .authorizedWhenInUse:
            return false
        }
    }
    
    public var locationPermissionDeniedOrRestricted: Bool {
        switch CLLocationManager.authorizationStatus() {
        case .restricted, .denied:
            return true
            
        case .authorizedAlways, .authorizedWhenInUse, .notDetermined:
            return false
        }
    }
    
    // MARK: Initialization
    
    /// param useLastLocationAtStartup - If true attempt to use the last known system location initially rather than
    ///     waiting for a location update
    public init(useLastLocationAtStartup: Bool) {
        super.init()
        coreLocationManager.delegate = self
        
        if useLastLocationAtStartup, let lastLocation = coreLocationManager.location {
            NSLog("Using last system location for startup: \(lastLocation)") // logger not yet initialized
            self.location = lastLocation
        }
    }
    
    // MARK: CLLocationManagerDelegate

    // Main callback from core location to update our location
    public func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        //logDebug("Location Manager didUpdateLocations: \(locations.last)")
        //locations.last?.distanceTo(location!.coordinate)
        location = locations.last
    }

    public func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus)
    {
        logDebug("LocationManager did change auth status: \(status)")
        let status = [CLAuthorizationStatus.authorizedWhenInUse].contains(status)
        locationPermissionChangedPromise.fulfill(with: status)
        locationAuthorized.value = status
    }

    public func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        logDebug("LocationManager failed to update with error \(error)")
    }
    
    // MARK: LocationProvider

    // Note: createRequest header draws from this location directly
    public private(set) var location: CLLocation? {
        didSet {
            //logDebug("location did set: \(location), value changed: \(location != oldValue)")
            if let location = location
            {
                if oldValue == nil {
                    logDebug("Location manager acquired initial location: \(location)")
                } else {
                    //logDebug("Location manager updated location: \(location)")
                }
                locationPromise.fulfill(with: location)
            } else {
                logDebug("Location manager reset location to nil")
                locationPromise.resetValue()
            }
        }
    }

    public var hasLocation: Bool {
        return location != nil
    }

    /// Called at startup, on entering foreground, and in the case of no permissions
    /// queued for later when location permissions change.
    /// This method also logs the location permission status to the server
    public func startMonitoringLocationWhenPermissionGranted()
    {
        let status = CLLocationManager.authorizationStatus()
        switch status {
            case .authorizedAlways, .authorizedWhenInUse:
                logEvent(.permission_location_status, [LoggingKey.status: LoggingValue.allowed])
                // Note: significant location change API requires "always on" location perms
                //logDebug("Location manager start monitoring significant location changes")
                //coreLocationManager.startMonitoringSignificantLocationChanges()
                logDebug("Location manager start monitoring at 100m")
                coreLocationManager.desiredAccuracy = kCLLocationAccuracyHundredMeters
                coreLocationManager.startUpdatingLocation()

            case .restricted, .denied, .notDetermined:
                logEvent(.permission_location_status, [LoggingKey.status: LoggingValue.denied])
                // If not authorized for location yet try again when perm changes
                locationPermissionChangedPromise.resetValue()
                locationPermissionChangedPromise.then {_ in
                    self.startMonitoringLocationWhenPermissionGranted()
                }
                // Nothing to do here.
                break
        }
    }

    // Note: Important that we stop any significant location change monitoring else we will get background updates.
    public func stopMonitoringLocation() {
        switch CLLocationManager.authorizationStatus() {
        case .authorizedAlways, .authorizedWhenInUse:
            // Note: significant location change API requires "always on" location perms
            //coreLocationManager.stopMonitoringSignificantLocationChanges()
            coreLocationManager.stopUpdatingLocation()

        case .restricted, .denied, .notDetermined:
            // Nothing to do here.
            break
        }
    }
    
    public func requestLocationPermission(completionHandler: @escaping (Bool) -> Void = { _ in }) {
        switch CLLocationManager.authorizationStatus() {
        case .authorizedAlways, .authorizedWhenInUse:
            completionHandler(true)
            
        case .restricted, .denied:
            logError("Requesting location permission when it has been denied")
            completionHandler(false)
            
        case .notDetermined:
            locationPermissionChangedPromise.resetValue()
            locationPermissionChangedPromise.then(block: completionHandler)
            
            coreLocationManager.requestWhenInUseAuthorization()
        }
    }
    
    public func executeWhenLocationAvailable(block: @escaping (CLLocation) -> Void) {
        locationPromise.then(block: block)
    }

}

public protocol LocationProvider
{
    var location: CLLocation? { get }
    var hasLocation: Bool { get }
    func executeWhenLocationAvailable(block: @escaping (CLLocation) -> Void)
    func requestLocationPermission(completionHandler: @escaping (Bool) -> Void)
    func startMonitoringLocationWhenPermissionGranted()
    func stopMonitoringLocation()
    var shouldPromptForLocationPermission: Bool { get }
    var locationPermissionDeniedOrRestricted: Bool { get }
}

