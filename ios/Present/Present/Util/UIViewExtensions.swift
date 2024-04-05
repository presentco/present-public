//
//  UIViewExtensions.swift
//  Present
//
//  Created by Patrick Niemeyer on 1/7/16.
//  Copyright © 2016 Present Company. All rights reserved.
//

import CoreGraphics
import UIKit

public extension UIView
{
    public var isShown: Bool {
        get {
            return !self.isHidden
        }
        set {
            self.isHidden = !newValue
        }
    }

    /// Simple rounded corners with the supplied radius
    public var roundedCorners: CGFloat {
        get {
            return layer.cornerRadius
        }
        set {
            layer.cornerRadius = newValue
            //layer.borderWidth = 0 // ???
        }
    }
    
    public func roundCornersToHeight() {
        self.roundedCorners = self.bounds.height / 2.0
    }
    
    // MARK: Public Methods
    
    /// Sizes the receiver to be the same size as the superview.
    public func sizeToFitSuperview() {
        guard let superview = superview else {
            return
        }
        
        bounds = superview.bounds
        center = CGPoint(x: superview.bounds.midX, y: superview.bounds.midY)
    }
    
    public func sizeToFit(fixedWidth: CGFloat) {
        var desiredSize = sizeThatFits(CGSize(width: fixedWidth, height: CGFloat.greatestFiniteMagnitude))
        desiredSize.width = fixedWidth
        
        bounds.size = desiredSize
    }
    
    public func layoutIfVisible() {
        setNeedsLayout()
        
        guard window != nil else {
            // We don't have a window, so we can't possibly be visible.
            return
        }
        
        layoutIfNeeded()
    }
    
    public func shake(withDuration duration: TimeInterval = 0.4, completion: @escaping  () -> Void = {}) {
        let originalTransform = transform
        transform = originalTransform.translatedBy(x: 20.0, y: 0.0)
        UIView.animate(withDuration: duration, delay: 0.0, usingSpringWithDamping: 0.3, initialSpringVelocity: 1.0, options: [.curveLinear], animations: {
            self.transform = originalTransform
            
        }) { _ in
            completion()
        }
    }
    
    /// Specify which corners are rounded with the supplied radius
    // Note: If this causes performance issues we may have to get rid of it and fall back
    // Note: to slicing off corners by overlaying views.
    func roundCorners( _ corners : UIRectCorner, radius : CGFloat )
    {
        let path = UIBezierPath(roundedRect:self.bounds, byRoundingCorners:corners, cornerRadii: CGSize(width: radius, height: radius))
        let maskLayer = CAShapeLayer()
        maskLayer.path = path.cgPath
        self.layer.mask = maskLayer
    }
    
    func pulse( _ scale : CGFloat = 1.0, endScale : CGFloat = 1.0, completion : @escaping (Bool)->Void = { complete in } )
    {
        UIView.animate( withDuration: 0.1*Double(scale), animations: {
                UIView.setAnimationCurve(.easeOut)
                self.transform = CGAffineTransform(scaleX: 1.05*scale, y: 1.05*scale)
                self.alpha = 0.9*scale
            }, completion: { complete in
                UIView.animate( withDuration: 0.1*Double(scale), animations: {
                UIView.setAnimationCurve(.easeOut)
                    if endScale == 1.0 {
                        self.transform = CGAffineTransform.identity
                    } else {
                        self.transform = CGAffineTransform(scaleX: endScale, y: endScale)
                    }
                self.alpha = 1.0
                }, completion: completion )
        } )
    }
    
    func setShadow( _ on :  Bool ) {
        if on { setShadow() } else {
            layer.shadowOpacity = 0.0
        }
    }
    
    func setShadow(
        shadowXOffset : CGFloat = 0.0,
        shadowYOffset : CGFloat = 0.5,
        shadowOpacity : Float = 0.3,
        shadowRadius : CGFloat = 1.0,
        setShadowPath : Bool = true
    ) {
        ShadowUtil.setShadow( self, shadowXOffset: shadowXOffset, shadowYOffset: shadowYOffset, shadowOpacity: shadowOpacity, shadowRadius : shadowRadius, setShadowPath: setShadowPath)
    }
}

public extension UIView
{
    /// Load the view for this class from a XIB file
    public func viewFromNibForClass() -> UIView {
        return viewFromNibForClass(classType: type(of: self), index: 0)
    }
    
