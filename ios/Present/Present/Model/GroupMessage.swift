//
//  GroupMessage.swift
//  Present
//
//  Created by Dan Federman on 5/9/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import PresentProtos

// Circle Comment
public struct GroupMessage: Comparable, Hashable
{
    // MARK: Public Properties

    /// Opaque, unique token that identifies the group this message belongs to on the server.
    public let groupId: PresentUUID
    /// Opaque, unique token that identifies the message on the server.
    public let messageId: PresentUUID
    public let author: Person
    public let date: Date
    public let text: String

    public var isOutgoing: Bool {
        return author.isMe
    }

    public let attachment: Attachment?

    public let didFailToSend: Bool // TODO: Replace with a sent status matching the model?

    // The comment index. Maybe be nil for an outgoing message that does not have an index assigned by the server.
    public let index: Int?

    // MARK: Initialization
    
    public init( groupToken: PresentUUID, messageToken: PresentUUID, index: Int?, author: Person, date: Date,
        text: String, attachment: Attachment? = nil, didFailToSend: Bool)
    {
        self.groupId = groupToken
        self.messageId = messageToken
        self.author = author
        self.date = date
        self.text = text
        self.attachment = attachment
        self.didFailToSend = didFailToSend
        self.index = index
    }

    /// @param personManager for resolving the author and potentially other references later
    public init(with groupMessageProto: Present.CommentResponse, personManager: PersonManager)
    {
        self.init(
            groupToken: PresentUUID(uuidString: groupMessageProto.groupId),
            messageToken: PresentUUID(uuidString: groupMessageProto.uuid),
            index: Int(groupMessageProto.index),
            author: personManager.getPerson(forProto: groupMessageProto.author),
            date: Date(millisecondsSince1970: groupMessageProto.creationTime),
            text: groupMessageProto.comment,
            attachment: groupMessageProto.hasContent ? Attachment(proto: groupMessageProto.content) : nil,
            didFailToSend: false
        )
    }

    public init(
        groupId: PresentUUID,
        messageId: PresentUUID = PresentUUID(),
        date: Date = Date(),
        author: Person, text: String, attachment: Attachment?,
        didFailToSend: Bool = false
    )
    {
        self.groupId = groupId
        self.author = author
        self.text = text
        self.attachment = attachment
        self.messageId = messageId
        self.date = date
        self.didFailToSend = didFailToSend
        self.index = nil
    }
    
    public init(messageDidFailToSend message: GroupMessage) {
        self.init(
            groupToken: message.groupId,
            messageToken: message.messageId,
            index: message.index,
            author: message.author,
            date: message.date,
            text: message.text,
            attachment: message.attachment,
            didFailToSend: true
        )
    }
    
    public static func from(uploadableMessage: UploadableMessage, forGroup group: Group, me: Person) -> GroupMessage
    {
        switch uploadableMessage {
            case let .text(text):
                return GroupMessage(groupId: group.groupToken, author: me, text: text, attachment: nil)
    
            case let .textRetry(messageId, text, sendDate):
                return GroupMessage(
                    groupId: group.groupToken, messageId: messageId, date: sendDate, author: me, text: text, attachment: nil, didFailToSend: true)
    
            case let .attachment(uploadableAttachment):
                return GroupMessage(groupId: group.groupToken, author: me, text: "", attachment: uploadableAttachment.asAttachment)
    
            case let .attachmentRetry(messageId, uploadableAttachment, sendDate):
                return GroupMessage(
                    groupId: group.groupToken, messageId: messageId, date: sendDate, author: me, text: "", attachment: uploadableAttachment.asAttachment, didFailToSend: true)
        }
    }
    
    // MARK: Hashable
    
    public var hashValue: Int {
        return messageId.hashValue
    }
    
    // MARK: Comparable

    public static func <(lhs: GroupMessage, rhs: GroupMessage) -> Bool {
        guard lhs.date.timeIntervalSinceReferenceDate != rhs.date.timeIntervalSinceReferenceDate else {
            return lhs.groupId.uuidString < rhs.groupId.uuidString
        }

        return lhs.date.timeIntervalSinceReferenceDate < rhs.date.timeIntervalSinceReferenceDate
    }

    public static func <=(lhs: GroupMessage, rhs: GroupMessage) -> Bool {
        return lhs.date.timeIntervalSinceReferenceDate <= rhs.date.timeIntervalSinceReferenceDate
    }

