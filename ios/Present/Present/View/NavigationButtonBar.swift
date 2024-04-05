//
//  NavigationButtonBar.swift
//  Present
//
//  Created by Dan Federman on 7/12/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import Relativity


public final class NavigationButtonBar: UIView {
    
    // MARK: Public Properties
    
    /// Buttons to display in the bar. They should be sized prior to setting them.
    public var buttons = [ThemeableButton]() {
        didSet {
            oldValue.forEach {
                $0.removeFromSuperview()
            }
            buttons.forEach {
                addSubview($0)
            }
            setNeedsLayout()
        }
    }
    
    // MARK: UIView
    
    public override func layoutSubviews() {
        super.layoutSubviews()
        
        // Assume that buttons have been sized prior to setting them here.
        
        switch buttons.count {
        case Int.min...0:
            // Nothing to do here.
            break
            
        case 1:
            buttons[0].middle --> middle
            
        case 2:
            distributeSubviewsHorizontally {
                ~1~ <> buttons[0] <> ~1~ <> buttons[1]
            }
            
        case 3:
            distributeSubviewsHorizontally {
                ~1~ <> buttons[0] <> ~1~ <> buttons[1] <> ~1~ <> buttons[2]
            }
            
        case 4...Int.max:
            var distribution = [DistributionItem]()
            
            let outerDistribution = ~2~
            
            for (buttonIndex, button) in buttons.enumerated() {
                if buttonIndex == 0 {
                    distribution.append(contentsOf: outerDistribution <> button)
                    
                } else {
                    distribution.append(contentsOf: ~3~ <> button)
                }
            }
            
            distribution.append(outerDistribution)
            
            distributeSubviewsHorizontally { () -> [DistributionItem] in
                distribution
            }
            
        default:
            fatalError("Swift is dumb and can't recognize that the above is exhaustive")
        }
    }
    
}
