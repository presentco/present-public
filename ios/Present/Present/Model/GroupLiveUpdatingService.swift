//
//  GroupLiveUpdatingService.swift
//  Present
//
//  Created by Dan Federman on 5/10/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import PresentProtos
import Starscream

public final class GroupLiveUpdatingService: WebSocketDelegate, LegacyApplicationServices
{
    // MARK: Private Static Methods
    
    private static let authorizationKey = "xxx"

    private static var concurrentWebSocketCount: Int = 0 // For sanity check

    // MARK: Private Properties

    private var socket: WebSocket? {
        didSet {
            socket?.delegate = self
            socket?.connect()
        }
    }

    private let service: PresentService
    private var observers = [GroupLiveUpdatingObserver]()
    private let workQueue = DispatchQueue(label: "GroupLiveUpdatingService Work Queue")
    private let requestHeaderGenerator: RequestHeaderGenerator

    private let liveServerURLPromise = PromisedValue<URL>()
    private var retryDelay = RetryDelay(initialDelayMillis: 200, maxDelayMillis: 5000)
    private var requestedLiveServer = false // Have we initiated a request to the live server?

    // MARK: Public Properties
    
    public let groupId: PresentUUID
    
    // MARK: Initialization

    public init(groupToken: PresentUUID, requestHeaderGenerator: RequestHeaderGenerator, service: PresentService) {
        self.groupId = groupToken
        self.requestHeaderGenerator = requestHeaderGenerator
        self.service = service
    }

    private func findLiveServer()
    {
        service.findLiveServer(groupId: groupId) { response in
            switch response {
                case let .error(errorText):
                    logError("Error in find live server: \(errorText)")
                    self.workQueue.asyncAfter(self.retryDelay) { [weak self] in
                        self?.findLiveServer()
                    }
                    break
                case let .success(liveServerResponse):
                    // TODO: Make retry with exponential backoff using RetryDelay a feature of our PresentService.Reponse type
                    self.retryDelay.reset()
                    var urlComponents = URLComponents()
                    urlComponents.scheme = "wss"
                    urlComponents.host = liveServerResponse.host
                    urlComponents.port = Int(liveServerResponse.port)
                    urlComponents.path = "/comments"
                    self.liveServerURLPromise.fulfill(with: urlComponents.url!)
                    break
            }
        }
    }
    
    // MARK: GroupLiveUpdatingService
    
    public func register(observer: GroupLiveUpdatingObserver)
    {
        // One time, serialized initialization of the find live server request.
        workQueue.async {
            if !self.requestedLiveServer { self.findLiveServer() }
        }
        
        liveServerURLPromise.then { url in
            self.workQueue.async {
                if self.socket == nil {
                    self.socket = WebSocket(url: url)

                    // sanity check
                    GroupLiveUpdatingService.concurrentWebSocketCount += 1
                    if GroupLiveUpdatingService.concurrentWebSocketCount > 1 {
                        logDebug("Concurrent web socket count = \(GroupLiveUpdatingService.concurrentWebSocketCount)")
                    }
                }
                self.observers.append(observer)
            }
        }
    }

    // Disconnect the socket and unregister observers
    public func unregisterAllObservers() {
        workQueue.async {
            logDebug("Disconnecting Live Server for \(self.groupId)")
            if let socket = self.socket, socket.isConnected {
                socket.disconnect()
            }
            self.socket = nil
            GroupLiveUpdatingService.concurrentWebSocketCount -= 1
            self.observers.removeAll()
            self.retryDelay.reset() // Reset our backoff.
        }
    }
    
    // MARK: WebSocketDelegate
    
    public func websocketDidConnect(socket: WebSocket) {
        logDebug("Live Server connected for \(groupId)")
        
        let liveCommentRequest = Present.LiveCommentsRequest.getBuilder()
        liveCommentRequest.groupId = groupId.uuidString
        
        // TODO: Temporary workaround for no-login case while live server request requires user id.
        let NO_USER_ID = "00000000-0000-0000-0000-000000000000"
        liveCommentRequest.userId = applicationServices.userManager.me.value?.id ?? NO_USER_ID
        
        liveCommentRequest.header = requestHeaderGenerator.newHeader(authorizationKey: GroupLiveUpdatingService.authorizationKey, location: nil)
        liveCommentRequest.version = 1
        
        socket.write(data: try! liveCommentRequest.build().data()) {
            //logDebug("RPC sent LiveCommentsRequest\n\(liveCommentRequest)")
            logDebug("RPC sent LiveCommentsRequest")
        }
        
        retryDelay.reset()
    }
    
    // TODO: Is this called in response to our own disconnect in unregister?
    public func websocketDidDisconnect(socket: WebSocket, error: NSError?)
    {
        if let error = error {
            logDebug("Live Server disconnected from \(groupId) due to \(error.localizedDescription)")
        } else {
            logDebug("Live Server disconnected from \(groupId)")
        }
        
        workQueue.async {
            // Notify observers that we experienced a disconnect
            self.observers.forEach { $0.didDisconnectFromLiveService(on: self) }

            // Attempt reconnect after a delay
            self.workQueue.asyncAfter(self.retryDelay) { [weak self] in
                guard let strongSelf = self, let socket = strongSelf.socket, !socket.isConnected else {
                    return
                }
                
                logDebug("Live Server attempting to reconnect to \(strongSelf.groupId)")
                socket.connect()
            }
        }
    }
    
    public func websocketDidReceiveMessage(socket: WebSocket, text: String) {
        // Nothing to do here.
    }
    
    public func websocketDidReceiveData(socket: WebSocket, data: Data)
    {
        if let groupMessageProto = try? Present.CommentResponse.parseFrom(data: data) {
            logDebug("CommentResponse\n\(groupMessageProto)")
            
            workQueue.async {
                let groupMessage = GroupMessage(
                    groupToken: self.groupId,
                    messageToken: PresentUUID(uuidString: groupMessageProto.uuid),
                    index: Int(groupMessageProto.index),
                    author: self.applicationServices.personManager.getPerson(forProto: groupMessageProto.author),
                    date: Date(millisecondsSince1970: groupMessageProto.creationTime),
                    text: groupMessageProto.comment,
                    attachment: groupMessageProto.hasContent ? Attachment(proto: groupMessageProto.content) : nil,
                    didFailToSend: false
                )
                
                if groupMessageProto.hasDeleted && groupMessageProto.deleted {
                    self.observers.forEach { $0.didDelete(message: groupMessage, on: self) }
                    
                } else {
                    self.observers.forEach { $0.didReceive(message: groupMessage, on: self) }
                }
            }
            
        } else if let response = try? Present.LiveCommentsResponse.parseFrom(data: data) {
            logDebug("LiveCommentsResponse\n\(response)")
            
            workQueue.async {
                self.observers.forEach { $0.didConnectToLiveService(on: self) }
            }
            
        } else {
            logError("Live Server received unparsable response")
        }
    }
    
}

public protocol GroupLiveUpdatingObserver: class
{
    func didReceive(message: GroupMessage, on liveUpdatingService: GroupLiveUpdatingService)
    func didDelete(message: GroupMessage, on liveUpdatingService: GroupLiveUpdatingService)

    /// Called whenever the live service connects.
    func didConnectToLiveService(on liveUpdatingService: GroupLiveUpdatingService)
    /// Called whenever the live service disconnects.
    func didDisconnectFromLiveService(on liveUpdatingService: GroupLiveUpdatingService)
}


