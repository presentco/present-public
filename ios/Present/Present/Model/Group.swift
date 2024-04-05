//
//  Group.swift
//  Present
//
//  Created by Dan Federman on 5/9/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import CoreLocation
import Foundation
import PresentProtos
import RxSwift

public final class Group
{
    // MARK: Private Properties
    
    private let groupManager: GroupManager
    private let liveServer: GroupLiveUpdatingService
    private let userNotificationPoster: UserNotificationPoster
    
    private let groupMessageObservers = LegacyObservable<GroupAndMessages, GroupMessageObserver>() { value, observer in
        observer.didUpdate(messages: value.messages, in: value.group)
    }
    private let groupObservers = LegacyObservable<Group, GroupObserver>() { value, observer in
        observer.didUpdate(group: value)
    }
    
    public private(set) var created: Date
    
    fileprivate let workQueue = DispatchQueue(label: "Group Work Queue")
    
    fileprivate enum Model {
        case loading
        case loaded([GroupMessage])
    }
    fileprivate var model: Model {
        didSet {
            switch model {
            case .loading:
                break
                
            case let .loaded(messages):
                groupMessageObservers.set(value: GroupAndMessages(group: self, messages: messages))
                updateGroupObservers()
            }
        }
    }

    // MARK: Public Properties

    /// Opaque, unique token that identifies the groupToken on the server.
    // TODO: Rename
    public let groupToken: PresentUUID
    public var id: String {
        return groupToken.uuidString
    }

    public private(set) var title: String // title
    public private(set) var owner: Person
    public private(set) var description: String?
    public private(set) var categories: Set<String>
    public var joinedCount: Int
    public private(set) var commentCount: Int
    public private(set) var lastCommentTime: Date
    public private(set) var location: CLLocationCoordinate2D
    public private(set) var locationName: String?
    public private(set) var coverPhoto: URL?
    public private(set) var coverPhotoToken: PresentUUID?
    public private(set) var shareURL: URL
    public private(set) var schedule: Present.Schedule?
    public private(set) var preapproval: Present.GroupMemberPreapproval
    public private(set) var space: Present.SpaceResponse?
    public private(set) var discoverable: Bool
    
    public var joinRequests: Int {
        didSet {
            rx.joinRequests.onNext(joinRequests)
        }
    }
    
    public private(set) var membershipStatus: Present.GroupMembershipState? {
        didSet {
            rx.membershipStatus.onNext(membershipStatus)
        }
    }
    
    /// Helper to bridge legacy code: Simplify membership state to successful join or not.
    public var isJoined: Bool {
        switch membershipStatus ?? .none {
            case .none, .invited, .rejected, .unjoined:
                return false
            case .requestedGroupMembershipState:
                return false
            case .active:
                return true
        }
    }

    public private(set) var isMuted: Bool {
        didSet {
            rx.isMuted.onNext(isMuted)
        }
    }

    /// The group contains unread messages
    public private(set) var isUnread: Bool {
        didSet {
            rx.isUnread.onNext(isUnread)
        }
    }
    
    /// Index of the latest read message
    public private(set) var lastRead: Int

    // If true content has never been read
    public var isNew : Bool {
        get {
            return isUnread
        }
    }
    
    // MARK: Rx observables
    
    public class Rx
    {
        var group: Group?
        public lazy var metadataUpdate = ReplaySubject<Void>.create()
        public let membershipStatus = ReplaySubject<Present.GroupMembershipState?>.create()
        public let isMuted = ReplaySubject<Bool>.create()
        public let joinRequests = ReplaySubject<Int>.create()
        public let isUnread = ReplaySubject<Bool>.create()
        
        //  Combine the individual update observables into one signal
        private lazy var individualModified =
            Observable.combineLatest(
                membershipStatus, isMuted, joinRequests, isUnread
            ).distinctUntilChanged { [weak self] (
                    old: (Present.GroupMembershipState?, Bool, Int, Bool),
                    new: (Present.GroupMembershipState?, Bool, Int, Bool)
                ) ->Bool in
                //log("group: \(self?.group?.title ?? "") distinct until changed: \(old == new)"); dump(old, name: "old"); dump(new, name: "new")
                return old == new
            }
            .map { _ in Void() }

        /// Combined observer for any changes to the group, individual or bulk metadata update.
        public lazy var modified =
            Observable.combineLatest(metadataUpdate, individualModified)
            .map { [weak self] _ in Void() }
            .share()

        init(group: Group) {
            self.group = group
        }
    }
    public lazy var rx = Rx(group: self)

