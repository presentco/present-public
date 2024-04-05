//
//  GroupMessagesViewControllerModel.swift
//  Present
//
//  Created by Dan Federman on 3/24/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import PresentProtos

public protocol GroupMessagesViewControllerModelDelegate: class {
    func setMessages(_ messages: [JSQMessageModel])
    func appendIncomingMessage(_ message: JSQMessageModel)
}

// A logical model holding a copy of the group messages and driving changes to the (jsq) view model via the delegate.
// Note: This is basically an adapter between the group message model and the message view controller (jsq) view model
// Note: that also holds a mixed collection of message related functionality for sending, blocking, etc.
// TODO: I'd like to remove this or clarify its role.  The delegate seems backwards.
public class GroupMessagesViewControllerModel: GroupMessageObserver, LegacyApplicationServices
{
    // This is the owning view controller.
    public weak var delegate: GroupMessagesViewControllerModelDelegate?
    
    public let activityScreenPresenter: RootViewController?
    
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
        
        // TODO: switch to an Rx API
        // Registering with the group triggers an update with the initial group of messages.
        _ = group.register(groupMessageObserver: self, observerQueue: workQueue)
    }
    
    // MARK: Public Properties
    
    // MARK: GroupMessageObserver
    
    // Observe changes to the underlying group messages, cache them here
    public func didUpdate(messages newMessages: [GroupMessage], in group: Group)
    {
        logx("group messages: didUpdate: \(newMessages.count), thread=\(Thread.current), workQueue=\(workQueue)")
        
        // Remove any stale persisted failed messages
        applicationServices.userManager.groupManager.removeFromPersistedFailedMessages(messages: newMessages.filter { !$0.didFailToSend })

        // Add any previously failed messages
        let failedMessages = getFailedMessages(forGroup: group)
        logx("group messages: Adding \(failedMessages.count) previously failed messages to message list")
        let messages = Group.uniqueAndSort(newMessages: newMessages, with: failedMessages)
        
        // Avoid calling the listener again for an identical set of messages
        // But insure that the listener is called at least once, even if for an empty message set.
        // (allowing it to configure views accordingly)
        if !self.messages.isEmpty && messages == self.messages  { return }
        
        let previousMessages = self.messages
        self.messages = messages
        
        if previousMessages.count == 0 {
            main {
                self.delegate?.setMessages(messages.map { JSQMessageModel(groupMessage: $0) })
            }
            
        } else {
            if let lastMessage = messages.last, messages.count == previousMessages.count + 1 && messages.dropLast().map({ $0.messageId }) == previousMessages.map({ $0.messageId }) {
                main {
                    self.delegate?.appendIncomingMessage(JSQMessageModel(groupMessage: lastMessage))
                }
                
            } else {
                main {
                    self.delegate?.setMessages(messages.map { JSQMessageModel(groupMessage: $0) })
                }
            }
        }
        
    }
    
    private func getFailedMessages(forGroup group: Group) -> [GroupMessage]
    {
        guard let me = self.applicationServices.userManager.me.value else { return [] }
        let failedUploadableMessages = self.applicationServices.userManager.groupManager.getFailedMessages(forGroup: group.groupToken)
        logx("faileduploadablemessages = \(failedUploadableMessages)")
        
        return failedUploadableMessages.compactMap {
            return GroupMessage.from(uploadableMessage: $0, forGroup: group, me: me)
        }
    }
    
    public var shouldRevealKeyboardWhenPresented: Bool {
        return false
    }
    
    public func canReportAbuse() -> Bool {
        // Group messages can always report abuse.
        return true
    }
    
    public func canDelete(message: JSQMessageModel) -> Bool {
        // In Groups users can only delete their own messages.
        return message.isMine || (UserManager.shared?.userIsAdmin ?? false)
    }
    
    public func reportAbusive(message: JSQMessageModel, for reason: Group.AbuseReason) {
        logEvent(.circle_chat_report_comment)
        group.reportAsAbusive(messageWithToken: PresentUUID(uuidString: message.messageId), for: reason)
    }
    
    // MARK: Message sending and resending
    
    public func send(message: UploadableMessage, progressListener: ProgressListener?) -> JSQMessageModel?
    {
        guard let me = applicationServices.userManager.me.value else {
            logError("Attempting to send a message before user logged in has been set")
            return nil
        }
        
        let groupMessage = GroupMessage.from(uploadableMessage: message, forGroup: group, me: me)
        
        // Insert the message into our store optimistically, as Group send() will do.
        workQueue.async {
            self.messages.append(groupMessage)
        }

        logEvent(.circle_chat_add_comment) // TODO
        logEvent(.circle_chat_tap_send_comment)
        
        // Note: Group send() inserts the optimistic message into the group messages model prior to sending.
        // Note: Do this on the work queue to guarantee that the above message was inserted (ug).
        // Note: The result should be that when the group observer fires we will already have the message and it
        // Note: will not update the view.
        // TODO: Get rid of all of this.
        workQueue.async {
            self.group.send(message: groupMessage, progressListener: progressListener)
                .onError { error in
                    logError("Failed to upload group message due to error – \(error)")
                    // Note: Group send() replaces the optimisic sent message in the group messages model with a failure message
                    // Note: and persists the failed message.
                }.neverDisposed()
                // Note: If we continue the refactoring and return this Completable to the caller be careful
                // Note: to allow upload to complete in the background.
        }

        return JSQMessageModel(groupMessage: groupMessage)
    }
    
    /// Take the existing JSQ MessageModel and retry upload.
    @discardableResult
    public func resend(message: JSQMessageModel, progressListener: ProgressListener?) -> JSQMessageModel
    {
        // Create the uploadable message simple text or attachment wrapper
        let uploadableMessage: UploadableMessage
        if let attachment = message.attachment?.asUploadable {
            uploadableMessage = .attachmentRetry(messageId: PresentUUID(message.messageId), attachment: attachment, sendDate: message.date)
        } else {
            uploadableMessage = .textRetry(messageId: PresentUUID(message.messageId), messageText: message.text, sendDate: message.date)
        }
        
        guard let sentMessage = send(message: uploadableMessage, progressListener: progressListener) else {
            logError("Failed to retrieve resent message")
            return message
        }
        
        return sentMessage
    }

    public func delete(message: JSQMessageModel, errorHandler: @escaping (UserDisplayableError) -> Void, successHandler: @escaping () -> Void) {
        logEvent(.circle_chat_delete_comment);
        group.delete(messageWithToken: PresentUUID(uuidString: message.messageId)) { (response) in
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
