//
//  ContactsManager.swift
//  Present
//
//  Created by Patrick Niemeyer on 10/3/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import PresentProtos
import RxSwift
import Contacts

public class ContactsManager: LegacyApplicationServices
{
    let contactImporter = ContactImporter()
    
    public struct Rx {
        public let authorizationStatus = ReplaySubject<CNAuthorizationStatus>.create()
    }
    public let rx = Rx()
    
    public init()
    {
        // Publish the auth status
        updateAuthorizationStatus()
        
        // This will fire on subsequent foregrounding of the app
        NotificationCenter.default.addObserver(self, selector: #selector(applicationWillEnterForeground), name: NSNotification.Name.UIApplicationWillEnterForeground, object: nil)
    }
    
    /// Get contacts with individual phone numbers, annotated with Present user info for members
    public func getPhoneContacts()->Single<[PhoneContactPerson]>
    {
        return getContacts()
            // De-duplicate contacts with identical names (which apparently can happen)
            // by merging them
            .map { (contacts:[Contact])->[Contact] in
                let contactByFullnameMap = Dictionary(grouping: contacts, by: { $0.nameData.fullName })
                return contactByFullnameMap.map { (_, contacts: [Contact])->Contact in
                    var contact = contacts.first!
                    contact.phoneNumbers = Array<PhoneNumber>(Set<PhoneNumber>(contacts.flatMap { $0.phoneNumbers }))
                    return contact
                }
            }
            // Split into separate phone contacts, each containing the original Contact info
            // with an individual target phone number.
            .map { (contacts:[Contact])->[PhoneContact] in
                contacts
                    .flatMap { contact in contact.phoneNumbers.map {
                        PhoneContact(phoneNumber: $0, contact: contact)
                        // Remove toll free numbers (businesses)
                        }.filter {
                            !$0.phoneNumber.isTollFree()
                        }
                }
            }
            // Annotate each with any user information from the server found for the phone number.
            .flatMap { (phoneContacts:[PhoneContact])->Single<[PhoneContactPerson]> in
                return self.applicationServices.service
                    .addContacts(contacts: phoneContacts)
                    .map { (phoneUsers:[Present.PhoneUserResponse])->[PhoneContactPerson] in
                        
                        // Make a map of the found users by phone number
                        let phoneUserMap: Dictionary<String,Present.UserResponse> = Dictionary(uniqueKeysWithValues: phoneUsers.map { ($0.phoneNumber, $0.user) } )
                        
                        return phoneContacts
                        // Add user info to the corresponding contacts
                        .map {
                            if let user = phoneUserMap[$0.phoneNumber.serverString] {
                                let person = self.applicationServices.personManager.getPerson(forProto: user)
                                return PhoneContactPerson( phoneContact: $0, user: person)
                            } else {
                                return PhoneContactPerson(phoneContact: $0, user: nil)
                            }
                        }
                        // Re-consolidate contacts for matched users:
                        // If any of the underlying contact's phone numbers matched a user
                        // remove the remaining unmatched phones for that contact.
                        .filter {
                            let phoneNumbers = $0.phoneContact.contact.phoneNumbers
                            // Did any of the phone numbers match a user?
                            let contactPhoneMatchedUser = Set(phoneUserMap.keys)
                               .intersection(phoneNumbers.map { $0.serverString }).count > 0
                            // If no phones matched a user keep them all
                            if !contactPhoneMatchedUser { return true }
                            // A phone matched so only keep the one with the associated user
                            return $0.user != nil
                        }
                        // Remove contacts corresponding to the current user
                        .filter { !($0.user?.isMe ?? false) }
                        // Sort using the FullName default ordering
                        .sorted { $0.nameData < $1.nameData }
                    }
            }
            // TESTING
            //.delaySubscription(5.0, scheduler: MainScheduler.instance)
    }
    
    public func getContacts()->Single<[Contact]>
    {
        return Single.create { observer in
            self.contactImporter.requestAccess { authStatus in
                switch authStatus {
                    case .authorized:
                        self.contactImporter.importContacts { importResult in
                            switch(importResult) {
                            case let .contacts(contacts):
                                if let lastSyncTime = self.applicationServices.userManager.userSession.lastContactsSync {
                                    log("last sync contacts time: \(lastSyncTime.timeIntervalSinceNow)")
                                } else {
                                    log("never synced contacts")
                                }
                                observer(.success(contacts))
                            case let .permissionDenied(authorizationStatus):
                                logDebug("permission denied: \(authorizationStatus)")
                                observer(.error(PresentServiceError.clientError))
                            }
                        }
                    case .denied, .notDetermined, .restricted:
                        observer(.error(PresentServiceError.clientError))
                        break
                }
            }
            return Disposables.create()
        }
    }
}

// MARK: Authorization

public extension ContactsManager
{
    public var authorizationStatus: CNAuthorizationStatus {
        return CNContactStore.authorizationStatus(for: ContactImporter.entityType)
    }

    public func requestAccess(withCompletionHandler completionHandler: @escaping (CNAuthorizationStatus) -> Void) {
        contactImporter.requestAccess(withCompletionHandler: completionHandler)
    }
    
    // Prompt the contact access permissions based on the current iOS authorization status.
    // If never requested we ask iOS to prompt for permissions.  If previously denied or reverted
    // send the user to settings.
    func promptForContactPermissions(
        from viewController: UIViewController,
        completionHandler: @escaping (CNAuthorizationStatus) -> Void = { _ in },
        dismissedHandler: @escaping () -> Void = {})
    {
        // TODO: We don't call the completion handler in the case of sending the user to settings.
        // TODO: Maybe we should codify these possible outcomes and return that
        // TODO: the client - e.g. enum { authChanged, sentToSettings, dismissed }
        switch authorizationStatus
        {
            case .authorized:
                logError("Prompting for contacts perm even when already enabled.")
                // Nothing to do, already authorized
                updateAuthorizationStatus()
                completionHandler(.authorized)

            case .restricted, .denied:
                logEvent(type: .view, "Prompt Accept Contacts Permission -- Previously Denied")
                
                let promptTitle = "Allow Notification Services"
                let promptMessage = "Contacts permissions were previously denied. Go to Settings to enable them."
                let notNowButtonTitle = "Not Now"
                let settingButtonTitle = "Settings"
                
                let alertController = UIAlertController(
                    title: promptTitle, message: promptMessage, preferredStyle: .alert)

                alertController.addAction(withTitle: settingButtonTitle) { _ in
                    logEvent(type: .tap, "App Accept Contacts Permission Via iOS Settings")
                    UIApplication.shared.open(URL(string: UIApplicationOpenSettingsURLString)!)
                }

                alertController.addAction(withTitle: notNowButtonTitle, style: .cancel) { _ in
                    logEvent(type: .tap, "App Deny Contacts Permission")
                    dismissedHandler()
                }

                viewController.present(alertController, animated: true)

            case .notDetermined:
                logEvent(type: .view, "Prompt Accept Contact Permission")
                requestAccess(withCompletionHandler: { (status) in
                    self.updateAuthorizationStatus()
                    completionHandler(status)
                })
        }
    }

    // MARK: notification listener
    
    private func updateAuthorizationStatus() {
        log("contacts manager auth status = \(authorizationStatus)")
        rx.authorizationStatus.onNext(authorizationStatus)
    }
    
    @objc
    private func applicationWillEnterForeground() {
        updateAuthorizationStatus()
    }
    
}
