
import Foundation
import MapKit
import FBAnnotationClustering

public protocol GroupMapViewDelegate : class {
    func didSelect(group: Group)
}

/**
    Live group map view.
 */
public final class GroupMapViewController: UIViewController, UIGestureRecognizerDelegate
{
    // MARK: Services
    
    weak var delegate: GroupMapViewDelegate?
    var groupManager: GroupManager?
    
    let clusteringManager = FBClusteringManager()
    
    // MARK: Map state
    
    let minRegionWidth = 1600.0 /*meters*/
    static let fakeGroupRadius : Double = 100
    
    // relates size of group to map size for case where we center on group
    let groupMapWidthFactor = 5.0
    
    var region : MKCoordinateRegion!
    
    var initialRegionSet = false
    
    var groups : [Group]?
    var selectedGroup : Group? // If set on entry the map will be centered on this group
    
    var count : Int = 0 // Testing
    
    // MARK: UI Elements
    
    @IBOutlet var mapView: MKMapView!
    
    // MARK: UIViewController
    
    override public func viewDidLoad()
    {
        if let group = selectedGroup {
            setRegionForGroup( group: group )
        }
        
        initMapView()
    }
    
    public func configure(with groups: [Group] )
    {
        self.groups = groups
        
        mapView.removeAnnotations(mapView.annotations)
        clusteringManager.removeAnnotations(mapView.annotations)
        
        for group in groups {
            self.addPin( forGroup: group )
        }
        
        // Kick off clustering
        clusteringManager.setAnnotations(mapView.annotations.filter{ $0 is GroupAnnotation })
        
        updateRegion()
    }
    
    // Called when groups or user location are first available to set the initial region and
    // thereafter when groups are updated (e.g. due to category selection).
    // This method coordkinates waiting for both the user location and groups to be available.
    func updateRegion()
    {
        // Dispatch to main to synchronize on the initial region set flag.
        DispatchQueue.main.async {
            // If the initial region has already been shown we update based on the groups.
            if self.initialRegionSet {
                if self.groups != nil {
                    self.setRegionForGroups()
                }
            } else {
                // Else we wait for location and groups to be available for the initial user region.
                if self.mapView.userLocation.location != nil && self.groups != nil {
                    self.showInitialUserLocation()
                }
            }
        }
    }
    
    // Called from updateRegion()
    // If the minimum required count of annotations is not shown find the nearest (min count) to the current map coordinates and expand the map to include them.
    func setRegionForGroups()
    {
        self.mapView(self.mapView, regionDidChangeAnimated: false)
        
        let allAnnotations = self.clusteringManager.allAnnotations() ?? []
        let _visibleAnnotations = self.visibleAnnotations()
        
        let minVisibleAnnotations = 10
        if _visibleAnnotations.count < minVisibleAnnotations // not enough shown
            && _visibleAnnotations.count < allAnnotations.count // more available to show
        {
            let nearestCount = 10
            _ = self.showNearestAnnotations(location: self.mapView.centerCoordinate, count: nearestCount)
        } else {
            // already showing at least min visible annotations
        }
    }
    
    // Get the flat list of annotations in the visible area (including those contained inside clusters)
    func visibleAnnotations() -> [MKAnnotation] {
        return mapView.visibleAnnotations()
            .filter { $0 is GroupAnnotation || $0 is ClusterAnnotation }
            .flatMap { annotation -> [MKAnnotation] in
            if let clusterAnnotation = annotation as? ClusterAnnotation {
                return clusterAnnotation.annotations.compactMap{ $0 as? MKAnnotation }
            } else {
                return [annotation]
            }
        }
    }
    
    // If we have a group center the map on its location
    func setRegionForGroup( group : Group ) {
        let centerCoordinate = group.location
        let size = GroupMapViewController.fakeGroupRadius * groupMapWidthFactor
        self.region = MKCoordinateRegionMakeWithDistance( centerCoordinate, size, size)
        mapView.region = self.region
    }
    
    //func tapOnMap(gestureRecognizer: UIPanGestureRecognizer) { }
    
    fileprivate class func setShadow(view:UIView) {
        view.setShadow(shadowXOffset: 0, shadowYOffset: 4.0, shadowOpacity: 0.13, shadowRadius: 4.0, setShadowPath: true)
    }
}

// MARK: - MKMapViewDelegate