    // MARK: Initialization
    
    public required init(
        groupToken: PresentUUID,
        messages: [GroupMessage]?,
        created: Date,
        topicName: String,
        owner: Person,
        description: String?,
        categories: Set<String>,
        joinedCount: Int,
        commentCount: Int,
        lastCommentTime: Date,
        shareURL: URL,
        location: CLLocationCoordinate2D,
        locationName: String?,
        coverPhoto: URL?,
        coverPhotoToken: PresentUUID?,
        service: GroupManager,
        liveServer: GroupLiveUpdatingService,
        userNotificationPoster: UserNotificationPoster,
        isMuted: Bool,
        isJoined: Bool,
        isUnread: Bool,
        lastRead: Int,
        schedule: Present.Schedule?,
        preapproval: Present.GroupMemberPreapproval,
        space: Present.SpaceResponse?,
        discoverable: Bool,
        joinRequests: Int,
        membershipStatus: Present.GroupMembershipState?
    )
    {
        self.groupManager = service
        self.liveServer = liveServer
        self.userNotificationPoster = userNotificationPoster
        
        self.groupToken = groupToken
        if let messages = messages {
            model = .loaded(messages)
        } else {
            model = .loading
        }
        
        self.created = created
        self.title = topicName
        self.owner = owner
        self.description = description
        self.categories = categories
        self.joinedCount = joinedCount
        self.commentCount = commentCount
        self.lastCommentTime = lastCommentTime
        self.shareURL = shareURL
        self.location = location
        self.locationName = locationName
        self.coverPhoto = coverPhoto
        self.coverPhotoToken = coverPhotoToken
        self.isMuted = isMuted
        self.isUnread = isUnread
        self.lastRead = lastRead
        self.schedule = schedule
        self.preapproval = preapproval
        self.space = space
        self.discoverable = discoverable
        self.joinRequests = joinRequests
        self.membershipStatus = membershipStatus
    }
    
    /// Shallow: does not copy messages
    public func clone() -> Group
    {
        return Group(
            groupToken: groupToken,
            messages: nil,
            created: self.created,
            topicName: self.title,
            owner: self.owner,
            description: self.description,
            categories: self.categories,
            joinedCount: self.joinedCount,
            commentCount: self.commentCount,
            lastCommentTime: self.lastCommentTime,
            shareURL: self.shareURL,
            location: self.location,
            locationName: self.locationName,
            coverPhoto: self.coverPhoto,
            coverPhotoToken: self.coverPhotoToken,
            service: self.groupManager,
            liveServer: self.liveServer,
            userNotificationPoster: self.userNotificationPoster,
            isMuted: self.isMuted,
            isJoined: self.isJoined,
            isUnread: self.isUnread,
            lastRead: self.lastRead,
            schedule: self.schedule,
            preapproval: self.preapproval,
            space: self.space,
            discoverable: self.discoverable,
            joinRequests: self.joinRequests,
            membershipStatus: self.membershipStatus
        )
    }
        
    // MARK: Public Methods – Server-Driven Actions
    // MARK: Public Methods – User-Driven Actions
    
    public var userAuthorizedToParticipate: Bool {
        // We can always comment on our own group
        if owner.isMe { return true }
        switch preapproval {
        case .anyone:
            return true
        case .unknownGroupMemberPreapproval, .friendsOfMembers, .friends, .inviteOnly:
            return isJoined
        }
    }

    /// Send a message for this group
    /// On a send failure this method inserts the message with the failure annotation into the model, where it
    /// replaces the optimistically presented version by id.
    /// Observer side effects: Updating the model will fire observers
    public func send(
        message: GroupMessage, progressListener: ProgressListener? = nil)->Completable
    {
        logDebug("group: send")
        
        // Optimistically insert the message into our store.
        workQueue.async {
            self.insert_inWorkQueue(newMessages: [message])
        }
        
        return groupManager.send(groupMessage: message, toGroupWithToken: groupToken, progressListener: progressListener)
                .onError { [weak self, groupManager] error in
                    logx("message failed to send: updating")
                    
                    groupManager.persistFailedMessage(message: message)
                    
                    guard let self = self else { return }
                    self.workQueue.async {
                        // Replace the optimistic message with an error.
                        self.insert_inWorkQueue(newMessages: [GroupMessage(messageDidFailToSend: message)])
                        main {
                            self.userNotificationPoster.add(
                                RemoteNotification.notificationForFailedToUpload(message: message), withCompletionHandler: nil)
                        }
                    }
            }.onCompleted { [groupManager] in
                logx("message sent")
                // If the message was a successful retry of a previously failed message remove it from
                // the failed messages list.
                if message.didFailToSend {
                    logx("previously failed message sent, removing form store")
                    groupManager.removeFromPersistedFailedMessages(messages: [message])
                }
            }
    }
    
