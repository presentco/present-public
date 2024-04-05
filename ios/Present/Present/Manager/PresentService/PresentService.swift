//
//  PresentService.swift
//  Present
//
//  Created by Patrick Niemeyer on 12/18/15.
//  Copyright © 2015 Present Company. All rights reserved.
//

import CoreLocation
import Foundation
import PresentProtos
import ProtocolBuffers
import RxSwift

// TODO: Remove this or rename it as we update legacy code.
// TODO: Note that this never actually was intended as a user displayable message.
public typealias UserDisplayableError = String

public enum PresentServiceError : Error {
    
    // The server or network call failed (may be retried)
    case serverError

    // The client request was invalid or malformed (should not be retried)
    case clientError

    // The client did not understand the server response (should not be retried)
    case invalidResponse
}

/// The network services API
public class PresentService: PersonPostingService, URLResolvingService
{
    // MARK: Public Static Properties
    
    public static let apiVersion = 2
    
    // MARK: Public Properties
    
    public let requestManager = URLRequestManager()
    public let logPayload = false
    public let suppressRpcLoggingFor: Set<String> = ["synchronize", "search"]

    // TODO: currently public so that we can update the selected location. Make this observable.
    public let requestHeaderGenerator: RequestHeaderGenerator

    // MARK: Private Properties
    
    private let locationProvider: LocationProvider
    private let serverEndpoint = Server.api.endpoint
    
    // MARK: Initialization
    
    public init(
        locationProvider: LocationProvider, requestHeaderGenerator: RequestHeaderGenerator,
        backgroundTaskManager: BackgroundTaskManager
    ) {
        self.locationProvider = locationProvider
        self.requestHeaderGenerator = requestHeaderGenerator
        self.requestManager.set(backgroundTaskManager: backgroundTaskManager)
    }
    
    public func getPastActivity(after earliestDate: Date?, completionHandler: @escaping (Response<UserDisplayableError, [Activity]>) -> Void) {
        let protoBuilder = Present.PastActivityRequest.getBuilder()
        if let earliestDate = earliestDate {
            protoBuilder.startTime = earliestDate.millisecondsSince1970
        }
        
        enqueueRequest(
            service: .activity,
            rpc: "getPastActivity",
            protoBuilder: protoBuilder,
            responseBuilder: Present.PastActivityResponse.getBuilder()) { (response) in
                completionHandler(response.translateSuccess(with: { (pastActivityResponse) -> [Activity] in
                    return pastActivityResponse.events.compactMap {
                        guard let target = Presentable(proto: $0.defaultTarget.getOneOfResponse()) else {
                                return nil
                        }
                        
                        return Activity(
                            activityToken: PresentUUID(uuidString: $0.uuid),
                            date: Date(millisecondsSince1970: $0.when),
                            photo: $0.hasIcon ? URL(string: $0.icon) : nil,
                            summary: $0.summary,
                            target: target
                        )
                    }
                }))
        }
    }
    
    // MARK: GroupCreatingService
    
    public func createGroup(
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
        
        completionHandler: @escaping (Response<UserDisplayableError, Present.GroupResponse>) -> Void)
    {
        locationProvider.executeWhenLocationAvailable { [weak self] (userLocation) in
            let protoBuilder = Present.PutGroupRequest.getBuilder()
            protoBuilder.uuid = groupToken.uuidString
            protoBuilder.location = location.asProtoCoordinate
            if let locationName = locationName {
                protoBuilder.locationName = locationName
            }
            protoBuilder.createdFrom = userLocation.coordinate.asProtoCoordinate
            //protoBuilder.radius = radius
            protoBuilder.title = topicName
            
            if let description = description {
                protoBuilder.description_ = description
            }
            if let categories = categories {
                protoBuilder.categories = categories.map { $0.rawValue }
            }
            
            protoBuilder.spaceId =
                womenOnly ? PresentSpace.womenOnly.id : PresentSpace.everyone.id
            
            protoBuilder.discoverable = discoverable
            
            if let preapproval = preapproval {
                protoBuilder.preapprove = preapproval
            }
            
            if let startDate = startDate {
                let scheduleBuilder = protoBuilder.getScheduleBuilder()
                scheduleBuilder.startTime = startDate.millisecondsSince1970
                if let stopDate = stopDate {
                    scheduleBuilder.endTime = stopDate.millisecondsSince1970
                }
                protoBuilder.schedule = try? scheduleBuilder.build()
            }

            func createGroup(with protoBuilder: Present.PutGroupRequest.Builder) {
                self?.enqueueRequest(
                    service: .group,
                    rpc: "putGroup",
                    protoBuilder: protoBuilder,
                    responseBuilder: Present.PutGroupResponse.getBuilder(),
                    progressListener: { progress in progressListener?(Normalized(value: 0.9 + progress.value * 0.1)) }
                ) { response in
                    completionHandler(response.translateSuccess { $0.group })
                }
            }
            
            switch coverPhoto {
            case .none:
                // The created or updated group should have no cover image.
                createGroup(with: protoBuilder)
                
            case let .existing(existingImageId):
                // Utilize a pre-existing cover image already uploaded to the server.
                protoBuilder.cover = try! Present.ContentReferenceRequest.getBuilder()
                    .setType(.jpeg)
                    .setUuid(existingImageId.uuidString)
                    .build()
                
                createGroup(with: protoBuilder)
                
            case let .new(newImage):
                // Upload a new cover image to the server and use the returned PresentUUID to create or update the group.
                self?.uploadImage(image: newImage, progressListener: { progress in progressListener?(Normalized(value: progress.value * 0.9)) }) { response in
                    switch response {
                    case let .error(errorText):
                        completionHandler(.error(errorText))
                        
                    case let .success(imagePresentUUID):
                        protoBuilder.cover = try! Present.ContentReferenceRequest.getBuilder()
                            .setType(.jpeg)
                            .setUuid(imagePresentUUID.uuidString)
                            .build()
                        
                        createGroup(with: protoBuilder)
                    }
                }
            }
        }
    }
    
    // Requests to join a group.
    public func joinGroup(groupId: String)->Single<Present.GroupMembershipState> {
        let protoBuilder = Present.JoinGroupRequest().getBuilder()
        protoBuilder.groupId = groupId
        return Single.create { observer in
            self.enqueueRequest(
                service: .group, rpc: "joinGroup",
                protoBuilder: protoBuilder,
                responseBuilder: Present.JoinGroupResponse.getBuilder(),
                errorHandler: { _ in observer(.error(PresentServiceError.serverError)) },
                successHandler: {
                    if let response = try? $0.build() {
                        observer(.success(response.result))
                    } else {
                        observer(.error(PresentServiceError.invalidResponse))
                   }
                }
            )
            return Disposables.create()
        }
    }
    
    // Leaves a group or revokes a request to join.
    public func leaveGroup(groupId: String)->Completable {
        let protoBuilder = Present.LeaveGroupRequest().getBuilder()
        protoBuilder.groupId = groupId
        return enqueueRequestForEmptyResponse(service: .group, rpc: "leaveGroup", protoBuilder: protoBuilder)
    }
    
