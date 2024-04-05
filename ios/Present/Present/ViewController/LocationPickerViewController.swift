//
//  LocationPickerViewController.swift
//  Present
//
//  Created by Patrick Niemeyer on 6/18/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import PresentProtos

public class LocationPickerViewController: SearchTableViewController
{
    override public func viewDidLoad()
    {
        initTable() // init the table before we run config
        super.viewDidLoad()

        navBar.title.text = "Change Location"
        searchBar.placeholder = "Search for a city"
    }
    
    let tableViewModel = TableViewModel()
    
    private func initTable()
    {
        tableViewModel.bind(searchTextSource: searchText)
        tableView.bind(model: tableViewModel)
        
        let citiesSection = tableViewModel.addSection(cellType: LocationPickerCell.self)
        
        service.getCitiesAvailable().onSuccess { cities in
            citiesSection.items = cities
            citiesSection.items.insert(nil, at: 0)
            }.disposed(by: disposal)
        
        citiesSection.selected.bind { city in
            self.save(location: city)
            self.screenPresenter.goBack()
            }.disposed(by: disposal)
        
        
        citiesSection.filter = { row, text in row?.name.lowercased().contains(text.lowercased()) ?? false }
    }
    
    private func save(location: Present.City?) {
        userManager.selectedLocation.value = location
        // TODO: Request header generator should listen itself
        service.requestHeaderGenerator.selectedLocation = location
    }
}

public class LocationPickerCell: UITableViewCell, TableViewModelCell
{
    public typealias M = Present.City?
    
    let icon = UIImageView().then {
        $0.image = #imageLiteral(resourceName: "PinIcon")
        $0.contentMode = .scaleAspectFit
    }
    let label = UILabel().then {
        $0.font = UIFont.systemFont(ofSize: 18, weight: .semibold)
    }
    
    public required override init(style: UITableViewCellStyle, reuseIdentifier: String?) {
        super.init(style: .default, reuseIdentifier: reuseIdentifier);
        UIStackView(arrangedSubviews: [icon, label, UIView()/*consume extra space*/]).do {
            $0.spacing = 10
            addSubview($0)
            $0.constrainToFillSuperview(insets: UIEdgeInsets(top: 0, left: 10, bottom: 0, right: 10))
        }
        //selectionStyle = .none
    }
    
    required public init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    public func apply(model: Present.City?) {
        label.text = model?.name ?? "Current location"
    }
}