    /// Delete a message on this group
    /// Observer side effects: Updating the model will fire observers
    public func delete(messageWithToken messageToken: PresentUUID, completionHandler: @escaping (Response<UserDisplayableError, ()>) -> Void) {
        // Optimistically delete the message from our store.
        workQueue.async {
            switch self.model {
            case .loading:
                logError("Removing message from group while still in loading state!")
                
            case let .loaded(messages):
                self.model = .loaded(messages.filter({ $0.messageId != messageToken }))
            }
        }
        
        // Inform the server.
        groupManager.delete(messageWithToken: messageToken, completionHandler: completionHandler)
    }

    /// Insert a new message or update an existing cached message on this group.
    /// Observer side effects: Updating the model will fire observers
    public func insertOrUpdate(messages: [GroupMessage]) {
        workQueue.async {
            self.insert_inWorkQueue(newMessages: messages)
        }
    }
    public func insertOrUpdate(message: GroupMessage) {
        workQueue.async {
            self.insert_inWorkQueue(newMessages: [message])
        }
    }

    /// Reload the comments
    /// Observer side effects: Updating the model will fire observers
    public func reloadPastComments() {
        workQueue.async {
            self.model = .loading
            self.getPastComments()
        }
    }

    /// Observer side effects: This method fires the observers to update status
    public func joinGroup()->Single<Present.GroupMembershipState> {
        return groupManager.joinGroup(groupId: id).onSuccess { status in
            self.membershipStatus = status
            switch status {
                case .active:
                    self.joinedCount += 1
                default: break
            }
            self.updateGroupObservers()
        }
    }
    /// Observer side effects: This method fires the observers to update status
    public func leaveGroup()->Completable {
        return groupManager.leaveGroup(groupId: self.id).onCompleted {
            self.membershipStatus = .unjoined
            self.joinedCount -= 1
            self.updateGroupObservers()
        }
    }
    
    /// Mute the group
    /// Observer side effects: This method fires the observers to update status
    public func mark(asMuted muted: Bool) {
        guard isMuted != muted else { return }
        
        isMuted = muted
        
        if muted {
            groupManager.mute(groupWithToken: groupToken, completionHandler: { _ in
                // TODO:(dan) We should never get an error here. We should retry forever.
            })
        } else {
            groupManager.unmute(groupWithToken: groupToken, completionHandler: { _ in
                // TODO:(dan) We should never get an error here. We should retry forever.
            })
        }
        updateGroupObservers()
    }

    /// Mark the entire group as read based on the latest comment index.
    /// Observer side effects: This method fires the observers to update status
    public func markAllRead()
    {
        guard case .loaded = model else {
            logError("Group: Attempt to mark all read before messages loaded")
            return
        }

        let lastRead = maxCommentIndexInUse() ?? -1 // Use index -1 if there are no messages
        self.lastRead = lastRead
        log("markAllRead changing lastRead from: \(self.lastRead) to \(lastRead)")
        
        self.isUnread = false

        // Update the server
        groupManager.markRead(groupId: groupToken, lastRead: lastRead) { _ in }
        updateGroupObservers()
    }

    /// Observer side effects: None
    public func reportAsAbusive(for reason: Group.AbuseReason) {
        // Just pass the message to the server.
        groupManager.reportAbusiveGroup(withToken: groupToken, for: reason) { _ in
            // TODO:(dan) We should never get an error here. We should retry forever.
        }
    }

    /// Observer side effects: None
    public func reportAsAbusive(messageWithToken messageToken: PresentUUID, for reason: Group.AbuseReason) {
        // Just pass the message to the server.
        groupManager.reportAbusiveComment(withToken: messageToken, for: reason) { _ in
            // TODO:(dan) We should never get an error here. We should retry forever.
        }
    }

    // MARK: Live server functionality
    
    public func beginListeningForUpdates() {
        //log("group: \(self.title) begin listening for updates")
        getPastComments(andPurgeCachedMessages: true)
        liveServer.register(observer: self)
        
        // TODO:(dan) Error handling! If retry takes a long time, show some kind of "disconnected" UI. Needs design.
    }
    
    public func endListeningToUpdates() {
        liveServer.unregisterAllObservers()
    }
    
