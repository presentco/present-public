//
//  UserScrollerCell.swift
//  Present
//
//  Created by Patrick Niemeyer on 10/3/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import MIBadgeButton_Swift

public protocol UserScrollerCellDelegate : class {
    func selectedUser(user: PersonData)
}

public class UserScrollerCell : UIView
{
    @IBOutlet weak var imageButton: MIBadgeButton! {
        didSet {
            imageButton.clipsToBounds = true
            imageButton.addTarget(self, action: #selector(imageButtonPressed), for: .touchUpInside)
            imageButton.imageView?.contentMode = .scaleAspectFill
        }
    }
    @IBOutlet weak var imageButtonHeight: NSLayoutConstraint!
    @IBOutlet weak var label: UILabel!
    @IBOutlet weak var creatorLabel: UILabel! {
        didSet {
            creatorLabel.textColor = UIColor(hex: 0x999999)
        }
    }
    
    public weak var delegate: UserScrollerCellDelegate?
    public var user: PersonData?
    
    convenience init(user: PersonData, imageHeight: CGFloat) {
        self.init(frame: CGRect.zero)
        configureUser(user: user, imageHeight: imageHeight)
    }
    
    convenience init() {
        self.init(frame: CGRect.zero)
    }

    override init(frame: CGRect) {
        super.init(frame: frame)
        initView()
    }
    
    required public init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        initView()
    }
    
    private func initView() {
        let view = viewFromNibForClass()
        view.frame = bounds
        addSubview(view)
    }

    // todo: should allow height to be set later for layout
    public func configureUser(user: PersonData, imageHeight: CGFloat)
    {
        self.user = user
        
        if user.hasPhotoData {
            if let url = user.photoURLData {
                ImageManager.shared.getImage(atURL: url, successHandler: { profilePhoto in
                    self.imageButton.setImage(profilePhoto, for: .normal)
                })
            } else {
                self.imageButton.setImage(user.photoData, for: .normal)
            }
        } else {
            // Show the initials
            // TODO: Height may be at default here
            // TODO: Cache this image
            self.imageButton.setImage(Initials.generateInitialsImage(name: user.nameData, height: imageHeight), for: .normal)
        }
        label.text = user.nameData.givenName 
        
    }
    
    override public func layoutSubviews() {
        imageButton.layer.cornerRadius = imageButton.bounds.width/2.0
    }
    
    @objc private func imageButtonPressed() {
        guard let user = user else { return }
        delegate?.selectedUser(user: user)
    }
}
