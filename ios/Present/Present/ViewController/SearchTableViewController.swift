//
//  ProfileFriendsViewController.swift
//  Present
//
//  Created by Patrick Niemeyer on 6/19/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import PresentProtos

public class SearchTableViewController: SearchViewController
{
    var tableView = UITableView().then {
        $0.separatorInset = UIEdgeInsets(top: 0, left: 9, bottom: 0, right: 9)
        $0.separatorColor = UIColor(hex: 0xF8F8F8)
        $0.rowHeight = 41
    }
    
    override public func viewDidLoad() {
        super.viewDidLoad()
        tableView.do {
            content.addSubview($0)
            $0.constrainToFillSuperview()
        }
    }
}




