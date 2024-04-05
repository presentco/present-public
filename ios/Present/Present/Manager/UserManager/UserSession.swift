//
//  UserSession.swift
//  Present
//
//  Created by Patrick Niemeyer on 10/3/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import Valet
import PresentProtos

/// Manage state related to the current user associated with this client.
// TODO: Consolidate all UserDefaults calls into this and scope them with our namespace.
/// A lightweight wrapper for keychain and UserDefaults, scoped by a namespace to allow partitioning user info per server.
public final class UserSession
{
    // MARK: Properties
    public let identifier: String

    private let keychain = VALValet(sharedAccessGroupIdentifier: UserSession.sharedAccessGroupIdentifier, accessibility: .afterFirstUnlock)!

    // MARK: Private Static Properties
    
    private static let sharedAccessGroupIdentifier = "UserSession"
    
    // MARK: Initialization

    /// parameter namespace - scope all user session data by this namespace identifier.
    public init(namespace: String) {
        self.identifier = namespace
        
        guard keychain.canAccessKeychain() else {
            // If we can't access the keychain, then we've been woken up after a reboot and before the device has been unlock.
            // In this state we have no access to our own identity, and can not proceed. Crash.
            fatalError()
        }
        
        deleteKeychainDataIfApplicationHasBeenDeletedAndReinstalled()
    }
    
    // MARK: Keychain Stored Properties
    
    public var clientToken: PresentUUID {
        let key = scopedKey(for: .clientToken)
        let clientToken: PresentUUID
        
        if let persistedClientTokenString = keychain.string(forKey: key) {
            clientToken = PresentUUID(uuidString: persistedClientTokenString)
            
        } else {
            clientToken = PresentUUID()
            keychain.setString(clientToken.uuidString, forKey: key)
        }
        
        return clientToken
    }

    public var lastKnownAuthorizationStatus: Present.Authorization.NextStep? {
        get {
            let key = scopedKey(for: .lastKnownAuthStatus)
            if let auth = keychain.string(forKey: key) {
                do {
                    return try Present.Authorization.NextStep.fromString(str: auth)
                } catch {
                    logError("Unknown auth value in keychain: \(auth)")
                    keychain.removeObject(forKey: key)
                    return nil
                }
            } else {
                // no auth stored, check legacy users
                return hasLinkedFacebookLegacy ? .proceed : nil
            }
        }
        set {
            let key = scopedKey(for: .lastKnownAuthStatus)
            if let auth = newValue {
                // Note: fromString/toString here (not description)
                keychain.setString(auth.toString(), forKey: key)
            } else {
                keychain.removeObject(forKey: key)
            }
        }
    }

    // Deprecated.
    private var hasLinkedFacebookLegacy: Bool {
        get {
            return keychain.containsObject(forKey: scopedKey(for: .hasLinkedFacebookSentinel))
        }
        set {
            let key = scopedKey(for: .hasLinkedFacebookSentinel)
            if newValue {
                keychain.setString("loggedIn-v0.1.2", forKey: key)
            } else {
                keychain.removeObject(forKey: key)
            }
        }
    }

    /// Check if we have a persisted user profile (thread-safe)
    public var hasPersistedUserProfile: Bool {
        /// Note: VALValet claims to be thread-safe
        return keychain.containsObject(forKey: scopedKey(for: .userProfile))
    }

    /// Get the persisted user profile if any (thread-safe)
    public var persistedUserProfile: Present.UserProfile? {
        get {
            /// Note: VALValet claims to be thread-safe
            if let persistedUserProfileData = keychain.object(forKey: scopedKey(for: .userProfile)),
                let persistedUserProfile = try? Present.UserProfile.getBuilder().mergeFrom(data: persistedUserProfileData).build() {
                return persistedUserProfile
            } else {
                return nil
            }
        }
        
        set {
            let key = scopedKey(for: .userProfile)
            if let persistedUserProfile = newValue {
                keychain.setObject(persistedUserProfile.data(), forKey: key)
            } else {
                keychain.removeObject(forKey: key)
            }
        }
    }
    