    public func sendMessage(
        groupMessage: GroupMessage,
        toGroupWithToken groupToken: PresentUUID,
        progressListener: ProgressListener?)->Completable
    {
        return Completable.create() { observer in
            self.locationProvider.executeWhenLocationAvailable { [weak self] (location) in
                let protoBuilder = Present.PutCommentRequest.getBuilder()
                protoBuilder.uuid = groupMessage.messageId.uuidString
                logx("Send message with message uuid: \(protoBuilder.uuid)")
                protoBuilder.groupId = groupToken.uuidString
    
                let uploadDispatchGroup = DispatchGroup()
                uploadDispatchGroup.enter()

                // Upload any attachments
                if let attachment = groupMessage.attachment?.asUploadable {
                    protoBuilder.comment = "" // Set content since it is required.
    
                    switch attachment {
                    case let .localImage(image):
                        self?.uploadImage(image: image, progressListener: { progress in progressListener?(Normalized(value: progress.value * 0.9)) }) { response in
                            switch response {
                            case .error(_):
                                observer(.error(PresentServiceError.serverError))

                            case let .success(imagePresentUUID):
                                protoBuilder.content = try! Present.ContentReferenceRequest.getBuilder()
                                    .setType(.jpeg)
                                    .setUuid(imagePresentUUID.uuidString)
                                    .build()
    
                                uploadDispatchGroup.leave()
                            }
                        }
    
                    case .localMovie:
                        fatalError("Uploading video is unsupported.")
                    }
    
                } else {
                    protoBuilder.comment = groupMessage.text
                    uploadDispatchGroup.leave()
                }
    
                // Wait for attachment upload if needed
                uploadDispatchGroup.notify(queue: .main) {
                    self?.enqueueRequest(
                        service: .group,
                        rpc: "putComment",
                        protoBuilder: protoBuilder,
                        requestPresentUUID: groupMessage.messageId,
                        responseBuilder: Present.Empty.getBuilder(),
                        progressListener: { progress in progressListener?(Normalized(value: 0.9 + progress.value * 0.1)) }
                    ) { response in
                        switch response {
                        case .error(_):
                            observer(.error(PresentServiceError.serverError))
                        case .success:
                            observer(.completed)
                        }
                    }
                }
            }
            return Disposables.create()
        }
    }
    
    public func reportAbusiveGroup(withToken groupToken: PresentUUID, for reason: Group.AbuseReason, completionHandler: @escaping (Response<UserDisplayableError, ()>) -> Void) {
        let protoBuilder = Present.FlagGroupRequest.getBuilder()
        protoBuilder.groupId = groupToken.uuidString
        protoBuilder.reason = reason.asFlagReasonProto
        
        enqueueRequestForEmptyResponse(
            service: .group,
            rpc: "flagGroup",
            protoBuilder: protoBuilder,
            completionHandler: completionHandler
        )
    }
    
    public func reportAbusiveComment(withToken commentToken: PresentUUID, for reason: Group.AbuseReason, completionHandler: @escaping (Response<UserDisplayableError, ()>) -> Void) {
        let protoBuilder = Present.FlagCommentRequest.getBuilder()
        protoBuilder.commentId = commentToken.uuidString
        protoBuilder.reason = reason.asFlagReasonProto
        
        enqueueRequestForEmptyResponse(
            service: .group,
            rpc: "flagComment",
            protoBuilder: protoBuilder,
            completionHandler: completionHandler
        )
    }
    
    public func delete(groupWithToken groupToken: PresentUUID, completionHandler: @escaping (Response<UserDisplayableError, ()>) -> Void) {
        let protoBuilder = Present.DeleteGroupRequest.getBuilder()
        protoBuilder.groupId = groupToken.uuidString
        
        enqueueRequestForEmptyResponse(
            service: .group,
            rpc: "deleteGroup",
            protoBuilder: protoBuilder,
            completionHandler: completionHandler
        )
    }
    
    public func delete(messageWithToken messageToken: PresentUUID,
                       completionHandler: @escaping (Response<UserDisplayableError, ()>) -> Void)
    {
        let protoBuilder = Present.DeleteCommentRequest.getBuilder()
        protoBuilder.commentId = messageToken.uuidString
        
        enqueueRequestForEmptyResponse(
            service: .group,
            rpc: "deleteComment",
            protoBuilder: protoBuilder,
            completionHandler: completionHandler
        )
    }
    
    public func mute(groupWithToken groupToken: PresentUUID, completionHandler: @escaping (Response<UserDisplayableError, ()>) -> Void) {
        let protoBuilder = Present.MuteGroupRequest.getBuilder()
        protoBuilder.groupId = groupToken.uuidString
        
        enqueueRequestForEmptyResponse(
            service: .group,
            rpc: "muteGroup",
            protoBuilder: protoBuilder,
            completionHandler: completionHandler
        )
    }
    
    public func unmute(groupWithToken groupToken: PresentUUID, completionHandler: @escaping (Response<UserDisplayableError, ()>) -> Void) {
        let protoBuilder = Present.UnmuteGroupRequest.getBuilder()
        protoBuilder.groupId = groupToken.uuidString
        
        enqueueRequestForEmptyResponse(
            service: .group,
            rpc: "unMuteGroup",
            protoBuilder: protoBuilder,
            completionHandler: completionHandler
        )
    }

    public func markRead(groupId: PresentUUID, lastRead: Int, completionHandler: @escaping (Response<UserDisplayableError, Void>) -> Void)
    {
        logDebug("markRead: group=\(groupId), lastRead = \(lastRead)")
        let protoBuilder = Present.MarkReadRequest.getBuilder()
        protoBuilder.groupId = groupId.uuidString
        protoBuilder.lastRead = Int32(lastRead)
        enqueueRequestForEmptyResponse(
            service: .group,
            rpc: "markRead",
            protoBuilder: protoBuilder,
            completionHandler: completionHandler
        )
    }

    /// For contacts with phones matching an existing present user, return the phone and user response.
    public func addContacts(contacts: [PhoneContact]) ->Single<[Present.PhoneUserResponse]>
    {
        let protoBuilder = Present.AddContactsRequest.getBuilder()
        protoBuilder.contacts = contacts.map {
            $0.toContactRequest()
        }
        return Single.create { observer in
            self.enqueueRequest(
                service: .user,
                rpc: "addContacts",
                protoBuilder: protoBuilder,
                responseBuilder: Present.AddContactsResponse.getBuilder(),
                errorHandler: { _ in observer(.error(PresentServiceError.serverError)) },
                successHandler: {
                    if let response = try? $0.build() {
                        observer(.success(response.results))
                    } else {
                        observer(.error(PresentServiceError.invalidResponse))
                    }
                }
            )
            return Disposables.create()
        }
    }

    public func getNearbyGroups(spaceId: String? = nil)->Single<Present.NearbyGroupsResponse>
    {
        logDebug("getNearbyGroups location=\(String(describing: requestHeaderGenerator.selectedLocation)), space=\(String(describing: spaceId))")
        return Single.create { observer in
            self.locationProvider.executeWhenLocationAvailable { [weak self] (location) in
                let protoBuilder = Present.NearbyGroupsRequest.getBuilder()
                protoBuilder.location = location.coordinate.asProtoCoordinate
                    self?.enqueueRequest(
                        service: .group, rpc: "getNearbyGroups",
                        spaceId: spaceId,
                        protoBuilder: protoBuilder,
                        responseBuilder: Present.NearbyGroupsResponse.getBuilder(),
                        errorHandler: { _ in observer(.error(PresentServiceError.serverError)) },
                        successHandler: {
                            if let response = try? $0.build() {
                                observer(.success(response))
                            } else {
                                observer(.error(PresentServiceError.invalidResponse))
                            }
                        }
                    )
                }
                return Disposables.create()
            }
    }

