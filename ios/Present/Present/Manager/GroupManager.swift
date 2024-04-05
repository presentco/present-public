//
//  GroupManager.swift
//  Present
//
//  Created by Dan Federman on 5/9/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import CoreLocation
import Foundation
import PresentProtos
import UserNotifications
import RxSwift
import Then

public final class GroupManager: GroupObserver, LegacyApplicationServices
{
    // MARK: Private Properties
    
    private let disposal = DisposeBag()

    private let service: PresentService
    private let liveServerFactory: (PresentUUID, RequestHeaderGenerator, PresentService) -> GroupLiveUpdatingService
    private let requestHeaderGenerator: RequestHeaderGenerator
    private let userNotificationPoster: UserNotificationPoster
    
    // Note: Dealing with mutual dependency of group manager and person / user manager here.
    // Note: If we can reduce the coupling in these legacy managers we can eliminate this.
    public var personManager: PersonManager?

    public var userManager: UserManager? {
        didSet {
            guard let userManager = userManager else { fatalError() }
            Observable.combineLatest (
                userManager.selectedLocation.observable,
                userManager.isWoman.startWith(false))
            { location, isWoman in
                log("Selected location: \(String(describing: location)) or isWoman status: \(isWoman) changed. Refreshing nearby groups")
                self.refreshNearbyGroups()
            }.neverDisposed()
        }
    }

    private let workQueue = DispatchQueue(label: "GroupManager Work Queue")

    // This holds messages that have arrived via notification before the groups have been loaded
    private var pendingMessagesToAdd = Set<GroupMessage>()

    // TODO: Migrate away from this to a simple group list. Group is a class type and there is no need to
    // TODO: manipulate lists of ids here.
    // The cache of groups by id
    private var groupTokenToGroupMap = [PresentUUID : Group]()

    //
    // MARK: Nearby Groups
    //

    // TODO: Migrate away from this
    // The list of nearby groups as ids available in the group cache
    // Note: This method performs housekeeping and updates observers upon changes.
    private var nearbyPresentUUIDs: [PresentUUID]? {
        didSet {
            nearbyGroupLegacyObservable.set(value: nearbyGroupsList)
            nearbyGroups.value = nearbyGroupsList
        }
    }

    // TODO: Migrate away from this to the Rx
    // Get the nearby groups as an array
    private var nearbyGroupsList: [Group]? {
        return nearbyPresentUUIDs?.compactMap { groupTokenToGroupMap[$0] }
    }

    // TODO: Migrate away from this to the Rx
    private lazy var nearbyGroupLegacyObservable = LegacyObservable<[Group]?, NearbyGroupsObserver>() { value, observer in
        if let value = value {
            observer.nearbyGroupsDidChange(to: value)
        } else {
            observer.nearbyGroupsFailedToLoad()
        }
    }
    
    public let nearbyGroups = ObservableValue<[Group]>()

    //
    // MARK: Joined Groups
    //

    // TODO: Migrate away from this.
    // TODO: Get rid of PresentUUID
    // The list of joined groups as ids available in the group cache
    // Note: Since it is only a list of ids it must be currently called explicitly on changes to cached group metadata.
    private var joinedPresentUUIDs: [PresentUUID]? {
        didSet {
            updateJoinedGroups()
        }
    }

    // Get the joined groups as an array
    private var joinedGroupsList: [Group]? {
        return joinedPresentUUIDs?.compactMap { groupTokenToGroupMap[$0] }
    }
    
    // The public Rx observable joined groups.
    public let joinedGroups = ObservableValue<[Group]>()
    
    // The public Rx observable joined groups.
    public lazy var joinedGroupsUnreadCount: Observable<Int> = self.joinedGroups.observable.map {
        self.calculateJoinedGroupsUnreadCount(joinedGroups: $0)
    }

    // The public Rx observable of combined joined group unread count and incoming friend request count.
    // Note: This is provided because it is used in a couple of places and was historically managed here.
    public lazy var joinedGroupsAndFriendsUnreadCount: Observable<Int> =
        Observable.combineLatest(
            joinedGroupsUnreadCount/*.startWith(0)*/,
            personManager.unwrappedOrFatal().myIncomingFriendRequests.observable()/*.startWith([])*/
        ).map { joinedGroupsUnreadCount, incomingFriendRequests in
            return joinedGroupsUnreadCount + incomingFriendRequests.count
        }