    public var hasAgreedToTermsOfService: Bool {
        get {
            return keychain.containsObject(forKey: scopedKey(for: .areTermsOfServiceApprovedSentinel))
        }
        
        set {
            let key = scopedKey(for: .areTermsOfServiceApprovedSentinel)
            if newValue {
                keychain.setString("termsOfServiceApproved-v0.1.2", forKey: key)
            } else {
                keychain.removeObject(forKey: key)
            }
        }
    }
    
    public var hasConfirmedFullName: Bool {
        get {
            return keychain.containsObject(forKey: scopedKey(for: .hasConfirmedFullName))
        }
        
        set {
            let key = scopedKey(for: .hasConfirmedFullName)
            if newValue {
                keychain.setString("confirmedFullName-v0.1.2", forKey: key)
            } else {
                keychain.removeObject(forKey: key)
            }
        }
    }
    
    public var fullName: FullName? {
        get {
            guard let persistedFullNameData = keychain.object(forKey: scopedKey(for: .fullName)) else {
                return nil
            }
            
            return FullName(data: persistedFullNameData)
        }
        
        set {
            let key = scopedKey(for: .fullName)
            if let fullName = newValue {
                keychain.setObject(fullName.asData, forKey: key)
                
            } else {
                keychain.removeObject(forKey: key)
            }
        }
    }
    
    public var personalDescription: String? {
        get {
            return keychain.string(forKey: scopedKey(for: .personalDescription))
        }
        
        set {
            let key = scopedKey(for: .personalDescription)
            if let personalDescription = newValue {
                keychain.setString(personalDescription, forKey: key)
                
            } else {
                keychain.removeObject(forKey: key)
            }

        }
    }
    
    public var interests: [GroupCategory]? {
        get {
            guard let interestsData = keychain.object(forKey: scopedKey(for: .interests)),
                let interestsArray = NSKeyedUnarchiver.unarchiveObject(with: interestsData) as? NSArray else {
                return nil
            }
            let interestStrings = interestsArray.compactMap { value->String? in
                guard let interest = value as? String else { return nil }
                return interest
            }
            return GroupCategories.from(strings: interestStrings)
        }
        
        set {
            let key = scopedKey(for: .interests)
            if let interests = newValue {
                let interestsData = NSKeyedArchiver.archivedData(withRootObject: interests.map { $0.rawValue } as NSArray)
                keychain.setObject(interestsData, forKey: key)
                
            } else {
                keychain.removeObject(forKey: key)
            }
        }
    }

    public var hasSetUserPhoto: Bool {
        get {
            return keychain.containsObject(forKey: scopedKey(for: .hasSetUserPhoto))
        }
        
        set {
            let key = scopedKey(for: .hasSetUserPhoto)
            if newValue {
                keychain.setString("sentinel-v0.1.2", forKey: key)
            } else {
                keychain.removeObject(forKey: key)
            }
        }
    }
    
    // MARK: UserDefaults Stored Properties
    
    /*
     // TODO: Would this be better as json?
    public class UserSettings: JsonConvertible
    {
        public weak var userSession: UserSession?
        
        /// The user indicated to hide women only content
        public var hideWomenOnly: Date? {
            didSet {
                userSession?.userSettings = self
            }
        }
        
        private enum CodingKeys: String, CodingKey {
            case hideWomenOnly
        }
    }
    
    /// Get the persisted user settings
    public var userSettings: UserSettings {
        get {
            let json = userDefaults.string(forKey: scopedKey(for: .userSettings)) ?? .emptyJson
            return UserSettings(jsonString: json).unwrappedOrFatal()
        }
        set {
            userDefaults.set(newValue.toJson().unwrappedOrFatal(), forKey: scopedKey(for: .userSettings))
        }
    }*/
    
