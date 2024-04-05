//
//  RemoteNotification.swift
//  Present
//
//  Created by Dan Federman on 3/28/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import PresentProtos
import UserNotifications


public enum RemoteNotification: CustomStringConvertible
{
    // MARK: Private Static Properties

    private static let messageTokenKey = "messageId" // chat message id
    private static let groupIdKey = "groupId" // group id
    private static let commentIdKey = "commentId" // group comment id
    private static let commentKey = "comment" // group comment proto
    private static let userIdKey = "userId" // show user profile by id

    private static let failedToUploadGroupMessage = "failedToUploadGroupMessage"

    // MARK: Initialization

    // Creating a RemoteNotification requires us to know who we are. We shouldn't be receiving remote notifications unless we're logged in.
    public init?(userInfo: [AnyHashable : Any], personManager: PersonManager)
    {
        // This is a notification about a group
        if let groupId = userInfo[RemoteNotification.groupIdKey] as? String
        {
            // Is it a group comment with a comment proto?
            if let groupCommentProtoString = userInfo[RemoteNotification.commentKey] as? String,
                let groupCommentProtoData = Data(base64Encoded: groupCommentProtoString),
                let groupCommentProto = try? Present.CommentResponse.getBuilder().mergeFrom(data: groupCommentProtoData).build()
            {
                logDebug("RemoteNotification: found group comment with proto")
                self = .updateGroupWithComment(groupComment: GroupMessage(with: groupCommentProto, personManager: personManager))
            }
            // Is it a group comment with tokens?
            else if let commentId = userInfo[RemoteNotification.commentIdKey] as? String
            {
                logDebug("RemoteNotification: found group comment with comment id")
                // Group comment notification
                // TODO: add a groupWithComment here to pass the comment
                // TODO: For now we'll just always open the group chat when launched with a group
                self = .updateGroupWithCommentTokens(groupId: groupId, commentId: commentId)
            }
            // Is it a group notification
            else {
                // General group notification, will direct to the group view
                self = .updateGroupWithToken(groupId: groupId)
            }
        }
        else if let userId = userInfo[RemoteNotification.userIdKey] as? String {
            log("RemoteNotification: found user id")
            self = .userId(userId: userId)
        }
        // This is a failed upload local notification???
        else if let failedToUploadGroupMessageData = userInfo[RemoteNotification.failedToUploadGroupMessage] as? Data,
                  let failedToUploadGroupMessage = PersistableGroupMessage(from: failedToUploadGroupMessageData)?.groupMessage {
            self = .failedToSendGroup(message: failedToUploadGroupMessage)

        }
        // Not recognized
        else {
            return nil
        }
    }

    public init?(notificationContent: UNNotificationContent, personManager: PersonManager) {
        self.init(userInfo: notificationContent.userInfo, personManager: personManager)
    }

    // MARK: Cases
    
    // TODO: Rename these to reflect that they are the type of notification recieved - e.g. chatWithMessage?
    // TODO: These names make no sense in a showXXX() context.
    case updateGroupWithComment(groupComment: GroupMessage)
    case updateGroupWithCommentTokens(groupId: String, commentId: String)
    case updateGroupWithToken(groupId: String)
    case userId(userId: String)
    case failedToSendGroup(message: GroupMessage)
    
    // MARK: Public Static Methods
    
    public static func badgeCount(forRemoteNotificationPayload userInfo: [AnyHashable : Any]) -> Int? {
        return (userInfo["aps"] as? [AnyHashable : Any])?["badge"] as? Int
    }
    
    public static func notificationForFailedToUpload(message: GroupMessage) -> UNNotificationRequest {
        let notificationContent = UNMutableNotificationContent()
        notificationContent.body = NSString.localizedUserNotificationString(forKey: "Message failed to send", arguments: nil)
        notificationContent.userInfo = [ failedToUploadGroupMessage : PersistableGroupMessage(groupMessage: GroupMessage(messageDidFailToSend: message)).asData ]
        
        return UNNotificationRequest(identifier: message.messageId.uuidString,
                                     content: notificationContent,
                                     trigger: nil)
    }
    
    // MARK: CustomStringConvertible
    
    public var description: String {
        let description: String
        
        switch self {
            case let .updateGroupWithToken(groupToken):
                description = ".updateGroupWithToken \(groupToken)"
            
            case let .updateGroupWithComment(groupComment):
                description = ".updateGroupWithComment \(groupComment)"
            
            case let .updateGroupWithCommentTokens(groupId, commentId):
                description = ".updateGroupWithCommentTokens \(groupId), \(commentId)"
                
            case let .failedToSendGroup(message):
                description = ".failedToSendGroup \(message)"
            
            case let .userId(userId):
                description = ".userId \(userId)"
        }
        
        return "\(String(describing: type(of: self))): \(description)"
    }
}