    // TODO: DUPLICATE: REMOVE
    public func getJoinedGroups(
        completionHandler: @escaping (Response<UserDisplayableError, Present.JoinedGroupsResponse>) -> Void)
    {
        return getJoinedGroups(forUser: nil, completionHandler: completionHandler)
    }

    
    // TODO: DUPLICATE: REMOVE
    // @param userId optional user for whom to get saved groups
    public func getJoinedGroups(
        forUser userId: String?,
        completionHandler: @escaping (Response<UserDisplayableError, Present.JoinedGroupsResponse>) -> Void)
    {
        let protoBuilder = Present.JoinedGroupsRequest.getBuilder()
        if let userId = userId {
            protoBuilder.userId = userId
        }
        
        self.enqueueRequest(
            service: .group,
            rpc: "getJoinedGroups",
            protoBuilder: protoBuilder,
            responseBuilder: Present.JoinedGroupsResponse.getBuilder()) { response in
                switch response {
                case let .error(errorText):
                    completionHandler(.error(errorText))
                    
                case let .success(filledResponseBuilder):
                    completionHandler(.success(try! filledResponseBuilder.build()))
                }
        }
    }
    
    // @param userId optional user for whom to get saved groups
    public func getJoinedGroups(forUserId userId: String?)-> Single<Present.JoinedGroupsResponse>
    {
        return Single.create { observer in
            let protoBuilder = Present.JoinedGroupsRequest.getBuilder()
            if let userId = userId {
                protoBuilder.userId = userId
            }
            
            self.enqueueRequest(
                service: .group,
                rpc: "getJoinedGroups",
                protoBuilder: protoBuilder,
                responseBuilder: Present.JoinedGroupsResponse.getBuilder(),
                errorHandler: { _ in observer(.error(PresentServiceError.serverError)) },
                successHandler: {
                    if let response = try? $0.build() {
                        observer(.success(response))
                    } else {
                        observer(.error(PresentServiceError.invalidResponse))
                    }
            }
            )
            return Disposables.create()
        }
    }
    
    public func getGroup(withToken groupToken: PresentUUID, completionHandler: @escaping (Response<UserDisplayableError, Present.GroupResponse>) -> Void)
    {
        let protoBuilder = Present.GroupRequest.getBuilder()
        protoBuilder.groupId = groupToken.uuidString
        
        enqueueRequest(
            service: .group,
            rpc: "getGroup",
            protoBuilder: protoBuilder,
            responseBuilder: Present.GroupResponse.getBuilder()) { response in
                switch response {
                case let .error(errorText):
                    completionHandler(.error(errorText))
                    
                case let .success(filledResponseBuilder):
                    completionHandler(.success(try! filledResponseBuilder.build()))
                }
        }
    }
    
    // MARK: Group
    
    // Deprecated: Use getMembers(forGroupId)
    public func getGroupMembers(withToken groupToken: PresentUUID, completionHandler: @escaping (Response<UserDisplayableError, [Present.UserResponse]>) -> Void)
    {
        let protoBuilder = Present.GroupMembersRequest.getBuilder()
        protoBuilder.groupId = groupToken.uuidString
        
        enqueueRequest(
            service: .group,
            rpc: "getGroupMembers",
            protoBuilder: protoBuilder,
            responseBuilder: Present.GroupMembersResponse.getBuilder()
        ) { response in
                switch response {
                case let .error(errorText):
                    completionHandler(.error(errorText))
                    
                case let .success(filledResponseBuilder):
                    completionHandler(.success(filledResponseBuilder.members))
                }
        }
    }
    
    public func getMembers(forGroupId groupId: String)->Single<[Present.UserResponse]>
    {
        let protoBuilder = Present.GroupMembersRequest.getBuilder()
        protoBuilder.groupId = groupId
        return Single.create { observer in
            self.enqueueRequest(
                service: .group, rpc: "getGroupMembers",
                protoBuilder: protoBuilder,
                responseBuilder: Present.GroupMembersResponse.getBuilder(),
                errorHandler: { _ in observer(.error(PresentServiceError.serverError)) },
                successHandler: {
                    if let response = try? $0.build() {
                        observer(.success(response.members))
                    } else {
                        observer(.error(PresentServiceError.invalidResponse))
                    }
            }
            )
            return Disposables.create()
        }
    }
    
    // Gets requests for membership into the given group.
    public func getMembershipRequests(forGroupId groupId: String)->Single<[Present.MembershipRequest]>
    {
        let protoBuilder = Present.MembershipRequestsRequest().getBuilder()
        protoBuilder.groupId = groupId
        return Single.create { observer in
            self.enqueueRequest(service: .group, rpc: "getMembershipRequests",
                protoBuilder: protoBuilder,
                responseBuilder: Present.MembershipRequestsResponse.getBuilder(),
                errorHandler: { _ in observer(.error(PresentServiceError.serverError)) },
                successHandler: {
                    if let response = try? $0.build() {
                        observer(.success(response.requests))
                    } else {
                        observer(.error(PresentServiceError.invalidResponse))
                    }
                }
            )
            return Disposables.create()
        }
    }

    // Adds members to the group. Users must be friends or have requested an invitation.
    public func addMembers(userIds: [String], phoneNumbers: [PhoneNumber] = [], toGroupId groupId: String)->Completable {
        let protoBuilder = Present.MembersRequest().getBuilder()
        protoBuilder.groupId = groupId
        protoBuilder.userIds = userIds
        protoBuilder.phoneNumbers = phoneNumbers.map { $0.serverString }
        return enqueueRequestForEmptyResponse(service: .group, rpc: "addMembers", protoBuilder: protoBuilder)
    }
    
    // Removes members from the group and/or rejects membership requests.
    public func removeMembers(userIds: [String], fromGroup groupId: String)->Completable {
        let protoBuilder = Present.MembersRequest().getBuilder()
        protoBuilder.groupId = groupId
        protoBuilder.userIds = userIds
        return enqueueRequestForEmptyResponse(service: .group, rpc: "removeMembers", protoBuilder: protoBuilder)
    }

    public func getPastComments(
        forGroupWithToken groupToken: PresentUUID,
        completionHandler: @escaping (Response<UserDisplayableError, [Present.CommentResponse]>) -> Void)
    {
        let protoBuilder = Present.PastCommentsRequest.getBuilder()
        protoBuilder.groupId = groupToken.uuidString
        
        self.enqueueRequest(
            service: .group,
            rpc: "getPastComments",
            protoBuilder: protoBuilder,
            responseBuilder: Present.PastCommentsResponse.getBuilder()) { response in
                switch response {
                case let .error(errorText):
                    completionHandler(.error(errorText))
                    
                case let .success(filledResponseBuilder):
                    completionHandler(.success(filledResponseBuilder.comments))
                }
        }
    }

    public func findLiveServer(groupId: PresentUUID,
       completionHandler: @escaping (Response<UserDisplayableError, Present.FindLiveServerResponse>) -> Void)
    {
        let protoBuilder = Present.FindLiveServerRequest.getBuilder()
        protoBuilder.groupId = groupId.uuidString

        enqueueRequest(
                service: .group,
                rpc: "findLiveServer",
                protoBuilder: protoBuilder,
                responseBuilder: Present.FindLiveServerResponse.getBuilder()) { response in
            switch response {
                case let .error(errorText):
                    completionHandler(.error(errorText))
                case let .success(filledResponseBuilder):
                    completionHandler(.success(try! filledResponseBuilder.build()))
            }
        }
    }
    
    // MARK: PersonPostingService
    
