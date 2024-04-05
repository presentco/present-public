//
//  CircularImageBorderView.swift
//  Present
//
//  Created by Dan Federman on 2/3/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import Relativity
import UIKit


/// A view that draws a circular profile image with a border around it.
public final class CircularImageBorderView: UIView
{
    // MARK: Private Properties

    private let imageView = UIImageView()

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
    
    func commonInitializer() {
        addSubview(imageView)
    }
    
    // MARK: UIView
    
    public override func layoutSubviews() {
        super.layoutSubviews()
        
        imageView.bounds.size = CGSize(width: imageDiameter, height: imageDiameter)
        imageView.middle --> .middle
        imageView.layer.cornerRadius = imageDiameter / 2.0
        
        layer.cornerRadius = diameter / 2.0
        layer.borderWidth = borderWidth
        layer.borderColor = borderColor.cgColor
    }
    
    public override func sizeThatFits(_ size: CGSize) -> CGSize {
        return CGSize(width: diameter, height: diameter)
    }
    
    // MARK: Public Properties
    
    public var diameter: CGFloat {
        return PixelRounder(for: self).ceilToPixel(imageDiameter + 2.0 * borderOutset + borderWidth)
    }
    
    public var imageDiameter: CGFloat = 45.0 {
        didSet {
            if oldValue != imageDiameter {
                setNeedsLayout()
            }
        }
    }
    
    public var borderWidth: CGFloat = 1.0 {
        didSet {
            if oldValue != borderWidth {
                setNeedsLayout()
            }
        }
    }
    
    public var borderOutset: CGFloat = 0.0 {
        didSet {
            if oldValue != borderOutset {
                setNeedsLayout()
            }
        }
    }
    
    public var borderColor: UIColor = .white {
        didSet {
            if oldValue != borderColor {
                setNeedsLayout()
            }
        }
    }
    
    public var image: UIImage? {
        didSet {
            if let image = image {
                imageView.image = image
                imageView.contentMode = .scaleAspectFill
                imageView.clipsToBounds = true
                if oldValue != image {
                    setNeedsLayout()
                }
                
            } else {
                imageView.image = nil
            }
            
            updateIsHidden()
        }
    }
    
    public override var backgroundColor: UIColor? {
        didSet {
            updateIsHidden()
        }
    }
    
    // MARK: Private Methods
    
    private func updateIsHidden() {
        isHidden = (image == nil && backgroundColor == nil)
    }
    
}
