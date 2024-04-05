//
//  ShowEventLoggingViewController.swift
//  Present
//
//  Created by Patrick Niemeyer on 6/5/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation

public class EventLoggingOverlay
{
    public static var shared = EventLoggingOverlay()
    
    public var showEvents = false

    let container = UIView()
    var label: UILabel?
    var heightConstraint: NSLayoutConstraint?
    var hideTime = Date()

    private var showing: Bool = false {
        didSet {
            if showing {
                container.alpha = 1.0
            } else {
                UIView.animate(
                    withDuration: 0.5,
                    animations: {
                        self.container.alpha = 0.0
                    },
                    completion: { complete in }
                )
            }
        }
    }
    
    public func show(text: String)
    {
        guard showEvents else { return }
        guard AppDelegate.shared.applicationDidFinishLaunching.isFullfilled else { return }
        main {
            let label = self.label ?? self.initLabel()
            if self.showing, let existingText = label.text {
                label.text = "\(existingText)\n\(text)"
            } else {
                label.text = text
            }
            let size = label.sizeThatFits(CGSize(width: label.bounds.width, height: CGFloat.greatestFiniteMagnitude))
            self.heightConstraint?.constant = size.height + 40
            self.showing = true

            self.hideTime = Date().addingTimeInterval(2.0)
            mainAfter(seconds: 2) {
                self.checkOverlayTime()
            }
        }
    }
    
    private func checkOverlayTime() {
        main {
            if self.hideTime <= Date() {
                self.showing = false
            } else {
                mainAfter(milliseconds: 500) {
                    self.checkOverlayTime()
                }
            }
        }
    }

    private func initLabel() -> UILabel
    {
        container.backgroundColor = UIColor(red: 106+55, green: 69+55, blue: 199+55, alpha: 0.9)
        container.roundedCorners = 8
        AppDelegate.shared.mainWindow.addSubview(container)
        container.anchorCenterSuperview()
        container.anchor(widthConstant: 280)
        heightConstraint = container.anchor(heightConstant: 70).first
        let label = UILabel()
        label.numberOfLines = 0
        label.font = UIFont.presentFont(ofSize: 12, weight: .regular)
        label.anchor(widthConstant: 240, heightConstant: 60)
        self.label = label
        container.addSubview(label)
        label.anchorCenterSuperview()
        return label
    }
}