    // MARK: Public Methods – Observers
    
    public func register(groupMessageObserver: GroupMessageObserver, observerQueue: DispatchQueue = .main) -> (()->Void) {
        return self.groupMessageObservers.register(observer: groupMessageObserver, on: observerQueue)
    }

    public func register(groupObserver: GroupObserver, observerQueue: DispatchQueue = .main) -> (()->Void) {
        return self.groupObservers.register(observer: groupObserver, on: observerQueue)
    }

    /// Update observers of a change in this group's metadata.
    /// Observer side effects:
    /// @param batchOperation: If true this is part of a batch operation by the group manager and we should not
    /// notify it of the update (it will batch). Individual observers of this group should receive a notification as usual.
    private func updateGroupObservers(batchOperation: Bool = false) {
        groupObservers.set(value: self)
        if !batchOperation {
            groupManager.didUpdate(group: self)
        }
    }

    // MARK: Private Methods

    /// Adds new messages to the existing messages, overwriting existing messages that have same message token as a new message.
    /// The returned array is sorted using GroupMessage's compare.
    public static func uniqueAndSort(newMessages: [GroupMessage], with existingMessages: [GroupMessage] = []) -> [GroupMessage] {
        var messageTokenToMessageMap = existingMessages.mapToDictionary { [$0.messageId : $0] }
        newMessages.forEach {
            messageTokenToMessageMap[$0.messageId] = $0
        }

        return Array(messageTokenToMessageMap.values).sorted()
    }

    /// Determine the highest comment index in use, taking into account newly sent and received messages.
    /// Returns nil if there are no comments.
    public func maxCommentIndexInUse() -> Int?
    {
        guard case let .loaded(messages) = model else { return nil }

        // Highest seq number found in currently loaded messages
        var maxIndex: Int? = messages
            .filter { $0.index != nil }
            .max { $0.index! < $1.index! }?
            .index

        // TODO: The above should be just our last message, right?
        // TODO: Sanity checking, then replace the above with this
        if let maxIndex = maxIndex, let firstMessage = messages.first {
            if maxIndex != firstMessage.index {
                logDebug("Group: first message index does not match max found index!")
            }
        }

        // Newly sent messages won't have a sequence number.
        // Increment maxSeq optimistically for them (preserving nil if there are none).
        let newlySentCount = messages.filter { $0.index == nil }.count
        if newlySentCount > 0 {
            maxIndex = (maxIndex ?? -1) + newlySentCount // if no messages newly sent start at index zero
        }

        return maxIndex
    }

    // TODO: Why doesn't this wrap the call in the work queue? (Why make each caller do it?)
    fileprivate func insert_inWorkQueue(newMessages: [GroupMessage])
    {
        switch model {
            case .loading:
                self.model = .loaded(Group.uniqueAndSort(newMessages: newMessages))

            case let .loaded(existingMessages):

                // Don't update if the new messages are identical to those contained in existing
                /*
                if newMessages.allSatisfy({ existingMessages.contains($0) }) {
                    logx("We already have these messages. No need to update.")
                    return
                }*/

                logx("Adding or updating messages:")
                //newMessages.forEach { logx("message: \($0.messageId), didFailToSend: \($0.didFailToSend)") }
                self.model = .loaded(Group.uniqueAndSort(newMessages: newMessages, with: existingMessages))
        }
    }
    
    fileprivate func getPastComments(andPurgeCachedMessages purgeCachedMessages: Bool = false)
    {
        groupManager.getPastComments(forGroupWithToken: groupToken) { [weak self] (response) in
            guard let strongSelf = self else { return }
            
            switch response {
            case .error:
                // Should never happen. We should retry forever.
                break
                
            case let .success(commentProtos):
                strongSelf.workQueue.async {
                    //logDebug("group: getPastComments result: received comment count = \(commentProtos.count)")
                    let newMessages = commentProtos.map {
                        strongSelf.groupManager.getGroupMessage(with: $0)
                    }
                    if purgeCachedMessages {
                        strongSelf.model = .loaded(newMessages.sorted())
                        
                    } else {
                        strongSelf.insert_inWorkQueue(newMessages: newMessages)
                    }
                }
            }
        }
    }
    
    // MARK: Private Structs
    
    private struct GroupAndMessages {
        public let group: Group
        public let messages: [GroupMessage]
        
    }
    
}

/// Observe comments received from the live server
extension Group: GroupLiveUpdatingObserver
{
    // MARK: GroupLiveUpdatingObserver