    // MARK: Initialization
    
    public required init(
        service: PresentService,
        liveServerFactory: @escaping (PresentUUID, RequestHeaderGenerator, PresentService) -> GroupLiveUpdatingService = GroupLiveUpdatingService.init,
        requestHeaderGenerator: RequestHeaderGenerator,
        userNotificationPoster: UserNotificationPoster = UNUserNotificationCenter.current())
    {
        self.service = service
        self.liveServerFactory = liveServerFactory
        self.requestHeaderGenerator = requestHeaderGenerator
        self.userNotificationPoster = userNotificationPoster
    }
    
    // MARK: Group retrieval

    /// Create or update group server call
    public func createOrUpdateGroup(
        withToken groupToken: PresentUUID,
        topicName: String,
        location: CLLocationCoordinate2D,
        locationName: String?,
        radius: CLLocationDistance,
        coverPhoto: ImageState,
        description: String? = nil,
        categories: [GroupCategory]? = nil,
        progressListener: ProgressListener?,
        womenOnly: Bool,
        discoverable: Bool,
        preapproval: Present.GroupMemberPreapproval?,
        startDate: Date?,
        stopDate: Date?,
        completionHandler: @escaping (Response<UserDisplayableError, Group>) -> Void
    ) {
        service.createGroup( withToken: groupToken, topicName: topicName, location: location, locationName: locationName, radius: radius, coverPhoto: coverPhoto, description: description, categories: categories, progressListener: progressListener, womenOnly: womenOnly, discoverable: discoverable, preapproval: preapproval, startDate: startDate, stopDate: stopDate
        ) { [weak self] result in
            guard let strongSelf = self else { return }
            
            logEvent(type: .action, "Create group")
            switch result {
            case let .error(errorText):
                completionHandler(.error(errorText))
                
            case let .success(response):
                strongSelf.userManager.unwrappedOrFatal().whenUserAuthorizedToProceedToApp {
                    let newGroup = strongSelf.createGroup(from: response)
                    
                    if let existingGroup = strongSelf.groupTokenToGroupMap[groupToken]
                    {
                        DispatchQueue.main.async {
                            // This was an edit to an existing group.
                            // Copy the metadata to it, allowing any UI to receive the changes.
                            // Batch operation false allows all observers including the group manager to receive the update
                            existingGroup.copyMetadata(from: newGroup, batchOperation: false)

                            // Notify our observers that our groups have updated.
                            // TODO:(dan) This would be unnecessary if group metadata itself was observable. Short of that, this works.
                            if let joinedGroups = strongSelf.joinedGroupsList, joinedGroups.contains(existingGroup) {
                                strongSelf.joinedGroups.value = joinedGroups
                            }
                            if let nearbyGroups = strongSelf.nearbyGroupsList, nearbyGroups.contains(existingGroup) {
                                strongSelf.nearbyGroupLegacyObservable.set(value: nearbyGroups)
                                strongSelf.nearbyGroups.value = nearbyGroups
                            }

                            completionHandler(.success(existingGroup))
                        }
                    } else {
                        // The request created a new group.
                        strongSelf.groupTokenToGroupMap[groupToken] = newGroup
                        if let joinedPresentUUIDs = strongSelf.joinedPresentUUIDs {
                            strongSelf.joinedPresentUUIDs = joinedPresentUUIDs + [newGroup.groupToken]
                            
                        } else {
                            strongSelf.joinedPresentUUIDs = [newGroup.groupToken]
                        }
                        
                        // We don't know if the server considers this group to be Nearby.
                        strongSelf.refreshNearbyGroups()
                        
                        DispatchQueue.main.async {
                            completionHandler(.success(newGroup))
                        }
                    }
                }
            }
        }
    }
    
