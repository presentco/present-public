
import CoreLocation
import MapKit
import Relativity


public struct SelectedLocation {
    let coordinate: CLLocationCoordinate2D
    let locationName: String
    let locationSubtitle: String
}
public protocol CreateGroupLocationDelegate : class {
    func setGroupLocation(location:SelectedLocation)
}

public final class CreateGroupLocationViewController: UIViewController, CreateGroupSearchPanelDelegate
{
    // MARK: Private State

    // Services
    
    private let locationProvider: LocationProvider
    private let screenPresenter: ScreenPresenter
    private weak var delegate : CreateGroupLocationDelegate?

    // Map state
    
    fileprivate let defaultRegionWidth = 400.0 /*meters*/
    fileprivate var regionSet: Bool = false
    fileprivate var pinAnnotation: MKPointAnnotation?
    fileprivate var finishedRenderingMap = false

    fileprivate let searchPanelViewController: CreateGroupSearchPanelViewController

    // MARK: UI Elements
    
    @IBOutlet weak var navBar: NavBar! {
        didSet {
            initNavBar()
        }
    }
    @IBOutlet weak var mapContainer: UIView!
    @IBOutlet weak var mapView: MKMapView! {
        didSet {
            initMapView()
        }
    }
    
    // MARK: Initialization
    
    public required init(
        delegate : CreateGroupLocationDelegate, screenPresenter: ScreenPresenter, locationProvider: LocationProvider,
        selectedLocation : SelectedLocation?)
    {
        self.delegate = delegate
        self.screenPresenter = screenPresenter
        self.locationProvider = locationProvider
        self.searchPanelViewController = CreateGroupSearchPanelViewController(locationProvider: locationProvider, selectedLocation: selectedLocation)
        super.init(nibName: nil, bundle: nil)
    }
    
    public required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func initNavBar()
    {
        navBar.do {
            $0.title.text = NSLocalizedString( "CreateGroupLocationViewControllerTitle", tableName: nil, bundle: .main,
                                               value: "Add Location", comment: "Title text for group location screen.")
            $0.isModal = true
            $0.backButton.isShown = true
            $0.backButton.addTarget(self, action: #selector(goBack), for: .touchUpInside)
            $0.saveButton.isShown = true
            $0.saveButton.setTitle("Done", for: .normal)
            $0.saveButton.applyCreateGroupTheme()
            $0.saveButton.addTarget(self, action: #selector(doneButtonPressed), for: .touchUpInside)
        }
    }
    
    private func initSearchPanel() {
        searchPanelViewController.delegate = self
        searchPanelViewController.view.setShadow(shadowXOffset:0, shadowYOffset: 7, shadowOpacity: 0.26, shadowRadius: 9/*18 in zeplin*/, setShadowPath: false)
        installChild(viewController: searchPanelViewController, in: mapContainer)
        searchPanelViewController.becomeFirstResponder()
    }
    
    // MARK: UIViewController
    
    public override func viewDidLoad() {
        super.viewDidLoad()
        
        initSearchPanel()
        updateDoneButtonEnabledState()
    }
    
    public override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
    }
    
    public override func viewDidLayoutSubviews()
    {
        super.viewDidLayoutSubviews()
        
        // search panel controls its own height
        searchPanelViewController.view.bounds.size = CGSize(width: mapContainer.bounds.width, height: 0)
        searchPanelViewController.view.topLeft --> .topLeft
        searchPanelViewController.view.frame = CGRect(x:0, y:0, width:mapContainer.bounds.width, height:0)
    }
    
    // MARK: Button Handling
    
    fileprivate func updateDoneButtonEnabledState() {
        navBar.saveButton.isEnabled = finishedRenderingMap
            && searchPanelViewController.selectedSearchResult != nil
    }
    
    // Cancel the selection and return to the previous screen
    @objc private func goBack() {
        screenPresenter.goBack()
    }
    
    @objc func doneButtonPressed()
    {
        if let result = searchPanelViewController.selectedSearchResult,
            let coordinate = result.locationCoordinate
        {
            let name = result.title
            let subtitle = result.subtitle
            delegate?.setGroupLocation(location: SelectedLocation(coordinate: coordinate, locationName: name, locationSubtitle: subtitle))
        }
        screenPresenter.goBack()
    }
    
    // MARK: CreateGroupSearchPanelDelegate
    
    public func searchTextChanged() {
        // search text no longer in selected state.
        updateDoneButtonEnabledState()
    }
    
    /// The user selected a location from the search panel list.
    public func searchResultSelected(result searchResult: CreateGroupSearchResult)
    {
        // Drop a pin on the user selected location.
        switch searchResult.type {
        case .customLocation:
            guard let location = getUserLocation() else {
                logError("Unable to determine user location.")
                return
            }
            setPin(location, title: searchResult.title, subtitle: searchResult.subtitle)
            
        case .area, .currentLocation, .none, .place, .previouslySelected:
            if let coordinate = searchResult.locationCoordinate {
                setPin(coordinate, title: searchResult.title, subtitle: searchResult.subtitle)
                setMapLocation(coordinate)
            }
        }
        
        searchPanelViewController.setExpanded(false)
        updateDoneButtonEnabledState()
    }
}

// MARK: - MKMapViewDelegate

extension CreateGroupLocationViewController: MKMapViewDelegate
{
    // Receive notification when the user location is available
    public func mapView(_ mapView: MKMapView, didUpdate userLocation: MKUserLocation)
    {
        // Only set region once
        if regionSet { return } 
        setMapToUserLocation()
    }

