//
//  FeedViewController.swift
//  Present
//
//  Created by Patrick Niemeyer on 4/26/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import PresentProtos
import UIKit
import SwiftyPickerPopover
import StringStylizer
import WebKit

// A view controller managing one BackedWebView
public class WebViewController: UIViewController, ApplicationServices
{
    // MARK: Properties
    
    var name = "WebViewController"
    
    /// Events types to be logged for internal urls dispatched by the web view.
    /// (This allows us to log different event types based on context.)
    var dispatchEvents: [URLManager.DispatchType: LoggingEvent] = [:]
    
    // TODO: Remove space
    // The selected space, if any to be logged with internal url dispatch events
    var dispatchSpace: PresentSpace?

    /// The current web view loading completion.
    /// Managed by loadHomeView() and updateHomeViewAfterLoad()
    var loadWebViewCompletion: Completion?
    
    // Simple lock intended to be called only from the main thread, wraps both the loading
    // of the RPC fetched HTML and the page render.
    var webContentLoading: Bool = false
    
    let webViewActivityIndicator = PresentLogoActivityIndicator()
    let webViewBaseUrl = PresentLinks.presentBaseUrl

    var webView = BackedWebView()
    
    var webViewActivityIndicatorAnimating : Bool = false {
        didSet {
            //logDebug("activity indicator animating = \(webViewActivityIndicatorAnimating)")
            if webViewActivityIndicatorAnimating {
                webViewActivityIndicator.startAnimating()
                webView.pullToRefreshEnabled = false
            } else {
                webViewActivityIndicator.stopAnimating()
                webView.pullToRefreshEnabled = true
            }
        }
    }

    // MARK: Initialization

    public override func viewDidLoad() {
        super.viewDidLoad()
        webViewActivityIndicator.name = "\(name): web view activity indicator"
        initWebView()
        //logDebug("CardHomeWebViewController: view did load, start spinner")
        installChild(viewController: webViewActivityIndicator, in: webView) {
            $0.constrainToFillSuperview()
        }
        webViewActivityIndicatorAnimating = true
        //refreshContentIfNeeded()
    }

    private func refreshContent() {
        loadWebView()
    }
}

@objc extension WebViewController: BackedWebViewDelegate, WKNavigationDelegate
{
    func initWebView()
    {
        webView.name = name
        webView.configure { webView in
            webView.scrollView.bounces = true
            webView.scrollView.showsVerticalScrollIndicator = false
            webView.scrollView.showsHorizontalScrollIndicator = false
            webView.backgroundColor = .white // updated from HTML later
            //webView.layer.borderWidth = 1.0
            //webView.layer.borderColor = UIColor(red: 225, green: 225, blue: 233).cgColor
            webView.navigationDelegate = self
        }
        webView.delegate = self
        view.addSubview(webView)
        webView.constrainToFillSuperview()
    }
    
    // Load the specified html into the web view
    func loadHTML(html: String)
    {
        // Only reload on updates
        if let currentHtml = webView.currentHtml, currentHtml == html {
            //logDebug("\(name) loadHtml: HTML unchanged, not reloading.")
            fireLoadCompletion()
            return
        } else {
            //logDebug("\(name) loadHtml: HTML changed, reloading.")
        }
        
        webView.loadHTML(html: html, baseURL: webViewBaseUrl)
    }
    
    // TODO: Consolidate common stuff here
    // Load the web content.  Overridden by children.
    func loadWebView(completion: Completion? = nil) { }
    
    // MARK: WKNavigationViewDelegate
    
    // Invoked for the original load request and links selected by the user within the HTML
    public func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void)
    {
        //logDebug("\(name) webView:\(webView) decidePolicyForNavigationAction:\(navigationAction) decisionHandler:\(decisionHandler)")
        
        guard let url = navigationAction.request.url else { return decisionHandler(.cancel) }
        
        // Must allow the base URL for the HTML, even though it's not actually loaded.
        if webViewBaseUrl.isEquivalent(toUrl: url) { return decisionHandler(.allow) }
        
        // Allow the override url, if any
        if let overrideUrlString = userManager.userSession.overrideHomeUrl,
            url.absoluteString == overrideUrlString {
            return decisionHandler(.allow)
        }
        
        webViewActivityIndicatorAnimating = true
        //log("dispatch url: \(url), space=\(String(describing: dispatchSpace))")
        URLManager.shared.dispatchUrl(url: url, dispatchSpace: dispatchSpace, dispatchEvents: dispatchEvents) {
            self.webViewActivityIndicatorAnimating = false
        }
        return decisionHandler(.cancel)
    }
    
    public func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
        //logDebug("\(name): webview did start provisional navigation")
    }
    
    public func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        //log("\(name): webview did finish navigation")
        webViewActivityIndicatorAnimating = false
        updateWebViewAfterLoad()
        fireLoadCompletion()
        // We actually loaded new content, present it.
        self.webView.presentNewContent()
    }
    
    public func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
        logDebug("\(name): webview did fail provisional navigation: \(navigation)")
        fireLoadCompletion()
    }
    
    public func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        logDebug("\(name): webview did fail navigation: \(navigation)")
        fireLoadCompletion()
    }
    
    
    // MARK: Content handling
    
    func updateWebViewAfterLoad()
    {
        let getBodyColorJS = "window.getComputedStyle(document.body).backgroundColor"
        webView.evaluateJavaScriptInNewContent(getBodyColorJS) { result, error in
            if let rgbString = result as? String,
                let color = UIColor(rgbString: rgbString)
            {
                //logDebug("\(self.name): setting bgcolor to: \(color)")
                self.webView.backgroundColor = color
            }
        }
    }
    
    // Called when the web view completes loading or fails
    func fireLoadCompletion()
    {
        let loadWebViewCompletion = self.loadWebViewCompletion
        DispatchQueue.main.async {
            //logDebug("\(self.name): load completion")
            loadWebViewCompletion?()
            self.loadWebViewCompletion = nil
            self.webContentLoading = false
            self.webViewActivityIndicatorAnimating = false
            self.webView.endRefreshing()
        }
    }
    
    /// The user pulled refresh on the web view
    func webViewRefreshPulled(webView: BackedWebView) -> Void {
        webView.beginAnimatingPullToRefreshLogo()
        loadWebView() {
            //logDebug("\(self.name): refresh complete")
            //self.presentRefreshController.endRefreshing()
        }
    }
}
