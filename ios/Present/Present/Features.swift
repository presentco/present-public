//
//  Features.swift
//  Present
//
//  Created by Patrick Niemeyer on 12/12/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation


/// Maintain the status of feature flags in the app.  This is based on the features returned from
/// the synchronization call in SyncStatusPoller.
public final class Features
{
    // TODO: Abstract this feature state caching by storing the full feature set here.
    // TODO: This will be easier with Swift 4 / Codable / JSON persistence.
    /// Should be called once during startup to observe the feature set
    public static func initialize() {
        //CoinsForJoins.initialize()
    }
}
