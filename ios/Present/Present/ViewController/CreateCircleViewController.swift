//
//  CreateCircleViewController.swift
//  Present
//
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import PresentProtos
import RxSwift
import Then

public protocol EditCircleDelegate : class {
    func groupWasEdited(group: Group)
}

public class CreateCircleViewController: PresentViewController
{
    public weak var editCircleDelegate: EditCircleDelegate?
    
    // All content is within the scrollview to allow us to acccomodate the keyboard
    @IBOutlet weak var contentScrollView: UIScrollView!

    @IBOutlet weak var navBar: NavBar! {
        didSet {
            navBar.do {
                $0.title.text = "Create Circle"
                $0.cancelButton.isShown = true
                $0.cancelButton.addTarget { [weak self] _ in self?.screenPresenter.goBack() }
                $0.saveButton.isShown = true
                $0.saveButton.addTarget { [weak self] _ in self?.createOrUpdateCircle() }
                $0.saveButton.setTitle("Done", for: .normal)
            }
        }
    }
    
    // MARK: Cover Photo
    
    @IBOutlet weak var coverPhotoButton: ThemeableButton! {
        didSet {
            coverPhotoButton.do {
                $0.backgroundColor = Palette.avatarLoadingColor
                $0.addTarget { [weak self] _ in self?.choosePhoto() }
                $0.roundCornersToHeight()
                $0.clipsToBounds = true
            }
        }
    }
    
    private var selectedPhoto: ImageSelectionState = .none {
        didSet {
            updateSelectedPhoto()
            updateSaveButton()
        }
    }
    private var selectedPhotoChanged: Bool {
        switch selectedPhoto {
        case .none, .previouslySelected:
            return false
        case .selected:
            return true
        }
    }
    
    // MARK: Title
    
    @IBOutlet weak var titleTextField: ThemeableTextField! {
        didSet {
            titleTextField.do {
                $0.autocapitalizationType = .words
                $0.keyboardType = .asciiCapable
                $0.autocorrectionType = .no
                $0.returnKeyType = .done
                $0.delegate = self
            }
        }
    }
    private var titleValid: Bool {
        return !titleTextField.text.isEmptyOrWhitespace
    }
    private var titleChanged: Bool {
        return titleTextField.text != group?.title
    }
    
    // MARK: Location

    @IBOutlet weak var locationTextField: ThemeableTextField!
    @IBOutlet weak var locationButton: ThemeableButton! {
        didSet {
            locationButton.addTarget { [weak self] _ in
                guard let sself = self else { return }
                sself.screenPresenter.presentCreateGroupLocation(with: sself, allowRetreat: true)
            }
        }
    }
    var selectedLocation: SelectedLocation? {
        didSet {
            if let selectedLocation = selectedLocation {
                locationTextField.text = selectedLocation.locationName
            } else {
                locationTextField.text = nil
            }
            updateSaveButton()
        }
    }
    private var locationChanged: Bool {
        return selectedLocation?.locationName != group?.locationName
    }
    private var locationValid: Bool {
        return selectedLocation != nil
    }
    
    // MARK: Description
    
    @IBOutlet weak var descriptionTextView: ThemeableTextView! {
        didSet {
            descriptionTextView.do {
                $0.isScrollEnabled = false
                $0.dataDetectorTypes = .all
                $0.autocapitalizationType = .sentences
                $0.returnKeyType = .default
                $0.delegate = self
            }
            // TODO: Move this into ThemeableTextView (using NSNotificationCenter observer)
            // Create the placeholder text
            let placeholderText  = UITextView().then {
                $0.text = "Describe Your Circle" // Localize
                $0.font = descriptionTextView.font
                $0.textColor = UIColor(hex: 0xBDBDBD)
                $0.backgroundColor = .clear
                $0.isUserInteractionEnabled = false
                descriptionTextView.superview?.addSubview($0) // doesn't work as a subview?
                $0.constrainToFill(view: descriptionTextView)
            }
            descriptionTextView.rx.text.bind { text in
                placeholderText.alpha = text.isEmpty ? 1.0 : 0.0
                }.disposed(by: disposal)
        }
    }
    private var descriptionChanged: Bool {
        return descriptionTextView.text != group?.description
    }

