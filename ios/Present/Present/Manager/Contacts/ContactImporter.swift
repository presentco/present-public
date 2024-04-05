//
//  ContactImporting.swift
//  Present
//
//  Created by Dan Federman on 1/2/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Contacts
import Foundation

public final class ContactImporter: ContactImporting
{
    // MARK: Private Static Properties
    
    static let entityType = CNEntityType.contacts
    
    private static let desiredContactKeys = [
        CNContactFormatter.descriptorForRequiredKeys(for: .fullName),
        CNContactEmailAddressesKey as CNKeyDescriptor,
        CNContactPhoneNumbersKey as CNKeyDescriptor,
        CNContactTypeKey as CNKeyDescriptor,
        CNContactImageDataKey as CNKeyDescriptor,
        CNContactImageDataAvailableKey as CNKeyDescriptor
    ]
    
    // MARK: Private Properties
    
    private let store = CNContactStore()
    private let serialQueue = DispatchQueue(label: "ContactImporter Serial Queue")

    private var importedContacts: [Contact]?

    // MARK: Initialization
    
    required public init(notificationCenter: NotificationCenter = .default) {
        notificationCenter.addObserver(self, selector: #selector(contactsDidChange), name: NSNotification.Name.CNContactStoreDidChange, object: nil)
    }
    
    // MARK: Public Methods
    
    public func requestAccess(withCompletionHandler completionHandler: @escaping (CNAuthorizationStatus) -> Void) {
        store.requestAccess(for: ContactImporter.entityType, completionHandler: { (accessGranted, error) in
            if accessGranted {
                completionHandler(.authorized)
            } else {
                completionHandler(CNContactStore.authorizationStatus(for: ContactImporter.entityType))
            }
        })
    }
    
    /// Imports contacts if access is authorized.
    /// - parameter completionHandler: Called once contacts have been imported. Executed on the main queue.
    /// Only 'person' type contacts are imported (not 'organization')
    /// Only contacts with non-empty names (as consolidated buy our Contact class full name)
    /// are returned.
    public func importContacts(withCompletionHandler completionHandler: @escaping (ContactImportResult) -> Void)
    {
        let authorizationStatus = CNContactStore.authorizationStatus(for: ContactImporter.entityType)
        guard case .authorized = authorizationStatus else {
            // Get onto the main queue to dispatch the completion handler.
            DispatchQueue.main.async {
                completionHandler(.permissionDenied(authorizationStatus))
            }
            return
        }
        
        // Get onto a background queue to avoid clogging the main queue.
        serialQueue.async
        {
            guard self.importedContacts == nil else {
                // We have already imported contacts.
                DispatchQueue.main.async {
                    completionHandler(.contacts(self.importedContacts!))
                }
                return
            }
            
            var unifiedContacts = [String:CNContact]()
            do {
                let contactContainers = try self.store.containers(matching: nil)
                let contactIdentifiers = contactContainers.map { $0.identifier }

                try contactIdentifiers.forEach {
                    let unifiedContactsForContainer = try self.store.unifiedContacts(
                        matching: CNContact.predicateForContactsInContainer(withIdentifier: $0),
                        keysToFetch: ContactImporter.desiredContactKeys)
                    
                    unifiedContactsForContainer.forEach { unifiedContacts[$0.identifier] = $0 }
                }
            } catch {
                logError("Failed to import contacts")
            }
            
            let importedContacts = [Contact](unifiedContacts.values
                .filter {
                    //let fullNameString = CNContactFormatter.string(from: $0, style: .fullName)
                    //log("contact: \(fullNameString), type=\($0.contactType.rawValue)")
                    return $0.contactType == .person
                }.map {  // CNContact
                    // Note: we were getting some crashes related to image data so trying to be extra careful
                    var image: UIImage? = nil
                    if $0.imageDataAvailable, let data = $0.imageData {
                        image = UIImage(data:data)
                    }

                    let fullNameString = CNContactFormatter.string(from: $0, style: .fullName)
                    
                    return Contact(identifier: $0.identifier,
                            givenName: $0.givenName,
                            familyName: $0.familyName,
                            fullName: fullNameString,
                            phoneNumbers: $0.phoneNumbers.compactMap { PhoneNumber(string: $0.value.stringValue, label: $0.label) },
                            emailAddresses: $0.emailAddresses.compactMap { EmailAddress(normalizedString: $0.value as String) },
                            image: image
                    )
                }.filter {
                    // Remove any contacts with explicitly empty first and last names.
                    // Note: For some reason some "organization" contacts are returned as "person" by
                    // Note: the API. This removes them.
                    !($0.givenName.trimmingWhitespace().isEmpty && $0.familyName.trimmingWhitespace().isEmpty)
                }
            )
            self.importedContacts = importedContacts
            //importedContacts.forEach { log("imported contact: \($0.nameData), phones=\($0.phoneNumbers.count)") }
            
            // Get onto the main queue to dispatch the completion handler.
            DispatchQueue.main.async {
                completionHandler(.contacts(importedContacts))
            }
        }
        
    }
    
    // MARK: Private Methods
    
    @objc
    private func contactsDidChange() {
        serialQueue.async {
            self.importedContacts = nil
        }
    }
}

public protocol ContactImporting {
    func requestAccess(withCompletionHandler completionHandler: @escaping (CNAuthorizationStatus) -> Void)
    func importContacts(withCompletionHandler completionHandler: @escaping (ContactImportResult) -> Void)
}

public enum ContactImportResult {
    case contacts([Contact])
    case permissionDenied(CNAuthorizationStatus)
}


