//
//  NavBar.swift
//  Present
//
//  Created by Patrick Niemeyer on 6/12/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation

public class NavBar: UIView
{
  
    @IBOutlet weak var title: UILabel!
    
    @IBOutlet weak var subtitle: UILabel! {
        didSet {
            subtitle.isHidden = true
        }
    }

    @IBOutlet weak var cancelButton: ThemeableButton!
    
    @IBOutlet weak var saveButton: ThemeableButton! {
        didSet {
            saveButton.setTitleColor(UIColor(hex: 0xbdbdbd), for: .disabled)
            saveButton.isEnabled = false
        }
    }
    
    var rightButton: ThemeableButton {
        get {
            return saveButton
        }
    }

    @IBOutlet weak var backButton: ThemeableButton!
       
    var isModal = false {
        didSet{
            if isModal {
                backButton.setImage(#imageLiteral(resourceName: "CancelSearchGlyph"), for: .normal)
            } else {
                backButton.setImage(#imageLiteral(resourceName: "LeftChevronGlyph"), for: .normal)
            }
        }
    }
        
    @IBOutlet weak var hamburgerButton: ThemeableButton!
    
    override init(frame: CGRect) {
        super.init(frame: frame)
   
        initViewFromNib()
    }
    
    required public init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        initViewFromNib()
    }
}
