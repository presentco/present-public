//
//  CreateGroupSearchPanelViewController.swift
//  Pat Niemeyer 04/20/2017
//  Copyright © 2017 Present Company. All rights reserved.
//

import CoreLocation
import Foundation
import Relativity
import MapKit
import Then
import UIKit


public protocol CreateGroupSearchPanelDelegate: class {
    func searchTextChanged()
    func searchResultSelected(result: CreateGroupSearchResult)
}

public final class CreateGroupSearchPanelViewController: UIViewController
{
    private struct LayoutMetrics {
        public static let collapsedHeight: CGFloat = 60
        public static let expandedHeight: CGFloat = 218
    }
    
    private enum ImageSelectionState {
        case none
        case previouslySelected
        case selected(UIImage)
    }
    
    // MARK: Initialization
    
    public required init(locationProvider: LocationProvider, selectedLocation location: SelectedLocation? = nil) {
        self.locationProvider = locationProvider
        self.prefillData = location
        super.init(nibName: nil, bundle: nil)
    }
    
    public required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    // MARK: Public Properties
    
    public weak var delegate: CreateGroupSearchPanelDelegate?

    // Current selection (resolved)
    public var selectedSearchResult: CreateGroupSearchResult? {
        didSet {
            if let result = selectedSearchResult {
                delegate?.searchResultSelected(result: result)
            }
        }
    }
    
    // MARK: Private Properties
    
    // Search Model
    fileprivate let maxSearchResults = 4 // Max search results in the condensed view
    fileprivate var searchLocation: CLLocation! // The location for search completion and local name suggestion
    fileprivate var searchCompleter: MKLocalSearchCompleter?
    fileprivate var searchCompleterResults = [MKLocalSearchCompletion]() // The most recent search completer results
    fileprivate var searchResults: [CreateGroupSearchResult] = [] // The model for the table view

    /// The "custom location" search result updated to show the user entered text and circle location.
    fileprivate let showCustomLocationOption = false
    fileprivate var customLocationSeachResult = CreateGroupSearchResult.customLocation()

    /// The reverse geocode derived location suggestion for the current circle location
    fileprivate var currentLocationSearchResult = CreateGroupSearchResult.currentLocation()

    // MARK: View Hierarchy
    /*
        Panel View
            Top View
               Top Label
               Location Field
            Table View
    */
    
    fileprivate let panelView = UIView()
    fileprivate let topView = UIView()
    fileprivate let topLabel = UILabel().then {
        $0.font = UIFont.presentFont(ofSize: 18, weight: .semibold)
        $0.text = "Location" // todo: Localize
    }
    fileprivate let locationField = ThemeableTextField().then {
        $0.apply_searchPanelTheme()
        $0.placeholder = "Enter Your Location" // TODO: Localize
    }
    fileprivate let tableView = UITableView().then {
        $0.separatorStyle = .none
    }
    
    fileprivate let locationProvider: LocationProvider
    fileprivate var prefillData: SelectedLocation?
    
    // MARK: UIResponder
    
    public override var canBecomeFirstResponder: Bool {
        return true
    }
    
    @discardableResult
    public override func becomeFirstResponder() -> Bool {
        super.becomeFirstResponder()
        
        // Once a result is selected stop automatically grabbing focus
        guard self.selectedSearchResult == nil else {
            return false
        }
        return locationField.isFirstResponder || locationField.becomeFirstResponder()
    }
    
    @discardableResult
    public override func resignFirstResponder() -> Bool {
        super.resignFirstResponder()
        
        return locationField.resignFirstResponder()
    }
    
    // MARK: UIViewController