    public func blockUser(withToken userToken: String, completionHandler: @escaping (Response<UserDisplayableError, ()>) -> Void) {
        let protoBuilder = Present.UserRequest.getBuilder()
        protoBuilder.userId = userToken
        
        enqueueRequestForEmptyResponse(
            service: .user,
            rpc: "blockUser",
            protoBuilder: protoBuilder,
            completionHandler: completionHandler
        )
    }
    
    public func unblockUser(withToken userToken: String, completionHandler: @escaping (Response<UserDisplayableError, ()>) -> Void) {
        let protoBuilder = Present.UserRequest.getBuilder()
        protoBuilder.userId = userToken
        
        enqueueRequestForEmptyResponse(
            service: .user,
            rpc: "unblockUser",
            protoBuilder: protoBuilder,
            completionHandler: completionHandler
        )
    }

    // MARK: login and auth status

    public func linkFacebook(
            withFacebookAccessToken facebookAccessToken: String,
            completionHandler: @escaping (Response<UserDisplayableError, Present.AuthorizationResponse>) -> Void)
    {
        let protoBuilder = Present.LinkFacebookRequest.getBuilder()
        protoBuilder.accessToken = facebookAccessToken

        enqueueRequest(
                service: .user,
                rpc: "linkFacebook",
                protoBuilder: protoBuilder,
                responseBuilder: Present.AuthorizationResponse.getBuilder()
        ) { response in
            completionHandler(response.translateSuccess { try! $0.build() })
        }
    }

    // Note: Only called from SyncStatusPoller
    public func synchronizeStatus(
        notificationsEnabled: Bool,
        completionHandler: @escaping (Response<UserDisplayableError, Present.SynchronizeResponse>) -> Void)
    {
        let protoBuilder = Present.SynchronizeRequest.getBuilder()
        protoBuilder.notificationsEnabled = notificationsEnabled
        logDebug("synchronizeStatus: notification status = \(notificationsEnabled)")
        enqueueRequest( service: .user,
                rpc: "synchronize",
                protoBuilder: protoBuilder,
                responseBuilder: Present.SynchronizeResponse.getBuilder()
        ) { response in
            completionHandler(response.translateSuccess { return try! $0.build() })
        }
    }

    public func completeSignup()->Single<Present.AuthorizationResponse>
    {
        return Single.create { observer in
            self.locationProvider.executeWhenLocationAvailable { _ in
                self.enqueueRequest(
                    service: .user,
                    rpc: "completeSignup",
                    protoBuilder: Present.Empty().getBuilder(),
                    responseBuilder: Present.AuthorizationResponse.getBuilder(),
                    errorHandler: { _ in observer(.error(PresentServiceError.serverError)) },
                    successHandler: {
                        if let response = try? $0.build() {
                            observer(.success(response))
                        } else {
                            observer(.error(PresentServiceError.invalidResponse))
                        }
                    }
                )
            }
            return Disposables.create()
        }
    }
    
    public func verify(url: String,
        completionHandler: @escaping (Response<UserDisplayableError, Present.AuthorizationResponse>) -> Void)
    {
        let protoBuilder = Present.VerifyRequest.getBuilder()
        protoBuilder.url = url
        
        enqueueRequest(
            service: .user,
            rpc: "verify",
            protoBuilder: protoBuilder,
            responseBuilder: Present.AuthorizationResponse.getBuilder()
        ) { response in
            completionHandler(response.translateSuccess { try! $0.build() })
        }
    }
    
    public func verify(code: String)->Single<Present.AuthorizationResponse>
    {
        let protoBuilder = Present.VerifyRequest.getBuilder()
        protoBuilder.code = code
        
        return Single.create { observer in
            self.locationProvider.executeWhenLocationAvailable { _ in
                self.enqueueRequest(
                    service: .user,
                    rpc: "verify",
                    priority: URLSessionTask.highPriority,
                    protoBuilder: protoBuilder,
                    responseBuilder: Present.AuthorizationResponse.getBuilder(),
                    errorHandler: { _ in observer(.error(PresentServiceError.serverError)) },
                    successHandler: {
                        if let response = try? $0.build() {
                            observer(.success(response))
                        } else {
                            observer(.error(PresentServiceError.invalidResponse))
                        }
                    }
                )
            }
            return Disposables.create()
        }
    }
    
    // MARK: ProfileService

    public func getUserProfile(completionHandler: @escaping (Response<UserDisplayableError, Present.UserProfile>) -> Void) {
        enqueueRequest(
            service: .user,
            rpc: "getUserProfile",
            responseBuilder: Present.UserProfile.getBuilder()
        ) { response in
            completionHandler(response.translateSuccess { try! $0.build() })
        }
    }
    
    public func setProfilePhoto(to profilePhoto: UIImage, progressListener: ProgressListener?, completionHandler: @escaping (Response<UserDisplayableError, URL>) -> Void) {
        uploadImage(image: profilePhoto,
               progressListener: { progress in progressListener?(Normalized(value: progress.value * 0.9)) },
               completionHandler: { response in
                switch response {
                case let .error(errorText):
                    completionHandler(.error(errorText))
                    
                case let .success(imagePresentUUID):
                    let protoBuilder = Present.PutUserPhotoRequest.getBuilder()
                    protoBuilder.photoRef = try! Present.ContentReferenceRequest.getBuilder()
                        .setType(.jpeg)
                        .setUuid(imagePresentUUID.uuidString)
                        .build()
                    
                    self.enqueueRequest(
                        service: .user,
                        rpc: "putUserPhoto",
                        protoBuilder: protoBuilder,
                        responseBuilder: Present.ContentResponse.getBuilder(),
                        progressListener: { progress in progressListener?(Normalized(value: 0.9 + progress.value * 0.1)) }
                    ) { response in
                        completionHandler(response.translateSuccess { URL(string: $0.content)! })
                    }
                }
        })
    }
    
    public func putUserProfile(userProfile: Present.UserProfileRequest.Builder, completionHandler: @escaping (Response<UserDisplayableError, ()>) -> Void) {
        enqueueRequestForEmptyResponse(
            service: .user,
            rpc: "putUserProfile",
            protoBuilder: userProfile,
            completionHandler: completionHandler
        )
    }
    
    public func setNotificationsEnabled(
        forJoinedGroups notificationsEnabledForJoinedGroups: Bool,
        completionHandler: @escaping (Response<UserDisplayableError, ()>) -> Void)
    {
        let protoBuilder = Present.UserNotificationSettings.getBuilder()
        protoBuilder.userCommentsOnJoinedGroup = notificationsEnabledForJoinedGroups
        
        enqueueRequestForEmptyResponse(
            service: .user,
            rpc: "putUserNotificationSettings",
            protoBuilder: protoBuilder,
            completionHandler: completionHandler
        )
    }
    
    public func setNotificationsEnabled(forUsersJoiningOwnedGroups notificationsEnabledForUsersJoiningOwnedGroups: Bool, completionHandler: @escaping (Response<UserDisplayableError, ()>) -> Void) {
        let protoBuilder = Present.UserNotificationSettings.getBuilder()
        protoBuilder.userJoinsOwnedGroup = notificationsEnabledForUsersJoiningOwnedGroups
        
        enqueueRequestForEmptyResponse(
            service: .user,
            rpc: "putUserNotificationSettings",
            protoBuilder: protoBuilder,
            completionHandler: completionHandler
        )
    }

