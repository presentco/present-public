//
//  UploadableMessage.swift
//  Present
//
//  Created by Dan Federman on 3/21/16.
//

import Foundation

public enum UploadableMessage
{
    /// A text message
    case text(_ messageText: String)
    
    /// A text message that is being retried with the same message id
    case textRetry(messageId: PresentUUID, messageText: String, sendDate: Date)
    
    // An attachment message
    case attachment(_ attachment: Attachment.Uploadable)
    
    /// An attachment message that is being retried with the same message id
    case attachmentRetry(messageId: PresentUUID, attachment: Attachment.Uploadable, sendDate: Date)
}
