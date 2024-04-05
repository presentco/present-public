//
//  UserManager.swift
//  Present
//
//  Created by Patrick Niemeyer on 12/31/15.
//  Copyright © 2015 Present Company. All rights reserved.
//

import Foundation
import PresentProtos
import RxSwift

/// Manage state and services related to the current user associated with this client.
/// This includes all persisted user state including client token, status of individual steps in on-boarding,
/// and latest UserProfile from server.
public final class UserManager: PersonPostingService
{
    public static var shared : UserManager!

    // MARK: Private Properties

    private let service: PresentService
    public let userSession: UserSession

    // TODO: We should try to remove these
    public let activityManager: ActivityManager
    public let groupManager: GroupManager

    // Promise for awaiting user profile data.
    let userProfilePromise: UserProfilePromise = UserProfilePromise()

    private let userAuthorizedToProceedToAppQueue = PromiseQueue()

    let spacesAvailable = PromisedValue<[Present.SpaceResponse]>()
    
    /// Set the client side current and stored user profile for the user
    /// @see putUserProfile() for server updates.
    /// @see executeWhenUserProfileAvailable
    public var userProfile: Present.UserProfile? {
        didSet {
            if let userProfile = userProfile {
                didSetUserProfile(userProfile: userProfile)
            }
        }
    }
    // Called from the user profile setter and init to impl side effects of setting the user profile.
    private func didSetUserProfile(userProfile: Present.UserProfile) {
        userSession.persistedUserProfile = userProfile // Persist the user profile
        if let userName = userProfile.name {
            self.userSession.fullName = FullName(userNameProto: userName)
        }
        userProfilePromise.fulfill(with: userProfile)
        me.value = UserProfilePerson(userProfile: userProfile)
    }

    // Updated by didSetUserProfile() with the persisted and live profile as available
    public lazy var me = ObservableValue<UserProfilePerson>()

    public func isMe(_ person: Person) -> Bool {
        return isMe(userId: person.id)
    }
    
    public func isMe(userId: String) -> Bool {
        guard let myId = self.me.value?.id else {
            // If there is no profile or persisted profile we do not yet have an identity
            return false
        }
        return userId == myId
    }
    
    public lazy var isWoman = me.observable.map {
            $0.userProfile.gender ==  .woman
        }

    // MARK: Initialization
    
    public init(userSession: UserSession, activityManager: ActivityManager, groupManager: GroupManager, service: PresentService)
    {
        self.userSession = userSession
        self.activityManager = activityManager
        self.groupManager = groupManager
        self.service = service

        // Publish the persisted user profile
        if let userProfile = userSession.persistedUserProfile {
            self.userProfile = userProfile
            didSetUserProfile(userProfile: userProfile) // init time, didSet won't be called.
        }

        // Publish authorized to proceed status
        if authorizedToProceedToApp {
            logDebug("Execute when user is authorized to proceed to the app")
            userAuthorizedToProceedToAppQueue.fulfill()
        }

        // Note: these could be moved into the respective manager inits
        whenUserAuthorizedToProceedToApp { [weak self] in

            // This will fetch the current user profile, however any startup items predicated on user profile
            // will have been fulfilled already by the last persisted version.
            //self?.fetchSpaces()
            self?.fetchUserProfile()
            self?.updateServicesAfterShortDelay()
        }

        activityManager.userManager = self
        UserManager.shared = self
    }

    public func updateServicesAfterShortDelay() {
        DispatchQueue.main.asyncAfter(milliseconds: 500) {
            logDebug("UserManager: update joined groups after short delay")
            self.groupManager.refreshJoinedGroups()
        }
    }
    
    // MARK: Public Properties

    // Return the client token from persisted storage.  (thread-safe)
    public var clientToken: PresentUUID {
        return userSession.clientToken // always exists (or is created)
    }

    // Begin local user auth state
    
    /// Receive an updated authorization response from the server.
    /// This method will update the persisted authorization status and user profile.
    public func setAuthorization(authResponse: Present.AuthorizationResponse)
    {
        // Don't override external authorization with auth responses from the server
        // TODO: Find a better way to avoid the race condition.
        guard externalAuthorization == nil else { return }
        _setAuthorization(authResponse: authResponse)
    }
    
    /// Update the authorization received by verifying an external code.
    /// (e.g. opening the app with a link from SMS.)
    /// This method will update the persisted authorization status and user profile.
    public func setExternalAuthorization(authResponse: Present.AuthorizationResponse) {
        _setAuthorization(authResponse: authResponse)
    }
    
