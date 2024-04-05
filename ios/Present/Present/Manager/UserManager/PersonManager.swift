//
//  CommunityManager.swift
//  Present
//
//  Created by Patrick Niemeyer on 3/28/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import PresentProtos
import RxSwift
import Then

public enum FriendRelationship {
    case updating, none, incomingRequest, outgoingRequest, friend
}

/// Manage state and services related to other Present users from the perspective of the current user associated
/// with this client.  Note that the current user may appear in Person results from these services and person
/// objects will have their isMe property set accordingly.
public class PersonManager
{
    /// Map of relationship by user id
    public typealias FriendRelationshipMap = Dictionary<String,FriendRelationship>
    
    public static var shared : PersonManager!
    
    public let userManager: UserManager
    public let service: PresentService
    
    public var recentlyViewedProfiles = Set<Person>() // Currently lasts for the session
    
    public lazy var myFriends: TransformableCachedSingle<[Person]> =
        myFriendsSingle.cached(name: "friends", for: .seconds(30))

    public lazy var myOutgoingFriendRequests: CachedSingle<[Person]> =
        myOutgoingFriendRequestsSingle.cached(name: "outgoing friend requests", for: .seconds(30))

    public lazy var myIncomingFriendRequests: TransformableCachedSingle<[Person]> =
        myIncomingFriendRequestsSingle.cached(name: "incoming friend requests", for: .seconds(30))
    
    public lazy var myFriendRelationships: TransformableCachedSingle<FriendRelationshipMap> =
        myFriendRelationshipsSingle.cached(name: "friend relationships", for: .seconds(10))
    
    /// Relationship status for an individual
    public func getRelationshipStatus(_ person: Person) -> Observable<FriendRelationship> {
        return getRelationshipStatus(userId: person.id)
    }
    
    /// Relationship status for an individual
    public func getRelationshipStatus(userId: String) -> Observable<FriendRelationship> {
        return myFriendRelationships.observable().map { statusDict in
            return statusDict[userId, default: .none]
        }.distinctUntilChanged()
    }
    
    // MARK: Singles mapped to our person data
    
    private lazy var myFriendsSingle: Single<[Person]> = {
        guard let me = self.userManager.me.value else { return Single.just([]) }
        return self.service.getFriends(userId: me.id).map {
            return self.getPeople(forProtos: $0)
                .sorted { $0.nameData < $1.nameData }
        }
    }()
    
    lazy var myOutgoingFriendRequestsSingle: Single<[Person]> = {
        return self.service.getOutgoingFriendRequests().map {
            return self.getPeople(forProtos: $0)
                .sorted { $0.nameData < $1.nameData }
        }
    }()
    
    private lazy var myIncomingFriendRequestsSingle: Single<[Person]> = {
        return self.service.getIncomingFriendRequests().map {
            return self.getPeople(forProtos: $0)
                .sorted { $0.nameData < $1.nameData }
        }
    }()
    
    /// This single composes a consistent view of relationship data from the underlying single sources.
    lazy var myFriendRelationshipsSingle: Single<FriendRelationshipMap> =
        Single.zip(
            myFriendsSingle,
            myOutgoingFriendRequestsSingle,
            myIncomingFriendRequestsSingle
        )
        .map { friends, outgoingRequests, incomingRequests in
            var result = FriendRelationshipMap()
            //log("myFriendRelationshipsSingle: incomingRequests=\(incomingRequests.map{$0.name}), outgoingRequests=\(outgoingRequests.map{$0.name}), friends=\(friends.map{$0.name})")
            incomingRequests.forEach { result[$0.id] = .incomingRequest }
            outgoingRequests.forEach { result[$0.id] = .outgoingRequest }
            friends.forEach { result[$0.id] = .friend }
            return result
        }

    // MARK: Init
    
    public init(userManager: UserManager, service: PresentService) {
        self.userManager = userManager
        self.service = service
        PersonManager.shared = self
    }
    
    // MARK: Friend management

    public func addFriend(phoneContact: PhoneContact)->Single<(FriendRelationship, Person)> {
        return addFriendRelationship(userIdOrPhone:phoneContact.id, fromAddFriendResponse: service.addFriend(phoneContact: phoneContact))
    }
    
    public func addFriend(userId: String)->Single<(FriendRelationship, Person)> {
        return addFriendRelationship(userIdOrPhone: userId, fromAddFriendResponse: service.addFriend(userId: userId))
    }
    