    public func setNotificationsEnabled(
            forJoinedGroups notificationsEnabledForJoinedGroups: Bool? = nil,
            forUsersJoiningOwnedGroups notificationsEnabledForUsersJoiningOwnedGroups: Bool? = nil,
            completionHandler: @escaping (Response<UserDisplayableError, ()>) -> Void)
    {
        guard notificationsEnabledForJoinedGroups != nil ||
              notificationsEnabledForUsersJoiningOwnedGroups != nil else { return }

        let protoBuilder = Present.UserNotificationSettings.getBuilder()
        if let notificationsEnabledForJoinedGroups = notificationsEnabledForJoinedGroups {
            protoBuilder.userCommentsOnJoinedGroup = notificationsEnabledForJoinedGroups
        }
        if let notificationsEnabledForUsersJoiningOwnedGroups = notificationsEnabledForUsersJoiningOwnedGroups {
            protoBuilder.userJoinsOwnedGroup = notificationsEnabledForUsersJoiningOwnedGroups
        }
        enqueueRequestForEmptyResponse(
                service: .user,
                rpc: "putUserNotificationSettings",
                protoBuilder: protoBuilder,
                completionHandler: completionHandler
        )
    }

    public func searchUsers(
        searchText: String, completionHandler: @escaping (Response<UserDisplayableError, ([Present.UserResponse])>) -> Void)
    {
        let protoBuilder = Present.UserSearchRequest.getBuilder()
        protoBuilder.searchText = searchText
        
        enqueueRequest(
                service: .user,
                rpc: "search",
                protoBuilder: protoBuilder,
                responseBuilder: Present.UsersResponse.getBuilder()
        ) { response in
            completionHandler(response.translateSuccess { try! $0.build().users })
        }
    }

    // DEPRECATED: Remove in favor of the Rx version
    /// Fetch an individual user profile with including all optional data.
    public func getUser(userId: String, completionHandler: @escaping (Response<UserDisplayableError, Present.UserResponse>) -> Void)
    {
        let protoBuilder = Present.UserRequest.getBuilder()
        protoBuilder.userId = userId

        enqueueRequest(
                service: .user,
                rpc: "getUser",
                protoBuilder: protoBuilder,
                responseBuilder: Present.UserResponse.getBuilder()
        ) { response in
            completionHandler(response.translateSuccess { try! $0.build() })
        }
    }
    
    public func getUser(userId: String) -> Single<Present.UserResponse>
    {
        let protoBuilder = Present.UserRequest.getBuilder()
        protoBuilder.userId = userId
        
        return Single.create { observer in
            self.enqueueRequest(
                service: .user,
                rpc: "getUser",
                protoBuilder: protoBuilder,
                responseBuilder: Present.UserResponse.getBuilder(),
                errorHandler: { _ in observer(.error(PresentServiceError.serverError)) },
                successHandler: {
                    if let response = try? $0.build() {
                        observer(.success(response))
                    } else {
                        observer(.error(PresentServiceError.invalidResponse))
                    }
                }
            )
            return Disposables.create()
        }
    }

    public func getFollowing(userId: String, completionHandler: @escaping (Response<UserDisplayableError, Present.UsersResponse>) -> Void)
    {
        userToUsersRequest(rpc: "getFollowing", userId: userId, completionHandler: completionHandler)
    }

    public func getFollowers(userId: String, completionHandler: @escaping (Response<UserDisplayableError, Present.UsersResponse>) -> Void)
    {
        userToUsersRequest(rpc: "getFollowers", userId: userId, completionHandler: completionHandler)
    }

    /// Used for simple calls taking a UserRequest and returning a UsersResponse on the User service
    public func userToUsersRequest(rpc: String, userId: String, completionHandler: @escaping (Response<UserDisplayableError, Present.UsersResponse>) -> Void)
    {
        let protoBuilder = Present.UserRequest.getBuilder()
        protoBuilder.userId = userId

        enqueueRequest(
            service: .user,
            rpc: rpc,
            protoBuilder: protoBuilder,
            responseBuilder: Present.UsersResponse.getBuilder()
        ) { response in
            completionHandler(response.translateSuccess { try! $0.build() })
        }

    }

    public func followUser(userId: String, completionHandler: @escaping (Response<UserDisplayableError, ()>) -> Void)
    {
        let protoBuilder = Present.UserRequest.getBuilder()
        protoBuilder.userId = userId

        enqueueRequestForEmptyResponse(
            service: .user,
            rpc: "follow",
            protoBuilder: protoBuilder,
            completionHandler: completionHandler
        )
    }
    
    //public lazy var followUser = toRxCompletable(followUser)

    public func unfollowUser(userId: String, completionHandler: @escaping (Response<UserDisplayableError, ()>) -> Void)
    {
        let protoBuilder = Present.UserRequest.getBuilder()
        protoBuilder.userId = userId
        
        enqueueRequestForEmptyResponse(
            service: .user,
            rpc: "unfollow",
            protoBuilder: protoBuilder,
            completionHandler: completionHandler
        )
    }

    // MARK: URLResolvingService
    
    public func resolve(url: URL, completionHandler: @escaping (Response<UserDisplayableError, Presentable>) -> Void)
    {
        let protoBuilder = Present.ResolveUrlRequest.getBuilder()
        protoBuilder.url = url.absoluteString
        
        enqueueRequest(
            service: .url,
            rpc: "resolveUrl",
            protoBuilder: protoBuilder,
            responseBuilder: Present.ResolveUrlResponse.getBuilder(),
            errorHandler: { err in
                completionHandler(.error(err))
            },
            successHandler: { response in
                guard let presentable = Presentable(proto: try! response.build().getOneOfResult()) else {
                    // Nothing worth doing here.
                    return
                }
                
                completionHandler(.success(presentable))
        })
    }
    
    // MARK: Misc
    
    public func countGroupReferrals(
        completionHandler: @escaping (Response<UserDisplayableError, UInt32>) -> Void)
    {
        enqueueRequest(service: .group,
            rpc: "countGroupReferrals",
            protoBuilder: Present.Empty().getBuilder(),
            responseBuilder: Present.CountGroupReferralsResponse.getBuilder(),
            errorHandler: { _ in },
            successHandler: { completionHandler(.success($0.membersReferred)) }
        )
    }
    
    public func getGroupReferrals(
        completionHandler: @escaping (Response<UserDisplayableError, [Present.GroupReferralResponse]>) -> Void)
    {
        enqueueRequest(service: .group,
                rpc: "getGroupReferrals",
                protoBuilder: Present.Empty().getBuilder(),
                responseBuilder: Present.GroupReferralsResponse.getBuilder(),
                errorHandler: { _ in },
                successHandler: { completionHandler(.success($0.referrals)) }
        )
    }
    
    public func getCitiesAvailable()->Single<[Present.City]> {
        return Single.create { observer in
            self.enqueueRequest(
                service: .group,
                rpc: "getCities",
                protoBuilder: Present.Empty().getBuilder(),
                responseBuilder: Present.CitiesResponse.getBuilder(),
                errorHandler: { _ in observer(.error(PresentServiceError.serverError)) },
                successHandler: {
                    if let response = try? $0.build() {
                        observer(.success(response.cities))
                    } else {
                        observer(.error(PresentServiceError.invalidResponse))
                    }
            }
            )
            return Disposables.create()
        }
    }
    
    public func getFriends(userId: String)->Single<[Present.UserResponse]> {
        let protoBuilder = Present.UserRequest().getBuilder()
        protoBuilder.userId = userId
        return enqueueRequestForUsersResponse(service: .user, rpc: "getFriends", protoBuilder: protoBuilder)
    }
    