    // MARK: Pre-Approval
    
    /// The currently user choice for pre-approval option
    @IBOutlet weak var preApproveText: UILabel! {
        didSet {
            preApproveText.text = nil
        }
    }
    
    @IBOutlet weak var preApproveSubtitle: UILabel!
    
    @IBOutlet weak var preApproveButton: UIButton! {
        didSet {
            preApproveButton.rx.tap.bind { [weak self] in
                guard let sself = self else { return }
                PreApprovalViewController().do {
                    $0.womenOnly = sself.womenOnlySwitch.isOn
                    $0.selectedPreApproval = sself.selectedPreApproval
                    $0.completion = { preApproval in
                        sself.selectedPreApproval = preApproval
                    }
                    sself.screenPresenter.present(screen: $0, allowRetreat: true, with: .slideInHorizontal)
                }
            }.disposed(by: disposal)
        }
    }
    
    private func updatePreapproval() {
        preApproveSubtitle.isHidden = selectedPreApproval == Present.GroupMemberPreapproval.anyone
        preApproveSubtitle.text = PrivacyText.getPreApprovalDescription(forPreApproval: selectedPreApproval, womenOnly: womenOnlySwitch.isOn)
        preApproveText.text = PrivacyText.getPreApprovalTitle(forPreApproval: selectedPreApproval, womenOnly: womenOnlySwitch.isOn)
    }
    
    var selectedPreApproval: Present.GroupMemberPreapproval? {
        didSet {
            updatePreapproval()
            updateSaveButton()
        }
    }
    
    private var preApprovalChanged: Bool {
        return selectedPreApproval != group?.preapproval
    }
    
    // MARK: Switches
    
    @IBOutlet weak var discoverableSwitch: UISwitch!
    
    private var discoverableChanged: Bool {
        return discoverableSwitch.isOn != group?.discoverable
    }
    
    @IBOutlet weak var discoverableSubtitle: UILabel!

    // TODO: Hide women only unless woman
    @IBOutlet weak var womenOnlyView: UIView! {
        didSet {
            userManager.whenUserProfileAvailable { profile in
                self.womenOnlyView.isShown = profile.gender == .woman
            }
        }
    }
    
    @IBOutlet weak var womenOnlySwitch: UISwitch!

    // MARK: Schedule

    @IBOutlet weak var startsField: UITextField! {
        didSet {
            datePicker().do {
                $0.rx.date.skip(1).bind { [weak self] date in
                    self?.selectedStartDate = date
                    }.disposed(by: self.disposal)
                startsField.inputView = $0
            }
        }
    }
    
    private var startsChanged: Bool {
        return selectedStartDate != group?.schedule?.startDate
    }

    @IBOutlet weak var startsLabel: UILabel!
    
    var selectedStartDate: Date? {
        didSet {
            if let date = selectedStartDate {
                startsLabel.text = formatDate(date: date)
                stopsFieldDatePicker.minimumDate = date
                if stopsFieldDatePicker.date < date {
                    let suggestedDate = date.addingTimeInterval(.hours(1))
                    stopsFieldDatePicker.date = suggestedDate
                    selectedStopDate = suggestedDate
                    if selectedStopDate != nil {
                        selectedStopDate = stopsFieldDatePicker.date
                    }
                }
                stopsField.isEnabled = true
                stopsContainer.isHidden = false
            } else {
                startsLabel.text = nil
                selectedStopDate = nil
                stopsField.isEnabled = false
                stopsContainer.isHidden = true
            }
            updateSaveButton()
        }
    }
    
    @IBOutlet weak var stopsContainer: UIView! {
        didSet {
            stopsContainer.isHidden = true
        }
    }
    
