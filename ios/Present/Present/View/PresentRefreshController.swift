//
//  PresentRefreshController.swift
//  Present
//
//  Created by Pegah Keshavarz on 11/5/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import UIKit

public class PresentRefreshController : NSObject
{
    public let refreshControl = UIRefreshControl()

    private let pullToRefreshLogoHeight: CGFloat = 32
    private var pullToRefreshLogo: UIImageView!
    
    // An associated scrollview to be observed for automatically updating the layout
    private var scrollView: UIScrollView?
    
    override public init()
    {
        refreshControl.tintColor = UIColor.clear
        // Note: If we don't set the background color the refresh control does not seem to resize as we pull.
        refreshControl.backgroundColor = .white

        pullToRefreshLogo = UIImageView(image: #imageLiteral(resourceName: "present-logo-purple"))
        pullToRefreshLogo.contentMode = .scaleAspectFit
        refreshControl.addSubview(pullToRefreshLogo)
        refreshControl.sendSubview(toBack: pullToRefreshLogo)
    }

    public func addTarget(_ target: Any?, action: Selector) {
        refreshControl.addTarget(target, action: action, for: UIControlEvents.valueChanged)
    }
    
    /// Adds the KVO observer for the content offset (removed on deinit)
    public func addScrollView(scrollView: UIScrollView) {
        self.scrollView = scrollView
        scrollView.addObserver(self, forKeyPath: "contentOffset", options: [.new, .old], context: nil)
    }
    
    override public func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey : Any]?, context: UnsafeMutableRawPointer?)
    {
        guard let kp = keyPath else {
            super.observeValue(forKeyPath: keyPath, of: object, change: change, context: context)
            return
        }
        if kp == "contentOffset" {
            updatePullToRefresh()
        } else {
            super.observeValue(forKeyPath: keyPath, of: object, change: change, context: context)
        }
    }

    deinit {
        scrollView?.removeObserver(self, forKeyPath: "contentOffset", context: nil)
    }

    public func endRefreshing() {
        refreshControl.endRefreshing()
    }

    public func beginAnimatingPullToRefreshLogo()
    {
        let expand: CGFloat = 1.2
        UIView.animate(
            withDuration: 1.0/3, delay: 0, options: [.curveLinear],
            animations: {
                var xform = CGAffineTransform.identity
                xform = xform.rotated(by: CGFloat.pi/4.0)
                xform = xform.scaledBy(x: expand, y: expand)
                self.pullToRefreshLogo.transform = xform
        },
            completion: { finished in
                UIView.animate(
                    withDuration: 1.0/3, delay: 0, options: [.curveLinear],
                    animations: {
                        var xform = CGAffineTransform.identity
                        xform = xform.rotated(by: CGFloat.pi/2.0)
                        self.pullToRefreshLogo.transform = xform
                },
                    completion: {finished in
                        self.pullToRefreshLogo.transform = .identity
                        if self.refreshControl.isRefreshing {
                            self.beginAnimatingPullToRefreshLogo()
                        }
                })
            }
        )
    }
    
    public func updatePullToRefresh() {
        let size = min(refreshControl.bounds.height * 0.7, pullToRefreshLogoHeight * 1.5)
        pullToRefreshLogo.bounds = CGRect(x:0, y:0, width: size, height:size)
        pullToRefreshLogo.center = CGPoint(x:refreshControl.bounds.width/2, y:refreshControl.bounds.height/2)
    }
    
    public var backgroundColor: UIColor? {
        get { return refreshControl.backgroundColor }
        set { refreshControl.backgroundColor = newValue }
    }
}


