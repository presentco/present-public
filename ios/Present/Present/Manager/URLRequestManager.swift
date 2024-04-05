//
//  URLRequestManager.swift
//
//  Karl Adam on 6/12/15.
//  Pat Niemeyer
//

import Foundation
import PresentProtos


public enum RPCResponse: CustomStringConvertible {
    
    case success(URLRequest, Present.Response)
    case error(URLRequest, String)
    
    // MARK: Public Properties
    
    public var rpcName: String {
        let originalRequest: URLRequest
        switch self {
        case let .success(request, _):
            originalRequest = request
            
        case let .error(request, _):
            originalRequest = request
        }
        
        return originalRequest.url?.lastPathComponent ?? ""
    }
    
    // MARK: CustomStringConvertible
    
    public var description: String {
        switch self {
        case .success:
            return "\(rpcName)Response"
            
        case let .error(_, errorText):
            return "\(rpcName)Response: \(errorText)"
        }
    }
    
}


public typealias ProgressListener = (Normalized<Float>) -> Void


public final class URLRequestManager
{
    // MARK: Public Static Properties
    
    public static let maximumConcurrentRequests = 4

    // MARK: Private Properites

#if DEBUG
    /// Simluate slow connection for testing.
    private let debug_networkResponseSimulatedDelayInMilliseconds = UInt64(0.0) * MSEC_PER_SEC
#endif

    private let urlSession: URLSession
    private let sessionDelegate = SessionDelegate()

    private weak var backgroundTaskManager: BackgroundTaskManager?
    private var requestToBackgroundTaskIdentifierMap = [URLRequest : UIBackgroundTaskIdentifier]()


    // MARK: Initialization
    
    public required init() {
        let urlSessionConfig = URLSessionConfiguration.default
        urlSessionConfig.timeoutIntervalForRequest = 30

        let networkQueue = OperationQueue()
        networkQueue.maxConcurrentOperationCount = URLRequestManager.maximumConcurrentRequests
        
        urlSession = URLSession(configuration: urlSessionConfig, delegate: sessionDelegate, delegateQueue: networkQueue)
        urlSession.sessionDescription = "Present URL Requests Manager Session"
    }
    
    // MARK: Public Methods
    
    public func set(backgroundTaskManager: BackgroundTaskManager) {
        self.backgroundTaskManager = backgroundTaskManager
    }

    @discardableResult public func enqueue(
        urlRequest request: URLRequest,
        priority: Float = URLSessionTask.defaultPriority,
        progressListener: ProgressListener? = nil,
        completion: @escaping (RPCResponse) -> Void
    ) -> URLSessionDataTask
    {
        DispatchQueue.main.async {
            self.requestToBackgroundTaskIdentifierMap[request] = self.backgroundTaskManager?.beginBackgroundTask(expirationHandler: {
                logError("\(request.url?.lastPathComponent ?? "")Request failed in background.")
            })
        }
        
        let task = urlSession.dataTask(with: request)
        { (data, response, error) in
            #if DEBUG
                if self.debug_networkResponseSimulatedDelayInMilliseconds > UInt64(0.0) {
                    usleep(UInt32(self.debug_networkResponseSimulatedDelayInMilliseconds))
                }
            #endif

            defer {
                DispatchQueue.main.async {
                    if let backgroundTaskIdentifier = self.requestToBackgroundTaskIdentifierMap[request] {
                        self.backgroundTaskManager?.endBackgroundTask(backgroundTaskIdentifier)
                    }
                }
            }
            
            guard error == nil else {
                logDebug("Error in response from: \(request.url?.lastPathComponent ?? ""), \(String(describing: error))")
                completion(.error(request, self.humanReadableError(for: error! as NSError)))
                return
            }
            
            let orgResponse = response;
            guard let response = response as? HTTPURLResponse else {
                logDebug("Error in response from: \(request.url?.lastPathComponent ?? ""), response: \(String(describing: orgResponse))")
                completion(.error(request, NSLocalizedString("URLRequestManagerErrorReceivedIncompleteResponse",
                                                             tableName: nil, bundle: .main,
                                                             value: "Present encountered a network problem. Wait a minute and then try again.",
                                                             comment: "Error text for when the server sends down a response with a type that the client can't parse.")))
                return
            }

            //logDebug("HTTPUrlResponse headers = \(response.allHeaderFields)")

            guard response.statusCode == 200 else {
                logDebug("Error in response from: \(request.url?.lastPathComponent ?? ""), response code: \(response.statusCode), response = \(response)")
                completion(.error(request, self.humanReadableError(forHTTPStatusCode: response.statusCode)))
                return
            }
            
            guard let data = data else {
                logDebug("Error no data in response from: \(request.url?.lastPathComponent ?? ""), response code: \(response.statusCode), response = \(response)")
                completion(.error(request, NSLocalizedString("URLRequestManagerErrorReceivedIncompleteResponse",
                                                             tableName: nil, bundle: .main,
                                                             value: "Present is having issues right now. Try again.",
                                                             comment: "Error text for when the server sends down an incomplete response.")))
                return
            }
            
            guard let protoResponse = try? Present.Response.parseFrom(data: data) else {
                logDebug("Error in proto response from: \(request.url?.lastPathComponent ?? ""), response code: \(response.statusCode), response = \(response)")
                completion(.error(request, NSLocalizedString("PresentServiceErrorReceivedUnparsableEmptyResponse",
                                                             tableName: nil, bundle: .main,
                                                             value: "Present encountered a problem. Wait a minute and then try again.",
                                                             comment: "Error text for when the server sends down a response that the client can't parse.")))
                return
            }
            
            guard !protoResponse.hasError else {
                // The server encountered an error. We should never hit this. Instead we should receive a valid proto response that contains error information.
                logDebug("Received server error response for \(request.url?.lastPathComponent ?? "Unknown"), \(protoResponse.error)")
                completion(.error(request, NSLocalizedString("URLRequestManagerErrorReceivedErrorResponse",
                                                             tableName: nil, bundle: .main,
                                                             value: "Oops! Present encountered a problem.",
                                                             comment: "Error text for when the server sends down a response telling the client something went wrong.")))
                return
            }
            
            completion(.success(request, protoResponse))
        }
        task.priority = priority

        if let progressListener = progressListener {
            sessionDelegate.set(progressListener: progressListener, for: task)
        }
        
        task.resume()
        return task
    }
    