    // Update the local joined set and call the server
    public func joinGroup(groupId: String)->Single<Present.GroupMembershipState> {
        return service.joinGroup(groupId: groupId).onSuccess { status in
            switch status {
                case .active:
                    self.workQueue.async {
                        // TODO: Get rid of PresentUUID
                        if let joinedPresentUUIDs = self.joinedPresentUUIDs {
                            self.joinedPresentUUIDs = joinedPresentUUIDs + [PresentUUID(groupId)]
                        } else {
                            self.joinedPresentUUIDs = [PresentUUID(groupId)]
                        }
                    }
            default:
                break
            }
        }
    }
    // Update the local joined set and call the server
    public func leaveGroup(groupId: String)->Completable {
        return service.leaveGroup(groupId: groupId).onCompleted {
            self.workQueue.async {
                self.joinedPresentUUIDs = self.joinedPresentUUIDs?.filter({ $0 != PresentUUID(groupId) })
            }
        }
    }

    // Call the server to update read status
    /// - Parameter: lastRead the comment index of the last read comment
    public func markRead(groupId: PresentUUID, lastRead: Int, completionHandler: @escaping (Response<UserDisplayableError, ()>) -> Void) {
        service.markRead(groupId: groupId, lastRead: lastRead, completionHandler: completionHandler)
    }

    public func send(groupMessage: GroupMessage, toGroupWithToken groupToken: PresentUUID, progressListener: ProgressListener?)->Completable
    {
        // Just pass the message to the service.
        return service.sendMessage(groupMessage: groupMessage, toGroupWithToken: groupToken, progressListener: progressListener)
    }
    
    public func reportAbusiveGroup(withToken groupToken: PresentUUID, for reason: Group.AbuseReason, completionHandler: @escaping (Response<UserDisplayableError, ()>) -> Void) {
        // Just pass the message to the service.
        service.reportAbusiveGroup(withToken: groupToken, for: reason, completionHandler: completionHandler)
    }
    
    public func reportAbusiveComment(withToken commentToken: PresentUUID, for reason: Group.AbuseReason, completionHandler: @escaping (Response<UserDisplayableError, ()>) -> Void) {
        // Just pass the message to the service.
        service.reportAbusiveComment(withToken: commentToken, for: reason, completionHandler: completionHandler)
    }
    
    public func delete(groupWithToken groupToken: PresentUUID, completionHandler: @escaping (Response<UserDisplayableError, ()>) -> Void) {
        service.delete(groupWithToken: groupToken) { [weak self] (response) in
            guard let strongSelf = self else {
                return
            }
            
            switch response {
            case .error:
                // TODO:(dan) Let the user know?
                // Nothing to do here.
                break
                
            case .success:
                strongSelf.workQueue.async {
                    strongSelf.groupTokenToGroupMap[groupToken] = nil
                    strongSelf.joinedPresentUUIDs = strongSelf.joinedPresentUUIDs?.filter { $0 != groupToken }
                    strongSelf.nearbyPresentUUIDs = strongSelf.nearbyPresentUUIDs?.filter { $0 != groupToken }
                }
            }
            completionHandler(response)
        }
    }
    
    public func delete(messageWithToken messageToken: PresentUUID, completionHandler: @escaping (Response<UserDisplayableError, ()>) -> Void) {
        // Just pass the message to the service.
        service.delete(messageWithToken: messageToken, completionHandler: completionHandler)
    }
    
    public func mute(groupWithToken groupToken: PresentUUID, completionHandler: @escaping (Response<UserDisplayableError, ()>) -> Void) {
        // Just pass the message to the service.
        service.mute(groupWithToken: groupToken, completionHandler: completionHandler)
    }
    
    public func unmute(groupWithToken groupToken: PresentUUID, completionHandler: @escaping (Response<UserDisplayableError, ()>) -> Void) {
        // Just pass the message to the service.
        service.unmute(groupWithToken: groupToken, completionHandler: completionHandler)
    }
    
    // MARK: Group Retrieval

    public func getPastComments(forGroupWithToken groupToken: PresentUUID, completionHandler: @escaping (Response<UserDisplayableError, [Present.CommentResponse]>) -> Void) {
        // Just pass the message to the service.
        service.getPastComments(forGroupWithToken: groupToken, completionHandler: completionHandler)
    }
    
