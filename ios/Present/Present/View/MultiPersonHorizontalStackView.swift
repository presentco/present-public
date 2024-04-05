//
//  MultiPersonHorizontalStackView.swift
//  Present
//
//  Created by Dan Federman on 7/18/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import Relativity
import Then
import UIKit


public final class MultiPersonHorizontalStackView: UIView {
    
    // MARK: Public Properties
    
    public var personProfileImagesToDisplay = [UIImage]() {
        didSet {
            personViews = personProfileImagesToDisplay.map { image in
                return UIImageView().then {
                    $0.contentMode = .scaleAspectFill
                    $0.image = image
                }
            }
        }
    }
    
    public var additionalCountToDisplay: Int = 0 {
        didSet {
            if additionalCountToDisplay > 0 {
                let additionalPeopleLabel = UILabel()
                additionalPeopleLabel.text = "+\(additionalCountToDisplay)"
                additionalPeopleLabel.font = .presentFont(ofSize: 14.0, weight: .semibold)
                additionalPeopleLabel.adjustsFontSizeToFitWidth = true
                additionalPeopleLabel.textColor = .white
                additionalPeopleLabel.backgroundColor = UIColor(hex: 0x71_41_DB)
                additionalPeopleLabel.textAlignment = .center
                self.additionalPeopleLabel = additionalPeopleLabel
                
            } else {
                additionalPeopleLabel = nil
            }
            
            setNeedsLayout()
        }
    }
    public var personDimension: CGFloat = 33.0 {
        didSet {
            setNeedsLayout()
        }
    }
    public var personOverlap: CGFloat = 7.0 {
        didSet {
            setNeedsLayout()
        }
    }
    
    // MARK: UIView
    
    public override func sizeThatFits(_ size: CGSize) -> CGSize {
        guard personViews.count > 0 || additionalPeopleLabel != nil else {
            // We have nothing to display.
            return .zero
        }
        
        let viewCount = CGFloat(personViews.count + (additionalPeopleLabel != nil ? 1 : 0))
        let personWidth = viewCount * personDimension
        let overlap = (viewCount - 1) * personOverlap
        return CGSize(
            width: personWidth - (overlap > 0 ? overlap : 0),
            height: personDimension
        )
    }
    
    public override func layoutSubviews() {
        let personSize = CGSize(
            width: personDimension,
            height: personDimension
        )
        personViews.forEach {
            $0.bounds.size = personSize
            $0.layer.cornerRadius = personDimension / 2.0
            $0.clipsToBounds = true
        }
        
        var previousAnchor = left
        for personView in personViews {
            previousAnchor <-- personView.left
            previousAnchor = personView.right - personOverlap.horizontalOffset
        }
        
        if let additionalPeopleLabel = additionalPeopleLabel {
            additionalPeopleLabel.bounds.size = personSize
            additionalPeopleLabel.layer.cornerRadius = personDimension / 2.0
            additionalPeopleLabel.clipsToBounds = true
            previousAnchor <-- additionalPeopleLabel.left
        }
    }
    
    // MARK: Private Properties
    
    private var personViews = [UIImageView]() {
        didSet {
            oldValue.forEach { $0.removeFromSuperview() }
            personViews.forEach { addSubview($0) }
            setNeedsLayout()
        }
    }
    private var additionalPeopleLabel: UILabel? {
        didSet {
            oldValue?.removeFromSuperview()
            if let additionalPeopleLabel = additionalPeopleLabel {
                addSubview(additionalPeopleLabel)
            }
            setNeedsLayout()
        }
    }
}