    // MARK: Private Methods
    
    private func humanReadableError(for error: NSError) -> String {
        if error.domain == NSURLErrorDomain {
            switch error.code {
            case NSURLErrorNotConnectedToInternet:
                return NSLocalizedString("URLRequestManagerErrorNotConnectedToInternet",
                                         tableName: nil, bundle: .main,
                                         value: "Could not connect to the internet.",
                                         comment: "Error text for when the client can't connect to the internet.")
            default:
                logError("Received unexpected URLError \(error.code)")
                return NSLocalizedString("URLRequestManagerErrorUnknown",
                                         tableName: nil, bundle: .main,
                                         value: "Present couldn't connect to the server.",
                                         comment: "Error text for when the client couldn't connect to the server.")
            }
            
        } else {
            // NSErrors should be system/network errors, not HTTP errors.
            return NSLocalizedString("URLRequestManagerErrorReceivedUnparseableError",
                                     tableName: nil, bundle: .main,
                                     value: "Oops! Present couldn't do that. Wait a minute and try again.",
                                     comment: "Error text for when the server sends down an error the client can't parse.")
        }
    }
    
    private func humanReadableError(forHTTPStatusCode statusCode: Int) -> String {
        switch statusCode {
        case 400, 404:
            return NSLocalizedString("URLRequestManagerErrorReceivedHTTPBadRequest",
                                     tableName: nil, bundle: .main,
                                     value: "Present encountered a problem! Please check the App Store for updates.",
                                     comment: "Error text for when the client sent a malformed request to the server. This usually indicates the app is old and needs to be updated.")
        default:
            return NSLocalizedString("URLRequestManagerErrorReceivedBadServerResponse",
                                     tableName: nil, bundle: .main,
                                     value: "Present encountered a problem! Please try again in a few minutes.",
                                     comment: "Error text for when the server tells the client the server is having issues.")
        }
    }
    
    // MARK: Private Class
    
    private final class SessionDelegate: NSObject, URLSessionDelegate, URLSessionTaskDelegate {
        
        // MARK: URLSessionDelegate
        
        public func urlSession(_ session: URLSession, didReceive challenge: URLAuthenticationChallenge, completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {
            guard let serverTrust = challenge.protectionSpace.serverTrust else {
                logError("No server trust found")
                completionHandler(.cancelAuthenticationChallenge, nil)
                return
            }
            
            completionHandler(.useCredential, URLCredential(trust: serverTrust))
        }
        
        public func urlSession(_ session: URLSession, task: URLSessionTask, didSendBodyData bytesSent: Int64, totalBytesSent: Int64, totalBytesExpectedToSend: Int64) {
            workQueue.async {
                if let progress = self.sessionTaskToProgressListenerMap[task] {
                    progress(Normalized(value: Float(totalBytesSent), from: 0.0, to: Float(totalBytesExpectedToSend)))
                }
                
                if task.state == .completed {
                    self.sessionTaskToProgressListenerMap[task] = nil
                }
            }
        }
        
        // MARK: Public Methods
        
        public func set(progressListener: @escaping ProgressListener, for task: URLSessionTask) {
            workQueue.async {
                self.sessionTaskToProgressListenerMap[task] = progressListener
            }
        }
        
        // MARK: Private Properties
        
        private var sessionTaskToProgressListenerMap = [URLSessionTask : ProgressListener]()
        private let workQueue = DispatchQueue(label: "URLRequestManager.SessionDelegate WorkQueue")
    }
}