    public func getFacebookFriends()->Single<[Present.UserResponse]> {
        let protoBuilder = Present.Empty().getBuilder()
        return enqueueRequestForUsersResponse(service: .user, rpc: "getFacebookFriends", protoBuilder: protoBuilder)
    }
    
    public func getIncomingFriendRequests()->Single<[Present.UserResponse]> {
        let protoBuilder = Present.Empty().getBuilder()
        return enqueueRequestForUsersResponse(service: .user, rpc: "getIncomingFriendRequests", protoBuilder: protoBuilder)
    }
    
    public func getOutgoingFriendRequests()->Single<[Present.UserResponse]> {
        let protoBuilder = Present.Empty().getBuilder()
        return enqueueRequestForUsersResponse(service: .user, rpc: "getOutgoingFriendRequests", protoBuilder: protoBuilder)
    }
    
    public func addFriend(userId: String)->Single<Present.AddFriendResponse> {
        let protoBuilder = Present.UserRequest.getBuilder()
        protoBuilder.userId = userId
        return addFriend(userRequest: protoBuilder)
    }
    
    public func addFriend(phoneContact: PhoneContact)->Single<Present.AddFriendResponse> {
        let protoBuilder = Present.UserRequest.getBuilder()
        protoBuilder.phoneNumber = phoneContact.phoneNumber.serverString
        return addFriend(userRequest: protoBuilder)
    }

    private func addFriend(userRequest protoBuilder: Present.UserRequest.Builder)->Single<Present.AddFriendResponse>
    {
        return Single.create { observer in
            self.enqueueRequest(
                service: .user,
                rpc: "addFriend",
                protoBuilder: protoBuilder,
                responseBuilder: Present.AddFriendResponse.getBuilder(),
                errorHandler: { _ in observer(.error(PresentServiceError.serverError)) },
                successHandler: {
                    if let response = try? $0.build() {
                        observer(.success(response))
                    } else {
                        observer(.error(PresentServiceError.invalidResponse))
                    }
            }
            )
            return Disposables.create()
        }
            // TESTING
            //.delaySubscription(10.0, scheduler: MainScheduler.instance)
    }
    
    public func removeFriend(userId: String)->Completable {
        let protoBuilder = Present.UserRequest().getBuilder()
        protoBuilder.userId = userId
        return enqueueRequestForEmptyResponse(service: .user, rpc: "removeFriend", protoBuilder: protoBuilder)
            // TESTING
            //.delaySubscription(10.0, scheduler: MainScheduler.instance)
    }
        
    public func getSpacesAvailable(
        completionHandler: @escaping (Response<String, [Present.SpaceResponse]>) -> Void)
    {
        enqueueRequest(service: .user,
                       rpc: "getSpaces",
                       protoBuilder: Present.Empty().getBuilder(),
                       responseBuilder: Present.SpacesResponse.getBuilder(),
                       errorHandler: { _ in },
                       successHandler: { completionHandler(.success($0.spaces)) }
        )
    }
    
    public func getHome(completionHandler: @escaping (Response<UserDisplayableError, String>) -> Void) {
        print("highpriority = \(URLSessionTask.highPriority)")
        enqueueRequest(service: .group,
                rpc: "getHome",
                priority: URLSessionTask.highPriority,
                protoBuilder: Present.Empty().getBuilder(),
                responseBuilder: Present.HtmlResponse.getBuilder(),
                errorHandler: { _ in },
                successHandler: { completionHandler(.success($0.html)) }
        )
    }

    public func getFeedHtml(forSpaceId spaceId: String) -> Single<String>
    {
        logDebug("PresentService: getFeedHtml for space: \(spaceId)")
        return Single.create { observer in
            let protoBuilder = Present.FeedHtmlRequest.getBuilder()
            protoBuilder.spaceId = spaceId
            self.locationProvider.executeWhenLocationAvailable { _ in
                self.enqueueRequest(service: .group,
                    rpc: "getFeedHtml",
                    priority: URLSessionTask.highPriority,
                    protoBuilder: protoBuilder,
                    responseBuilder: Present.HtmlResponse.getBuilder(),
                    errorHandler: { _ in observer(.error(PresentServiceError.serverError)) },
                    successHandler: { observer(.success($0.html)) }
                )
            }
            return Disposables.create()
        }
    }
    
    public func getExploreHtml(forSpaceId spaceId: String? = nil) -> Single<String>
    {
        logDebug("PresentService: getExploreHtml for space: \(String(describing: spaceId))")
        return Single.create { observer in
            let protoBuilder = Present.ExploreHtmlRequest.getBuilder()
            if let spaceId = spaceId {
                protoBuilder.spaceId = spaceId
            }
            self.locationProvider.executeWhenLocationAvailable { _ in
                self.enqueueRequest(service: .group,
                    rpc: "getExploreHtml",
                    priority: URLSessionTask.highPriority,
                    protoBuilder: protoBuilder,
                    responseBuilder: Present.HtmlResponse.getBuilder(),
                    errorHandler: { _ in observer(.error(PresentServiceError.serverError)) },
                    successHandler: { observer(.success($0.html)) }
                )
            }
            return Disposables.create()
        }
    }

    public func getNearbyFeed(completionHandler: @escaping (Response<UserDisplayableError, String>) -> Void) {
        locationProvider.executeWhenLocationAvailable { [weak self] (location) in
            let protoBuilder = Present.NearbyGroupsRequest.getBuilder()
            protoBuilder.location = location.coordinate.asProtoCoordinate
            self?.enqueueRequest(service: .group,
                    rpc: "getNearby",
                    priority: 1.0, // max
                    protoBuilder: protoBuilder,
                    responseBuilder: Present.HtmlResponse.getBuilder(),
                    errorHandler: { _ in },
                    successHandler: { completionHandler(.success($0.html)) }
            )
        }
    }
    
    public func fetchCurrentBuildSupportStatus(successHandler: @escaping (Present.VersionCheckResult.Status, URL) -> Void) {
        enqueueRequest(
            service: .ping,
            rpc: "versionCheck",
            protoBuilder: Present.Empty().getBuilder(),
            responseBuilder: Present.VersionCheckResult.getBuilder(),
            errorHandler: { _ in /* Nothing to do here. */ },
            successHandler: { response in
                guard let upgradeURL = URL(string: response.latestVersionUrl) else {
                    // Nothing worth doing here.
                    return
                }
                
                successHandler(response.status, upgradeURL)
        })
    }

    public func notificationReceived(notificationJson: String, completionHandler: @escaping (Response<UserDisplayableError, ()>) -> Void) {
        let protoBuilder = Present.NotificationReceivedRequest.getBuilder()
        protoBuilder.notification = notificationJson
        enqueueRequestForEmptyResponse(
                service: .user,
                rpc: "notificationReceived",
                protoBuilder: protoBuilder,
                completionHandler: completionHandler
        )
    }

    /** Save or update the device token used in notifications */
    public func updateDeviceToken(deviceToken: String, completionHandler: @escaping (UserDisplayableError?) -> Void) {
        let protoBuilder = Present.PutDeviceTokenRequest.Builder()
        protoBuilder.deviceToken = deviceToken
        
        #if APP_STORE || BETA
            protoBuilder.apnsEnvironment = .production
        #else
            protoBuilder.apnsEnvironment = .sandbox
        #endif
        
        enqueueRequestForEmptyResponse(
            service: .user,
            rpc: "putDeviceToken",
            protoBuilder: protoBuilder,
            errorHandler: completionHandler,
            successHandler: { completionHandler(nil) }
        )
    }
    
    // MARK: Phone Verification
    
