//
//  RequestHeaderGenerator.swift
//  Present
//
//  Created by Dan Federman on 5/31/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import CoreLocation
import Foundation
import PresentProtos


public final class RequestHeaderGenerator
{
    // MARK: Properties

    // Note: RequestHeaderGenerator holds a copy of the (observable) selectedLocation because it needs
    // this prior to the startup of UserManager.  The request header generator is given any persisted
    // selected location from the user session at init() time and this field is updated manually by the
    // (one) location where it is changed. Alternately we could register a listener after application startup.
    public var selectedLocation: Present.City?

    private let clientToken: PresentUUID

    // MARK: Initialization
    
    public init(clientToken: PresentUUID, selectedLocation: Present.City?) {
        self.clientToken = clientToken
        self.selectedLocation = selectedLocation
    }
    
    // MARK: Public Methods

    public func newHeader(
        with uniqueIdentifier: PresentUUID = PresentUUID(),
        authorizationKey: String = "unimplemented",
        location: CLLocation?,
        spaceId: String? = nil
    ) -> Present.RequestHeader
    {
        let header = Present.RequestHeader.getBuilder()
        header.clientUuid = clientToken.uuidString
        header.requestUuid = uniqueIdentifier.uuidString
        header.authorizationKey = authorizationKey
        header.platform = Present.Platform.ios
        header.apiVersion = UInt32(PresentService.apiVersion)
        header.clientVersion = Bundle.main.applicationVersion
        header.platformVersion = UIDevice.current.systemVersion
        if let coordinate = location?.coordinate.asProtoCoordinate {
            header.location = coordinate
        }
        if let selectedLocation = selectedLocation {
            //logDebug("selected location = \(selectedLocation.location)")
            header.selectedLocation = selectedLocation.location
        }
        if let spaceId = spaceId {
            //log("using space = \(spaceId)")
            header.spaceId = spaceId
        } else {
            //log("using default space (everyone)")
        }

        return try! header.build()
    }
}
