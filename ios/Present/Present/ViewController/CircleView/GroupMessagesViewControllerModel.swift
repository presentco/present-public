//
//  GroupMessagesViewControllerModel.swift
//  Present
//
//  Created by Dan Federman on 3/24/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import PresentProtos

public class GroupMessagesViewControllerModel: GroupMessageObserver, MessagesViewControllerModel, LegacyApplicationServices
{

    // MARK: Private Properties

    private let group: Group
    private let personPostingService: PersonPostingService
    private let urlResolvingService: URLResolvingService
    private let presentablePresenter: RootViewController
    private let workQueue = DispatchQueue(label: "GroupMessageViewControllerModel Worker Queue")
    private var messages = [GroupMessage]()

    // MARK: Initialization
    
    public init(group: Group, personPostingService: PersonPostingService, urlResolvingService: URLResolvingService,
                screenPresenter: RootViewController) {
        self.group = group
        self.urlResolvingService = urlResolvingService
        self.personPostingService = personPostingService
        self.activityScreenPresenter = screenPresenter
        self.presentablePresenter = screenPresenter
        
        // TODO: deregister
        _ = group.register(groupMessageObserver: self, observerQueue: workQueue)
    }
    
    // MARK: Public Properties
    
    // MARK: GroupMessageObserver
    
    public func didUpdate(messages: [GroupMessage], in group: Group)
    {
        // Avoid calling the listener again for an identical set of messages
        // But insure that the listener is called at least once, even if for an empty message set.
        // (allowing it to configure views accordingly)
        if !self.messages.isEmpty && messages == self.messages  {
            return
        }
        
        let previousMessages = self.messages
        self.messages = messages
        
        if previousMessages.count == 0 {
            DispatchQueue.main.async {
                self.messageListener?.set(messages: messages.map { MessageModel(groupMessage: $0) })
            }
            
        } else {
            if let lastMessage = messages.last, messages.count == previousMessages.count + 1 && messages.dropLast().map({ $0.messageToken }) == previousMessages.map({ $0.messageToken }) {
                DispatchQueue.main.async {
                    self.messageListener?.didReceive(message: MessageModel(groupMessage: lastMessage))
                }
                
            } else {
                DispatchQueue.main.async {
                    self.messageListener?.set(messages: messages.map { MessageModel(groupMessage: $0) })
                }
            }
        }
    }
    
    // MARK: MessagesViewControllerModel
    
    public weak var messageListener: MessageListener?
    
    public let activityScreenPresenter: RootViewController?
    
    public var shouldRevealKeyboardWhenPresented: Bool {
        return false
    }
    
    public func canReportAbuse() -> Bool {
        // Group messages can always report abuse.
        return true
    }
    
    public func canDelete(message: MessageModel) -> Bool {
        // In Groups users can only delete their own messages.
        return message.isMine || (UserManager.shared?.isAdmin ?? false)
    }
    
    public func reportAbusive(message: MessageModel, for reason: Group.AbuseReason) {
        logEvent(.circle_chat_report_comment)
        group.reportAsAbusive(messageWithToken: PresentUUID(uuidString: message.messageToken), for: reason)
    }
    
    public func send(messages: [UploadableMessage], progressListener: ProgressListener?, completionHandler: @escaping () -> Void) -> [MessageModel]
    {
        guard let me = applicationServices.userManager.me.value else {
            logError("Attempting to send a message before me has been set")
            completionHandler()
            return []
        }
        
        let uploadCompleteDispatchGroup = DispatchGroup()
        uploadCompleteDispatchGroup.enter()
        defer {
            uploadCompleteDispatchGroup.leave()
        }
        
        var failedToUploadMessageTokens = [String]()
        uploadCompleteDispatchGroup.notify(queue: .main) {
            completionHandler()
        }
        
        let groupMessagesToUpload: [GroupMessage] = messages.flatMap {
            switch $0 {
            case let .text(text):
                return GroupMessage(newOutgoingMessageWithPresentUUID: group.groupToken, author: me, text: text, attachment: nil)
                
            case let .attachment(uploadableAttachment):
                return GroupMessage(newOutgoingMessageWithPresentUUID: group.groupToken, author: me, text: "", attachment: uploadableAttachment.asAttachment)
            }
        }
        
        workQueue.async {
            self.messages.append(contentsOf: groupMessagesToUpload)
        }

        logEvent(.circle_chat_add_comment) // TODO
        logEvent(.circle_chat_tap_send_comment)
        for groupMessage in groupMessagesToUpload {
            uploadCompleteDispatchGroup.enter()
            group.send(message: groupMessage, progressListener: progressListener) { (response) in
                switch response {
                case let .error(errorText):
                    logError("Failed to upload group message due to error – \(errorText)")
                    failedToUploadMessageTokens.append(groupMessage.messageToken.uuidString)
                    
                case .success:
                    // Nothing to do here.
                    break
                }
                
                uploadCompleteDispatchGroup.leave()
            }
        }
        
        return groupMessagesToUpload.compactMap { MessageModel(groupMessage: $0) }
    }
    
    public func resend(message: MessageModel, progressListener: ProgressListener?, completionHandler: @escaping () -> Void) -> MessageModel {
        let uploadableMessage: UploadableMessage
        if let attachment = message.attachment?.asUploadable {
            uploadableMessage = .attachment(attachment)
            
        } else {
            uploadableMessage = .text(message.text)
        }
        
        guard let sentMessage = send(messages: [uploadableMessage], progressListener: progressListener, completionHandler: completionHandler).first else {
            logError("Failed to retrieve resent message")
            return message
        }
        
        return sentMessage
    }

    public func delete(message: MessageModel, errorHandler: @escaping (UserDisplayableError) -> Void, successHandler: @escaping () -> Void) {
        logEvent(.circle_chat_delete_comment);
        group.delete(messageWithToken: PresentUUID(uuidString: message.messageToken)) { (response) in
            switch response {
            case let .error(errorText):
                errorHandler(errorText)
                
            case .success:
                successHandler()
            }
        }
    }
    
    public func markAsRead() {
        // Nothing to do here yet.
    }
    
    public func presentScreen(for url: URL) {
        logEvent(type: .action, "Opened Web Link From Group")
        urlResolvingService.resolve(url: url) { (response) in
            switch response {
            case let .success(presentable):
                self.presentablePresenter.presentScreen(for: presentable)
                
            case .error:
                // Nothing to do here.
                break
            }
        }
    }
    
    // MARK: PersonPostingService
    
    public func blockUser(withToken userToken: String, completionHandler: @escaping (Response<UserDisplayableError, ()>) -> Void) {
        logEvent(type: .tap, "Block User")
        personPostingService.blockUser(withToken: userToken, completionHandler: completionHandler)
    }
    
    public func unblockUser(withToken userToken: String, completionHandler: @escaping (Response<UserDisplayableError, ()>) -> Void) {
        // Just pass along the message.
        personPostingService.unblockUser(withToken: userToken, completionHandler: completionHandler)
    }
    
}