    public func mapViewDidFinishRenderingMap(_ mapView: MKMapView, fullyRendered: Bool) {
        finishedRenderingMap = true
        updateDoneButtonEnabledState()
    }

    public func mapView(_ mapView: MKMapView, regionDidChangeAnimated animated: Bool) { }
    
    public func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
        return CreateGroupLocationViewController.mapView(mapView, viewFor: annotation)
    }
    
    public class func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView?
    {
        if annotation is MKPointAnnotation
        {
            let reuseId = "purplePin"
            if let reusedView = mapView.dequeueReusableAnnotationView(withIdentifier: reuseId) {
                return reusedView
            }
            let view = MKAnnotationView(annotation: annotation, reuseIdentifier: reuseId)
            view.image = #imageLiteral(resourceName: "locationPinPurple.pdf")
            view.canShowCallout = true
            return view
        }
        return nil
    }
}

// MARK: - Map Functionality

extension CreateGroupLocationViewController
{
    fileprivate func initMapView()
    {
        mapView.delegate = self
        mapView.showsUserLocation = true
        let tapGestureRecognizer = UITapGestureRecognizer(target: self, action: #selector(tapOnMap(_:)))
        tapGestureRecognizer.cancelsTouchesInView = false
        mapView.addGestureRecognizer(tapGestureRecognizer)
    }
    
    public func setMapToUserLocation() {
        // Set the map view region
        guard let location = mapView.userLocation.location else {
            return
        }
        
        setMapLocation(location.coordinate)
    }

    // Move the map to center the bubble at a new location
    public func setMapLocation(_ coordinate : CLLocationCoordinate2D) {
        let region = MKCoordinateRegionMakeWithDistance(coordinate, defaultRegionWidth, defaultRegionWidth)
        mapView.region = region
        regionSet = true
    }
    
    func setPin(_ coord : CLLocationCoordinate2D, title : String, subtitle : String)
    {
        if let pinAnnotation = pinAnnotation {
            mapView.removeAnnotation(pinAnnotation)
        }
        let pin = MKPointAnnotation()
        pin.coordinate = coord
        pin.title = title
        let subtitleMaxLen = 24
        pin.subtitle = CreateGroupLocationViewController.truncateTitle(subtitle, maxLen: subtitleMaxLen)
        mapView.addAnnotation(pin)
        self.pinAnnotation = pin
        mapView.selectAnnotation(pin, animated: true)
    }
    
    // TODO: Push this into the search result by giving the search panel the current location
    // Get the location for the selected search result
    private func getSelectedLocation() -> CLLocationCoordinate2D?
    {
        guard let searchResult = searchPanelViewController.selectedSearchResult else {
            return nil
        }
        
        switch searchResult.type {
        case .none:
            logError("No location information is available from the selection.")
            return nil
            
        case .currentLocation, .customLocation:
            return getUserLocation()
            
        case .area, .place, .previouslySelected:
            return searchResult.locationCoordinate
        }
    }

    // Return the map coordinate of the user location
    fileprivate func getUserLocation() -> CLLocationCoordinate2D? {
        return mapView.userLocation.location?.coordinate
    }
    
    @objc func tapOnMap(_ gestureRecognizer: UIPanGestureRecognizer) {
        searchPanelViewController.setExpanded(false)
    }
    
    func userTouchedMapView() {
        searchPanelViewController.setExpanded(false)
    }
    
    /// Truncate the title string at the first comma and impose a max length
    public class func truncateTitle( _ title : String, maxLen: Int ) -> String
    {
        var truncatedTitle = title
        if let indexOfComma = title.range(of: ",")?.lowerBound {
            truncatedTitle = title.substring(to: indexOfComma)
        }

        return truncatedTitle.truncate(maxLen)
    }
}

// MARK : View controller presentation

public extension CreateGroupLocationViewController
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
