//
//  WebView.swift
//  Present
//
//  Created by Patrick Niemeyer on 2/15/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import PresentProtos
import UIKit
import WebKit

protocol WebViewDelegate : class {
    func webViewRefreshPulled(webView: WebView) -> Void
}
class WebView
{
    var name: String = "WebView"
    var view = WKWebView()
    var html: String?
    let refreshController = PresentRefreshController()
    var isLoadingObservation: NSKeyValueObservation?
    weak var delegate: WebViewDelegate?
    weak var navigationDelegate: WKNavigationDelegate? {
        didSet {
            view.navigationDelegate = navigationDelegate
        }
    }
    private var lastLoadingStatus: Bool?
    
    init() {
        // Init JS and local storage preferences
        let preferences = WKPreferences()
        preferences.javaScriptEnabled = true
        view.configuration.preferences = preferences
        view.configuration.websiteDataStore = WKWebsiteDataStore.default()

        // Observe loading status changes
        isLoadingObservation = view.observe(\.isLoading) { [weak self] (webView, change) in
            let status = webView.isLoading
            if status != self?.lastLoadingStatus {
                self?.webViewLoadingStatusChanged(to: webView.isLoading)
            }
            self?.lastLoadingStatus = status
        }
        setupPullToRefresh(action: #selector(webViewRefreshPulled))
    }
    
    public var show: Bool = true {
        didSet {
            view.alpha = show ? 1.0 : 0.0
        }
    }
    
    public func setupPullToRefresh(action: Selector) { // set up pull to refresh
        //logDebug("\(name): set up pull to refresh")
        refreshController.addTarget(self, action: action)
        refreshController.addScrollView(scrollView: view.scrollView)
        pullToRefreshEnabled = true
    }
    
    public var pullToRefreshEnabled : Bool = true {
        didSet {
            if pullToRefreshEnabled && !view.scrollView.subviews.contains(refreshController.refreshControl) {
                // TODO: If I use the iOS10 explicit refresh control the scrollview jumps down when refreshing stops.
                //webView.scrollView.refreshControl = presentRefreshController.refreshControl
                view.scrollView.addSubview(refreshController.refreshControl)
                //logDebug("pull to refresh enabled")
            }
            if !pullToRefreshEnabled && view.scrollView.subviews.contains(refreshController.refreshControl) {
                refreshController.refreshControl.removeFromSuperview()
                //logDebug("pull to refresh disabled")
            }
        }
    }
    
    /// The user pulled refresh on the web view
    @objc func webViewRefreshPulled() {
        //logDebug("\(name): refresh pulled")
        delegate?.webViewRefreshPulled(webView:self)
    }
    
    func loadHTMLString(html: String, baseURL: URL) {
        //logDebug("\(name): webview load html, sinceLaunch=\(AppDelegate.sinceLaunch)")
        view.loadHTMLString(html, baseURL: baseURL)
    }
    func load(_ urlRequest: URLRequest) {
        //logDebug("\(name): webview load urlrequest")
        view.load(urlRequest)
    }
    func stopLoading() {
        view.stopLoading()
    }
    
    func webViewLoadingStatusChanged(to isLoading: Bool) {
        //log("\(name): webview loading status changed to: \(isLoading), sinceLaunch=\(AppDelegate.sinceLaunch)")
        //if isLoading == false { testLocalStorage() }
    }
    
    /*
    func testLocalStorage() {
        let jsread = """
            function fr() {
                return localStorage.getItem('key')
            }
            fr();
            """
        view.evaluateJavaScript(jsread) { (result, error) -> Void in
            log("LocalStorage: got \(result), \(error)")
        }
        
        let jswrite = """
            function fw() {
                localStorage.setItem('key', Date());
            }
            fw();
            """
        view.evaluateJavaScript(jswrite) { (result, error) -> Void in log("set item") }
    }*/
}



