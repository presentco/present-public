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
public class JSQMessageModel: JSQMessage
{
    // MARK: Private Enum
    
    private enum RawMessage {
        case groupMessage(GroupMessage)
        case uploadableMessage(messageToken: String, UploadableMessage)
    }
    
    // MARK: Public Properties
    
    // Unique id for the message.
    public let messageId: String
    
    public let author: Person
    
    public let attachment: Attachment?
    
    public var isMine: Bool {
        return author.isMe
    }
    
    /// Whether this message should display a datestamp. Determined by MessageMetadataDisplayStrategy.
    public var showDate = false
    /// Whether this message should display a timestamp. Determined by MessageMetadataDisplayStrategy.
    public var showTimestamp = false
    /// Whether this message should display a username. Determined by MessageMetadataDisplayStrategy.
    public var showUsername = false

    // MARK: State
    
    public enum State {
        case ready, sending, failed
    }
    
    public var state: State = .ready
    
    public var failedToSend: Bool {
        get {
            return state == .failed
        }
        set {
            if newValue { state = .failed }
        }
    }

    // MARK: Initialization
    
    public convenience init(groupMessage: GroupMessage) {
        self.init(author: groupMessage.author, rawMessage: .groupMessage(groupMessage), failedToSend: groupMessage.didFailToSend)
    }
    
    /// Construct a text or media message from a bubble comment.
    private init(author: Person, rawMessage: RawMessage, failedToSend: Bool) {
        self.author = author
        
        let senderId = author.userToken
        let senderDisplayName = author.name
        
        var date: Date
        let text: String
        switch rawMessage {
        case let .groupMessage(groupMessage):
            messageId = groupMessage.messageId.uuidString
            
            attachment = groupMessage.attachment
            date = groupMessage.date
            text = groupMessage.text
            
        case let .uploadableMessage(messageToken, uploadableMessage):
            self.messageId = messageToken
            date = Date()
            
            switch uploadableMessage
            {
                case let .attachment(uploadableAttachment):
                    attachment = uploadableAttachment.asAttachment
                    text = ""
                
                case let .attachmentRetry(_, uploadableAttachment, sendDate):
                    self.attachment = uploadableAttachment.asAttachment
                    text = ""
                    date = sendDate // date of the original send attempt

                case let .text(message):
                    attachment = nil
                    text = message
                
                case let .textRetry(_, message, sendDate):
                    attachment = nil
                    text = message
                    date = sendDate
            }
        }
        
        if let attachment = attachment {
            super.init(senderId: senderId, senderDisplayName: senderDisplayName, date: date, media: attachment.asMediaItem(outgoing: author.isMe))
            
        } else {
            super.init(senderId: senderId, senderDisplayName: senderDisplayName, date: date, text: text)
        }
        
        self.failedToSend = failedToSend
    }
    
    public required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    // MARK: NSObject
    
    public override func isEqual(_ object: Any?) -> Bool {
        guard let otherMessageModel = object as? JSQMessageModel else {
            return false
        }
        
        return super.isEqual(otherMessageModel)
            && self.messageId == otherMessageModel.messageId
            && self.failedToSend == otherMessageModel.failedToSend
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
    
    override public func mediaViewDisplaySize() -> CGSize {
        return super.mediaViewDisplaySize().with { $0.width = UIScreen.main.bounds.width * 4/5}
    }
    
    override public func mediaView() -> UIView {
        if let view = cachedMediaView {
            return view
        }
        
        // create the media view
        let view = UIImageView()
        view.frame = CGRect(origin: .zero, size: mediaViewDisplaySize())
        view.contentMode = .scaleAspectFill
        view.clipsToBounds = true
        JSQMessagesMediaViewBubbleImageMasker.applyBubbleImageMask(toMediaView: view, isOutgoing:self.appliesMediaViewMaskAsOutgoing)

        switch attachment {
        case let .remoteImage(source):
            view.contentMode = .center
            //view.image = #imageLiteral(resourceName: "present-logo")
            view.backgroundColor = .lightGray
            view.alpha = 0.4
            DispatchQueue.global().async {
                ImageManager.shared.getImage(atURL: source, successHandler: { image in
                    view.contentMode = .scaleAspectFill
                    view.image = image
                    view.alpha = 1.0
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
