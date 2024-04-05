//
//  AppStore.swift
//  Present
//
//  Created by Patrick Niemeyer on 8/1/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation

public final class PresentLinks
{
    public static let presentBase = "https://present.co"
    public static let presentBaseStaging = "https://staging.present.co"
    
    public static let presentAppBase = "https://app.present.co"
    public static let presentAppBaseStaging = "https://app.staging.present.co"

    public static let presentBaseUrl = URL(string: presentBase)!

    // Link generator: https://linkmaker.itunes.apple.com/en-us/details/1241060084?country=us&mediaType=ios_apps&term=present+inspiring+...
    public static let presentAppLink = "https://itunes.apple.com/us/app/id1241060084?mt=8"
    
    public static let presentAppShortLink = "https://appsto.re/us/0lr-jb.i"
    
    public static let presentSupportUrl = URL(string: "https://present.co/support")!
    public static let presentWhyFacebookUrl = URL(string: "https://present.co/whyfacebook")!
    
    // Use the user profile referrer invite url if available over this.
    public static let presentGenericInvite = presentBase
    
    public static let twitterFollow = "https://twitter.com/letsbepresent"
    public static let instagramFollow = "https://www.instagram.com/letsbepresent/"
    public static let facebookFollow = "https://www.facebook.com/letsbepresent"
    
    public static let joinsForCoinsInfo = "http://present.co/joins"
    
    public static let terms = "https://www.present.co/tos.html"
    public static let termsUrl = URL(string: terms)!
    public static let privacyPolicy = "https://present.co/privacy.html"
    public static let privacyPolicyUrl = URL(string: privacyPolicy)!
}