    @IBOutlet weak var stopsField: UITextField! {
        didSet {
            stopsField.isEnabled = false
            stopsFieldDatePicker = datePicker().then {
                $0.rx.date.skip(1).bind { [weak self] date in
                    self?.selectedStopDate = date
                    }.disposed(by: self.disposal)
                stopsField.inputView = $0
            }
        }
    }
    var stopsFieldDatePicker: UIDatePicker!

    @IBOutlet weak var stopsLabel: UILabel!
    
    var selectedStopDate: Date? {
        didSet {
            if let date = selectedStopDate {
                stopsLabel.text = formatDate(date: date)
            } else {
                stopsLabel.text = nil
            }
            updateSaveButton()
        }
    }
    
    private var stopsChanged: Bool {
        return selectedStopDate != group?.schedule?.endDate
    }
    
    private let categoriesViewController = CategoriesViewController()
    
    private var categoriesChanged: Bool {
        return categoriesViewController.selectedCategories !=
            Set(GroupCategories.from(strings: group?.categories ?? []))
    }
    
    @IBOutlet weak var categoriesView: UIView!

    @IBOutlet weak var categoriesSubtitle: UITextField!
    
    // MARK: State

    private var group: Group?
    
    enum Mode { case create, edit }
    private var mode: Mode {
        return group == nil ? .create : .edit
    }
    
    // MARK: ViewController
    
    override public func viewDidLoad()
    {
        super.viewDidLoad()
        
        addKeyboardShowHideObservers()
        setDefaultValues()

        Observable.combineLatest(
            womenOnlySwitch.rx.isOn,
            titleTextField.rx.text,
            descriptionTextView.rx.text,
            discoverableSwitch.rx.isOn)
            .do(onNext: { womenOnly, title, description, discoverability in
                self.updateSaveButton()
                self.discoverableSubtitle.text = PrivacyText.getDiscoverabilityAndGenderTextFor(discoverable: discoverability, womenOnly: womenOnly)
                self.updatePreapproval()
            }).disposed(by: disposal)
        
        // Tap on view to dismiss
        UITapGestureRecognizer().do {
            view.addGestureRecognizer($0)
            $0.rx.event.bind { [weak self] _ in
                self?.view.endEditing(false)
                }.disposed(by: disposal)
        }
        
        installChild(viewController: categoriesViewController, in: categoriesView) {
            $0.constrainToFillSuperview()
        }
        categoriesViewController.delegate = self
        categoriesViewController.configure(categories: GroupCategory.all, picked: false)
        categoriesView.anchor(heightConstant: 145)
        categoriesViewController.rx.selectedCategories.onNext { [weak self] _ in
            self?.updateSaveButton()
        }.disposed(by: disposal)
    }
    
    private func setDefaultValues() {
        titleTextField.text = nil
        selectedLocation = nil
        descriptionTextView.text = nil
        selectedPreApproval = .anyone
        selectedStartDate = nil
        selectedStopDate = nil
        discoverableSwitch.isOn = true
        womenOnlySwitch.isOn = false
    }
    
    // MARK: ProfileViewControllerConfig
    
    public func configure(group: Group) { apply(group: group) }
    
    public func apply(group: Group)
    {
        loadViewIfNeeded()
        self.group = group

        configureCoverPhoto(group: group)

        if let locationName = group.locationName {
            selectedLocation = SelectedLocation(coordinate: group.location, locationName: locationName, locationSubtitle: "")
        }

        self.titleTextField.text = group.title
        self.descriptionTextView.text = group.description
        self.selectedPreApproval = group.preapproval
        self.womenOnlySwitch.do {
            $0.isOn = PresentSpace.isWomenOnly(space: group.space)
            $0.isEnabled = false // Can't change this after create
        }
        self.discoverableSwitch.isOn = group.discoverable
        self.navBar.title.text = "Edit Circle"
        self.navBar.saveButton.setTitle("Save", for: .normal)

        let categories = GroupCategories.from(strings: group.categories)
        setSelectedCategories(categories: categories)
    }
    