    public func requestPhoneVerification(number: PhoneNumber) -> Single<Present.RequestVerificationResponse>
    {
        return Single.create { observer in
            let protoBuilder = Present.RequestVerificationRequest.getBuilder()
            protoBuilder.phoneNumber = number.serverString
            self.enqueueRequest(service: .user,
                rpc: "requestVerification",
                priority: URLSessionTask.highPriority,
                protoBuilder: protoBuilder,
                responseBuilder: Present.RequestVerificationResponse.getBuilder(),
                errorHandler: { _ in
                    observer(.error(PresentServiceError.serverError))
                },
                successHandler: {
                    if let response = try? $0.build() {
                        observer(.success(response))
                    } else {
                        observer(.error(PresentServiceError.invalidResponse))
                    }
                }
            )
            return Disposables.create()
        }
    }
    
    // MARK: Admin functionality
    
    public func getValidStateTransitions(forUser userId: String) -> Single<[Present.ValidStateTransitionResponse]>
    {
        return Single.create { observer in
            let protoBuilder = Present.UserRequest.getBuilder()
            protoBuilder.userId = userId
            self.enqueueRequest( service: .user,
                rpc: "getValidStateTransitions",
                protoBuilder: protoBuilder,
                responseBuilder: Present.ValidStateTransitionsResponse.getBuilder(),
                errorHandler: { _ in
                    observer(.error(PresentServiceError.serverError))
                },
                successHandler: {
                    if let response = try? $0.build() {
                        observer(.success(response.validStateTransitions))
                    } else {
                        observer(.error(PresentServiceError.invalidResponse))
                    }
                }
            )
            return Disposables.create()
        }
    }
    
    public typealias UserStateId = String
    
    public func transitionState(forUser userId: String, toState stateId: UserStateId) -> Completable
    {
        let protoBuilder = Present.TransitionStateRequest.getBuilder()
        protoBuilder.userId = userId
        protoBuilder.stateId = stateId
        return enqueueRequestForEmptyResponse(service: .user, rpc: "transitionState", protoBuilder: protoBuilder)
    }
    
    private func createRequest(for message: GeneratedMessage, requestPresentUUID: PresentUUID, spaceId: String? = nil) -> Present.Request {
        let request = Present.Request.getBuilder()
        request.argument = message.data()
        
        // Add the message header that includes the api key and client uuid for authorization
        request.header = requestHeaderGenerator.newHeader(with: requestPresentUUID, location: locationProvider.location, spaceId: spaceId).data()
        
        return try! request.build()
    }
    
    private func createRequest(for protoBuilder: GeneratedMessageBuilder, requestPresentUUID: PresentUUID, spaceId: String? = nil) -> Present.Request {
        return createRequest(for: try! protoBuilder.build() as! GeneratedMessage, requestPresentUUID: requestPresentUUID, spaceId: spaceId)
    }
    
    // MARK: Caching
    
    private func cache(uploadableMessage: UploadableMessage, contentURLString: String?, thumbnailURLString: String?) {
        switch uploadableMessage {
            
            case let .attachment(uploadableAttachment):
                // Cache our message attachment in our image store so we don't have to download it later.
                cache(uploadableAttachment: uploadableAttachment, contentURLString: contentURLString, thumbnailURLString: thumbnailURLString)
            
            case .attachmentRetry:
                // This is a retry, already cached the image.
                break

            case .text, .textRetry:
                // Nothing to do here.
                break
        }
    }
    
    private func cache(uploadableAttachment: Attachment.Uploadable, contentURLString: String?, thumbnailURLString: String?) {
        switch uploadableAttachment {
        case let .localImage(image):
            if let contentURLString = contentURLString {
                ImageManager.shared.cacheImage(image, forUrl: contentURLString)
            } else {
                logError("Sent image message but no contentURLString received")
            }
            
        case let .localMovie(_, thumbnail):
            if let thumbnailURLString = thumbnailURLString {
                ImageManager.shared.cacheImage(thumbnail, forUrl: thumbnailURLString)
            } else {
                logError("Sent image message but no thumbnailURLString received")
            }
        }
    }
    
    private func uploadImage(
        image: UIImage, progressListener: ProgressListener?, errorHandler: @escaping (UserDisplayableError) -> Void, successHandler: @escaping (PresentUUID) -> Void)
    {
        uploadImage(image: image, progressListener: progressListener) { (response) in
            switch response {
            case let .error(errorText):
                errorHandler(errorText)
                
            case let .success(imagePresentUUID):
                successHandler(imagePresentUUID)
            }
        }
    }
    
    private func uploadImage(
        image: UIImage, progressListener: ProgressListener?, completionHandler: @escaping (Response<UserDisplayableError, (PresentUUID)>) -> Void)
    {
        let imagePresentUUID = PresentUUID()
        let protoBuilder = Present.ContentUploadRequest.getBuilder()
        protoBuilder.uuid = imagePresentUUID.uuidString
        protoBuilder.type = .jpeg
        protoBuilder.content = ImageManager.getJpgPhotoBytesForUpload(image)
        
        enqueueRequest(
            service: .content,
            rpc: "putContent",
            protoBuilder: protoBuilder,
            responseBuilder: Present.ContentResponse.getBuilder(),
            progressListener: progressListener) { response in
                completionHandler(response.translateSuccess { _ in imagePresentUUID })
        }
    }
    

}

// MARK: Request Creation

extension PresentService
{
    // MARK: Private Enums
    
    enum Service: String {
        case activity = "ActivityService"
        case chat = "MessagingService"
        case content = "ContentService"
        case group = "GroupService"
        case ping = "PingService"
        case url = "UrlResolverService"
        case user = "UserService"
    }
    
    private func enqueueRequestForEmptyResponse(
        service: Service, rpc: String, protoBuilder: GeneratedMessageBuilder)->Completable
    {
        return Completable.create { observer in
            self.enqueueRequest(
                service: service, rpc: rpc, protoBuilder: protoBuilder,
                responseBuilder: Present.UsersResponse.getBuilder(),
                errorHandler: { _ in observer(.error(PresentServiceError.serverError)) },
                successHandler: {
                    if let _ = try? $0.build() {
                        observer(.completed)
                    } else {
                        observer(.error(PresentServiceError.invalidResponse))
                    }
            }
            )
            return Disposables.create()
        }
    }
    
