//
//  Contact.swift
//  Present
//
//  Created by Patrick Niemeyer on 10/4/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import PresentProtos

public struct Contact: PersonData
{
    public let identifier: String

    public var givenName: String
    public var familyName: String
    public var phoneNumbers: [PhoneNumber]
    public var emailAddresses: [EmailAddress]
    public var image: UIImage?
    
    // MARK: PersonData

    public var id: String { return identifier }
    public var nameData: FullName
    public var bioData: String? { return nil }
    public var interestsData: Array<String>?
    public var hasPhotoData: Bool { return image != nil }
    public var photoData: UIImage? { return image }
    public var photoURLData: URL? { return nil }
    
    /// Copy contact info
    public init(contact: Contact) {
        self.identifier = contact.identifier
        self.givenName = contact.givenName
        self.familyName = contact.familyName
        self.nameData = contact.nameData
        self.phoneNumbers = contact.phoneNumbers
        self.emailAddresses = contact.emailAddresses
        self.image = contact.image
    }
    
    public init(
        identifier: String,
        givenName: String,
        familyName: String,
        fullName: String?,
        phoneNumbers: [PhoneNumber],
        emailAddresses: [EmailAddress],
        image: UIImage?
    ) {
        self.identifier = identifier
        self.givenName = givenName
        self.familyName = familyName
        self.nameData = FullName(givenName: givenName, familyName: familyName, fullName: fullName)
        self.phoneNumbers = phoneNumbers
        self.emailAddresses = emailAddresses
        self.image = image
    }
}

/// A contact limited to a single phone number
public struct PhoneContact {
    public var phoneNumber: PhoneNumber
    public var contact: Contact

     public func toContactRequest() -> Present.ContactRequest
     {
        let proto = Present.ContactRequest.getBuilder()
        proto.phoneNumber = phoneNumber.serverString
        proto.firstName = contact.givenName
        proto.lastName = contact.familyName
        proto.fullName = contact.nameData.fullName
        return try! proto.build()
     }
}

extension PhoneContact: PersonData
{
    /// The id of a PhoneContact is the phone number
    public var id: String { return phoneNumber.normalizedString }
    
    public var nameData: FullName { return contact.nameData }
    public var bioData: String? { return contact.bioData }
    public var interestsData: Array<String>? { return contact.interestsData }
    public var hasPhotoData: Bool { return contact.hasPhotoData }
    public var photoData: UIImage? { return contact.photoData }
    public var photoURLData: URL? { return contact.photoURLData }
    public var phoneNumbers: [PhoneNumber] { return contact.phoneNumbers }
}

/// A PhoneContact with an optional Person annotation for the cases where the contact
/// is known to represent a Present user
public class PhoneContactPerson
{
    public let phoneContact: PhoneContact
    public var user: Person?
    
    public init(phoneContact: PhoneContact, user: Person? = nil) {
        self.phoneContact = phoneContact
        self.user = user
    }
}

/// Phone contact person adopts the PersonData protocol so that it can be used in simple lists, etc.
/// When treated as PersonData it prefers the user data over the contact data.
extension PhoneContactPerson: PersonData
{
    public var id: String { return user?.id ?? phoneContact.id }
    
    public var nameData: FullName {
        if let nameData = user?.nameData, !nameData.isEmpty {
            return nameData
        } else {
            return phoneContact.nameData
        }
    }
    
    // The phone contact has an associated AND activated Present account, not just a "placeholder user".
    public var member: Bool { return user?.member ?? false }
    
    public var bioData: String? { return user?.bioData ?? phoneContact.bioData }
    public var interestsData: Array<String>? { return user?.interestsData ?? phoneContact.interestsData }
    public var hasPhotoData: Bool { return user?.hasPhotoData ?? phoneContact.hasPhotoData }
    public var photoData: UIImage? { return user?.photoData ?? phoneContact.photoData }
    public var photoURLData: URL? { return user?.photoURLData ?? phoneContact.photoURLData }
    public var phoneNumbers: [PhoneNumber] { return phoneContact.phoneNumbers }
}

