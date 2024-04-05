//
//  Person.swift
//  Present
//
//  Created by Dan Federman on 1/20/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import PresentProtos

/// PersonData is a protocol supported by Person and Contact for use in e.g. generic user lists
/// Note: "Data" suffix is to avoid naming collisions with impl classes.
public protocol PersonData
{
    var id: String { get } // user id or contact id
    var nameData: FullName { get } // named to avoid conflict
    var bioData: String? { get }
    var interestsData: Array<String>? { get }
    var hasPhotoData: Bool { get }
    var photoData: UIImage? { get }
    var photoURLData: URL? { get }
    var phoneNumbers: [PhoneNumber] { get }
}

public func ==(lhs: PersonData, rhs: PersonData) -> Bool {
    return lhs.id == rhs.id
        && lhs.nameData == rhs.nameData
        && lhs.bioData == rhs.bioData
        && lhs.interestsData! == rhs.interestsData!
        && lhs.photoURLData == rhs.photoURLData
        && lhs.phoneNumbers == rhs.phoneNumbers
}

public class Person: Hashable, PersonData, CustomStringConvertible
{
    // MARK: Public Properties

    public let userToken: String // userId
    public let name: String
    public let photoURL: URL
    public let bio: String
    public let interests: [String]
    public let friends: [Present.FriendResponse]
    public let signupLocation: String?
    public let link: String?
    public let member: Bool

    // This Person is the current user.
    public var isMe: Bool

    /// The current user is following this Person.
    public var following: Bool? = nil
    

    static let noUserPhotoURL = "https://lh3.googleusercontent.com/xxx"
    
    // TODO:(dan) This default should likely come from the server, and we should always have a bio.
    public static let defaultPersonalStatement = NSLocalizedString(
        "PersonDefaultPersonalStatement",
        tableName: nil,
        bundle: .main,
        value: "I’m new to Present, say hi!",
        comment: "Default personal description for a user"
    )
    
    // MARK: Equatable
    
    public static func ==(lhs: Person, rhs: Person) -> Bool {
        return lhs.userToken == rhs.userToken
    }
    
    // MARK: Initialization
    
    public init(userToken: String, name: String, photoURL: URL, personalStatement: String, isMe: Bool,interests: Array<String>, friends: Array<Present.FriendResponse>, signupLocation: String?, link: String?, member: Bool)
    {
        self.userToken = userToken
        self.name = name
        self.photoURL = photoURL
        self.bio = personalStatement
        self.isMe = isMe
        self.signupLocation = signupLocation
        self.interests = interests
        self.friends  = friends
        self.link = link
        self.member = member
    }
    
    public init(userProto: Present.UserResponse, isMe: Bool) {
        userToken = userProto.id
        name = userProto.name
        friends = userProto.friends
        photoURL = URL(string:
                userProto.hasPhoto ? userProto.photo : Person.noUserPhotoURL
            ).unwrappedOrFatal()
        interests = userProto.interests
        //if bio is empty or include whitespace show the statement
        if userProto.hasBio, !userProto.bio.removingCharacters(in: .whitespacesAndNewlines).isEmpty {
            bio = userProto.bio
        } else {
            bio = Person.defaultPersonalStatement
        }
        self.isMe = isMe
        self.signupLocation = userProto.hasSignupLocation ? userProto.signupLocation : nil
        self.link = userProto.link
        self.member = userProto.member
    }
    
    public init?(userProfile: Present.UserProfile) {
        
        guard userProfile.hasName else {
            logError("missing name")
            return nil
        }
        
        userToken = userProfile.id
        name = userProfile.name.first + " " + userProfile.name.last
        friends = userProfile.friends
        interests = userProfile.interests
        
        photoURL = URL(string:
            userProfile.hasPhoto ? userProfile.photo : Person.noUserPhotoURL
            ).unwrappedOrFatal()
        
        if userProfile.hasBio, !userProfile.bio.isEmpty {
            bio = userProfile.bio
            
        } else {
            bio = Person.defaultPersonalStatement
        }
        isMe = true
        self.signupLocation = nil
        self.link = userProfile.link
        self.member = true
    }
    
    // MARK: Hashable
    
    public var hashValue: Int {
        return userToken.hashValue
    }
    
    public var asUserProto: Present.UserResponse {
        return try! Present.UserResponse.getBuilder()
            .setId(userToken)
            .setName(name)
            .setInterests(interests)
            .setFriends(friends)
            .setPhoto(photoURL.absoluteString)
            .setBio(bio)
            .build()
    }
    
    // MARK: PersonData

