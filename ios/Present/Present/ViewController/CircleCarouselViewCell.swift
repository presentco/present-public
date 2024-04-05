//
//  PresentView.swift
//  Present
//
//  Created by Patrick Niemeyer on 10/10/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation

public class CircleCarouselViewCell: UICollectionViewCell
{
    public static let reuseIdentifier = "circle-carousel"
    
    @IBOutlet weak var cellContentView: UIView!
    @IBOutlet weak var image: UIImageView!
    @IBOutlet weak var titleText: UILabel!
    @IBOutlet weak var subtitleText: UILabel!

    var currentGroup: Group?

    convenience init() {
        self.init(frame: CGRect.zero)
    }

    override init(frame: CGRect) {
        super.init(frame: frame)
        initView()
    }

    required public init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
    
    override public func layoutSubviews() {
        super.layoutSubviews()
    }
    
    public func configure(withGroup group: Group)
    {
        currentGroup = group
        self.image.image = nil
        if let url = group.coverPhoto {
            ImageManager.shared.getImage(atURL: url) { coverImage in
                if let currentGroup = self.currentGroup, currentGroup.groupToken != group.groupToken { return }
                self.image.image = coverImage
            }
        } else {
            MapSnapshotManager.shared.mapSnapshot(
                at: group.location,
                size: CircleCarouselViewController.cardImageSize,
                name: group.title)
            { mapSnapshot in
                if let currentGroup = self.currentGroup, currentGroup.groupToken != group.groupToken { return }
                self.image.image = mapSnapshot
            }
        }
        titleText.text = group.title
        subtitleText.text = group.locationName
    }

    private func initView() {
        let view = viewFromNibForClass()
        view.frame = bounds
        addSubview(view)
    }
}