    // MARK: Helpers for external configuration
    
    public func setCircleTitle(_ text: String) {
        titleTextField.text = text
    }
    public func setCircleDescription(_ text: String) {
        descriptionTextView.text = text
    }
    
    /// Set the pre-selected or currently selected circle categories
    public func setCircleCategories(categories: Set<String>) {
        setSelectedCategories(categories: Array(categories))
    }
    
    // MARK: Categories
    
    public func setSelectedCategories(categories: [String]) {
        setSelectedCategories(categories: GroupCategories.from(strings: categories))
    }
    public func setSelectedCategories(categories: [GroupCategory]) {
        categoriesViewController.selectedCategories = Set(categories)
    }
    
    // MARK: Save
    
    // enable the save button on change
    private func updateSaveButton() {
        navBar.saveButton.isEnabled =
            (titleChanged || descriptionChanged || locationChanged || preApprovalChanged || discoverableChanged || startsChanged || stopsChanged || selectedPhotoChanged || categoriesChanged)
            && titleValid && locationValid
    }
    
    // create circle
    private func createOrUpdateCircle()
    {
        navBar.saveButton.isEnabled = false
        view.endEditing(false)
        startActivity()
        
        // TODO: if create mode save user preferences for: discoverable,women only, preapprove
        
        switch mode {
            case .create:
                logEvent(.circle_create_commit)
            case .edit:
                logEvent(.circle_edit_commit)
        }
        
        guard let topic = titleTextField.text?.trimmingWhitespace() else {
            logError("Circle Create/Edit: no topic selected")
            return
        }
        guard let location = selectedLocation else {
            logError("Circle Create/Edit: Incomplete location")
            return
        }
        
        let description = descriptionTextView.text != nil ? descriptionTextView.text.trimmingWhitespace() : nil
        
        let groupToken : PresentUUID = {
            if let group = group {
                return group.groupToken
            } else {
                return PresentUUID()
            }
        }()
        
        userManager.groupManager.createOrUpdateGroup(
            withToken: groupToken,
            topicName: topic,
            location: location.coordinate,
            locationName: location.locationName,
            radius: 0.0,
            coverPhoto: selectedPhoto.toImage(),
            description: description,
            categories: categoriesViewController.selectedCategories.isEmpty ? nil : Array(categoriesViewController.selectedCategories),
            progressListener: nil,
            womenOnly: womenOnlySwitch.isOn,
            discoverable: discoverableSwitch.isOn,
            preapproval: selectedPreApproval,
            startDate: selectedStartDate,
            stopDate: selectedStopDate
            
        ) { response in
            self.endActivity()
            switch response {
                case let .error(errorText):
                    logError("Create group error: \(errorText)")
                    UIAlertController.showAcknowledgeAlert(
                        fromViewController: self,
                        title: "Error Creating Circle",
                        message: "Unable to save your Circle")
                case let .success(newGroup):
                    if self.mode == .edit {
                        self.editCircleDelegate?.groupWasEdited(group: newGroup)
                    }
                    self.screenPresenter.goBack()
                    // The transition manager will queue this up for us.
                    if self.mode == .create {
                        self.screenPresenter.pushGroup(for: newGroup, showChat: true)
                    }
            }
        }
    }
    
    // MARK: Formatting
    
    private func datePicker() -> UIDatePicker {
        return UIDatePicker().then {
            $0.date = Date()
            $0.minimumDate = Date()
            $0.datePickerMode = .dateAndTime
            $0.backgroundColor = UIColor(hex: 0xD3D5DA)
        }
    }
    
    private func formatDate(date: Date) -> String {
        return Date.shortMonthFullDayYearDateFormatter.string(from: date)
            + "    " + Date.hourMinuteTimeFormatter.string(from: date)
        
    }

}


// MARK: - Photo selection

extension CreateCircleViewController
{
    private enum ImageSelectionState {
        case none
        case previouslySelected(uuid:PresentUUID, url:URL)
        case selected(UIImage)
        
