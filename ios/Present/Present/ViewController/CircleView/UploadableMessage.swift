//
//  UploadableMessage.swift
//  Present
//
//  Created by Dan Federman on 3/21/16.
//

import Foundation


public enum UploadableMessage {
    case text(String)
    case attachment(Attachment.Uploadable)
}
