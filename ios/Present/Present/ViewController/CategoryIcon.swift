//
//  CategoryIcon.swift
//  Present
//
//  Created by Patrick Niemeyer on 7/25/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation

//@IBDesignable 
public class CategoryIcon : UIView
{
    @IBOutlet weak var imageView: UIImageView!
    @IBOutlet weak var labelView: UILabel!
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        initView()
    }
    
    required public init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }

    private func initView() {
        let view = viewFromNibForClass()
        view.frame = bounds
        addSubview(view)
    }
}

