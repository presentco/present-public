//
//  ImageTextView.swift
//  Present
//
//  Created by Dan Federman on 2/3/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import Relativity
import UIKit


/// A view that draws an image and text side-by-side horizontally.
public final class ImageTextView: UIView {
    
    // MARK: View Model
    
    public struct ViewModel {
        public let image: UIImage
        public let text: String
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
        layoutMargins = .zero
        
        addSubview(imageView)
        addSubview(label)
    }
    
    // MARK: Public Properties
    
    public let label = UILabel()
    
    public var imageTextHorizontalMargin: CGFloat = 5.0 {
        didSet {
            if oldValue != imageTextHorizontalMargin {
                invalidateIntrinsicContentSize()
                setNeedsLayout()
            }
        }
    }
    
    // MARK: UIView
    
    /// The intrinsic content size of the view is the image plus padding plus label.
    public override var intrinsicContentSize: CGSize {
        let labelSize = label.intrinsicContentSize
        let layoutSizeForLabel = CGRect(origin: .zero, size: labelSize).insetBy(capAndBaselineOf: label.font)
        
        // If there is no image, use the label size.
        guard let imageSize = imageView.image?.size, imageSize != .zero else {
            return layoutSizeForLabel.size
        }
        
        // If there is no label, use the image size.
        guard let labelText = label.text, !labelText.isEmpty else {
            return imageSize
        }
        
        // Incorporate the image, label and interstitial spacing.
        return CGSize(
            width: imageSize.width + imageTextHorizontalMargin + layoutSizeForLabel.width,
            height: max(imageSize.height, layoutSizeForLabel.height)
        )
    }
    
    /// The view should always fit its intrinsic content size.
    public override func sizeThatFits(_ size: CGSize) -> CGSize {
        return intrinsicContentSize
    }
    
    public override func layoutSubviews() {
        super.layoutSubviews()
        
        // If there is no image, the label should fill the entire space without padding.
        guard let image = imageView.image, image.size != .zero else {
            label.sizeToFitSuperview()
            return
        }
        
        // The image view is sized to fit its contents.
        imageView.sizeToFit()
        
        // The label takes up the remaining space minus the horizontal margin.
        label.sizeToFit(fixedWidth: bounds.size.width - imageTextHorizontalMargin - image.size.width)
        
        // Pin the image to the left and the label to the right.
        left <-- imageView.left
        label.right --> right
    }

    // MARK: Public Methods
    
    public func apply(viewModel: ViewModel) {
        imageView.image = viewModel.image
        label.text = viewModel.text
        
        sizeToFit()
        
        isHidden = (viewModel.image.size == .zero && viewModel.text.isEmpty)
    }
    
    // MARK: Private Properties
    
    private let imageView = UIImageView()
    
}
