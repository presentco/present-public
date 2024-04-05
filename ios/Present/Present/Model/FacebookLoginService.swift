//
//  FacebookLoginService.swift
//  Present
//
//  Created by Dan Federman on 6/7/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import FacebookCore
import FBSDKCoreKit
import FBSDKLoginKit
import Foundation


public protocol FacebookLoginService {
    
    func login(from viewController: UIViewController, completionHandler: @escaping (FBSDKAccessToken?) -> Void)
    
}


public final class DefaultFacebookLoginService: FacebookLoginService {
    
    // MARK: Private Static Properties
    
    public static let desiredReadPermissions = ["public_profile", "user_friends", "email"]
    public static let requiredReadPermissions = ["public_profile"]

    // MARK: Private Properties

    private let desiredPermissions: [String]
    private let requiredPermissions: [String]

    // MARK: Initialization
    
    public init(
        desiredPermissions: [String] = DefaultFacebookLoginService.desiredReadPermissions,
        requiredPermissions: [String] = DefaultFacebookLoginService.requiredReadPermissions
    )
    {
        self.desiredPermissions = desiredPermissions
        self.requiredPermissions = requiredPermissions
    }
    
    // MARK: Public Methods
    
    public func login(from viewController: UIViewController, completionHandler: @escaping (FBSDKAccessToken?) -> Void)
    {
        logDebug("FacebookLoginService: login")
        guard FBSDKAccessToken.current() == nil else
        {
            logDebug("FacebookLoginService: current token not nil, refreshing")
            FBSDKAccessToken.refreshCurrentAccessToken { _,_,_  in
                if let permissions = FBSDKAccessToken.current()?.permissions as? Set<String>,
                    self.requiredPermissionsAre(subsetOf: permissions),
                    let currentToken = FBSDKAccessToken.current()
                {
                    logDebug("FacebookLoginService: refresh token returned permissions")
                    completionHandler(currentToken)
                } else {
                    // We were not given proper permissions. Log out.
                    logDebug("FacebookLoginService: refresh token failed, logging out.")
                    FBSDKLoginManager().logOut()
                    // Try again.
                    self.login(from: viewController, completionHandler: completionHandler)
                }
            }
            return
        }
        
        logDebug("FacebookLoginService: performing FBSDK login.")
        let loginManager = FBSDKLoginManager()
        loginManager.logIn(
            // Ask for as many permissions as we can without going through Facebook review.
            // For more information: https://developers.facebook.com/docs/facebook-login/permissions/
            withReadPermissions: desiredPermissions,
            from: viewController)
        { (loginResult, error) in
                /*
                #if TARGET_OS_SIMULATOR
                    logDebug("FacebookLoginService: We are in the simulator so returning fixed token.")
                    // Facebook SDK doesn't work well on the simulator – it always tells us the user canceled login.
                    // So use a hardcoded access token stolen from a device.
                    completionHandler(FBSDKAccessToken(tokenString: "",
                                                       permissions: self.desiredPermissions,
                                                       declinedPermissions: [],
                                                       appID: "",
                                                       userID: "",
                                                       expirationDate: Date().addingTimeInterval(1000),
                                                       refreshDate: Date().addingTimeInterval(500))
                    )
                    return
                #endif
                */
                
                guard let loginToken = loginResult?.token,
                    let grantedPermissions = loginResult?.grantedPermissions as? Set<String>,
                    self.requiredPermissionsAre(subsetOf: grantedPermissions)
                    else
                {
                    logDebug("FacebookLoginService: FBSDK login returned incomplete permissions")
                    completionHandler(nil)
                    return
                }
            
            logDebug("FacebookLoginService: FBSDK login returned valid permissions: tokenString=\(loginToken.tokenString), expiration=\(loginToken.expirationDate) userId=\(loginToken.userID) appId=\(loginToken.appID)")
                completionHandler(loginToken)
        }
    }
    
    // MARK: Private Methods
    
    private func requiredPermissionsAre(subsetOf grantedPermissions: Set<String>) -> Bool {
        return Set(requiredPermissions).isSubset(of: grantedPermissions)
    }
    
}