    public var hideWomenOnly: Date? {
        get {
            return userDefaults.object(forKey: scopedKey(for: .hideWomenOnly)) as? Date
        }
        set {
            userDefaults.set(newValue, forKey: scopedKey(for: .hideWomenOnly))
        }
    }
    
    public var lastReadActivity: Date? {
        get {
            return userDefaults.object(forKey: scopedKey(for: .lastReadActivity)) as? Date
        }
        set {
            userDefaults.set(newValue, forKey: scopedKey(for: .lastReadActivity))
        }
    }
    
    public var hasApprovedContactsAccess: Bool {
        get {
            return userDefaults.bool(forKey: scopedKey(for: .hasApprovedContactsAccess)) // false if never set
        }
        set {
            userDefaults.set(newValue, forKey: scopedKey(for: .hasApprovedContactsAccess))
        }
    }
    public var lastContactsSync: Date? {
        get {
            return userDefaults.object(forKey: scopedKey(for: .lastContactsSync)) as? Date
        }
        set {
            userDefaults.set(newValue, forKey: scopedKey(for: .lastContactsSync))
        }
    }
    
    public var selectedLocation: Present.City? {
        get {
            if let locationData = userDefaults.object(forKey: scopedKey(for: .selectedLocation)) as? Data,
                let location = try? Present.City.getBuilder().mergeFrom(data: locationData).build() {
                return location
            } else {
                return nil
            }
        }
        set {
            if let value = newValue {
                userDefaults.set(value.data(), forKey: scopedKey(for: .selectedLocation))
            } else {
                userDefaults.removeObject(forKey: scopedKey(for: .selectedLocation))
            }
        }
    }

    public var selectedSpace: Present.SpaceResponse? {
        get {
            if let spaceData = userDefaults.object(forKey: scopedKey(for: .selectedSpace)) as? Data,
               let space = try? Present.SpaceResponse.getBuilder().mergeFrom(data: spaceData).build() {
                return space
            } else {
                return nil
            }
        }
        set {
            if let value = newValue {
                userDefaults.set(value.data(), forKey: scopedKey(for: .selectedSpace))
            } else {
                userDefaults.removeObject(forKey: scopedKey(for: .selectedSpace))
            }
        }
    }

    public var isFirstLaunch: Bool {
        get { return !isPastFirstLaunch }
        set {
            isPastFirstLaunch = !newValue
        }
    }
    
    public var isPastFirstLaunch: Bool {
        get {
            return keychain.containsObject(forKey: scopedKey(for: .pastFirstLaunch))
        }
        set {
            let key = scopedKey(for: .pastFirstLaunch)
            if newValue {
                keychain.setString("pastFirstLaunch", forKey: key)
            } else {
                keychain.removeObject(forKey: key)
            }
        }
    }

    /// Debug option
    public var overrideHomeUrl: String? {
        get {
            return userDefaults.object(forKey: scopedKey(for: .overrideHomeUrl)) as? String
        }
        set {
            if let value = newValue {
                userDefaults.set(value, forKey: scopedKey(for: .overrideHomeUrl))
            } else {
                userDefaults.removeObject(forKey: scopedKey(for: .overrideHomeUrl))
            }
        }
    }
    
    /// Debug option
    public var nearbyJoinedCirclesOnly: Bool {
        get {
            return userDefaults.bool(forKey: scopedKey(for: .nearbyJoinedCirclesOnly)) // false if never set
        }
        set {
            userDefaults.set(newValue, forKey: scopedKey(for: .nearbyJoinedCirclesOnly))
        }
    }
    
    /// The current user's phone number captured during signup
    /// Note: stored in keychain for privacy
    public var phoneNumber: String? {
        get {
            return keychain.string(forKey: scopedKey(for: .phoneNumber))
        }
        
        set {
            let key = scopedKey(for: .phoneNumber)
            if let phoneNumber = newValue {
                keychain.setString(phoneNumber, forKey: key)
            } else {
                keychain.removeObject(forKey: key)
            }
        }
    }
    