    // MARK: Legacy Observable Methods
    
    // DEPRECATED.  Use the Rx API.
    public func register(nearbyGroupsObserver: NearbyGroupsObserver, observerQueue: DispatchQueue = .main) {
        _ = self.nearbyGroupLegacyObservable.register(observer: nearbyGroupsObserver, on: observerQueue)
    }
    
    /// Load the nearby groups from the server
    /// Observer side effects: Update the observers
    public func refreshNearbyGroups()
    {
        // Note: we no longer specify a space here for the combined feed.
        service.getNearbyGroups()
            .map { return $0.nearbyGroups }
            .onSuccess { [weak self] nearbyGroupsResponse in
                guard let sself = self else { return }
                sself.workQueue.async {
                    // convert protos to Groups on main queue
                    sself.updateGroups(from: nearbyGroupsResponse) { nearbyGroups in
                        // Update our model on the work queue
                        sself.workQueue.async {
                            // map by uuid
                            nearbyGroups.forEach {
                                sself.groupTokenToGroupMap[$0.groupToken] = $0
                            }

                            sself.insertPendingMessages_inWorkQueue()

                            sself.nearbyPresentUUIDs = nearbyGroups.map {
                                $0.groupToken
                            }

                            //sself.updateCategories(from: nearbyGroups)
                        }
                    }
                }
            }.onError { [weak self] _ in
                guard let sself = self else { return }
                sself.workQueue.async {
                    if sself.nearbyPresentUUIDs == nil {
                        // We don't have nearby groups yet. Make sure our observers know we failed to load.
                        sself.nearbyGroupLegacyObservable.set(value: nil)
                    }
                }
            }.disposed(by: disposal)
    }
    
    /// Load the joined groups from the server
    /// Observer side effects: Update the observers
    public func refreshJoinedGroups()
    {
        UserManager.shared.whenUserAuthorizedToProceedToApp
        {
            logDebug("GroupManager: refreshJoinedGroups")
            self.service.getJoinedGroups { [weak self] (response) in
                guard let strongSelf = self else { return }

                switch response
                {
                    case let .success(joinedGroupProtos):
                        strongSelf.workQueue.async {
                            // update from protos on the main queue
                            strongSelf.updateGroups(from: joinedGroupProtos.groups)
                            { joinedGroups in
                                // Update our model on the work queue
                                strongSelf.workQueue.async {
                                    // cache the groups in the map by id
                                    joinedGroups.forEach {
                                        strongSelf.groupTokenToGroupMap[$0.groupToken] = $0
                                    }

                                    strongSelf.insertPendingMessages_inWorkQueue()

                                    // Update the joined list
                                    var joinedGroupIds = joinedGroups.map { $0.groupToken }

                                    // Debug option to filter joined to only nearby groups
                                    if UserManager.shared.userSession.nearbyJoinedCirclesOnly {
                                        if let nearbyGroupIds = strongSelf.nearbyPresentUUIDs {
                                            joinedGroupIds = joinedGroupIds.filter {
                                                nearbyGroupIds.contains($0)
                                            }
                                        }
                                    }
                                    strongSelf.joinedPresentUUIDs = joinedGroupIds
                                }
                            }
                        }
                    case .error:
                        strongSelf.workQueue.async {
                            if strongSelf.joinedPresentUUIDs == nil {
                                // We use nil in the observable apply block to indicate an error condition
                                //strongSelf.joinedGroupsLegacyObservable.set(value: nil)
                            }
                        }
                }
            }
        }
    }

    /// Given a user-specific group hash from a group URL (e.g. /g/hash) search the local cache for a matching
    /// group and return it if found.
    public func getGroup(byURLHash hash: String) -> Group? {
        logDebug("GroupManager: getGroup by url hash, cache map has: \(groupTokenToGroupMap.count) groups")
        return groupTokenToGroupMap.values.first { $0.shareURL.lastPathComponent == hash }
    }
    
