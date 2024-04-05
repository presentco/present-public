//
//  OverlayViewController
//  Present
//
//  Created by Patrick Niemeyer
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation

public class PresentOverlayViewController: OverlayViewController, ApplicationServices { }

/// A container view controller that appears over the current context with a transparent,
/// fixed color, or blurred background.
public class OverlayViewController: UIViewController, UIGestureRecognizerDelegate
{
    public enum BackgroundType {
        case none, blur, color(UIColor)
    }
    
    public var backgroundType: BackgroundType = .none
    
    /// When set, taps outside of this view dismiss the controller.
    public var dismissableContentView: UIView?
    
    var blurEffectView: UIVisualEffectView?
    
    public init()
    {
        logn("init")
        super.init(nibName: nil, bundle: nil)
        
        modalTransitionStyle = .coverVertical
        modalPresentationStyle = .overCurrentContext
    }
    
    required public init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    public override func viewDidLoad()
    {
        switch backgroundType {
            case .none:
                break
            case .blur:
                let blurEffect = UIBlurEffect(style: .light)
                blurEffectView = UIVisualEffectView(effect: blurEffect).then {
                    view.insertSubview($0, at: 0)
                }
            case let .color(color):
                view.backgroundColor = color
        }


        let tapGestureRecognizer = UITapGestureRecognizer(target: self, action: #selector(tapDetected(_:)))
        tapGestureRecognizer.delegate = self
        view.addGestureRecognizer(tapGestureRecognizer)
    }
    
    public func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool
    {
        // Is the tap outside of the dismissable content view?
        // TODO: Why do taps on the content view get passed to the parent view anyway?
        // TODO: Is there a more elegant way to handle this?
        if let dismissableContentView = dismissableContentView {
            let begin = !dismissableContentView.frame.contains(gestureRecognizer.location(in: view))
            logn("gestureRecognizerShouldBegin: \(begin)")
            return begin
        }
        logn("gestureRecognizerShouldBegin: ignore")
        return false
    }
    
    @objc private func tapDetected(_ tapRecognizer: UITapGestureRecognizer) {
        tapOutsideContent()
    }

    func tapOutsideContent() {
        dismissOverlay()
    }

    /// Override to add behavior after dismissal
    func dismissOverlay() {
        logn("dismiss")
        super.dismiss(animated: true) // { }
    }

    public override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        blurEffectView?.frame = self.view.frame
    }
    
}


