//
//  CardView.swift
//
//  Created by Patrick Niemeyer on 2/16/16.
//  Copyright © 2016 Present Company. All rights reserved.
//

import CoreGraphics
import Foundation
import Relativity
import Then
import UIKit

public protocol CardViewDelegate : class {
    func joinPressed() -> Bool
}

public final class CardView : UIView
{
    // MARK: Public Properties

    // Note: the view model for the card view is maintained by the card view controller

    public weak var delegate : CardViewDelegate?

    public let backgroundImageView = UIImageView().then {
        $0.contentMode = .scaleAspectFill
        $0.clipsToBounds = true
    }

    public let dimmingView = UIView().then {
        $0.backgroundColor = .black
        $0.alpha = 0.06
        $0.isUserInteractionEnabled = false
    }

    public let footerView = CardViewFooterView()

    public let gradientView = GradientView().then {
        $0.isUserInteractionEnabled = false
    }

    public let bottomSeparator = UIView()

    public var gradientHeight: CGFloat = 0.0 {
        didSet {
            if oldValue != gradientHeight { setNeedsLayout() }
        }
    }

    public var footerHeight: CGFloat = 0.0 {
        didSet {
            if oldValue != footerHeight { setNeedsLayout() }
        }
    }

    public var bottomSeparatorHeight: CGFloat = 0.0 {
        didSet {
            if oldValue != bottomSeparatorHeight { setNeedsLayout() }
        }
    }

    public var showNewContentBadge: Bool {
        get {
            return !footerView.newContentBadge.isHidden
        }
        set {
            footerView.newContentBadge.isHidden = !newValue
        }
    }

    // MARK: Private Properties

    private let uuid = PresentUUID()

    private let shadowYOffset: CGFloat = 2.0
    private let shadowRadius : CGFloat = 4.0

    // MARK: Initialization
    