    /// Override authorization with the specified external authorization
    private var externalAuthorization: Present.AuthorizationResponse? = nil

    private func _setAuthorization(authResponse: Present.AuthorizationResponse) {
        self.userProfile = authResponse.userProfile
        self.authorizationStatus.value = authResponse.authorization.nextStep
    }

    /// Get or update the persisted user auth status, initialized to .signUp on first launch.
    /// If changing to proceed fulfill the user should proceed queue.
    /// This should only be updated via userManager setAuthorization() and setExternalAuthorization()
    // TODO: We should create a way to make the set functionality of ObservableValue private.
    public lazy var authorizationStatus = ObservableValue<Present.Authorization.NextStep>()
        .initialValue(self.userSession.lastKnownAuthorizationStatus ?? Present.Authorization.NextStep.authenticate)
        .setter { newValue, apply in
            apply(newValue)
            logDebug("UserManager: persist and publish auth status: \(String(describing: newValue))")
            self.userSession.lastKnownAuthorizationStatus = newValue
            if newValue == .proceed {
                self.userAuthorizedToProceedToAppQueue.fulfill()
            }
        }
    // TESTING LOGIN: Ignore persisted and server auth status and return fixed value
    //public lazy var authorizationStatus = FixedObservableValue.create(with: Present.Authorization.NextStep.authenticate)
    
    // TODO: This should be observable and subsume whenUserAuthorizedToProceedToApp
    /// Indicate true if the last persisted user auth status was to allow the user to proceed to use the app.
    /// This is used to optimistically allow the user entry but checked by the SyncStatusPoller in the background.
    public var authorizedToProceedToApp: Bool {
        return authorizationStatus.value == .proceed
    }
    
    /// The user has completed the AUTHENTICATE and SIGN_UP steps (as required) and been told by the *server*
    /// to PROCEED to the app. This state is cached locally and thereafter checked *asynchronously* at startup while
    /// the user is allowed to proceed optimistically to begin using the app.  The subsequent result may kick the user
    /// back to another state. After this state the user is valid for all types of requests.
    public func whenUserAuthorizedToProceedToApp(whenUserLoggedInBlock: @escaping () -> Void) {
        userAuthorizedToProceedToAppQueue.enqueue(block: whenUserLoggedInBlock)
    }

    /// Call completeSignup() and capture the authorization status
    public func completeSignup()->Single<Present.AuthorizationResponse>
    {
        return service.completeSignup()
            .onSuccess { authResponse in
                // Capture the authorization
                logEvent(.signup_complete)
                logDebug("UserManager: complete signup received authorization: \(authResponse)")
                self.setAuthorization(authResponse: authResponse)
            }
    }

    public var facebookLinked: Bool {
        return userProfile?.facebookLinked ?? false
    }
    
    public var phoneVerified: Bool {
        return userProfile?.phoneVerified ?? false
    }

    public var isBlocked: Bool {
        return authorizationStatus.value == Present.Authorization.NextStep.block
    }

    // MARK: Local user info state

    // The last user selected city or nil for nearby.
    public lazy var selectedLocation = makeOptionalObservable(path: \UserSession.selectedLocation)
    
    public lazy var hideWomenOnly = makeOptionalObservable(path: \UserSession.hideWomenOnly)
    
    public lazy var lastReadActivity = makeOptionalObservable(path: \UserSession.lastReadActivity)

    public var hasAgreedToTermsOfService: Bool {
        get {
            return userSession.hasAgreedToTermsOfService
        }
        set {
            userSession.hasAgreedToTermsOfService = newValue
        }
    }

    public var hasConfirmedFullName: Bool {
        get {
            return userSession.hasConfirmedFullName
        }
        
        set {
            userSession.hasConfirmedFullName = newValue
        }
    }

    public var fullName: FullName? {
        get {
            return userSession.fullName
        }
        
        set {
            userSession.fullName = newValue
            
            if let userProfileToUpdate = userProfile, let fullName = fullName {
                // Update our persisted profile with our new name.
                let updatedProfile = try! Present.UserProfile.getBuilder()
                    .mergeFrom(other: userProfileToUpdate)
                    .setName(fullName.asUserNameProto)
                    .build()
                
                userProfile = updatedProfile
            }
        }
    }
    