    public override func viewDidLoad()
    {
        super.viewDidLoad()

        addKeyboardShowHideObservers()

        view.backgroundColor = UIColor.white
        view.addSubview(panelView)
        panelView.clipsToBounds = true

        panelView.addSubview(topView)
        panelView.addSubview(tableView)
        
        tableView.dataSource = self
        tableView.delegate = self
        
        locationField.addTarget(self, action: #selector(searchTextChanged), for: .editingChanged)
        locationField.addTarget(self, action: #selector(searchTextEditingBegin), for: .editingDidBegin)
        
        topView.addSubview(topLabel)
        topView.addSubview(locationField)
        
        // Apply prefill information.
        if let prefillData = prefillData {
            // Fill in the location as from a search result.
            select(result: CreateGroupSearchResult(previouslySelected: prefillData.coordinate, title: prefillData.locationName))
        }
        
        configureSearch()
    }
    
    /*
    public override func viewDidAppear(_ animated: Bool) {
        view.setNeedsLayout() // Note: for iOS11 safe area update
    }*/
    
    public override func viewDidLayoutSubviews()
    {
        super.viewDidLayoutSubviews()

        view.frame.size.height = isExpanded ? LayoutMetrics.expandedHeight : LayoutMetrics.collapsedHeight

        panelView.frame = view.bounds
        let panelWidth = panelView.bounds.width

        topView.bounds.size = CGSize(width: panelWidth, height: LayoutMetrics.collapsedHeight)
        topView.top --> panelView.top

        let separatorHeight: CGFloat = 1.0
        let fieldHeight: CGFloat = (topView.bounds.height - 2 * separatorHeight) / 2.0
        let fieldInset: CGFloat = 30
        
        topLabel.sizeToFit()
        topLabel.topLeft --> panelView.topLeft + fieldInset.horizontalOffset + 10.verticalOffset
        
        locationField.bounds.size = CGSize(width: panelWidth - 2 * fieldInset, height: fieldHeight)
        locationField.topLeft --> topLabel.bottomLeft + 4.verticalOffset

        //let tableViewHorizInset = 44.horizontalOffset
        let tableViewHorizInset = fieldInset.horizontalOffset
        let tableViewVertPad = 0.verticalOffset
        tableView.bounds.size = (topView.bottomLeft + tableViewHorizInset) |--| (panelView.bottomRight - 6.horizontalOffset)
        tableView.topLeft --> topView.bottomLeft + tableViewHorizInset + tableViewVertPad
    }

    // MARK: height management
    
    public func setExpanded(_ expanded: Bool, animated: Bool = true) {
        if !expanded {
            resignFirstResponder()
        }
        
        guard expanded != isExpanded else {
            return
        }
        
        view.layoutIfNeeded()
        
        isExpanded = expanded
        
        UIView.animate(withDuration: animated ? 0.2 : 0.0) {
            self.view.layoutIfNeeded()
        }
    }
    
    private var isExpanded: Bool = false {
        didSet {
            guard isViewLoaded else {
                return
            }
            
            if oldValue != isExpanded {
                view.setNeedsLayout()
            }
        }
    }
    
    // MARK: Other

    private func theme(separator: SeparatorView) {
        separator.backgroundColor = UIColor.white
        separator.width = 0.5
        separator.color = Palette.createGroupPanelDividerColor
    }

    private func addLabel(withLocalizedTitle localizedLabel: String, to field: UITextField)
    {
        let label = UILabel()
        label.apply(theme: UILabel.Theme(
            textColor: Palette.createGroupPanelLabelColor,
            font: .presentFont(ofSize: 15.0, weight: .medium),
            textAlignment: .left
        ))
        label.text = localizedLabel
        label.sizeToFit()
        label.bounds.size.width += 8.0
        label.contentMode = .left
        
        field.leftView = label
        field.leftViewMode = .always
    }
}

// Search results table data
extension CreateGroupSearchPanelViewController : UITableViewDataSource, UITableViewDelegate {
    
    public func tableView( _ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return searchResults.count
    }
    
    public func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let id = CreateGroupSearchResultTableViewCell.reuseIdentifier
        let cell = ( tableView.dequeueReusableCell(withIdentifier: id) as? CreateGroupSearchResultTableViewCell) ??
            CreateGroupSearchResultTableViewCell(style: .default, reuseIdentifier: id)
        cell.configureWith(result: searchResults[indexPath.item])
        cell.selectionStyle = .none
        cell.separator.isHidden = true
        return cell
    }
    
    public func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        select(result: searchResults[indexPath.item])
    }

    public func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return CreateGroupSearchResultTableViewCell.expectedHeight
    }

    fileprivate func select(result: CreateGroupSearchResult) {
        locationField.text = result.title
        searchQueryChanged()
        
        result.resolveLocation { (resolvedResult, location) in
            self.selectedSearchResult = resolvedResult
        }
    }
    
}

