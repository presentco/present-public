//
//  CircleCarouselViewController.swift
//  Present
//
//  Created by Patrick Niemeyer on 3/20/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation

public protocol CircleCarouselViewControllerDelegate : class {
    func didSelectGroup(group: Group)
}

public class CircleCarouselViewController : UIViewController
{
    static let cardSize = CGSize(width:250, height: 190)
    static let cardImageSize = CGSize(width: 250, height: 140)
    let collectionView: UICollectionView
    var groups: [Group] = []
    let layout = UICollectionViewFlowLayout()

    weak var delegate: CircleCarouselViewControllerDelegate?

    public init()
    {
        layout.scrollDirection = .horizontal
        layout.estimatedItemSize = CircleCarouselViewController.cardSize
        layout.itemSize = CircleCarouselViewController.cardSize
        layout.minimumLineSpacing = 5.0
        collectionView = UICollectionView(frame: CGRect.zero, collectionViewLayout: layout)
        
        super.init(nibName: nil, bundle: nil)

        self.collectionView.backgroundColor = .white
        self.collectionView.collectionViewLayout = layout
        self.collectionView.dataSource = self
        self.collectionView.delegate = self
        
        self.collectionView.register(CircleCarouselViewCell.self, forCellWithReuseIdentifier: CircleCarouselViewCell.reuseIdentifier)

        view.addSubview(collectionView)
    }
    
    public var sectionInset: UIEdgeInsets {
        get {
            return layout.sectionInset
        }
        set {
            layout.sectionInset = newValue
        }
    }
    public required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override public func viewDidLayoutSubviews() {
        collectionView.sizeToFitSuperview()
    }

    public func configure(withGroups groups: [Group]) {
        self.groups = groups
        collectionView.reloadData()
    }
}

extension CircleCarouselViewController : UICollectionViewDataSource, UICollectionViewDelegate
{
    public func collectionView(_ collectionView: UICollectionView, numberOfSections: Int) -> Int {
        return 1 //return number of sections in collection view
    }

    public func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        //return 1 // number of rows in section
        return groups.count     //return number of sections in collection view
    }
    
    public func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell
    {
        let cell = (collectionView.dequeueReusableCell(withReuseIdentifier: CircleCarouselViewCell.reuseIdentifier, for: indexPath) as? CircleCarouselViewCell) ?? CircleCarouselViewCell()
        cell.bounds = CGRect(x:0, y:0, width: CircleCarouselViewController.cardSize.width, height: CircleCarouselViewController.cardSize.height)
        cell.configure(withGroup: groups[indexPath.item])
        //cell.backgroundColor =  indexPath.item % 2 == 0 ? .red : .blue
        cell.backgroundColor =  .white

        // Clip the content
        cell.cellContentView.roundedCorners = 5.0
        cell.cellContentView.clipsToBounds = true
        // Add the shadow
        cell.roundedCorners = 5.0
        cell.setShadow(shadowXOffset: 1, shadowYOffset: 1, shadowOpacity: 0.12, shadowRadius: 1, setShadowPath: true)

        return cell
    }
    
    //MARK: UICollectionViewDelegate
    
    public func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath)
    {
        AppDelegate.shared.screenPresenter.pushGroup(for: groups[indexPath.item])
    }
}