    public required override init(frame: CGRect)
    {
        super.init(frame: frame)
        
        addSubview(backgroundImageView)
        addSubview(dimmingView)
        addSubview(bottomSeparator)
        addSubview(gradientView)

        addSubview(footerView)
        footerView.joinedButtonTarget.addTarget(self, action: #selector(joinedButtonPressed), for: .touchUpInside)

        gradientView.layer.shadowColor = UIColor.black.cgColor
        gradientView.layer.shadowOpacity = 0.14
        gradientView.layer.shadowRadius = shadowRadius
        gradientView.layer.shadowOffset = CGSize(width: 0.0, height: shadowYOffset)
    }
    
    public convenience init() {
        self.init(frame: .zero)
    }
    
    public required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    // MARK: UIView
    
    public override func layoutSubviews()
    {
        super.layoutSubviews()
        
        backgroundImageView.bounds = CGRect(x: 0.0, y: 0.0, width: bounds.width, height: bounds.height - footerHeight - bottomSeparatorHeight)
        backgroundImageView.top --> top
        
        dimmingView.sizeToFitSuperview()
        
        bottomSeparator.bounds = CGRect(x: 0.0, y: 0.0, width: bounds.width, height: bottomSeparatorHeight)
        bottomSeparator.bottom --> bottom
        
        footerView.bounds.size = CGSize(width: bounds.width, height: footerHeight)
        footerView.bottom --> bottomSeparator.top
        footerView.setNeedsLayout()
        
        gradientView.bounds.size = CGSize(width: bounds.width, height: gradientHeight)
        gradientView.bottom --> bottomSeparator.top
        
        // Set a shadow path to improve performance. 
        // Also remove the shadow from the top side of the footer view.
        let rect = CGRect(x:0, y:6, width: gradientView.bounds.width, height: gradientView.bounds.height-6)
        gradientView.layer.shadowPath = UIBezierPath(rect: rect).cgPath
    }
    
    // MARK: Public Methods
    
    public func apply(viewModel: ViewModel) {
        backgroundImageView.image = viewModel.backgroundImage
        
        if viewModel.isJoined {
            footerView.joinedButton.imageView?.image = #imageLiteral(resourceName: "mycircles_selected")
            footerView.joinedButton.setImage(#imageLiteral(resourceName: "mycircles_selected"), for: .normal)
        } else {
            footerView.joinedButton.imageView?.image = #imageLiteral(resourceName: "mycircles_deselected")
            footerView.joinedButton.setImage(#imageLiteral(resourceName: "mycircles_deselected"), for: .normal)
        }
        
        footerView.titleLabel.text = viewModel.title
        footerView.locationView.apply(viewModel: viewModel.location)
        if let schedule = viewModel.schedule {
            footerView.scheduleView.apply(viewModel: schedule)
        }
        footerView.distanceView.text = viewModel.distance

        showNewContentBadge = viewModel.showNewContentBadge
        
        setNeedsLayout()
    }

    // MARK: Button handling

    @objc func joinedButtonPressed()
    {
        let statusChange = delegate?.joinPressed()
        if statusChange ?? false {
            // TODO: Disable the joined button for a short period of time to avoid double presses
            footerView.joinedButton.isEnabled = false
            footerView.joinedButtonTarget.isEnabled = false
            DispatchQueue.main.asyncAfter(milliseconds: 500) {
                self.footerView.joinedButton.isEnabled = true
                self.footerView.joinedButtonTarget.isEnabled = true
            }
        }
    }

    // MARK: Hashable

    public override var hashValue: Int {
        return uuid.hashValue
    }

    // MARK: View Model

    public struct ViewModel {

        public var backgroundImage: UIImage
        public var profile: UIImage?
        public var additionalPariticpantCount: Int
        public var title: String
        public var location: ImageTextView.ViewModel
        public var schedule: ImageTextView.ViewModel?
        public var distance: String
        public var showNewContentBadge: Bool
        public var isJoined: Bool
    }
}

// MARK: Footer View

public final class CardViewFooterView : UIView
{
    // MARK: Properties

    public let joinedButton = UIButton()
    public let joinedButtonTarget = UIButton()
    public let titleLabel = UILabel()
    public let locationView = ImageTextView()
    public let scheduleView = ImageTextView()
    public let distanceView = UILabel()
    public let newContentBadge = UIView()

    private struct LayoutConstants {
        static let margins = UIEdgeInsets(top: 0.0, left: 22.0, bottom: 0.0, right: 13.0)
    }

    // MARK: Initialization

    public required override init(frame: CGRect) {
        super.init(frame: frame)

        addSubview(joinedButton)
        addSubview(joinedButtonTarget)
        // Disable old style joined button
        joinedButton.isUserInteractionEnabled = false
        joinedButtonTarget.isEnabled = false
        
        addSubview(titleLabel)
        addSubview(locationView)
        addSubview(distanceView)
        addSubview(scheduleView)
        addSubview(newContentBadge)

        newContentBadge.backgroundColor = UIColor(hex: 0x5337C2)
    }

    public convenience init() {
        self.init(frame: .zero)
    }

    public required init(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    // MARK: UIView

    public override func layoutSubviews()
    {
        super.layoutSubviews()
        
        joinedButton.sizeToFit()
        joinedButton.right + LayoutConstants.margins.right.horizontalOffset --> right

        joinedButtonTarget.bounds = joinedButton.bounds.insetBy(dx: -10, dy: -10)
        joinedButtonTarget.center = joinedButton.center

        // Determine the full-height region in which to place the title and location.
        let titleAndSubtitleRegion: CGRect = {
            var region = self.bounds
            (_, region) = region.divided(atDistance: LayoutConstants.margins.left, from: .minXEdge)
            (_, region) = region.divided(atDistance: (joinedButton.left |--| right).width, from: .maxXEdge)
            return region
        }()

        // Size the title and location view according to the computed region width.
        titleLabel.sizeToFit(fixedWidth: titleAndSubtitleRegion.width)
        locationView.sizeToFit(fixedWidth: titleAndSubtitleRegion.width)
        
        scheduleView.isHidden = scheduleView.label.text == nil || scheduleView.label.text == ""
        if !scheduleView.isHidden {
            scheduleView.sizeToFit(fixedWidth: titleAndSubtitleRegion.width)
        }

        if scheduleView.isHidden {
            distributeSubviewsVertically(within: titleAndSubtitleRegion) {
                ~1~ <> titleLabel <> 10 <> locationView <> ~1~
            }
        } else {
            distributeSubviewsVertically(within: titleAndSubtitleRegion) {
                ~1~ <> titleLabel <> 10 <> locationView <> 10 <> scheduleView <> ~1~
            }
        }

        distanceView.sizeToFit()
        locationView.sizeToFit() // ?
        let maxSubtitleWidth = titleAndSubtitleRegion.width - distanceView.bounds.width - 20.0
        if locationView.bounds.width > maxSubtitleWidth {
            locationView.bounds.size.width = maxSubtitleWidth
        }
        titleLabel.bottomLeft + 10.verticalOffset <-- locationView.topLeft
        locationView.right + 10.horizontalOffset <-- distanceView.left
        if !scheduleView.isHidden {
            scheduleView.sizeToFit() // ?
            locationView.bottomLeft + 10.verticalOffset - 0.horizontalOffset <-- scheduleView.topLeft
        }

        let badgeRadius: CGFloat = 9.0
        newContentBadge.bounds.size = CGSize(width: badgeRadius, height: badgeRadius)
        newContentBadge.center = CGPoint(x:7+badgeRadius/2.0, y:titleAndSubtitleRegion.midY)
        newContentBadge.roundedCorners = newContentBadge.bounds.height/2.0
    }

}

// MARK: CardView Theming

extension CardView {

    func applyDefaultTheme() {
        footerView.titleLabel.apply(theme: UILabel.Theme(font: UIFont.presentFont(ofSize: 18.0, weight: .semibold), numberOfLines: 1))

        footerView.locationView.label.apply(theme: UILabel.Theme(font: UIFont.presentFont(ofSize: 14.0, weight: .medium), numberOfLines: 1))
        footerView.locationView.imageTextHorizontalMargin = 6.0
        let lightTheme = UILabel.Theme(textColor: UIColor(hex: 0xA4_AA_B3), font: UIFont.presentFont(ofSize: 14.0, weight: .medium), numberOfLines: 1)
        footerView.distanceView.apply(theme: lightTheme)
        footerView.scheduleView.label.apply(theme: lightTheme)

        footerHeight = CardViewController.footerHeight
        gradientHeight = CardViewController.footerHeight
        gradientView.colors = [.white, .white]
        bottomSeparatorHeight = CardViewController.separatorHeight
        bottomSeparator.backgroundColor = UIColor(hex: 0xEF_EF_F4)

        setNeedsLayout()
    }

}
