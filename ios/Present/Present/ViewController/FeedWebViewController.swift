//
//  FeedWebViewController.swift
//  Present
//
//  Created by Patrick Niemeyer on 5/2/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import RxSwift

/// A web view controller that loads the feed HTML.
///     --> GatedFeedWebViewController (women only, nearby)
///         --> FeedContentViewController
///             --> FeedWebViewController
public class FeedWebViewController: WebViewController
{
    public var htmlSource:  ((_ refresh:Bool)->Single<String>)?

    private let disposal = DisposeBag()
    
    override public func viewDidLoad()
    {
        super.viewDidLoad()
        
        // Reload on user selected location change.
        Observable.combineLatest (
            userManager.selectedLocation.observable,
            userManager.authorizationStatus.observable
        )
            .onNext { [weak self] location, authStatus in
                log("\(self?.name ?? "") calling load web view due to location or auth status change, location=\(String(describing: location?.name)), authStatus=\(authStatus)")
                self?.loadWebView()
            }.disposed(by: disposal)
    }
    
    override func loadWebView(completion: Completion? = nil) {
        self.loadWebView(refresh: true)
    }
    
    // TODO: We should move most of this logic into WebViewController and abstract the service call.
    func loadWebView(refresh: Bool = false, completion: Completion? = nil)
    {
        logn("\(name) loading web view")
        if webView.isLoading {
            webView.stopLoading()
            fireLoadCompletion()
        }
        
        if !webView.isRefreshing {
            webViewActivityIndicatorAnimating = true
        }
        loadWebViewCompletion = completion

        if let overrideUrl = userManager.userSession.overrideHomeUrl,
            let url = URL(string: overrideUrl)
        {
            logDebug("\(name): loadWebView using override url: \(overrideUrl)")
            var urlRequest = URLRequest(url: url)
            urlRequest.cachePolicy = .reloadIgnoringLocalAndRemoteCacheData
            webView.load(urlRequest)
        } else {
            //htmlSource.unwrappedOrFatal()(webView.isRefreshing)
            htmlSource?(refresh || webView.isRefreshing)
                .onSuccess { html in
                    main {
                        self.loadHTML(html: html)
                    }
                }.onError { error in
                    logError("\(self.name): Error loading html")
                    self.fireLoadCompletion()
                }.disposed(by: disposal)
        }
    }
}