    public var personalDescription: String? {
        get {
            return userSession.personalDescription
        }
        
        set {
            userSession.personalDescription = newValue
            
            if let userProfileToUpdate = userProfile, let personalDescription = personalDescription {
                // Update our persisted profile with our new name.
                let updatedProfile = try! Present.UserProfile.getBuilder()
                    .mergeFrom(other: userProfileToUpdate)
                    .setBio(personalDescription)
                    .build()
                
                userProfile = updatedProfile
            }
        }
    }
    
    public var interests: [GroupCategory]? {
        get {
            return userSession.interests
        }
        
        set {
            userSession.interests = newValue
            
            if let userProfileToUpdate = userProfile, let interests = interests {
                // Update our persisted profile with our new name.
                let updatedProfile = try! Present.UserProfile.getBuilder()
                    .mergeFrom(other: userProfileToUpdate)
                    .setInterests(interests.map { $0.rawValue })
                    .build()
                
                userProfile = updatedProfile
            }
        }
    }

    public var hasSetUserPhoto: Bool {
        get {
            return userSession.hasSetUserPhoto
        }
        
        set {
            userSession.hasSetUserPhoto = newValue
        }
    }
    
    // End local user info state
    
    public var userIsAdmin: Bool {
        guard let userProfile = userProfile else { return false }
        return userProfile.isAdmin
    }
    
    // MARK: PersonPostingService
    
    public func blockUser(withToken userToken: String, completionHandler: @escaping (Response<UserDisplayableError, ()>) -> Void) {
        service.blockUser(withToken: userToken) { [weak self] (response) in
            switch response {
                case .success:
                    self?.activityManager.fetchActivity()
                    self?.groupManager.purgeAndRefreshLoadedGroups()
                case .error:
                    break
            }
            completionHandler(response)
        }
    }
    
    public func unblockUser(withToken userToken: String, completionHandler: @escaping (Response<UserDisplayableError, ()>) -> Void) {
        service.unblockUser(withToken: userToken) { [weak self] (response) in
            switch response {
            case .success:
                self?.groupManager.purgeAndRefreshLoadedGroups()

            case .error:
                completionHandler(response)
            }
        }
    }
    
    // MARK: Public Methods

    // TODO: Decide if we are going to migrate usages of this to whenUserAvailable -
    // TODO: The issue is that UserProfile contains things that aren't included in a general UserResponse
    // TODO: Presumably we are moving toward the Person object to make it observable for changes.
    // TODO: Maybe make this a completely different item or compose them...
    public func whenUserProfileAvailable(whenUserProfileAvailableBlock: @escaping (Present.UserProfile) -> Void) {
        userProfilePromise.userProfilePromise.then(block: whenUserProfileAvailableBlock)
    }
    public func whenUserAvailable(on queue: DispatchQueue = .main, block: @escaping (Person) -> Void) {
        userProfilePromise.userPromise.then(on: queue, block: block)
    }

    public func linkFacebook(
        withFacebookAccessToken facebookAccessToken: String,
        completionHandler: @escaping (Response<UserDisplayableError, Present.AuthorizationResponse>) -> Void)
    {
        service.linkFacebook(withFacebookAccessToken: facebookAccessToken) { [weak self] (response) in
            guard let strongSelf = self else { return }
            
            switch response {
            case .error:
                // Nothing to do here.
                break
                
            case let .success(loginResponse):
                strongSelf.userProfile = loginResponse.userProfile

                if loginResponse.hasUserProfile && loginResponse.userProfile.hasName {
                    strongSelf.fullName = FullName(userNameProto: loginResponse.userProfile.name)
                }
            }
            
            completionHandler(response)
        }
    }
    
    public func setProfilePhoto(
        to profileImage: UIImage, errorHandler: @escaping (UserDisplayableError) -> Void, successHandler: @escaping () -> Void)
    {
        service.setProfilePhoto(to: profileImage, progressListener: nil) { [weak self] (response) in
            guard let strongSelf = self else { return }
            
            switch response
            {
            case let .error(errorText):
                errorHandler(errorText)
                
            case let .success(storageURL):
                ImageManager.shared.cacheImage(profileImage, forUrl: storageURL.absoluteString)
                strongSelf.hasSetUserPhoto = true
                
                if let userProfileToUpdate = strongSelf.userProfile {
                    // Update our persisted profile with our new name.
                    let updatedProfile = try! Present.UserProfile.getBuilder()
                        .mergeFrom(other: userProfileToUpdate)
                        .setPhoto(storageURL.absoluteString)
                        .build()
                    
                    strongSelf.userProfile = updatedProfile
                }
                
                // TODO: This Dan's hack to update the avatar photos
                // TODO: Go back to pro-actively updating the image cache and get rid of this.
                strongSelf.whenUserAuthorizedToProceedToApp {
                    strongSelf.groupManager.refreshNearbyGroups()
                    strongSelf.groupManager.refreshJoinedGroups()
                }
                
                successHandler()
            }
        }
    }
    
