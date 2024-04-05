//
//  BackedWebView.swift
//  Present
//
//  Created by Patrick Niemeyer on 2/16/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import PresentProtos
import UIKit
import WebKit
import RxSwift

protocol BackedWebViewDelegate : class {
    func webViewRefreshPulled(webView: BackedWebView) -> Void
}

/// A pair of web views that can be alternated for loading in the background
class BackedWebView : UIView, WebViewDelegate
{
    var name: String = "BackedWebView" {
        didSet {
            webView1.name = name+":WebView1"
            webView2.name = name+":WebView2"
            name = name+":BackedWebView"
        }
    }
    
    let webView1 = WebView()
    let webView2 = WebView()
    
    public lazy var contentOffset: Observable<CGFloat> =
    {
        let subject = ReplaySubject<CGFloat>.create()
        
        // TODO: Why are these never reporting positive content offsets?
        webView1.view.scrollView.rx.contentOffset.changed.bind { [weak self] offset in
            guard let sself = self else { return }
            //log("webview 1 offset changed: \(offset)")
            if sself.webView1 === sself.currentWebView { subject.onNext(offset.y) }
        }.disposed(by: disposal)
        webView2.view.scrollView.rx.contentOffset.changed.bind { [weak self] offset in
            guard let sself = self else { return }
            //log("webview 2 offset changed: \(offset)")
            if sself.webView2 === sself.currentWebView { subject.onNext(offset.y) }
            }.disposed(by: disposal)
        
        /*
        webView2.view.scrollView.rx.observeWeakly(CGFloat.self, "contentOffset").onNext
            { [weak self] offset in
                guard let sself = self else { return }
                log("webview 2 weak observer offset changed: \(offset)")
                if sself.webView2 === sself.currentWebView { subject.onNext(offset ?? 0) }
            }.disposed(by: disposal)
         */

        return subject.startWith(0)
    }()
    
    private let disposal = DisposeBag()
    
    // The visible web view
    private var currentWebView: WebView {
        didSet {
            //let name = currentWebView === webView1 ? "webView1" : "webView2"
            //log("current web view is: \(name)")
        }
    }
    
    // The non-visible web view
    private var backWebView: WebView {
        return currentWebView === webView1 ? webView2 : webView1
    }
    
    weak var delegate: BackedWebViewDelegate?
    
    weak var navigationDelegate: WKNavigationDelegate? {
        didSet {
            webView1.navigationDelegate = navigationDelegate
            webView2.navigationDelegate = navigationDelegate
        }
    }
    
    var currentHtml: String? {
        return currentWebView.html
    }
    
    var refreshController: PresentRefreshController  {
        return currentWebView.refreshController
    }
    
    var isLoading: Bool {
        return currentWebView.view.isLoading
    }
    
    var isRefreshing: Bool {
        return currentWebView.refreshController.refreshControl.isRefreshing
    }
    
    override var backgroundColor: UIColor? {
        didSet {
            super.backgroundColor = backgroundColor
            webView1.view.backgroundColor = backgroundColor
            webView1.refreshController.backgroundColor = backgroundColor
            webView2.view.backgroundColor = backgroundColor
            webView2.refreshController.backgroundColor = backgroundColor
        }
    }
    
    public var pullToRefreshEnabled : Bool {
        get { return currentWebView.pullToRefreshEnabled }
        set {
            webView1.pullToRefreshEnabled = newValue
            webView2.pullToRefreshEnabled = newValue
        }
    }
    
    init()
    {
        currentWebView = webView1
        super.init(frame: CGRect.zero)
        
        webView1.delegate = self
        webView2.delegate = self
        webView2.show = false

        addSubview(webView1.view)
        webView1.view.constrainToFillSuperview()
        addSubview(webView2.view)
        webView2.view.constrainToFillSuperview()
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func configure(block: (WKWebView)->Void) {
        block(webView1.view)
        block(webView2.view)
    }
    
    // Load the HTML in the backing view
    func loadHTML(html: String, baseURL: URL)
    {
        logDebug("WebView: start load of html")
        backWebView.html = html
        backWebView.loadHTMLString(html: html, baseURL: baseURL)
    }
    
    func load(_ request: URLRequest) {
        backWebView.html = nil
        backWebView.load(request)
    }
    
    func stopLoading() {
        backWebView.stopLoading()
    }
    
    func beginAnimatingPullToRefreshLogo() {
        currentWebView.refreshController.beginAnimatingPullToRefreshLogo()
    }
    func endRefreshing() {
        currentWebView.refreshController.endRefreshing()
    }
    
    /*
    override func layoutSubviews() {
        super.layoutSubviews()
        webView1.view.sizeToFitSuperview()
        webView2.view.sizeToFitSuperview()
    }*/
    
    func swapViews() {
        UIView.animate(withDuration: 0.2) {
            self.backWebView.show = true
            self.currentWebView.show = false
        }
        currentWebView = backWebView
    }
    
    // Evaluate the js in the current view
    func evaluateJavaScript(_ javaScriptString: String, completionHandler: ((Any?, Error?) -> Swift.Void)? = nil) {
        currentWebView.view.evaluateJavaScript(javaScriptString, completionHandler: completionHandler)
    }
    // Evaluate the js in the back view
    func evaluateJavaScriptInNewContent(_ javaScriptString: String, completionHandler: ((Any?, Error?) -> Swift.Void)? = nil) {
        backWebView.view.evaluateJavaScript(javaScriptString, completionHandler: completionHandler)
    }
    
    // MARK: WebViewDelegate
    
    func webViewRefreshPulled(webView: WebView) -> Void {
        //logDebug("\(name): refresh pulled")
        delegate?.webViewRefreshPulled(webView: self)
    }
    
    // Called by the navigation delegate when loading is complete to present the newly loaded content view
    func presentNewContent() -> Void
    {
        // Swap views
        logDebug("\(name): Presenting new content from background view.")
        DispatchQueue.main.async {
            self.swapViews()
        }
    }
    
    override func setNeedsLayout() {
        super.setNeedsLayout()
        currentWebView.view.setNeedsLayout()
    }
}