        func toImage() -> ImageState {
            switch self {
            case .none:
                return .none
            case let .selected(image):
                return .new(image)
            case let .previouslySelected(uuid, _):
                return .existing(uuid)
            }
        }
    }
    
    private func configureCoverPhoto(group:Group)
    {
        if coverPhotoButton.image(for: .normal) == nil {
            coverPhotoButton.setImage(MapSnapshotManager.loadingMapImage, for: .normal)
        }
        
        if let uuid = group.coverPhotoToken, let url = group.coverPhoto {
            selectedPhoto = .previouslySelected(uuid: uuid, url: url)
        } else {
            // Generate the map-type background image.
            let mapLocation = group.location
            let mapSize = coverPhotoButton.bounds.size
            MapSnapshotManager.shared.mapSnapshot(at: mapLocation, size: mapSize, name: group.title) { image in
                self.coverPhotoButton.setImage(image, for: .normal)
            }
        }
    }
    
    public func updateSelectedPhoto()
    {
        switch selectedPhoto {
        case .none:
            coverPhotoButton.alpha = 1.0
            coverPhotoButton.setImage(#imageLiteral(resourceName: "camera"), for: .normal)
        case let .previouslySelected(_,url):
            coverPhotoButton.alpha = 0.85
            ImageManager.shared.getImage(atURL: url) { [weak self] image in
                self?.coverPhotoButton.setImage(image, for: .normal)
            }
        case let .selected(image):
            coverPhotoButton.alpha = 0.85
            coverPhotoButton.setImage(image, for: .normal)
        }
    }
    
    public func selected(image: UIImage?) {
        logEvent(type: .tap, "Selected Image For Circle")
        if let image = image {
            selectedPhoto = .selected(image)
        } else {
            selectedPhoto = .none
        }
    }
    
    func choosePhoto()
    {
        logEvent(type: .tap, "Add Photo To Circle")
        
        let allowRemoval: Bool = {
            switch self.selectedPhoto {
            case .none:
                return false
            case .selected, .previouslySelected:
                return true
            }
        }()
        
        PhotoPicker.showPhotoPrompt(from: self, allowRemoval: allowRemoval, cropMode: .square)
        { [weak self] (result) in
            guard let strongSelf = self else { return }
            
            switch result {
            case let .chosePhoto(image):
                strongSelf.selected(image: image)
                
            case .choseToRemovePhoto:
                strongSelf.selected(image: nil)
                
            case .cancelled:
                break
            }
        }
    }
}

extension CreateCircleViewController: CreateGroupLocationDelegate
{
    public func setGroupLocation(location: SelectedLocation) {
        self.selectedLocation = location
    }
}

// MARK: UITextFieldDelegate (title fields)

extension CreateCircleViewController: UITextFieldDelegate
{
    public func textField(_ textField: UITextField, shouldChangeCharactersIn range: NSRange, replacementString string: String) -> Bool
    {
        // Prevent the user from deleting a title fully
        guard let editedString = (textField.text as NSString?)?.replacingCharacters(in: range, with: string),
            FullName(fullName: editedString) != nil || editedString.isEmpty else {
                textField.shake()
                return false
        }
        
        return true
    }
    
    public func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        textField.resignFirstResponder()
        return true
    }
    
    public func textFieldDidEndEditing(_ textField: UITextField) {
        textField.text = textField.text?.trimmingWhitespace()
    }
}

// MARK: UITextViewDelegate (description field)

extension CreateCircleViewController: UITextViewDelegate
{
    public func textViewShouldBeginEditing(_ textView: UITextView) -> Bool {
        return true
    }
    
    public func textView(_ textView: UITextView, shouldChangeTextIn range: NSRange, replacementText text: String) -> Bool
    {
        let editedString = (textView.text as NSString).replacingCharacters(in: range, with: text)
        guard editedString.count <= 500 else {
            textView.shake()
            return false
        }
        
        return true
    }
    
