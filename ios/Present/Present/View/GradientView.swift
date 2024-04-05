//
//  GradientView.swift
//  Present
//
//  Created by Dan Federman on 2/3/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation


public final class GradientView: ClearView
{
    public class func clearToWhite(endY: CGFloat = 1.0)->GradientView
    {
        let view = GradientView()
        view.colors = [
            UIColor(white:1.0, alpha: 0), // Can't use .clear because that is black alpha 0
            UIColor.white
        ]
        view.gradientLayer.startPoint = CGPoint(x:0, y:0.0)
        view.gradientLayer.endPoint = CGPoint(x:0, y:endY)
        return view
    }
    
    // MARK: Initialization
    
    public required override init(frame: CGRect) {
        super.init(frame: frame)
        
        commonInitializer()
    }
    
    public convenience init() {
        self.init(frame: .zero)
    }
    
    public required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        
        commonInitializer()
    }
    
    private func commonInitializer() {
        setupGradient()
        isUserInteractionEnabled = false
    }
    
    // MARK: Public Properties
    
    public var gradientLayer: CAGradientLayer = CAGradientLayer() {
        willSet {
            gradientLayer.removeFromSuperlayer()
        }
        
        didSet {
            setupGradient()
        }
    }
    
    /// Gradient colors specified along the vertical axis from top to bottom.
    public var colors: [UIColor] = [] {
        didSet {
            gradientLayer.colors = colors.map { $0.cgColor }
        }
    }
    
    // MARK: UIView
    
    public override func layoutSubviews() {
        super.layoutSubviews()
        
        gradientLayer.frame = CGRect(origin: .zero, size: frame.size)
    }
    
    // MARK: Private Functions
    
    private func setupGradient() {
        layer.insertSublayer(gradientLayer, at: 0)
    }
    
}