    public func didReceive(message: GroupMessage, on liveUpdatingService: GroupLiveUpdatingService) {
        logDebug("group: did receive")
        insertOrUpdate(message: message)
    }

    public func didDelete(message: GroupMessage, on liveUpdatingService: GroupLiveUpdatingService) {
        workQueue.async {
            switch self.model {
                case .loading:
                    logError("Removing message from group while still in loading state!")

                case let .loaded(messages):
                    self.model = .loaded(messages.filter({ $0.messageId != message.messageId }))
            }
        }
    }

    public func didConnectToLiveService(on liveUpdatingService: GroupLiveUpdatingService) {
        // Fetch past comments against to protect against us missing messages between our first fetch and our connecting to the live server.
        getPastComments()

        // TODO:(dan) Error recovery. Let our UI know.
    }

    public func didDisconnectFromLiveService(on liveUpdatingService: GroupLiveUpdatingService) {
        // TOOD:(dan) Error handling. Let our UI know.
    }
}

// MARK: - Group.AbuseReason

public extension Group {
    
    public enum AbuseReason {
        
        // MARK: Cases
        
        case inappropriate
        case spam
        
        // MARK: Public Properties
        
        public var asFlagReasonProto: Present.FlagReason {
            switch self {
            case .inappropriate:
                return .inappropriate
                
            case .spam:
                return .spam
            }
        }
        
    }
}

extension Group: Hashable
{
    // MARK: Hashable

    public var hashValue: Int {
        return groupToken.hashValue
    }

    // MARK: Equatable

    public static func ==(lhs: Group, rhs: Group) -> Bool {
        return lhs.groupToken == rhs.groupToken
            && lhs.created == rhs.created
            && lhs.title == rhs.title
            && lhs.owner == rhs.owner
            && lhs.description == rhs.description
            && lhs.categories == rhs.categories
            && lhs.joinedCount == rhs.joinedCount
            && lhs.commentCount == rhs.commentCount
            && lhs.lastCommentTime == rhs.lastCommentTime
            && lhs.isUnread == rhs.isUnread
            && lhs.lastRead == rhs.lastRead
            && lhs.location.asProtoCoordinate == rhs.location.asProtoCoordinate
            && lhs.locationName == rhs.locationName
            && lhs.coverPhoto == rhs.coverPhoto
            && lhs.coverPhotoToken == rhs.coverPhotoToken
            && lhs.shareURL == rhs.shareURL
            && lhs.schedule == rhs.schedule
            && lhs.preapproval == rhs.preapproval
            && lhs.isMuted == rhs.isMuted
            && lhs.membershipStatus == rhs.membershipStatus // drives isJoined
            && lhs.space == rhs.space
            && lhs.discoverable == rhs.discoverable
            && lhs.joinRequests == rhs.joinRequests
            && lhs.membershipStatus == rhs.membershipStatus
    }
    
    /// Copy all metadata from the specified group to this group
    /// Observer side effects:
    /// @param batchOperation: If true this is part of a batch operation by the group manager and we should not
    /// notify it of the update (it will batch). Individual observers of this group should receive a notification as usual.
    public func copyMetadata(from group: Group, batchOperation: Bool)
    {
        copy(from: group)
        rx.metadataUpdate.onNext()
        updateGroupObservers(batchOperation: batchOperation)
    }
    
    private func copy(from group: Group)
    {
        self.created = group.created
        self.title = group.title
        self.owner = group.owner
        self.description = group.description
        self.categories = group.categories
        self.joinedCount = group.joinedCount
        self.commentCount = group.commentCount
        self.lastCommentTime = group.lastCommentTime
        self.isUnread = group.isUnread
        self.lastRead = group.lastRead
        self.location = group.location
        self.locationName = group.locationName
        self.coverPhoto = group.coverPhoto
        self.coverPhotoToken = group.coverPhotoToken
        self.shareURL = group.shareURL
        self.schedule = group.schedule
        self.preapproval = group.preapproval
        self.isMuted = group.isMuted
        self.space = group.space
        self.discoverable = group.discoverable
        self.joinRequests = group.joinRequests
        self.membershipStatus = group.membershipStatus // drives isJoined
    }
}

public protocol GroupObserver: class {
    /// Called when a group's properties may have changed in any way, including message content that
    /// may affect calculated properties such as unread count.
    func didUpdate(group: Group)
}

public protocol GroupMessageObserver: class {
    /// Called when a group's messages update.
    func didUpdate(messages: [GroupMessage], in group: Group)
}

