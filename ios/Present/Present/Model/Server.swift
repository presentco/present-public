//
//  Server.swift
//  Present
//
//  Created by Dan Federman on 4/19/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import Valet

public class Server
{
    // MARK: Public Static Properties
    
    public static let api = Server.Endpoint(production: Server.apiProductionEndpoint,
                                            staging: URL(string: "xxx")!,
                                            local: URL(string: "yyy:8081/")!)

    // MARK: Public Static Methods
    
    public static func setDesiredEndpoint(to description: Endpoint.Description) {
        //guard UserManager.shared.isAdmin else { return }
        api.setDesiredEndpoint(to: description)
    }
    
    // MARK: Private Static Properties
    
    private static let apiProductionEndpoint = URL(string: "https://api-dot-present-production.appspot.com/")!
    
    // TODO:(dan) Get a real live production endpoint.
    private static let liveProductionEndpoint = URL(string: "wss://live.present.co")!
    
    // MARK: Public Classes
    
    public class Endpoint {
        
        // MARK: Public Static Properties
        
        public static var current: Description
        {
            guard let persistedEndpointDescription = keychain.string(forKey: endpointKey),
                  let endpointDescription = Description(rawValue: persistedEndpointDescription)
            else {
                #if APP_STORE || BETA
                    // Default to Production for external Beta builds.
                    return .production
                #else
                    // Default to Staging for internal Debug builds.
                    return .staging
                #endif
            }

            return endpointDescription
        }
        
        // MARK: Private Static Properties
        
        private static let keychain = VALValet(sharedAccessGroupIdentifier: "ServerEndpoint", accessibility: .afterFirstUnlock)!
        private static let endpointKey = "ServerEndpointDescription"
        
        // MARK: Initialization
        
        public required init(production: URL, staging: URL, local: URL) {
            endpoint = {
                #if LOCAL_SERVER
                    return local
                #else
                    switch Endpoint.current {
                    case .production:
                        return production
                    case .staging:
                        return staging
                    }
                #endif
            }()
        }
        
        // MARK: Public Properties
        
        public let endpoint: URL
        
        // MARK: Public Methods
        
        public func setDesiredEndpoint(to description: Description) {
            Endpoint.keychain.setString(description.rawValue, forKey: Endpoint.endpointKey)
        }
        
        // MARK: Public Enum
        
        public enum Description: String {
            case production = "Production"
            case staging = "Staging"
        }
    }
}
