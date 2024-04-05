//
//  Created by Patrick Niemeyer on 4/20/16.
//  Copyright © 2017 Present Company. All rights reserved.
//

import CoreLocation
import Foundation
import MapKit
import UIKit
import Relativity

public class CreateGroupSearchResultTableViewCell : UITableViewCell {
    public static let expectedHeight: CGFloat = 40
    public static let reuseIdentifier = "createGroupSearchResult"
    
    public var separator = SeparatorView()
    public var nameLabel = UILabel()
    public var locationLabel = UILabel()
    public var iconImage = UIImageView()
    public var iconContainer = UIView()
    
    private let nameLabelTheme = UILabel.Theme(
        textColor: UIColor.black,
        font: UIFont.presentFont(ofSize: 14, weight: .regular),
        numberOfLines: 1
    )
    
    private let locationLabelTheme = UILabel.Theme(
        textColor: UIColor(red: 164, green: 170, blue: 179, alpha: 1.0),
        font: UIFont.presentFont(ofSize: 13, weight: .regular),
        numberOfLines: 1
    )
    
    public override init(style: UITableViewCellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        
        contentView.addSubview(iconContainer)
        iconContainer.addSubview(iconImage)
        
        nameLabel.apply(theme: nameLabelTheme)
        contentView.addSubview(nameLabel)
        locationLabel.apply(theme: locationLabelTheme)
        locationLabel.lineBreakMode = .byTruncatingTail
        contentView.addSubview(locationLabel)
        
        separator.backgroundColor = UIColor.white
        separator.width = 0.5
        separator.color = Palette.createGroupPanelDividerColor
        contentView.addSubview(separator)
    }
    
    required public init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    // MARK: UIView
    
    public override func layoutSubviews() {
        super.layoutSubviews()
        
        let iconHeight : CGFloat = 16
        let iconWidth = iconHeight
        iconContainer.bounds.size = CGSize(width: iconWidth, height: contentView.bounds.height)
        iconContainer.left --> contentView.left
        
        iconImage.bounds.size = CGSize(width: iconWidth, height: iconHeight)
        iconImage.left --> iconContainer.left
        
        let iconPadRight = 5.horizontalOffset
        if locationLabel.text.isEmpty {
            nameLabel.sizeToFit()
            nameLabel.left --> iconContainer.right + iconPadRight
        } else {
            nameLabel.sizeToFit()
            nameLabel.topLeft --> iconContainer.topRight + iconPadRight + 14.verticalOffset
            locationLabel.sizeToFit()
            locationLabel.topLeft --> nameLabel.bottomLeft + 5.verticalOffset
        }
        
        let separatorProtrusion : CGFloat = 1.0
        separator.bounds.size = CGSize(width: (locationLabel.left |--| contentView.right).width + separatorProtrusion, height: 1.0)
        separator.bottomRight --> contentView.bottomRight - separatorProtrusion.horizontalOffset + 1.verticalOffset
    }
    
    public func configureWith(result: CreateGroupSearchResult) {
        
        // Set the title text.
        nameLabel.text = {
            switch result.type {
            case .none:
                return NSLocalizedString(
                    "CreateGroupSearchResultTableViewCellUnspecifiedName",
                    tableName: nil,
                    bundle: .main,
                    value: "Unknown",
                    comment: "Text of the location search result cell when a location is not known."
                )
            
            case .customLocation:
                return String(format: NSLocalizedString(
                    "CreateGroupSearchResultTableViewCellCustomLocationFormat",
                    tableName: nil,
                    bundle: .main,
                    value: "Add Custom Location: %@",
                    comment: "Format for adding a custom location. The first parameter is the title of the location."
                ), result.title)
                
            case .currentLocation:
                return NSLocalizedString(
                    "CreateGroupSearchResultTableViewCellCurrentLocationName",
                    tableName: nil,
                    bundle: .main,
                    value: "Current Location",
                    comment: "Text of the location search result cell for the current location."
                )
                
            case .previouslySelected:
                return NSLocalizedString(
                    "CreateGroupSearchResultTableViewCellPreviouslySelectedName",
                    tableName: nil,
                    bundle: .main,
                    value: "Placed Earlier",
                    comment: "Text of the location search result cell when editing a circle indicating the location is unchanged."
                )
                
            case .area, .place:
                return result.title
            }
        }()
        
        // Set the subtitle (normally an address)
        locationLabel.text = result.subtitle
        
        // Set the icon
        iconImage.contentMode = .scaleAspectFit
        switch result.type {
        case .area, .none, .place, .previouslySelected:
            iconImage.image = #imageLiteral(resourceName:"pin-icon")
        case .customLocation:
            iconImage.image = #imageLiteral(resourceName:"plus-icon")
        case .currentLocation:
            iconImage.image = #imageLiteral(resourceName:"location-icon")
        }
    }
    
}