    /// Load the view for this class from a XIB file
    public func viewFromNibForClass<T>(classType: T.Type, index : Int = 0) -> UIView {
        let bundle = Bundle(for: type(of: self))
        // Remove any generic suffix
        let className = String(String(describing: classType).split(separator: "<")[0])
        let nib = UINib(nibName:className, bundle: bundle)
        return nib.instantiate(withOwner: self, options: nil)[index] as! UIView
    }
    
    public func initViewFromNib() {
        initViewFromNib(classType: type(of: self))
    }
    
    /// Load the view for this class from a XIB file and add it
    /// Optionally specify the class to use for the naming of the associated XIB file.
    public func initViewFromNib<T>(classType: T.Type) {
        let view = viewFromNibForClass(classType: classType)
        addSubview(view)
        //view.frame = bounds
        view.constrainToFillSuperview()
    }
}

public extension UIView
{
    /// Fill the specified view, optionally below the specified view
    public func constrainToFill( view: UIView, below belowView: UIView? = nil, insets: UIEdgeInsets = .zero) {
        self.translatesAutoresizingMaskIntoConstraints = false
        self.leftAnchor.constraint(equalTo: view.leftAnchor, constant: insets.left).isActive = true
        self.rightAnchor.constraint(equalTo: view.rightAnchor, constant: insets.right).isActive = true
        
        self.topAnchor.constraint(equalTo: belowView?.bottomAnchor ?? view.topAnchor, constant: insets.top).isActive = true
        
        self.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: insets.bottom).isActive = true
    }
    
    public func constrainToFillSafeArea( view: UIView ) {
        self.translatesAutoresizingMaskIntoConstraints = false
        // left/right anchors are absolute, leading/trailing can interchange for
        // text views with different text local reading direction
        self.leftAnchor.constraint(equalTo: view.leftAnchor).isActive = true
        self.rightAnchor.constraint(equalTo: view.rightAnchor).isActive = true
        if #available(iOS 11.0, *) {
            self.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor).isActive = true
            // note inverted relation here
            view.safeAreaLayoutGuide.bottomAnchor.constraint(equalTo: self.bottomAnchor).isActive = true
        } else {
            self.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
            self.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        }
    }
    
    public func constrainToFillSuperview() {
        guard let superview = self.superview else {
            fatalError("no superview")
        }
        constrainToFill(view: superview)
    }
    public func constrainToFillSuperview(insets: UIEdgeInsets) {
        guard let superview = self.superview else {
            fatalError("no superview")
        }
        constrainToFill(view: superview, insets: insets)
    }
    
    public func constrainToFillSuperviewSafeArea() {
        guard let superview = self.superview else {
            fatalError("no superview")
        }
        constrainToFillSafeArea(view: superview)
    }
}

public extension UIView
{
    public func toFront() {
        self.superview?.bringSubview(toFront: self)
    }
}

/// SwifterSwift
/// https://github.com/SwifterSwift/SwifterSwift/blob/master/Sources/Extensions/UIKit/UIViewExtensions.swift
public extension UIView
{
    /// SwifterSwift: Add anchors from any side of the current view into the specified anchors and returns the newly added constraints.
    ///
    /// - Parameters:
    ///   - top: current view's top anchor will be anchored into the specified anchor
    ///   - left: current view's left anchor will be anchored into the specified anchor
    ///   - bottom: current view's bottom anchor will be anchored into the specified anchor
    ///   - right: current view's right anchor will be anchored into the specified anchor
    ///   - topConstant: current view's top anchor margin
    ///   - leftConstant: current view's left anchor margin
    ///   - bottomConstant: current view's bottom anchor margin
    ///   - rightConstant: current view's right anchor margin
    ///   - widthConstant: current view's width
    ///   - heightConstant: current view's height
    /// - Returns: array of newly added constraints (if applicable).
    @discardableResult
    public func anchor(
        top: NSLayoutYAxisAnchor? = nil,
        left: NSLayoutXAxisAnchor? = nil,
        bottom: NSLayoutYAxisAnchor? = nil,
        right: NSLayoutXAxisAnchor? = nil,
        topConstant: CGFloat = 0,
        leftConstant: CGFloat = 0,
        bottomConstant: CGFloat = 0,
        rightConstant: CGFloat = 0,
        widthConstant: CGFloat = 0,
        heightConstant: CGFloat = 0) -> [NSLayoutConstraint] {
        // https://videos.letsbuildthatapp.com/
        translatesAutoresizingMaskIntoConstraints = false
        
        var anchors = [NSLayoutConstraint]()
        
        if let top = top {
            anchors.append(topAnchor.constraint(equalTo: top, constant: topConstant))
        }
        
        if let left = left {
            anchors.append(leftAnchor.constraint(equalTo: left, constant: leftConstant))
        }
        
        if let bottom = bottom {
            anchors.append(bottomAnchor.constraint(equalTo: bottom, constant: -bottomConstant))
        }
        
        if let right = right {
            anchors.append(rightAnchor.constraint(equalTo: right, constant: -rightConstant))
        }
        
        if widthConstant > 0 {
            anchors.append(widthAnchor.constraint(equalToConstant: widthConstant))
        }
        
        if heightConstant > 0 {
            anchors.append(heightAnchor.constraint(equalToConstant: heightConstant))
        }
        
        anchors.forEach({$0.isActive = true})
        
        return anchors
    }
    