    public var onboardingAddFriendsCompleted: Bool {
        get {
            return userDefaults.bool(forKey: scopedKey(for: .onboardingAddFriendsCompleted)) // false if never set
        }
        set {
            userDefaults.set(newValue, forKey: scopedKey(for: .onboardingAddFriendsCompleted))
        }
    }
    
    public var failedGroupMessages: FailedGroupMessagesCollection {
        get {
            if let json = keychain.string(forKey: scopedKey(for: .failedGroupMessages)),
                let collection = FailedGroupMessagesCollection(jsonString: json)
            {
                return collection
            }
            return FailedGroupMessagesCollection([])
        }
        
        set {
            let key = scopedKey(for: .failedGroupMessages)
            if let json = newValue.toJson() {
                keychain.setString(json, forKey: key)
            } else {
                logError("failed to save failed messages to json")
            }
        }
    }
    
    // MARK: Public Methods
    
    /// Debug-only method for resetting our login credentials.
    public func resetAllData() {
        deleteLoginData()
    }
    
    public func ensureAccessToKeychain() {
        if !keychain.canAccessKeychain() {
            fatalError("Unable to access keychain!")
        }
    }
    
    // MARK: Private Methods
    
    private func scopedKey(for key: Key) -> String {
        return key.rawValue.appending("-\(identifier)")
    }
    
    private var userDefaults : UserDefaults {
        return UserDefaults.standard
    }
    
    private func deleteKeychainDataIfApplicationHasBeenDeletedAndReinstalled() {
        let applicationDeletionCanaryKey = "applicationDeletionCanary"
        if !UserDefaults.standard.bool(forKey: applicationDeletionCanaryKey) {
            /*
             If we are logged in, but we do not have the application deletion canary in the keychain, then we know the user has deleted the app and re-installed it.
             When want the user to be logged out when they delete and install the app. So when we detect this state we should delete our login data.
             Note: Deleting an app and reinstalling does not clear the keychain, but it does clear user defaults.
             */
            deleteLoginData()
        }
        
        UserDefaults.standard.set(true, forKey: applicationDeletionCanaryKey)
    }
    
    private func deleteLoginData() {
        // clear keychain
        keychain.removeAllObjects()
        // clear UserDefaults
        if let bundle = Bundle.main.bundleIdentifier {
            UserDefaults.standard.removePersistentDomain(forName: bundle)
        }
    }
    
    // MARK: Private Enums
    
    private enum Key: String {
        case lastReadActivity = "lastReadActivity"
        case hideWomenOnly = "hideWomenOnly"
        case clientToken = "clientToken"
        case userProfile = "userProfile"
        case fullName = "fullName"
        case personalDescription = "personalDescription"
        case interests = "interests"
        case hasLinkedFacebookSentinel = "hasLoggedIn-v0.1.2"
        case hasLoggedIn = "hasLoggedIn-v0.1.5"
        case lastKnownAuthStatus = "lastKnownUserAuth-1.0"
        case areTermsOfServiceApprovedSentinel = "hasAgreedToTermsOfService-v0.1.2"
        case hasConfirmedFullName = "hasConfirmedFullName-v0.1.2"
        case hasSetUserPhoto = "hasSetUserPhoto-v0.1.2"
        case hasPromptedForUserNotificationPermission = "hasPromptedForUserNotificationPermission"
        case hasApprovedContactsAccess = "hasApprovedContactsAccess"
        case lastContactsSync = "lastContactsSync"
        case selectedLocation = "selectedLocation"
        case selectedSpace = "selectedSpace"
        case overrideHomeUrl = "overrideHomeUrl"
        case nearbyJoinedCirclesOnly = "nearbyJoinedCirclesOnly"
        case pastFirstLaunch = "pastFirstLaunch"
        case phoneNumber = "phoneNumber"
        case onboardingAddFriendsCompleted = "onboardingAddFriendsCompleted"
        case failedGroupMessages = "failedGroupMessages"
    }
}