    // TODO: Accept both optional user id and phone here and allow handling separately below.
    private func addFriendRelationship(userIdOrPhone: String, fromAddFriendResponse response: Single<Present.AddFriendResponse>)
        ->Single<(FriendRelationship, Person)>
    {
        // Set the updating state
        log("addFriendRelationship setting .updating status")
        let updating = updateFriendStatusTransform(userId: userIdOrPhone, relationship: .updating)
        return response
            .map {
                let user = self.getPerson(forProto: $0.user)
                var relationship: FriendRelationship
                switch $0.result {
                    case .accepted:
                        relationship = .friend
                    case .requested:
                        relationship = .outgoingRequest
                }
                log("addFriendRelationship response: \(relationship), user=\(user)")

                //
                // Update the local models
                // Note: This has gotten too complicated attempting to manipulate the model using
                // pure Rx. We should interpose a model to encapsulate it.

                // Expire the updating status (spinner)
                updating.expire()
                
                // Show a new friend immediately
                if relationship == .friend {
                    self.addToFriendsTransform(person: user).expire(in: .seconds(1))
                }
                
                // Set the final status using the incoming id (which may be
                // a phone or a user id)
                self.updateFriendStatusTransform(userId: userIdOrPhone, relationship: relationship).expire(in: .seconds(1))
                
                // If the call created a new user on the server mark the
                // status on that id as well.
                self.updateFriendStatusTransform(userId: user.id, relationship: relationship).expire(in: .seconds(1))
                
                // If the add was for an incoming friend request clear it
                self.removeFromIncomingFriendRequestTransform(userId: userIdOrPhone).expire(in: .seconds(1))
                
                // If it was a new friend add it
                
                //self.myFriendRelationships.invalidate()
                
                return (relationship, self.getPerson(forProto: $0.user))
            }
    }
    
    public func removeFriend(userId: String)->Completable
    {
        // Set the updating state
        log("removeFriendRelationship setting .updating status")
        let updating = updateFriendStatusTransform(userId: userId, relationship: .updating)
        return service.removeFriend(userId: userId)
            .do {
                log("removeFriendRelationship complete")

                // Resolve the updating state
                updating.expire()
                
                self.updateFriendStatusTransform(userId: userId, relationship: .none).expire(in: .seconds(1))
                
                // If this was a friend request clear it
                self.removeFromIncomingFriendRequestTransform(userId: userId).expire(in: .seconds(1))
                
                self.myFriends.invalidate()
                self.myOutgoingFriendRequests.invalidate()
                self.myIncomingFriendRequests.invalidate()
            }
    }
    
    /// Invalidate all friend data and refresh it.
    public func refreshFriendData() {
        self.myFriends.refresh()
        self.myOutgoingFriendRequests.refresh()
        self.myIncomingFriendRequests.refresh()
        
        // friend relationships is based on its own (consistent) fetch of the singles
        // (it does not sit on top of the cached friends, outgoing, incoming)
        self.myFriendRelationships.refresh()
    }

    /// Change the current friend status:
    ///     - Remove a friend or outgoing friend request
    ///     - Add a new friend, or accept an incoming friend request
    public func toggleFriendStatus(userId: String, fromStatus: FriendRelationship)->Single<ToggleFriendStatusResult>
    {
        log("toggle friend status: \(userId), from status: \(fromStatus)")
        switch fromStatus {
            case .updating:
                return Single.error(PresentError.apiError) // invalid state
            case .none, .incomingRequest:
                return self.addFriend(userId: userId)
                    .map {
                        let (friendRelationship, person) = $0
                        return ToggleFriendStatusResult.added(friendRelationship: friendRelationship, person: person)
                    }
            case .friend, .outgoingRequest:
                return Single.create { observer in
                    return self.removeFriend(userId: userId)
                        .onCompleted {
                            observer(.success(ToggleFriendStatusResult.removed))
                        }.onError {
                            observer(.error($0))
                        }.subscribe()
                }
        }
    }
    
    private func updateFriendStatusTransform(userId: String, relationship: FriendRelationship) -> TransformableCachedSingle<FriendRelationshipMap>.Transform {
        return myFriendRelationships.transform { return $0.with { $0[userId] = relationship } }
    }
    private func removeFromIncomingFriendRequestTransform(userId: String) -> TransformableCachedSingle<[Person]>.Transform {
        return myIncomingFriendRequests.transform {
            return $0.filter { $0.id != userId }
        }
    }
    private func addToFriendsTransform(person: Person) -> TransformableCachedSingle<[Person]>.Transform {
        return myFriends.transform {
            return $0.contains { $0.id == person.id } ? $0 : $0.appending(person)
        }
    }

    // MARK: People management
    
    /// Resolve UserResponse protos to Person objects
    // TODO: Cache and resolve to a unique instance
    public func getPeople(forProto: Present.UsersResponse) -> [Person] {
        return getPeople(forProtos: forProto.users)
    }

    /// Resolve UserResponse protos to Person objects
    // TODO: Cache and resolve to a unique instance
    public func getPeople(forProtos: [Present.UserResponse]) -> [Person] {
        return forProtos.map {
            getPerson(forProto: $0)
        }
    }
    
    public func getPeople(forProtos:[Present.FriendResponse]) -> [Person] {
        return forProtos.map {
            getPerson(forProto: $0.user)
        }
    }

    /// Resolve UserResponse proto to Person object
    // TODO: Cache and resolve to a unique instance
    public func getPerson(forProto: Present.UserResponse) -> Person {
        return Person(userProto: forProto, isMe: self.userManager.isMe(userId: forProto.id))
    }

    public func getPerson(userId: String) -> Single<Person> {
        return service.getUser(userId: userId)
            .map {
                return self.getPerson(forProto: $0)
            }
    }
}

extension Dictionary: Then { }

public enum ToggleFriendStatusResult {
    case added(friendRelationship: FriendRelationship, person: Person)
    case removed
    case noChange
}