    /// Resolve a group by id or optional group from proto while preserving group identity and observables
    /// for groups already loaded
    /// This method will load the group from the server if needed.
    public func getGroup(
        withToken groupToken: PresentUUID,
        groupProto: Present.GroupResponse? = nil,
        forceRefresh: Bool = false,
        completionHandler: @escaping (Response<UserDisplayableError, Group>) -> Void)
    {
        logDebug("GroupManager: getGroup")
        workQueue.async {
            if !forceRefresh, let existingGroup = self.groupTokenToGroupMap[groupToken] {
                logDebug("GroupManager: getGroup found cached group")
                DispatchQueue.main.async {
                    completionHandler(.success(existingGroup))
                }
            } else {
                // We don't have this group. Refresh our joined and nearby groups.
                //self.refreshNearbyGroups()
                //self.refreshJoinedGroups()
                
                func insert(groupProto: Present.GroupResponse)
                {
                    self.workQueue.async {
                        // If between our previous check and now we found this group already, use the group we already have.
                        if let existingGroup = self.groupTokenToGroupMap[groupToken] {
                            DispatchQueue.main.async {
                                self.updateGroups(from: [groupProto]) { groups in
                                    completionHandler(.success(existingGroup))
                                }
                            }

                        } else {
                            logDebug("GroupManager: getGroup inserting group into cache")
                            // TODO:(dan) Actually get muted data in this RPC rather than guessing.
                            let group = self.createGroup(from: groupProto)
                            self.groupTokenToGroupMap[group.groupToken] = group

                            DispatchQueue.main.async {
                                completionHandler(.success(group))
                            }
                        }
                    }
                }
                
                if !forceRefresh, let groupProto = groupProto {
                    insert(groupProto: groupProto)
                } else {
                    // Get the individual group as well.
                    logDebug("GroupManager: getGroup loading group from server")
                    self.service.getGroup(withToken: groupToken) { (result) in
                        switch result {
                        case let .error(errorText):
                            DispatchQueue.main.async {
                                completionHandler(.error(errorText))
                            }
                            
                        case let .success(groupProto):
                            insert(groupProto: groupProto)
                        }
                    }
                }
            }
        }
    }

    /// Resolve groups from protos while preserving group identity and observables
    /// for groups already loaded
    public func getGroups(from groupProtos: [Present.GroupResponse], completion: @escaping Result<[Group]>) {
        updateGroups(from: groupProtos, completion: completion)
    }
    
    /// Resolve a single group from a proto while preserving group identity and observables
    /// for groups already loaded
    public func getGroup(from groupProto: Present.GroupResponse, completion: @escaping Result<Group>) {
        updateGroups(from: [groupProto], completion: { result in completion(result[0]) })
    }

    /// Received a group message from a notification.
    public func didReceive(groupMessage: GroupMessage) {
        workQueue.async {
            if let existingGroup = self.groupTokenToGroupMap[groupMessage.groupId] {
                logDebug("GroupManager: didReceive message for existing group: \(existingGroup.title)")
                existingGroup.insertOrUpdate(message: groupMessage)
            } else {
                logDebug("GroupManager: didReceive message for absent group: \(groupMessage.groupId)")
                self.pendingMessagesToAdd.insert(groupMessage)
                self.refreshJoinedGroups()
                self.refreshNearbyGroups()
            }
        }
    }

    /// Received a group message without a payload from a notification.
    public func didReceive(groupId: String, commentId: String) {
        logDebug("GroupManager: didReceive comment id update: \(commentId)")
        // TODO: use getComment to retrieve it
    }

    public func purgeAndRefreshLoadedGroups() {
        workQueue.async {
            self.groupTokenToGroupMap.values.forEach {
                $0.reloadPastComments()
            }
            self.refreshJoinedGroups()
            self.refreshNearbyGroups()
        }
    }
    
    // MARK: Private Methods

