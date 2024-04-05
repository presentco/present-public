//
//  AddMemberListCell.swift
//  Present
//
//  Created by Patrick Niemeyer on 9/28/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation

public protocol AddMemberListCellSelection : class {
    func isMember(userId: String)->Bool
    func isSelected(userId: String)->Bool
    func selectMemberListCell(cell: AddMemberListCell)
    func deselectMemberListCell(cell: AddMemberListCell)
}

// TODO: This is an older cell type - We should migrate it to PresentListCell
// Cell holding a model that represents PersonData
public class AddMemberListCell: UITableViewCell, TableViewModelCell
{
    public typealias M = PersonData
    
    public static let reuseIdentifier = "invite-friends-reuse-id"
    
    public var user: PersonData?
    
    public weak var delegate: AddMemberListCellSelection?
    
    public var imageHeight: CGFloat = 60

    @IBOutlet weak var imageButton: UIButton! {
        didSet {
            imageButton.addTarget(self, action: #selector(buttonPressed), for: .touchUpInside)
            imageButton.isUserInteractionEnabled = true
        }
    }
    @IBOutlet weak var label: UILabel!
    
    @IBOutlet weak var selectedMark: UIView! {
        didSet {
            selectedMark.roundedCorners = selectedMark.bounds.height / 2.0
            selectedStatus = false
        }
    }
    @IBOutlet weak var button: UIButton! {
        didSet {
            button.addTarget(self, action: #selector(buttonPressed), for: .touchUpInside)
        }
    }
    @IBOutlet weak var phoneLabel: UILabel!

    // TODO: Make setter with animated option and use it from table view cell setup.
    public var selectedStatus :  Bool = false {
        didSet {
            if selectedStatus {
                let presentPurple = UIColor(red:149, green: 84, blue: 214)
                selectedMark.backgroundColor = presentPurple
                imageButton.layer.borderWidth = 2.0
                imageButton.layer.borderColor = presentPurple.cgColor
            } else {
                selectedMark.backgroundColor = UIColor(hex: 0xEF_EF_F4)
                imageButton.layer.borderWidth = 0.0
            }
        }
    }
    
    public required override init(style: UITableViewCellStyle, reuseIdentifier: String?) {
        super.init(style: .default, reuseIdentifier: reuseIdentifier);
        initView()
    }
    
    required public init() {
        super.init(style: .default, reuseIdentifier: AddMemberListCell.reuseIdentifier);
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
    
    @objc func buttonPressed()
    {
        if selectedStatus {
            // deselect
            selectedMark.pulse(1.2, endScale: 0.5) { _ in
                self.selectedStatus = false
                self.selectedMark.pulse(1.0, endScale: 1.0) { _ in
                    self.delegate?.deselectMemberListCell(cell: self)
                }
            }
        } else {
            // select
            selectedStatus = true
            selectedMark.pulse(1.2)
            delegate?.selectMemberListCell(cell: self)
        }
    }

    public func apply(model: PersonData) {
        configureUser(user: model)
    }
    
    public func configureUser(user: PersonData)
    {
        //logDebug("configure user: \(user.nameData.fullName)")
        self.selectionStyle = .none
        self.user = user
        self.selectedStatus = delegate?.isSelected(userId: user.id) ?? false
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
            // TODO: Cache this image
            self.imageButton.setImage(Initials.generateInitialsImage(name: user.nameData, height: imageHeight), for: .normal)
        }
        label.text = user.nameData.fullName
        
        if delegate?.isMember(userId: user.id) ?? false {
            phoneLabel.text = "Member"
            button.isEnabled = false
            selectedMark.isHidden = true
        } else {
            phoneLabel.text = nil
            button.isEnabled = true
            selectedMark.isHidden = false
        }
    }
    
    override public func layoutSubviews() {
        imageButton.layer.cornerRadius = imageButton.bounds.width/2.0
        imageButton.clipsToBounds = true
    }
}