/// Search API / completion functionality
extension CreateGroupSearchPanelViewController : MKLocalSearchCompleterDelegate {
    
    func configureSearch()
    {
        let searchCompleter = MKLocalSearchCompleter()
        searchCompleter.filterType = .locationsOnly
        searchCompleter.delegate = self
        self.searchCompleter = searchCompleter
        
        let range = 1000.0 // m
        if let location = prefillData
        {
            // Initial search location is preset location
            searchCompleter.region = MKCoordinateRegionMakeWithDistance(location.coordinate, range, range)
            let clLocation = CLLocation(latitude: location.coordinate.latitude, longitude: location.coordinate.longitude)
            self.setSearchLocation(to: clLocation)
        } else {
            locationProvider.executeWhenLocationAvailable { location in
                // Initial search location is user location.
                searchCompleter.region = MKCoordinateRegionMakeWithDistance(location.coordinate, range, range)
                self.setSearchLocation(to: location)
            }
        }
    }
    
    // The circle center position. Initially the user location.
    func setSearchLocation(to location: CLLocation) {
        searchLocation = location
        populateLocationSuggestions()
    }
    
    // Init the suggestions derived from current location alone
    func populateLocationSuggestions()
    {
        locationProvider.executeWhenLocationAvailable { [weak self] location in
            CLGeocoder().reverseGeocodeLocation(location) { [weak self] (placemarks, _) in
                guard let strongSelf = self, let placemark = placemarks?.first else {
                    return
                }
                strongSelf.currentLocationSearchResult.configureWith(placemark: placemark)
                strongSelf.updateSearchResults()
            }
        }
    }

    @objc func searchTextEditingBegin() {
        setExpanded(true)
    }
    
    func searchQueryChanged() {
        if let searchCompleter = searchCompleter {
            searchCompleter.queryFragment = locationField.text ?? ""
        }
        
        customLocationSeachResult.title = locationField.text ?? ""
    }
    
    @objc func searchTextChanged()
    {
        searchQueryChanged()
        selectedSearchResult = nil

        delegate?.searchTextChanged()
        // The search completer will not generate a callback when the fragment goes to empty string.
        // We update the results here to allow us to remove the user text based custom location result.
        if locationField.text == "" {
            updateSearchResults()
        }
        setExpanded(true)
    }
    
    public func completerDidUpdateResults(_ completer: MKLocalSearchCompleter)
    {
        searchCompleterResults = completer.results
        updateSearchResults()
    }
    
    /// Update the search results from the latest search completer results, 
    /// appending any fixed results and the "custom location" meta search result.
    func updateSearchResults()
    {
        let limit = maxSearchResults
        var results =
            // limit count
            searchCompleterResults.prefix(limit)
            // map to our model
            .map { CreateGroupSearchResult( searchCompletion: $0 ) }

        // Add the current location search result at the top
        results.insert(currentLocationSearchResult, at: 0)

        // Add the custom location search result at the bottom (if enabled)
        if showCustomLocationOption && locationField.text != nil && locationField.text != "" {
            results.append( customLocationSeachResult )
        }
        
        // set the model and update the table
        searchResults = results
        tableView.reloadData()
    }
    
}


// MARK: ThemeableTextField Theming

fileprivate extension ThemeableTextField
{
    func apply_searchPanelTheme() {
        apply(theme: ThemeableTextField.Theme(
            textColor: Palette.createGroupPanelTextColor,
            placeholderTextColor: UIColor(red: 164, green: 170, blue: 179),
            backgroundColor: UIColor.white,
            font: .presentFont(ofSize: 14.0, weight: .regular),
            textAlignment: .left,
            tintColor: Palette.createGroupCursorColor
        ))
        autocapitalizationType = .words
    }
}

// MARK : View controller presentation

public extension CreateGroupSearchPanelViewController
{
    public override var shouldAutorotate: Bool {
        return false
    }
    
    public override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
        return .portrait
    }
    
    public override var prefersStatusBarHidden: Bool {
        return true
    }
}
