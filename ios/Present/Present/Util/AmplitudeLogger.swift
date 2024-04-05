//
//  AmplitudeLogger.swift
//  Present
//
//  Pat Niemeyer
//

import Aardvark
import Foundation
import Amplitude_iOS

public final class AmplitudeLogger: NSObject, ARKLogObserver
{
    let prodApiKey =    "xxx"
    let stagingApiKey = "yyy"
    
    weak public var logDistributor: ARKLogDistributor?

    public init(withUserId userId: String?)
    {
        let apiKey: String
        switch Server.Endpoint.current {
            case .production:
                logDebug("Using production API key for Amplitude logging")
                apiKey = prodApiKey
            case .staging:
                logDebug("Using staging API key for Amplitude logging")
                apiKey = stagingApiKey
        }
        if let userId = userId {
            Amplitude.instance().initializeApiKey(apiKey, userId: userId)
        } else {
            Amplitude.instance().initializeApiKey(apiKey)
        }
    }

    /// Set the user id known. Amplitude will associate the anonymous user data with the correct user going forward.
    public func setUserId(userId: String) {
        if Amplitude.instance().userId != userId {
            Amplitude.instance().setUserId(userId)
            logDebug("Set user id for Amplitude logging: \(userId)") // should be ok to log here
        }
    }

    public func observe(_ logMessage: ARKLogMessage) {
        guard logMessage.userInfo[ExternalLogPermittedSentinel] != nil else { return }
        Amplitude.instance().logEvent(logMessage.externalDescription, withEventProperties: logMessage.externalAttributes)

    }
}
