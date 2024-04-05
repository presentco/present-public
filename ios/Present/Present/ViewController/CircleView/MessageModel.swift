//
//  MessageModel.swift
//  Present

import Foundation
import JSQMessages
import PresentProtos


/**
 Extend the JSQMessage to add whatever we need from our own model.
 JSQMessage can represent a text or media messages depending on initialization.
 */
public class MessageModel: JSQMessage {
    
    // MARK: Initialization
    
    public init(optimisticMessageModel: MessageModel, confirmedMessageToken: String) {
        messageToken = confirmedMessageToken
        self.author = optimisticMessageModel.author
        self.attachment = optimisticMessageModel.attachment
        self.failedToSend = false
        
        let senderId = optimisticMessageModel.author.userToken
        let senderDisplayName = optimisticMessageModel.author.name
        let date = optimisticMessageModel.date
        if let attachment = attachment {
            super.init(senderId: senderId, senderDisplayName: senderDisplayName, date: date, media: attachment.asMediaItem(outgoing: author.isMe))
            
        } else {
            super.init(senderId: senderId, senderDisplayName: senderDisplayName, date: date, text: optimisticMessageModel.text)
        }
    }
    
    public convenience init(groupMessage: GroupMessage) {
        self.init(author: groupMessage.author, rawMessage: .groupMessage(groupMessage), failedToSend: groupMessage.didFailToSend)
    }
    
    /// Construct a text or media message from a bubble comment.
    private init(author: Person, rawMessage: RawMessage, failedToSend: Bool) {
        self.author = author
        
        let senderId = author.userToken
        let senderDisplayName = author.name
        
        let date: Date
        let text: String
        switch rawMessage {
        case let .groupMessage(groupMessage):
            messageToken = groupMessage.messageToken.uuidString
            
            attachment = groupMessage.attachment
            date = groupMessage.date
            text = groupMessage.text
            
        case let .uploadableMessage(messageToken, uploadableMessage):
            self.messageToken = messageToken
            date = Date()
            
            switch uploadableMessage {
            case let .attachment(uploadableAttachment):
                attachment = uploadableAttachment.asAttachment
                text = ""
                
            case let .text(message):
                attachment = nil
                text = message
            }
        }
        
        self.failedToSend = failedToSend
        
        if let attachment = attachment {
            super.init(senderId: senderId, senderDisplayName: senderDisplayName, date: date, media: attachment.asMediaItem(outgoing: author.isMe))
            
        } else {
            super.init(senderId: senderId, senderDisplayName: senderDisplayName, date: date, text: text)
        }
    }
    
    public required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    // MARK: Public Properties
    
    // Unique id for the message. Could be a comment token, group token, or chat message token.
    public let messageToken: String
    
    public let author: Person
    
    public let attachment: Attachment?
    
    public let failedToSend: Bool
    
    public var isMine: Bool {
        return author.isMe
    }
    
    /// Whether this message should display a datestamp. Determined by MessageMetadataDisplayStrategy.
    public var showDate = false
    /// Whether this message should display a timestamp. Determined by MessageMetadataDisplayStrategy.
    public var showTimestamp = false
    /// Whether this message should display a username. Determined by MessageMetadataDisplayStrategy.
    public var showUsername = false
    
    // MARK: NSObject
    
    public override func isEqual(_ object: Any?) -> Bool {
        guard let otherMessageModel = object as? MessageModel else {
            return false
        }
        
        return super.isEqual(otherMessageModel)
            && self.messageToken == otherMessageModel.messageToken
            && self.failedToSend == otherMessageModel.failedToSend
    }
    
    // MARK: Private Enum
    
    private enum RawMessage {
        case groupMessage(GroupMessage)
        case uploadableMessage(messageToken: String, UploadableMessage)
    }
}


// MARK: - VideoMessageModel


public class VideoMessageModel: JSQVideoMediaItem {
    
    // MARK: Initialization
    
    public init(attachment: Attachment.Movie, outgoing: Bool) {
        self.attachment = attachment
        
        super.init(maskAsOutgoing: outgoing)
        
        switch attachment {
        case let .remoteMovie(source, _):
            fileURL = source
            
        case let .localMovie(source, _):
            fileURL = source
        }
    }
    
    public required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    // MARK: Public Properties
    
    public let attachment: Attachment.Movie
    
    // MARK: JSQMessageMediaData
    
    public override func mediaView() -> UIView {
        if let view = cachedMediaView {
            return view
        }
        
        let size = mediaViewDisplaySize()
        
        let imageView = UIImageView(frame: CGRect(x: 0.0, y: 0.0, width: size.width, height: size.height) )
        imageView.clipsToBounds = true
        imageView.contentMode = .scaleAspectFill // aspect fit would defeat the bubble mask
        
        // Loading background image, color
        imageView.backgroundColor = UIColor.darkGray
        
        switch attachment {
        case let .remoteMovie(_, thumbnailSource):
            ImageManager.shared.getImage(atURL: thumbnailSource, successHandler: { image in
                imageView.image = image
            })
            
        case let .localMovie(_, thumbnail):
            imageView.image = thumbnail
        }
        
        imageView.backgroundColor = UIColor.clear // clear for aspect fit
        
        // Loading play icon overlay
        let iconView = UIImageView(frame: CGRect(x: 0.0, y: 0.0, width: size.width, height: size.height) )
        iconView.clipsToBounds = true
        let playIcon = UIImage.jsq_defaultPlay().jsq_imageMasked(with: UIColor.white)
        //let playIcon = UIImage.jsq_defaultPlayImage()
        iconView.image = playIcon
        iconView.backgroundColor = UIColor.clear
        iconView.contentMode = .center
        
        imageView.addSubview( iconView )
        
        JSQMessagesMediaViewBubbleImageMasker.applyBubbleImageMask(toMediaView: imageView, isOutgoing: self.appliesMediaViewMaskAsOutgoing)
        
        self.cachedMediaView = imageView
        return imageView
    }
    
    public var cachedMediaView: UIView?
    
}


// MARK: - PhotoMessageModel


public class PhotoMessageModel: JSQPhotoMediaItem {
    
    // MARK: Initialization
    
    public init(attachment: Attachment.Photo, outgoing: Bool) {
        self.attachment = attachment
        
        super.init(maskAsOutgoing: outgoing)
    }
    
    public required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    // MARK: Public Properties
    
    public let attachment: Attachment.Photo
    
    // MARK: JSQMessageMediaData
    
    public override func mediaView() -> UIView {
        if let view = cachedMediaView {
            return view
        }
        
        // create the media view
        let size = mediaViewDisplaySize()
        let view = UIImageView()
        view.contentMode = .scaleAspectFill
        view.frame = CGRect(x:0.0, y:0.0, width:size.width, height:size.height)
        view.clipsToBounds = true
        JSQMessagesMediaViewBubbleImageMasker.applyBubbleImageMask(toMediaView: view, isOutgoing:self.appliesMediaViewMaskAsOutgoing)
        
        switch attachment {
        case let .remoteImage(source):
            DispatchQueue.global().async {
                ImageManager.shared.getImage(atURL: source, successHandler: { image in
                    view.image = image
                })
            }
            
        case let .localImage(image):
            view.image = image
        }
        
        self.cachedMediaView = view
        return view
    }
    
    public var cachedMediaView: UIView?
    
}
