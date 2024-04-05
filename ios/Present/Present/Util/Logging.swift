//
//  Logging.swift
//  Present
//
//  Created by Dan Federman on 2/2/17.
//  Copyright © 2017 Present Company. All rights reserved.
//


import Aardvark
import Foundation

public let ExternalLogPermittedSentinel = "ExternalLogPermitted"

public extension ARKLogMessage {
    
    public var externalDescription: String {
        let prefix: String
        switch type {
        case .error:
            prefix = "ERROR: "
        case .default, .screenshot, .separator:
            prefix = ""
        }
        
        return "\(prefix)\(text)"
    }
    
    public var externalAttributes: [String : Any] {
        var externalAttributes = [String : Any]()
        
        for (key, value) in userInfo {
            if let key = key as? String {
                guard key != ExternalLogPermittedSentinel else {
                    // Do not add properties we use for determining log destinations to our external attributes.
                    continue
                }
                
                externalAttributes[key] = value
            }
        }
        
        return externalAttributes
    }
    
}


public enum LogType: CustomStringConvertible {
    
    case view
    case tap
    case action
    case error
    case metric // Curated for Amplitude

    // MARK: CustomStringConvertible
    
    public var description: String {
        switch self {
        case .view:
            return "View"
        case .tap:
            return "Tap"
        case .action:
            return "Action"
        case .error:
            return "Error"
        case .metric:
            return "Metric"
        }
    }
    
    // MARK: Public Properties
    
    public var aardvarkLogType: ARKLogType {
        switch self {
        case .view, .tap, .action, .metric:
            return .default
        case .error:
            return .error
        }
    }
}

/// Relays log events to all consumers. Log data must not have PII.
public func logEvent(type: LogType, _ description: String, properties: [NSObject : AnyObject] = [:]) {
    var attributes = properties
    
    switch(type) {
        case .error, .action, .tap, .view:
            // No sentinel, this is the equivalent of debug logging
            log("\(type.description): \(description)", type: type.aardvarkLogType, userInfo: attributes)
        case .metric:
            logDebug("Logged event: \(description), with attributes: \(attributes)")
            // This sentinel allows the log to be published externally.
            attributes[ExternalLogPermittedSentinel as NSString] = NSNumber(booleanLiteral: true)
            log(description, type: type.aardvarkLogType, userInfo: attributes)
            EventLoggingOverlay.shared.show(text: "LogEvent: \(description)")
    }
}

public func logEvent(_ constant: LoggingEvent?, _ properties: [LoggingKey: LoggingValue] = [:]) {
    guard let constant = constant else { return }
    logEvent(constant, properties)
}

public func logEvent(_ constant: LoggingEvent, _ properties: [LoggingKey: LoggingValue] = [:]) {
    logEvent(type: .metric, constant.rawValue, properties: castProps(properties))
}

public func logEvent(_ constant: LoggingEvent?, stringProperties: [LoggingKey: String]) {
    guard let constant = constant else { return }
    logEvent(constant, stringProperties: stringProperties)
}

public func logEvent(_ constant: LoggingEvent, stringProperties: [LoggingKey: String]) {
    logEvent(type: .metric, constant.rawValue, properties: castProps(stringProperties))
}

// TODO: Remove the space now?
public func logEvent(_ constant: LoggingEvent, space: PresentSpace?) {
    guard let space = space else {
        logError("missing space for logging: \(constant)")
        return
    }
    logEvent(constant, stringProperties: [LoggingKey.space_id: space.rawValue])
}

public func logEvent(_ constant: LoggingEvent, stringProperties: [LoggingKey: [String]]) {
    logEvent(type: .metric, constant.rawValue, properties: castProps(stringProperties))
}

/// Convenience that logs a LogType.action with string properties
public func logAction(_ description: String, _ properties: [String: String] = [:]) {
    logEvent(type: .action, description, properties: castProps(properties))
}

private func castProps(_ properties: [LoggingKey: LoggingValue]) -> [NSObject : AnyObject] {
    var outProps: [NSObject : AnyObject] = [:]
    for prop in properties { outProps[prop.key.rawValue as NSObject] = prop.value.rawValue as AnyObject }
    return outProps
}
// TODO: This is not working (or has stopped working) in Amplitude.
// TODO: It's unclear whether arrays are supported.
private func castProps(_ properties: [LoggingKey: [String]]) -> [NSObject : AnyObject] {
    var outProps: [NSObject : AnyObject] = [:]
    for prop in properties { outProps[prop.key.rawValue as NSObject] = prop.value as AnyObject }
    return outProps
}
private func castProps(_ properties: [LoggingKey: String]) -> [NSObject : AnyObject] {
    var outProps: [NSObject : AnyObject] = [:]
    for prop in properties { outProps[prop.key.rawValue as NSObject] = prop.value as AnyObject }
    return outProps
}
private func castProps(_ properties: [String: String]) -> [NSObject : AnyObject] {
    var outProps: [NSObject : AnyObject] = [:]
    for prop in properties { outProps[prop.key as NSObject] = prop.value as AnyObject }
    return outProps
}

/// Relays errors to all consumers. Log data must not have PII.
public func logError(_ description: String, properties: [NSObject : AnyObject] = [:]) {
    #if DEBUG
        do {
            throw NSError(domain: "ErrorLog", code: 0, userInfo: nil)
            
        } catch {
            // Nothing to do here. We're only throwing to get your attention. Look up the stack!
        }
    #endif
    
    logEvent(type: .error, description, properties: properties)
}

/// Relays log events to internal-only consumers. Log data never leaves device: may contain PII.
public func logDebug(type: ARKLogType = .default, _ description: String, properties: [NSObject : AnyObject] = [:])
{
    let prefix=""
    //let prefix="~~\(AppDelegate.sinceLaunch) "
    // The lack of the sentinel user info attribute prevents the log to be published externally.
    log(prefix + description, type: type, userInfo: properties)
}