extension GroupMapViewController: MKMapViewDelegate
{
    // Indicate whether clusters can be expanded by further zooming in on the map.
    var allowExpandClusters : Bool
    {
        let minMapScaleForClustering = 200.0 // meters
        let metersPerDegreeLatitude = 111000.0 // aproximate for 38 deg N.
        let mapScale = mapView.region.span.latitudeDelta * metersPerDegreeLatitude
        return mapScale > minMapScaleForClustering
    }

    public func mapView(_ mapView: MKMapView, regionDidChangeAnimated animated: Bool)
    {
        let scale = Double(mapView.bounds.size.width) / mapView.visibleMapRect.size.width
        let expandable = allowExpandClusters
        if let fbAnnotations = self.clusteringManager.clusteredAnnotations(within: mapView.visibleMapRect, withZoomScale:scale)
        {
            // Create our own version of the cluster annotations so that we can add the expandable property
            let annotations = fbAnnotations.map { annotation -> (Any) in
                if let clusterAnnotation = annotation as? FBAnnotationCluster {
                    return ClusterAnnotation(clusterAnnotation, expandable: expandable)
                } else {
                    return annotation
                }
            }
            self.clusteringManager.displayAnnotations(annotations, on:mapView)
        }
        
        // Hack, make sure user location annotation is drawn above others
        if let userLocationPin = mapView.annotations.first(where: { $0 is MKUserLocation }),
           let view = self.mapView(mapView, viewFor: userLocationPin) {
            view.superview?.bringSubview(toFront: view)
        }
        
        //print("visible annotations = ", visibleAnnotations().count) // lags?
    }
    
    // Determine the view rendered for a given map annotation (point based pin/marker)
    public func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView?
    {
        if annotation is MKUserLocation {
            let view = MKAnnotationView(annotation: annotation, reuseIdentifier: "userlocation")
            let borderWidth = 8.0 / 3.0
            let centerSize = 46.0 / 3.0
            let size = centerSize + borderWidth*2.0
            view.backgroundColor = UIColor(red: 5, green: 124, blue: 255)
            view.layer.borderWidth = CGFloat(borderWidth)
            view.layer.borderColor = UIColor.white.cgColor
            view.layer.cornerRadius = CGFloat(size)/2.0
            view.frame = CGRect(x:0, y:0, width: size, height: size)
            view.canShowCallout = false
            view.isUserInteractionEnabled = false
            GroupMapViewController.setShadow(view:view)
            return view
        }
        
        if annotation is MKPointAnnotation
        {
            let reuseId = "purplePin"
            let annotationView: MKAnnotationView
            if let reusedView = mapView.dequeueReusableAnnotationView(withIdentifier: reuseId) {
                annotationView = reusedView
            } else {
                annotationView = MKAnnotationView(annotation: annotation, reuseIdentifier: reuseId)
                annotationView.image = #imageLiteral(resourceName: "locationPinPurple.pdf")
                annotationView.canShowCallout = true
            }
            
            // Init the (potentially reused) view
            // TODO: make a class for this and reuse the participants view
            //setShadow(view: view) // Doesn't conform to the image
            if let groupAnnotation = annotation as? GroupAnnotation {
                annotationView.rightCalloutAccessoryView = createCalloutParticipantsView(group: groupAnnotation.group)
            }
            
            return annotationView
        }
        
        if let cluster = annotation as? ClusterAnnotation
        {
            let count = cluster.annotations.count
            let reuseId = "clusterPin:\(cluster.expandable)"
            let annotationView: ClusterAnnotationView
            
            if let reusedView = mapView.dequeueReusableAnnotationView(withIdentifier: reuseId) as? ClusterAnnotationView {
                annotationView = reusedView
            } else {
                annotationView = ClusterAnnotationView(clusterAnnotation: cluster, reuseIdentifier: reuseId)
                annotationView.canShowCallout = !cluster.expandable
            }
            
            // Init the (potentially reused) view
            annotationView.label.text = "\(count)"
            
            // Make sure that the underlying cluster will be selectable, even if show callout is false (must have a title)
            cluster.title = " "
            
            return annotationView
        }
        
        return nil
    }
    