    public static func >=(lhs: GroupMessage, rhs: GroupMessage) -> Bool {
        return lhs.date.timeIntervalSinceReferenceDate >= rhs.date.timeIntervalSinceReferenceDate
    }

    public static func >(lhs: GroupMessage, rhs: GroupMessage) -> Bool {
        guard lhs.date.timeIntervalSinceReferenceDate != rhs.date.timeIntervalSinceReferenceDate else {
            return lhs.groupId.uuidString > rhs.groupId.uuidString
        }

        return lhs.date.timeIntervalSinceReferenceDate > rhs.date.timeIntervalSinceReferenceDate
    }

    // MARK: Equatable

    public static func ==(lhs: GroupMessage, rhs: GroupMessage) -> Bool {
        return lhs.messageId == rhs.messageId
                && lhs.didFailToSend == rhs.didFailToSend
                && lhs.text == rhs.text
                && lhs.author == rhs.author
                && lhs.date == rhs.date
                // TODO
                //&& lhs.attachment == rhs.attachment
    }
}


// MARK: - PersistableGroupMessage

public final class PersistableGroupMessage: NSObject, NSSecureCoding
{
    
    // MARK: NSSecureCoding
    
    public static var supportsSecureCoding: Bool {
        return true
    }
    
    // MARK: Private Static Properties
    
    private static let groupTokenKey = "groupToken"
    private static let messageTokenKey = "messageToken"
    private static let authorKey = "author"
    private static let dateKey = "date"
    private static let textKey = "text"
    private static let attachmentKey = "attachment"
    private static let didFailToSendKey = "didFailToSend"
    
    // MARK: Initialization
    
    public required init(groupMessage: GroupMessage) {
        self.groupMessage = groupMessage
        
        super.init()
    }
    
    public required init?(coder aDecoder: NSCoder) {
        guard let groupToken = aDecoder.decodeObject(of: NSString.self, forKey: PersistableGroupMessage.groupTokenKey),
            let messageToken = aDecoder.decodeObject(of: NSString.self, forKey: PersistableGroupMessage.messageTokenKey),
            let author = aDecoder.decodeObject(of: PersistablePerson.self, forKey: PersistableGroupMessage.authorKey),
            let date = aDecoder.decodeObject(of: NSDate.self, forKey: PersistableGroupMessage.dateKey),
            let text = aDecoder.decodeObject(of: NSString.self, forKey: PersistableGroupMessage.textKey),
            let didFailToSend = aDecoder.decodeObject(of: NSNumber.self, forKey: PersistableGroupMessage.didFailToSendKey)?.boolValue else {
                return nil
        }
        
        let attachment = aDecoder.decodeObject(of: PersistableAttachment.self, forKey: PersistableGroupMessage.attachmentKey)?.attachment
        
        groupMessage = GroupMessage(groupToken: PresentUUID(uuidString: groupToken),
                                    messageToken: PresentUUID(uuidString: messageToken),
                                    index: nil,
                                    author: author.person,
                                    date: date as Date,
                                    text: String(text),
                                    attachment: attachment,
                                    didFailToSend: Bool(didFailToSend))
        
        super.init()
    }
    
    public required init?(from data: Data) {
        guard let unarchivedSelf = NSKeyedUnarchiver.unarchiveObject(with: data) as? PersistableGroupMessage else {
            return nil
        }
        
        groupMessage = unarchivedSelf.groupMessage
    }
    
    // MARK: NSCoding
    
    public func encode(with aCoder: NSCoder) {
        aCoder.encode(groupMessage.groupId.uuidString as NSString, forKey: PersistableGroupMessage.groupTokenKey)
        aCoder.encode(groupMessage.messageId.uuidString as NSString, forKey: PersistableGroupMessage.messageTokenKey)
        aCoder.encode(PersistablePerson(person: groupMessage.author), forKey: PersistableGroupMessage.authorKey)
        aCoder.encode(groupMessage.date as NSDate, forKey: PersistableGroupMessage.dateKey)
        aCoder.encode(groupMessage.text as NSString, forKey: PersistableGroupMessage.textKey)
        if let attachment = groupMessage.attachment {
            aCoder.encode(PersistableAttachment(attachment: attachment), forKey: PersistableGroupMessage.attachmentKey)
        }
        aCoder.encode(NSNumber(booleanLiteral: groupMessage.didFailToSend), forKey: PersistableGroupMessage.didFailToSendKey)
    }
    
    // MARK: Public Properties
    
    public let groupMessage: GroupMessage
    
    public var asData: Data {
        return NSKeyedArchiver.archivedData(withRootObject: self)
    }
}