    /// Receive newly loaded group protos from the server and integrate them into our cache, creating new groups as needed
    /// and updating existing groups.
    /// All metadata is copied on the main thread.
    private func updateGroups(
        from groupProtos: [Present.GroupResponse], completion: @escaping Result<[Group]>)
    {
        DispatchQueue.main.async {
            var groups = [Group]()
            for groupProto in groupProtos {
                groups.append({
                    if let existingGroup = self.groupTokenToGroupMap[PresentUUID(uuidString: groupProto.uuid)] {
                        // Update an existing group
                        // Batch operation true suppresses the listener notifications to this group manager
                        // (individual group listeners are informed as usual).
                        existingGroup.copyMetadata(
                            from: self.createGroup(from: groupProto), batchOperation: true)
                        return existingGroup
                    } else {
                        // Create a new group
                        return self.createGroup(from: groupProto)
                    }
                }())
            }
            completion(groups)
        }
    }
    
    /// Create a new Group from a proto.
    /// See GroupManager getGroup(groupId, proto, ...) for resolving a potentially new group from proto while
    /// preserving group identity and observables for groups already loaded
    // TODO: Get rid of @param me
    private func createGroup(from groupProto: Present.GroupResponse) -> Group
    {
        var coverPhoto: URL? = nil
        var coverPhotoToken: PresentUUID? = nil
        
        if groupProto.hasCover && groupProto.cover.hasContent {
            if let photo = URL(string: groupProto.cover.content) {
                coverPhoto = photo
                coverPhotoToken = PresentUUID(uuidString: groupProto.cover.uuid)
            } else {
                logError("Invalid photo and token pair encountered in parsing a group response.")
            }
        }
        
        let groupToken = PresentUUID(uuidString: groupProto.uuid)

        let owner = personManager.unwrappedOrFatal().getPerson(forProto: groupProto.owner)
        return Group(
            groupToken: groupToken,
            messages: nil,
            created: Date(millisecondsSince1970: groupProto.creationTime),
            topicName: groupProto.title,
            owner: owner,
            description: groupProto.description_,
            categories: Set(groupProto.categories),
            joinedCount: Int(groupProto.memberCount),
            commentCount: Int(groupProto.commentCount),
            lastCommentTime: Date(millisecondsSince1970: groupProto.lastCommentTime),
            shareURL: URL(string: groupProto.url)!,
            location: groupProto.location.as2DCoordinate,
            locationName: groupProto.locationName,
            coverPhoto: coverPhoto,
            coverPhotoToken: coverPhotoToken,
            service: self,
            liveServer: liveServerFactory(groupToken, requestHeaderGenerator, service),
            userNotificationPoster: userNotificationPoster,
            isMuted: groupProto.muted,
            isJoined: groupProto.saved,
            isUnread: groupProto.unread,
            lastRead: Int(groupProto.lastRead),
            schedule: groupProto.schedule,
            preapproval: groupProto.preapprove,
            space: groupProto.hasSpace ? groupProto.space : nil,
            discoverable: groupProto.discoverable,
            joinRequests: Int(groupProto.joinRequests),
            membershipStatus: groupProto.membershipState
        )
    }

    private func insertPendingMessages_inWorkQueue() {
        var insertedPendingMessages = Set<GroupMessage>()
        pendingMessagesToAdd.forEach {
            if let existingGroup = groupTokenToGroupMap[$0.groupId] {
                insertedPendingMessages.insert($0)
                existingGroup.insertOrUpdate(message: $0)
            }
        }

        // Remove the inserted messages from the pending set
        // This is the equivalent of a removeAll(collection), which Swift apparently doesn't have.
        pendingMessagesToAdd.formSymmetricDifference(insertedPendingMessages)
    }


    // MARK: Update

    // Called after groups have changed joined status to perform count housekeeping and update observers
    private func updateJoinedGroups()
    {
        logDebug("GroupManager: updateJoinedGroups")

        // Sanity check that our joined groups are marked correctly:
        //if (joinedGroups?.contains { !$0.isJoined } ?? false) {
            //logDebug("GroupManager: Joined groups mismatch:!!!")
        //}

        main {
            self.joinedGroups.value = self.joinedGroupsList
        }
    }

