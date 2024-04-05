//
//  ProfileFriendsViewController.swift
//  Present
//
//  Created by Patrick Niemeyer on 6/19/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import PresentProtos

public class SearchViewController: PresentViewController
{
    @IBOutlet weak var navBar: NavBar! {
        didSet {
            navBar.backButton.isShown = true
            navBar.backButton.rx.tap.bind { [weak self] in
                    self?.screenPresenter.goBack()
                }.disposed(by: disposal)
        }
    }
    
    @IBOutlet weak var searchLabel: UILabel!
    
    @IBOutlet weak var searchBar: UISearchBar! {
        didSet {
            searchBar.backgroundImage = UIImage()
        }
    }
    
    public lazy var searchText = searchBar.rx.text.map { text in
        return SearchText.forText(text)
    }
    
    @IBOutlet weak var searchSeparator: UIView!
    @IBOutlet weak var content: UIView!

    override public func viewDidLayoutSubviews() {
        // Hack a border on the UISearchBar
        searchBar.subviews.first?.subviews.first { $0 is UITextField }?.do {
            $0.roundCornersToHeight()
            $0.layer.borderWidth = 1.0
            $0.layer.borderColor = UIColor.hex(0xf2f2f2).cgColor
        }
    }
    
    override public init() {
        // Children use our xib
        super.init(nibName: String(describing: SearchViewController.self), bundle: nil)
    }
    
    public required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
}