    public func putUserProfile(completionHandler: @escaping (Response<UserDisplayableError, ()>) -> Void) {
        let userProfileBuilder = Present.UserProfileRequest.getBuilder()
        
        if let personalDescription = personalDescription {
            userProfileBuilder.bio = personalDescription
        }
        
        if let fullName = fullName {
            userProfileBuilder.name = fullName.asUserNameProto
        }
        
        if let interests = interests {
            userProfileBuilder.interests = interests.map { $0.rawValue }
        }
        
        // TODO:dan Also add the photo here if it hasn't been uploaded before.
        
        service.putUserProfile(userProfile: userProfileBuilder, completionHandler: completionHandler)
    }
    
    public func setNotificationsEnabled(forJoinedGroups notificationsEnabledForJoinedGroups: Bool) {
        service.setNotificationsEnabled(forJoinedGroups: notificationsEnabledForJoinedGroups) { [weak self] (response) in
            switch response {
            case .error:
                break
            case .success:
                self?.fetchUserProfile()
            }
        }
    }
    
    public func setNotificationsEnabled(forUsersJoiningOwnedGroups notificationsEnabledForUsersJoiningOwnedGroups: Bool) {
        service.setNotificationsEnabled(forUsersJoiningOwnedGroups: notificationsEnabledForUsersJoiningOwnedGroups) { [weak self] (response) in
            switch response {
            case .error:
                break
            case .success:
                self?.fetchUserProfile()
            }
        }
    }

    public func setNotificationsEnabled(
            forJoinedGroups notificationsEnabledForJoinedGroups: Bool? = nil,
            forUsersJoiningOwnedGroups notificationsEnabledForUsersJoiningOwnedGroups: Bool? = nil)
    {
        service.setNotificationsEnabled(forJoinedGroups: notificationsEnabledForJoinedGroups,
                forUsersJoiningOwnedGroups: notificationsEnabledForUsersJoiningOwnedGroups) { [weak self] (response) in
            switch response {
                case .error:
                    break
                case .success:
                    self?.fetchUserProfile()
            }
        }
    }

    /// Store an updated user profile received from the server.
    public func storeUserProfile(userProfile: Present.UserProfile) {
        userSession.persistedUserProfile = userProfile
    }

    // MARK: Public Debug Methods
    
    public func resetLogin() {
        userSession.resetAllData()
    }
    
    // MARK: Private Methods
    
    private func fetchUserProfile() {
        guard authorizedToProceedToApp else { return }
        
        service.getUserProfile { (response) in
            switch response {
            case .error:
                // TODO:(dan) Error handling! Inform the user?
                break
                
            case let .success(profile):
                self.userProfile = profile
            }
        }
    }

    // We currently have both a UserProfile and a Person with equivalent info.
    // We should complete the migration to Person or revert to UserProfile everywhere.
    // For the interim this is a simple wrapper to insure that they are handled uniformly.
    struct UserProfilePromise
    {
        // Provide the server UserProfile model once when available.
        // Note: Fulfilled by UserManager init() from persisted profile if available (common case)
        // and upon any update to profile caused by setting/updating properties on userManager.
        let userProfilePromise = PromisedValue<Present.UserProfile>()
        
        // Provide the Person model version of user profile data including: the user id, name, photo, bio.
        // Note: Fulfilled whenever userProfilePromise is set.
        let userPromise = PromisedValue<Person>()
        
        // Fulfill both promises
        func fulfill(with value: Present.UserProfile?)
        {
            if let userProfile = value {
                userProfilePromise.fulfill(with: userProfile)
                if let me = Person(userProfile: userProfile) {
                    userPromise.fulfill(with: me)
                }
            } else {
                userProfilePromise.resetValue()
                userPromise.resetValue()
            }
        }
    }
    
    /// Make an ObservableValue of an optional type that wraps the specified read/write UserSession var.
    /// The stream may include nils.
    func makeOptionalObservable<E>(path: ReferenceWritableKeyPath<UserSession, E?>) -> OptionalObservableValue<E>
    {
        return OptionalObservableValue<E>()
            .initialValue(userSession[keyPath: path])
            .setter { newValue, apply in
                apply(newValue)
                self.userSession[keyPath: path] = newValue
        }
    }
    
}