    /// Calculate the total unread count for the user's joined groups.
    /// The unread count reflects groups with unread messages as well as membership requests
    private func calculateJoinedGroupsUnreadCount(joinedGroups: [Group]) -> Int {
        return joinedGroups.filter {
            let result = $0.isNew || $0.isUnread || ($0.owner.isMe && $0.joinRequests > 0)
            //log("calculate unread count: result=\(result), joined group=\($0.title), isNew=\($0.isNew), isUnread=\($0.isUnread), joinRequests=\($0.joinRequests)")
            return result
            }.count
    }

    private func updateNearbyGroups() {
        logDebug("GroupManager: updateNearbyGroups")
        nearbyGroupLegacyObservable.set(value: nearbyGroupsList)
        main {
            self.nearbyGroups.value = self.nearbyGroupsList
        }
    }

    // MARK: GroupObserver

    /// Called by an individual group to indicate that it has updated metadata or message content.
    /// This method fires the group observers for the relevant collections.
    /// This method is suppressed for batch updates of groups loaded from the server.
    /// Note: GroupManager does not have to register as an observer; groups invoke this update method directly.
    /// Note: This method is also called from the joined and nearby group setters.
    public func didUpdate(group: Group) {
        logDebug("Group manager saw an update to group: \(group.title)")
        // TODO: We should refine these observables to indicate which groups have updated.
        if group.isJoined {
            updateJoinedGroups()
        }
        if let nearbyGroups = nearbyGroupsList {
            if nearbyGroups.contains(group) {
                updateNearbyGroups()
            }
        }
    }
    
    // Group Messages
    public func getGroupMessage(with groupMessageProto: Present.CommentResponse) -> GroupMessage
    {
        return GroupMessage(
            groupToken: PresentUUID(uuidString: groupMessageProto.groupId),
            messageToken: PresentUUID(uuidString: groupMessageProto.uuid),
            index: Int(groupMessageProto.index),
            author: personManager.unwrappedOrFatal().getPerson(forProto: groupMessageProto.author),
            date: Date(millisecondsSince1970: groupMessageProto.creationTime),
            text: groupMessageProto.comment,
            attachment: groupMessageProto.hasContent ? Attachment(proto: groupMessageProto.content) : nil,
            didFailToSend: false
        )
    }
    
    public func getFailedMessages(forGroup groupId: PresentUUID) -> [UploadableMessage]
    {
        let failedMessagesCollection = userManager.unwrappedOrFatal().userSession.failedGroupMessages
        logx("get failed messages for group, count=\(failedMessagesCollection.failedMessages.count)")
        //failedMessagesCollection.failedMessages.forEach { logx("failed message: \($0.messageId), type: \($0.type)") }
        return failedMessagesCollection.failedMessages
            .filter {
                $0.groupId == groupId.uuidString
            }.compactMap { (fgm:FailedGroupMessage)->UploadableMessage? in
                switch(fgm.type) {
                case .text:
                    guard let text = fgm.text else {
                        logError("unable to find text")
                        return nil
                    }
                    return UploadableMessage.textRetry(messageId: PresentUUID(fgm.messageId), messageText: text, sendDate: fgm.firstAttemptedSendDate)
                case .image:
                    guard let cacheKey = fgm.image,
                        let image = applicationServices.imageManager.getStoredImage(forCacheKey: cacheKey) else
                    {
                        logError("unable to find cached image for key: \(fgm.image ?? "nil")")
                        return nil
                    }
                    let attachment = Attachment.Uploadable.localImage(image: image)
                    return UploadableMessage.attachmentRetry(
                        messageId: PresentUUID(fgm.messageId),
                        attachment: attachment,
                        sendDate: fgm.firstAttemptedSendDate)
                }
        }
    }
    
    public func removeFromPersistedFailedMessages(messages: [GroupMessage]) {
        removeFromPersistedFailedMessages(messageIds: messages.map { $0.messageId.uuidString })
    }
    