    // An annotation was selected. If the annotation has a callout it will be shown.
    public func mapView(_ mapView: MKMapView, didSelect view: MKAnnotationView)
    {
        // A group was tapped and the callout was shown. Prepare to detect a subsequent tap and show the group.
        if view.annotation is GroupAnnotation, view.gestureRecognizers == nil {
            let gesture = UITapGestureRecognizer(target: self, action: #selector(calloutTapped(sender:)))
            view.addGestureRecognizer(gesture)
        }
        
        // A cluster was tapped. Dive into the group of annotations or cycle through them.
        if let clusterView = view as? ClusterAnnotationView,
            let clusterAnnotation = clusterView.annotation as? ClusterAnnotation
        {
            if clusterAnnotation.expandable {
                mapView.showAnnotations(clusterAnnotation.annotations as! [MKAnnotation], animated: true)
            } else {
                // Cycle through the annotations in the cluster, choosing one for the callout each time.
                if view.gestureRecognizers == nil {
                    // Prepare for subsequent taps
                    let gesture = UITapGestureRecognizer(target: self, action: #selector(clusterTapped(sender:)))
                    view.addGestureRecognizer(gesture)
                    showClusterAnnotation(forClusterView: clusterView)
                }
            }
        }
    }
    
    // Called when the user location is known
    public func mapView(_ mapView: MKMapView, didUpdate userLocation: MKUserLocation)
    {
        // Dispatch to main to synchronize on initial region set flag
        DispatchQueue.main.async {
            guard !self.initialRegionSet else { return } // Only initialize based on user location once
            self.updateRegion()
        }
    }
    
    // Called from updateRegion()
    func showInitialUserLocation()
    {
        guard let location = mapView.userLocation.location else {
            logError("Call to show user location without location available")
            return
        }
        // Find the nearest N groups and show them
        let nearestCount = 50
        _ = self.showNearestAnnotations(location: location.coordinate, count: nearestCount, minRegionWidth: self.minRegionWidth)
        
        self.initialRegionSet = true
    }
    
    public func mapView(_ mapView: MKMapView, didAdd views: [MKAnnotationView])
    {
        // Hack, make sure user location annotation is drawn above others
        for view in views {
            if view.annotation is MKUserLocation {
                view.superview?.bringSubview(toFront: view)
            } else {
                view.superview?.sendSubview(toBack: view)
            }
        }
    }
    
    // Find the nearest N groups to the specified location and show them.
    // Optionally specify a minimum region width in meters.
    // Return the region shown, if any.
    func showNearestAnnotations(location: CLLocationCoordinate2D, count : Int, minRegionWidth : Double? = nil) -> MKCoordinateRegion?
    {
        guard let annotations = clusteringManager.allAnnotations() else { return nil }
        let nearest = nearestAnnotations(annotations: annotations, location: location, count: count)
        //print("nearest \(nearest.count) of \(annotations.count-1) annotations")
        guard let region = boundingRegionForAnnotations(forAnnotations: nearest, minWidthMeters: minRegionWidth) else { return nil }
        mapView.setRegion(region, animated: true)
        return region
    }
    
    func nearestAnnotations(annotations: [Any], location: CLLocationCoordinate2D, count : Int) -> [MKAnnotation] {
        let nearest = annotations.compactMap{ $0 as? GroupAnnotation }.sorted { (ann1, ann2) in
            return ann1.group.location.distanceTo(location) <
                ann2.group.location.distanceTo(location)
            }.prefix(count)
        return Array(nearest)
    }
    
    /// @param padding is a fraction in terms of the map width 
    func boundingRegionForAnnotations(
        forAnnotations annotations: [MKAnnotation],
        minWidthMeters: Double? = nil,
        maxWidthMeters: Double = 1000*1000, 
        paddingFraction: Double = 0.15
        ) -> MKCoordinateRegion?
    {
        guard !annotations.isEmpty else { return nil }
        
        // Find the bounding box region for the annotations
        var topLeftCoord = CLLocationCoordinate2D()
        topLeftCoord.latitude = -90
        topLeftCoord.longitude = 180
        var bottomRightCoord = CLLocationCoordinate2D()
        bottomRightCoord.latitude = 90
        bottomRightCoord.longitude = -180
        for annotation in annotations {
            topLeftCoord.longitude = fmin(topLeftCoord.longitude, annotation.coordinate.longitude)
            topLeftCoord.latitude = fmax(topLeftCoord.latitude, annotation.coordinate.latitude)
            bottomRightCoord.longitude = fmax(bottomRightCoord.longitude, annotation.coordinate.longitude)
            bottomRightCoord.latitude = fmin(bottomRightCoord.latitude, annotation.coordinate.latitude)
        }
        
        // Make the region
        var region: MKCoordinateRegion = MKCoordinateRegion()
        region.center.latitude = topLeftCoord.latitude - (topLeftCoord.latitude - bottomRightCoord.latitude) / 2.0
        region.center.longitude = topLeftCoord.longitude + (bottomRightCoord.longitude - topLeftCoord.longitude) / 2.0
        region.span.latitudeDelta = fabs(topLeftCoord.latitude - bottomRightCoord.latitude) 
        region.span.longitudeDelta = fabs(bottomRightCoord.longitude - topLeftCoord.longitude)
        
        // Add padding
        let pad = region.span.longitudeDelta * paddingFraction
        region.span.latitudeDelta = region.span.latitudeDelta + 2.0 * pad
        region.span.longitudeDelta = region.span.longitudeDelta + 2.0 * pad
        
        // Impose size limits
        let topRightCoord = CLLocationCoordinate2D(latitude: topLeftCoord.latitude, longitude: bottomRightCoord.longitude)
        let regionWidth = topLeftCoord.distanceTo(topRightCoord) // m
        
        // Expand to the minimum region width
        if let minWidthMeters = minWidthMeters {
            if regionWidth < minWidthMeters {
                region = MKCoordinateRegionMakeWithDistance(region.center, minWidthMeters, minWidthMeters)
                region = mapView.regionThatFits(region)
            }
        }
        
        // Shrink to max region width
        if regionWidth > maxWidthMeters {
            region = MKCoordinateRegionMakeWithDistance(region.center, maxWidthMeters, maxWidthMeters)
            region = mapView.regionThatFits(region)
        }
        
        return region
    }
    
    //public func mapViewDidFinishRenderingMap(_ mapView: MKMapView, fullyRendered: Bool) { }
    
    // Create the right side view for the group annotation callout
    private func createCalloutParticipantsView(group: Group) -> UIView
    {
        let participantsView = MultiPersonHorizontalStackView()
        
        participantsView.personProfileImagesToDisplay = [#imageLiteral(resourceName: "user-woman-512")]
        ImageManager.shared.getImage(atURL: group.owner.photoURL) { image in
            participantsView.personProfileImagesToDisplay = [image]
        }
        
        if group.joinedCount > 1 {
            participantsView.additionalCountToDisplay = group.joinedCount - 1
        }
        participantsView.sizeToFit()
        
        return participantsView
    }
    
    @objc func calloutTapped(sender:UITapGestureRecognizer) {
        guard let annotation = (sender.view as? MKAnnotationView)?.annotation else { return }
        guard let groupAnnotation = annotation as? GroupAnnotation else { return }
        // If the user tapped the selected annotation push the group
        if mapView.selectedAnnotations.contains(where: { $0 === annotation }) {
            delegate?.didSelect(group: groupAnnotation.group)
        }
    }
    
    @objc func clusterTapped(sender:UITapGestureRecognizer)
    {
        guard let clusterAnnotationView = (sender.view as? ClusterAnnotationView) else { return }
        guard let clusterAnnotation = clusterAnnotationView.annotation as? ClusterAnnotation else { return }
        let label = clusterAnnotationView.label
        // Tap on the pin shows the next group in the callout
        if label.bounds.contains(sender.location(in: label)) {
            // If already selected (callout showing) show the next group
            if clusterAnnotationView.isSelected {
                showNextClusterAnnotation(forClusterView: clusterAnnotationView)
            } else {
                // else show the current (or first) group
                showClusterAnnotation(forClusterView: clusterAnnotationView)
            }
        } else {
            // tap in the callout opens the group
            guard let selectedGroup = clusterAnnotation.selectedGroup else { return }
            delegate?.didSelect(group: selectedGroup.group)
        }
    }

    func showClusterAnnotation(forClusterView clusterView: ClusterAnnotationView) {
        guard let cluster = clusterView.annotation as? ClusterAnnotation else { return }
        guard let groupAnnotation = cluster.selectedGroup else { return }
        cluster.title = groupAnnotation.title
        cluster.subtitle = groupAnnotation.subtitle
        clusterView.rightCalloutAccessoryView = createCalloutParticipantsView(group: groupAnnotation.group)
    }
    
    func showNextClusterAnnotation(forClusterView clusterView: ClusterAnnotationView) {
        guard let cluster = clusterView.annotation as? ClusterAnnotation else { return }
        _ = cluster.nextGroup
        showClusterAnnotation(forClusterView: clusterView)
    }
    
    fileprivate func initMapView()
    {
        mapView.delegate = self
        mapView.showsUserLocation = true
    }

    // Move the map to center the group at a new location
    public func setMapLocation(_ coordinate : CLLocationCoordinate2D) {
        let region = MKCoordinateRegionMakeWithDistance(coordinate, minRegionWidth, minRegionWidth)
        mapView.region = region
        initialRegionSet = true
    }
    
    func addPin(forGroup group: Group) {
        let pin = GroupAnnotation(forGroup: group)
        addPin(annotation: pin, coord: group.location, title: group.title, subtitle: group.locationName ?? "", isSelected: group.groupToken == selectedGroup?.groupToken)
    }
    
    // Add the annotation model.  This will be styled by mapView:viewFor annotation
    func addPin(
        annotation: MKPointAnnotation? = nil,
        coord : CLLocationCoordinate2D, title : String, subtitle : String,
        isSelected: Bool = false
    )
    {
        let pin = annotation ?? MKPointAnnotation()
        pin.coordinate = coord
        pin.title = title
        let subtitleMaxLen = 24
        pin.subtitle = CreateGroupLocationViewController.truncateTitle(subtitle, maxLen: subtitleMaxLen)
        mapView.addAnnotation(pin)
        if isSelected {
            mapView.selectAnnotation(pin, animated: true)
        }
    }
    
    // Return the map coordinate of the user location
    fileprivate func getUserLocation() -> CLLocationCoordinate2D? {
        return mapView.userLocation.location?.coordinate
    }
    
    // MARK: Annotation and annotation view classes
    
    class GroupAnnotation : MKPointAnnotation
    {
        let group: Group
        init(forGroup group: Group) {
            self.group = group
        }
    }
    
    class ClusterAnnotation : FBAnnotationCluster
    {
        var expandable : Bool
        private var selectedChild = 0
        
        var selectedGroup : GroupAnnotation? {
            guard self.annotations.count > 0 else { return nil }
            let index = selectedChild % self.annotations.count
            return self.annotations[index] as? GroupAnnotation
        }
        
        var nextGroup : GroupAnnotation? {
            get {
                selectedChild += 1 // This will start at index 1 but, these aren't ordered anyway.
                return selectedGroup
            }
        }
        
        init(_ fbAnnotationCluster : FBAnnotationCluster, expandable : Bool)
        {
            self.expandable = expandable
            super.init()
            self.coordinate = fbAnnotationCluster.coordinate
            self.title = fbAnnotationCluster.title
            self.subtitle = fbAnnotationCluster.subtitle
            self.annotations = fbAnnotationCluster.annotations
        }
    }
    
    // The view for the cluster annotation.
    class ClusterAnnotationView : MKAnnotationView
    {
        public var label = UILabel()
        
        init(clusterAnnotation: ClusterAnnotation, reuseIdentifier: String)
        {
            super.init(annotation: clusterAnnotation, reuseIdentifier: reuseIdentifier)
            
            if clusterAnnotation.expandable {
                let clusterPinSize = 32
                self.bounds = CGRect(x:0,y:0,width:clusterPinSize,height:clusterPinSize)
                initCircularView(view: self)
                label.frame = self.bounds
                label.font = UIFont.presentFont(ofSize: 14, weight: .bold)
                GroupMapViewController.setShadow(view:self)
            } else {
                //let image = #imageLiteral(resourceName: "emptyPin")
                let image = #imageLiteral(resourceName: "emptyPin15")
                self.image = image
                self.bounds = CGRect(x:0, y:0, width: image.size.width, height: image.size.height)
                let labelYOffset = 7
                label.frame = CGRect(x:0, y:CGFloat(-labelYOffset), width: image.size.width, height: image.size.height)
                label.font = UIFont.presentFont(ofSize: 16, weight: .bold)
            }
            
            label.textColor = .white
            label.textAlignment = .center
            self.addSubview(label)
        }
        
        required init?(coder aDecoder: NSCoder) {
            fatalError("init(coder:) has not been implemented")
        }
        
        func initCircularView(view: UIView)
        {
            view.roundedCorners = view.bounds.height/2.0
            view.layer.borderWidth = 2.0
            view.layer.borderColor = UIColor.white.cgColor
            
            let gradient = CAGradientLayer()
            gradient.frame = view.bounds
            gradient.startPoint = gradient.frame.origin.offsetBy(dx: 0, dy: -gradient.frame.height/2.0)
            gradient.endPoint = gradient.frame.origin.offsetBy(dx: 0, dy: gradient.frame.height/2.0)
            gradient.colors = [/*UIColor(hex: 0x7141db).cgColor,*/ UIColor(hex: 0x3023ae).cgColor, UIColor(hex: 0xc96dd8).cgColor]
            gradient.cornerRadius = view.bounds.height/2.0
            view.layer.insertSublayer(gradient, at: 0)
        }
    }
    
}

// MARK : View controller presentation

extension GroupMapViewController
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