    public var id: String { return userToken }
    // TODO: Add firstName, lastName to Person
    public var nameData: FullName {
        return FullName(fullName: name) ?? NoName()
    }
    public var bioData: String? { return bio }
    public var interestsData: Array<String>? { return interests }
    public var friendsData: Array<Present.FriendResponse>? { return friends }
    public var hasPhotoData: Bool { return true }
    public var photoData: UIImage? { return nil }
    public var photoURLData: URL? { return photoURL }
    public var phoneNumbers: [PhoneNumber] { return [] }
    
    public var description: String { return "Person: name=\(self.nameData.fullName)" }
}

// A Person entity with a full user profile, representing the current user.
public class UserProfilePerson: Person
{
    public let userProfile: Present.UserProfile
    
    override public init?(userProfile: Present.UserProfile) {
        self.userProfile = userProfile
        super.init(userProfile: userProfile)
    }
}

// MARK: - PersistablePerson


// TODO: Get rid of this and related...
// TODO: I think this is only used for the local notification that a message failed to send while we were in the background
// TODO: Why doesn't this just store the uuid or string version of the proto as we send in other notifs if needed?
public final class PersistablePerson: NSObject, NSSecureCoding {
    
    // MARK: NSSecureCoding
    
    public static var supportsSecureCoding: Bool {
        return true
    }
    
    // MARK: Private Static Properties
    
    private static let userTokenKey = "userToken"
    private static let nameKey = "name"
    private static let interestsKey = "interests"
    private static let friendsKey = "friends"
    private static let photoURLKey = "photoURL"
    private static let personalStatementKey = "personalStatement"
    private static let isMeKey = "isMe"
    
    // MARK: Initialization
    
    public required init(person: Person) {
        self.person = person
        
        super.init()
    }
    
    public required init?(coder aDecoder: NSCoder) {
        guard let userToken = aDecoder.decodeObject(of: NSString.self, forKey: PersistablePerson.userTokenKey),
            let name = aDecoder.decodeObject(of: NSString.self, forKey: PersistablePerson.nameKey),
            let photoURL = aDecoder.decodeObject(of: NSURL.self, forKey: PersistablePerson.photoURLKey),
            let interests = aDecoder.decodeObject(of: NSArray.self, forKey: PersistablePerson.interestsKey),
            let personalStatement = aDecoder.decodeObject(of: NSString.self, forKey: PersistablePerson.personalStatementKey),
            let friends = aDecoder.decodeObject(of: NSArray.self, forKey: PersistablePerson.friendsKey),
            let isMe = aDecoder.decodeObject(of: NSNumber.self, forKey: PersistablePerson.isMeKey)?.boolValue else {
                return nil
        }
        
        person = Person(userToken: String(userToken),
                        name: String(name),
                        photoURL: photoURL as URL,
                        personalStatement: String(personalStatement),
                        isMe: Bool(isMe), interests: interests as! Array<String>,
                        friends: friends as! Array<Present.FriendResponse>,
                        signupLocation: nil,
                        link: nil, member: true)
        
        super.init()
    }
    
    public required init?(from data: Data) {
        guard let unarchivedSelf = NSKeyedUnarchiver.unarchiveObject(with: data) as? PersistablePerson else {
            return nil
        }
        
        person = unarchivedSelf.person
    }
    
    // MARK: NSCoding
    
    public func encode(with aCoder: NSCoder) {
        aCoder.encode(person.userToken as NSString, forKey: PersistablePerson.userTokenKey)
        aCoder.encode(person.name as NSString, forKey: PersistablePerson.nameKey)
        aCoder.encode(person.interests as NSArray, forKey: PersistablePerson.interestsKey)
        aCoder.encode(person.friends as NSArray, forKey: PersistablePerson.friendsKey)
        aCoder.encode(person.photoURL as NSURL, forKey: PersistablePerson.photoURLKey)
        aCoder.encode(person.bio as NSString, forKey: PersistablePerson.personalStatementKey)
        aCoder.encode(NSNumber(booleanLiteral: person.isMe), forKey: PersistablePerson.isMeKey)
    }
    
    // MARK: Public Properties
    
    public let person: Person
    
    public var asData: Data {
        return NSKeyedArchiver.archivedData(withRootObject: self)
    }

}

public protocol PersonPostingService {
    func blockUser(withToken userToken: String, completionHandler: @escaping (Response<UserDisplayableError, ()>) -> Void)
    func unblockUser(withToken userToken: String, completionHandler: @escaping (Response<UserDisplayableError, ()>) -> Void)
}