    private func enqueueRequestForUsersResponse(
        service: Service, rpc: String, protoBuilder: GeneratedMessageBuilder)->Single<[Present.UserResponse]>
    {
        return Single.create { observer in
            self.enqueueRequest(
                service: service, rpc: rpc, protoBuilder: protoBuilder,
                responseBuilder: Present.UsersResponse.getBuilder(),
                errorHandler: { _ in observer(.error(PresentServiceError.serverError)) },
                successHandler: {
                    if let response = try? $0.build() {
                        observer(.success(response.users))
                    } else {
                        observer(.error(PresentServiceError.invalidResponse))
                    }
            }
            )
            return Disposables.create()
        }
    }
    
    
    /**
        Wrap the message in the header, construct the POST request for it and enqueue it with
        the URL request manager.
     */
    private func enqueueRequest<T>(
        service: Service,
        rpc: String,
        spaceId: String? = nil,
        priority: Float = URLSessionTask.defaultPriority,
        protoBuilder: GeneratedMessageBuilder = Present.Empty().getBuilder(),
        requestPresentUUID: PresentUUID = PresentUUID(),
        responseBuilder: T,
        progressListener: ProgressListener? = nil,
        priority
        completionHandler: @escaping (Response<UserDisplayableError, T>) -> Void) where T: GeneratedMessageBuilder
    {
        if !suppressRpcLoggingFor.contains(rpc) {
            logDebug("---> RPC request: \(rpc)\(logPayload ? "\n"+protoBuilder.description : "")")
        }
        let date = Date()
        let request = createRequest(for: protoBuilder, requestPresentUUID: requestPresentUUID, spaceId: spaceId)
        let urlRequest = createURLRequest(for: request, service: service, rpc: rpc, requestPresentUUID: requestPresentUUID)
        requestManager.enqueue(
            urlRequest: urlRequest,
            progressListener: progressListener,
            completion: { rpcResponse in
                DispatchQueue.main.async {
                    switch rpcResponse {
                    case let .success(_, protoResponse):
                        // Use the type-specific builder to unpack the data.
                        guard let responseMessage = try? responseBuilder.mergeFrom(data: protoResponse.resultBytes) else {
                            logError("\(rpcResponse.rpcName)Response Error: could not unpack response")
                            completionHandler(.error(NSLocalizedString(
                                "PresentServiceErrorReceivedUnexpectedResponse",
                                tableName: nil,
                                bundle: .main,
                                value: "Oops! Present needs a minute.",
                                comment: "Error text for when the server sends down an unexpected response type that the client can't parse.")
                                )
                            )
                            return
                        }
                        
                        if !self.suppressRpcLoggingFor.contains(rpc) {
                            if self.logPayload {
                                logDebug("<--- RPC response: \(rpc), \(rpcResponse.description) – \(Date().millisecondsSince1970 - date.millisecondsSince1970)ms\n\(responseMessage)")
                            } else {
                                logDebug("<--- RPC response: \(rpc), \(Date().millisecondsSince1970 - date.millisecondsSince1970)ms")
                            }
                        }
                        completionHandler(.success(responseMessage))
                        
                    case let .error(_, errorText):
                        logError("\(rpcResponse.description)")
                        completionHandler(.error(errorText))
                    }
                }
        })
    }
    
    private func enqueueRequest<T>(
        service: Service,
        rpc: String,
        spaceId: String? = nil,
        priority: Float = URLSessionTask.defaultPriority,
        protoBuilder: GeneratedMessageBuilder,
        requestPresentUUID: PresentUUID = PresentUUID(),
        responseBuilder: T,
        progressListener: ProgressListener? = nil,
        errorHandler: @escaping (UserDisplayableError) -> Void,
        successHandler: @escaping (T) -> Void) where T: GeneratedMessageBuilder
    {
        enqueueRequest(
            service: service,
            rpc: rpc,
            spaceId: spaceId,
            protoBuilder: protoBuilder,
            responseBuilder: responseBuilder
        ) { response in
            switch response {
            case let .error(errorText):
                errorHandler(errorText)
                
            case let .success(filledResponseBuilder):
                successHandler(filledResponseBuilder)
            }
        }
    }
    
    /**
     Wrap the message in the header, construct the POST request for it and enqueue it with
     the URL request manager.
     @return the request uuid, which is often used as the new entity id
     */
    private func enqueueRequestForEmptyResponse(
        service: Service,
        rpc: String,
        protoBuilder: GeneratedMessageBuilder,
        requestPresentUUID: PresentUUID = PresentUUID(),
        progressListener: ProgressListener? = nil,
        completionHandler: @escaping (Response<UserDisplayableError, ()>) -> Void)
    {
        if !suppressRpcLoggingFor.contains(rpc) {
            logDebug("---> RPC request: \(rpc)\(logPayload ? "\n"+protoBuilder.description : "")")
        }
        let date = Date()
        let request = createRequest(for: protoBuilder, requestPresentUUID: requestPresentUUID)
        let urlRequest = createURLRequest(for: request, service: service, rpc: rpc, requestPresentUUID: requestPresentUUID)
        requestManager.enqueue(
            urlRequest: urlRequest,
            progressListener: progressListener,
            completion: { rpcResponse in
                DispatchQueue.main.async {
                    switch rpcResponse {
                    case .success:
                        if !self.suppressRpcLoggingFor.contains(rpc) {
                            if self.logPayload {
                                logDebug("<--- RPC response: \(rpc)\n\(rpcResponse.description) – \(Date().millisecondsSince1970 - date.millisecondsSince1970)ms")
                            } else {
                                logDebug("<--- RPC response: \(rpc), \(Date().millisecondsSince1970 - date.millisecondsSince1970)ms")
                            }
                        }
                        completionHandler(.success(()))
                        
                    case let .error(_, errorText):
                        logError("\(rpcResponse.rpcName)Response Error: \(rpcResponse.description)")
                        completionHandler(.error(errorText))
                    }
                }
        })
    }
    
    private func enqueueRequestForEmptyResponse(
        service: Service,
        rpc: String, protoBuilder: GeneratedMessageBuilder,
        requestPresentUUID: PresentUUID = PresentUUID(),
        progressListener: ProgressListener? = nil,
        errorHandler: @escaping (UserDisplayableError) -> Void,
        successHandler: @escaping () -> Void)
    {
        enqueueRequestForEmptyResponse(
            service: service,
            rpc: rpc,
            protoBuilder: protoBuilder,
            requestPresentUUID: requestPresentUUID,
            progressListener: progressListener) { response in
                switch response {
                case let .error(errorText):
                    errorHandler(errorText)
                    
                case .success:
                    successHandler()
                }
        }
    }
    
    // Create the MutableURLRequest to post to the API server
    // @param requestPresentUUID is technically redundant because it is already encoded
    // in the header, but passed here for use as a trace id.
    private func createURLRequest(for request: Present.Request, service: Service, rpc: String, requestPresentUUID: PresentUUID) -> URLRequest {
        let url = serverEndpoint.appendingPathComponent("api")
            .appendingPathComponent(service.rawValue)
            .appendingPathComponent(rpc)
        
        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = "post"
        
        let data = request.data()
        urlRequest.httpBody = data as Data
        
        // Trace header
        let traceHeader = "X-Cloud-Trace-Context"
        let TRACE_ID = requestPresentUUID.uuidString
        let SPAN_ID = 0 // Could be used to logically group requests
        let TRACE_TRUE = 1
        let traceHeaderValue = "\(TRACE_ID)/\(SPAN_ID)o=\(TRACE_TRUE)"
        urlRequest.setValue(traceHeaderValue, forHTTPHeaderField: traceHeader)
        
        // Support gzip
        let acceptEncoding = "Accept-Encoding"
        //let defaultAcceptEncoding = urlRequest.value(forHTTPHeaderField: acceptEncoding)
        //logDebug("default accept encoding = \(defaultAcceptEncoding)")
        urlRequest.setValue("gzip", forHTTPHeaderField: acceptEncoding)
        
        // Modify user agent to include gzip term as appengine flag
        let userAgent = "User-Agent"
        let defaultUserAgent = urlRequest.value(forHTTPHeaderField: userAgent) ?? "iOS Present App \(PresentService.apiVersion)"
        //logDebug("default user agent = \(defaultUserAgent)")
        urlRequest.setValue(defaultUserAgent + " (gzip)", forHTTPHeaderField: userAgent)
        
        return urlRequest
    }
    
}

public enum ImageState {
    case none
    case new(UIImage)
    case existing(PresentUUID)
}

public protocol URLResolvingService {
    func resolve(url: URL, completionHandler: @escaping (Response<UserDisplayableError, Presentable>) -> Void)
}
