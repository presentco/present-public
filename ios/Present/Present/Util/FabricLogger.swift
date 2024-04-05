//
//  FabricLogger.swift
//  Present
//
//  Created by Dan Federman on 2/2/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Aardvark
import Crashlytics
import Fabric
import Foundation


public final class FabricLogger: NSObject, ARKLogObserver {
    
    // MARK: Private Static Properties
    
    private static let initializeFabricOnce: () = {
        let _ = Fabric.with([Answers.self, Crashlytics.self])
    }()
    
    // MARK: Initialization
    
    public init(withUserId userId: String?) {
        super.init()
        FabricLogger.initializeFabricOnce
        if let userId = userId {
            setUserId(userId: userId)
        }
    }
    
    /// Set the user id known.
    public func setUserId(userId: String) {
        Crashlytics.sharedInstance().setUserIdentifier(userId)
        logDebug("Set user id for Amplitude logging: \(userId)") // should be ok to log here
    }
    
    // MARK: ARKLogObserver
    
    weak public var logDistributor: ARKLogDistributor?
    
    public func observe(_ logMessage: ARKLogMessage) {
        guard logMessage.userInfo[ExternalLogPermittedSentinel] != nil else {
            return
        }
        
        // Log to Answers.
        Answers.logCustomEvent(withName: logMessage.externalDescription, customAttributes: logMessage.externalAttributes)
        
        // Log to Crashlytics.
        CLSLogv("%@", getVaList([logMessage.externalDescription]))
    }
    
}