    public func textViewDidChange(_ textView: UITextView) {
        updateSaveButton()
        view.setNeedsLayout()
    }
    
    public func textViewShouldEndEditing(_ textView: UITextView) -> Bool {
        if textView == descriptionTextView && textView.text.isEmpty {
            textView.text = group?.title
        }
        
        return true
    }
    
    public func textViewDidEndEditing(_ textView: UITextView)
    {
        if textView == descriptionTextView {
            guard !textView.text.isEmpty else {
                textView.text = userManager.personalDescription
                return
            }
            textView.text = textView.text.trimmingWhitespace()
            view.setNeedsLayout()
        }
        
    }
}

// MARK: KeyboardWillShowHideListener

extension CreateCircleViewController: KeyboardWillShowHideListener
{
    public func keyboardWillShow(with animation: KeyboardAnimation)
    {
        main {
            // Add content insets to teh scroll view to make space for the keyboard
            let insets = UIEdgeInsets(top: 0, left: 0, bottom: animation.endFrame.height, right: 0)
            self.contentScrollView.contentInset = insets
            self.contentScrollView.scrollIndicatorInsets = insets
            
            if self.descriptionTextView.isFirstResponder {
                self.contentScrollView.scrollRectToVisible(
                    self.descriptionTextView.convert(self.descriptionTextView.frame, to: self.contentScrollView), animated: true)
            }
        }
    }
    
    public func keyboardWillHide(with animation: KeyboardAnimation) {
        main {
            self.contentScrollView.contentInset = UIEdgeInsets.zero
            self.contentScrollView.scrollIndicatorInsets = UIEdgeInsets.zero
        }
    }
}

public extension CreateCircleViewController
{
    override var prefersStatusBarHidden: Bool {
        return true
    }
}

public struct PrivacyText
{
    public static func getPreApprovalTitle(forPreApproval preApproval: Present.GroupMemberPreapproval?, womenOnly: Bool) -> String?
    {
        guard let preApproval = preApproval else { return nil }
        switch preApproval {
            case .unknownGroupMemberPreapproval:
                return nil
            case .anyone:
                if (womenOnly) {
                    return "Women"
                } else {
                    return "Anyone"
                }
            case .friendsOfMembers:
                return "Friends of Members"
            case .friends:
                return "Friends"
            case .inviteOnly:
                return "Invite Only"
        }
    }
    
    public static func getPreApprovalDescription(forPreApproval preApproval: Present.GroupMemberPreapproval?,
                                                 womenOnly: Bool) -> String?
    {
        guard let preApproval = preApproval else { return nil }
        switch preApproval {
            case .unknownGroupMemberPreapproval:
                return nil
            case .anyone:
                if (womenOnly) {
                    return "Only women can join."
                } else {
                    return "Anyone can join."
            }
            case .friendsOfMembers:
                if (womenOnly) {
                    return "Friends of members can join. Other women must be added or approved."
                } else {
                    return "Friends of members can join. Others must be added or approved."
                }
            case .friends:
                if (womenOnly) {
                    return "Friends of the owner can join. Other women must be added or approved."
                } else {
                    return "Friends of the owner can join. Others must be added or approved."
                }
            case .inviteOnly:
                if (womenOnly) {
                    return "Women must be added or approved."
                } else {
                    return "Members must be added or approved."
                }
            }
    }
    
    public static func getDiscoverabilityAndGenderTextFor(discoverable: Bool, womenOnly: Bool) -> String
    {
        if discoverable {
            if womenOnly {
                return "Women nearby can find this circle."
            } else {
                return "Anyone nearby can find this circle."
            }
        } else {
            return "Hidden from search."
        }
    }

}

extension CreateCircleViewController : CategoriesViewControllerDelegate {
    public func shouldSelect(category: GroupCategory)->Bool
    {
        if categoriesViewController.selectedCategories.count < 3 {
            return true
        } else {
            categoriesSubtitle.pulse()
            return false
        }
    }
}