    public func anchorCenterXToSuperview(constant: CGFloat = 0) {
        // https://videos.letsbuildthatapp.com/
        translatesAutoresizingMaskIntoConstraints = false
        if let anchor = superview?.centerXAnchor {
            centerXAnchor.constraint(equalTo: anchor, constant: constant).isActive = true
        }
    }
    
    /// SwifterSwift: Anchor center Y into current view's superview with a constant margin value.
    ///
    /// - Parameter withConstant: constant of the anchor constraint (default is 0).
    public func anchorCenterYToSuperview(constant: CGFloat = 0) {
        // https://videos.letsbuildthatapp.com/
        translatesAutoresizingMaskIntoConstraints = false
        if let anchor = superview?.centerYAnchor {
            centerYAnchor.constraint(equalTo: anchor, constant: constant).isActive = true
        }
    }
    
    /// SwifterSwift: Anchor center X and Y into current view's superview
    public func anchorCenterSuperview() {
        // https://videos.letsbuildthatapp.com/
        anchorCenterXToSuperview()
        anchorCenterYToSuperview()
    }
}

/// Adding to the SwifterSwift extensions
public extension UIView
{
    @discardableResult
    public func anchorCenterX(to view: UIView, constant: CGFloat = 0) -> UIView {
        translatesAutoresizingMaskIntoConstraints = false
        let anchor = view.centerXAnchor
        centerXAnchor.constraint(equalTo: anchor, constant: constant).isActive = true
        return self
    }
    
    @discardableResult
    public func anchorCenterY(to view: UIView, constant: CGFloat = 0) -> UIView {
        translatesAutoresizingMaskIntoConstraints = false
        let anchor = view.centerYAnchor
        centerYAnchor.constraint(equalTo: anchor, constant: constant).isActive = true
        return self
    }
    
    @discardableResult
    public func anchorCenter(to view: UIView) -> UIView {
        anchorCenterX(to: view)
        anchorCenterY(to: view)
        return self
    }
    
    /// constraintBlock allows capturing constraint without breaking the builder pattern
    @discardableResult
    public func anchorHeight(constant: CGFloat, constraintBlock: ((NSLayoutConstraint)->Void)?=nil) -> UIView {
        translatesAutoresizingMaskIntoConstraints = false
        let constraint = heightAnchor.constraint(equalToConstant: constant)
        constraint.isActive = true
        constraintBlock?(constraint)
        return self
    }
    
    @discardableResult
    public func anchorWidth(constant: CGFloat, constraintBlock: ((NSLayoutConstraint)->Void)?=nil) -> UIView {
        translatesAutoresizingMaskIntoConstraints = false
        let constraint = widthAnchor.constraint(equalToConstant: constant)
        constraint.isActive = true
        constraintBlock?(constraint)
        return self
    }
    
    @discardableResult
    public func anchorWidthHeight(widthConstant: CGFloat, heightConstant: CGFloat) -> UIView {
        translatesAutoresizingMaskIntoConstraints = false
        anchorWidth(constant: widthConstant)
        anchorHeight(constant: heightConstant)
        return self
    }
}

public extension UIView
{
    /// Add a full width (default black, 1px high) view inside the top of the view
    func addTopSeparator(color: UIColor = .black, height: CGFloat = 1.0)
    {
        UIView().do {
            self.addSubview($0)
            $0.backgroundColor = color
            $0.anchor(top: self.topAnchor, left: self.leftAnchor, right: self.rightAnchor, heightConstant: height)
        }
    }
    /// Add a full width (default black, 1px high) view inside the bottom of the view
    func addBottomSeparator(color: UIColor = .black, height: CGFloat = 1.0)
    {
        UIView().do {
            self.addSubview($0)
            $0.backgroundColor = color
            $0.anchor(left: self.leftAnchor, bottom: self.bottomAnchor, right: self.rightAnchor, heightConstant: height)
        }
    }
}