    public func removeFromPersistedFailedMessages(messageIds: [String]) {
        main {
            //logx("remove persisted failed messages: \(messageIds)")
            let set = Set(messageIds)
            let failedMessagesCollection = self.userManager.unwrappedOrFatal().userSession.failedGroupMessages
            //logx("remove persisted failed messages before: \(failedMessagesCollection.failedMessages.map { $0.messageId}.count)")
            failedMessagesCollection.failedMessages.removeAll {
                set.contains($0.messageId)
            }
            //logx("remove persisted failed messages after: \(failedMessagesCollection.failedMessages.map { $0.messageId}.count)")
            self.userManager.unwrappedOrFatal().userSession.failedGroupMessages = failedMessagesCollection
        }
    }
    
    public func persistFailedMessage(message: GroupMessage)
    {
        main {
            logx("persistFailedMessage add: \(message.messageId.uuidString)")
            let failedMessage: FailedGroupMessage
    
            // If there is an attachment cache it locally and store the cache key
            // Note: currently using the image cache for this but we should switch to something stronger
            let groupId = message.groupId
            if let attachment = message.attachment?.asUploadable
            {
                switch attachment {
                    case .localImage(let image):
                        logx("persist failed message for image")
                        let imageCacheKey = FailedGroupMessage.imageCacheKey(forMessageId: message.messageId.uuidString)
                        self.applicationServices.imageManager.cacheImage(image, forCacheKey: imageCacheKey)
                        failedMessage = FailedGroupMessage(
                            type: .image, groupId: groupId.uuidString, messageId: message.messageId.uuidString, firstAttemptedSendDate: message.date, image: imageCacheKey)
                    case .localMovie:
                        log("warning: attempt to persist movie for failed message")
                        return
                }
            } else {
                failedMessage = FailedGroupMessage(
                    type: .text, groupId: groupId.uuidString, messageId: message.messageId.uuidString, firstAttemptedSendDate: message.date, text: message.text )
            }
    

            // Copy and store it again
            let failedMessagesCollection = self.userManager.unwrappedOrFatal().userSession.failedGroupMessages
            //logx("persist failed messages before: \(failedMessagesCollection.failedMessages.map { $0.messageId}.count)")
            failedMessagesCollection.failedMessages.append(failedMessage)
            //logx("persist failed messages after: \(failedMessagesCollection.failedMessages.map { $0.messageId}.count)")
            self.userManager.unwrappedOrFatal().userSession.failedGroupMessages = failedMessagesCollection
        }
    }
}

public class FailedGroupMessagesCollection: JsonConvertible {
    public var failedMessages: [FailedGroupMessage]
    
    public init(_ failedMessages: [FailedGroupMessage]) {
        self.failedMessages = failedMessages
    }
}

// Note: enums with associated types aren't Codable else we might use one here.
public class FailedGroupMessage: JsonConvertible
{
    public enum MessageType: String, JsonConvertible {
        case text, image
    }
    public var type: MessageType
    
    // The circle to which the user attempted to send the message.
    public var groupId: String
    
    // The unique (client generated) message id for this message.
    // Note: We maintain the id to avoid any potential for duplication in the retry process.
    public var messageId: String
    
    // The date of the first attempt to send - This may be used for ordering.
    public var firstAttemptedSendDate: Date
    
    public var text: String?
    
    // An image stored in the local image cache.
    public var image: CacheKey?
    
    public init(
        type: MessageType, groupId: String, messageId: String, firstAttemptedSendDate: Date,
        text: String? = nil, image: CacheKey? = nil)
    {
        self.type = type
        self.groupId = groupId
        self.messageId = messageId
        self.firstAttemptedSendDate = firstAttemptedSendDate
        self.text = text
        self.image = image
    }
    
    // Return a unique cache key for any associated media item
    public static func imageCacheKey(forMessageId messageId: String) -> String {
        return "failed-message-attachment: \(messageId)"
    }
}


extension FailedGroupMessagesCollection: Then { }
extension FailedGroupMessage: Then { }

// DEPRECATED.  Use the Rx API.
public protocol NearbyGroupsObserver: class {
    func nearbyGroupsDidChange(to nearbyGroups: [Group])
    func nearbyGroupsFailedToLoad()
}

// REMOVE with refactoring to get rid of legacy observable internals
private protocol JoinedGroupsObserver: class {
    func joinedGroupsDidChange(to joinedGroups: [Group])
    func joinedGroupsFailedToLoad()
}
