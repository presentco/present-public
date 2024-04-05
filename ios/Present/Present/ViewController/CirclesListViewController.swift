//
//  CirclesListViewController.swift
//  Present
//

import Foundation
import CoreGraphics
import Foundation
import PresentProtos
import UIKit

public class CirclesListViewController: SearchTableViewController, UITableViewDelegate
{
    private let tableViewModel = TableViewModel()
    var section: TableViewModel.Section<PresentGroupListCell>?

    public func configure(groups: [Group]) {
        configure {
            $0.initTable()
            $0.section?.items = groups
            [$0.searchSeparator, $0.searchBar].forEach {
                $0.isShown = groups.count > 6
            }
        }
    }
    
    private func initTable()
    {
        self.tableView.rowHeight = 70
        searchBar.placeholder = "Search circles"
        
        tableViewModel.do {
            $0.bind(searchTextSource: searchText)
            tableView.bind(model: $0)
        }
        
        let filter = { (row: Group, text: String) in row.title.lowercased().contains(text.lowercased()) }
        
        self.section = tableViewModel.addSection(cellType: PresentGroupListCell.self)
            .then { section in
                section.selected.bind(onNext: screenPresenter.pushGroup).disposed(by: disposal)
                section.filter = filter
        }
    }
}

