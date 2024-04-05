//
//  CreateGroupViewController.swift
//  Created by Patrick Niemeyer on 12/31/15.
//

import CoreLocation
import MapKit
import Relativity
import PresentProtos
import RxSwift

public protocol CircleDetailViewDelegate: class {
    func report()
    func leave()
    func delete(deletedCompletion: @escaping ()->Void)
}

/// Display circle info including the cover photo, users, description and metadata.
/// This is used in the info view of the group view controller
public final class CircleDetailViewController: PresentViewController,
    UITextFieldDelegate, UITextViewDelegate, CreateGroupLocationDelegate,
    UserScrollerDelegate, UIGestureRecognizerDelegate
{
    // MARK: Properties
    
    public weak var delegate : CircleDetailViewDelegate?

    // Editor state
    
    private var group: Group?
    
    // A configuration block to be run at the appropriate time after loading
    private var configureBlock: ((CircleDetailViewController)->Void)? = nil
    
    // UI State
    
    // TODO: Remove selection functionality
    private var selectedLocation: SelectedLocation? {
        didSet {
            if let selectedLocation = selectedLocation {
                locationNameField.text = selectedLocation.locationName
                locationField.text = selectedLocation.locationSubtitle
            } else {
                locationNameField.text = nil
                locationField.text = nil
            }
            updateLocationText()
            updateLabels()
        }
    }

    // MARK: UI Elements

    // TODO: Remove
    var doneButton: ThemeableButton = ThemeableButton(type: .custom)
    
    @IBOutlet weak var navBar: NavBar! {
        didSet {
            navBar.applyCircleViewStyle()
            navBar.backButton.isShown = true
            navBar.backButton.addTarget { [weak self] _ in self?.goBack() }
            navBar.rightButton.do {
                $0.setTitle("Edit", for: .normal)
                $0.addTarget { [weak self] _ in
                    guard let group = self?.group else { return }
                    self?.screenPresenter.presentEditCircle(forEditing: group) }
                $0.isEnabled = true
                $0.isShown = false
            }
        }
    }

    @IBOutlet weak var locationNameField: UITextField!
    @IBOutlet weak var locationField: UITextField!
    
    @IBOutlet weak var descriptionPlaceholderField: UITextField!
    
    @IBOutlet weak var descriptionField: UITextView! {
        didSet {
            descriptionField.delegate = self
            // Remove all insets and padding
            descriptionField.textContainer.lineFragmentPadding = 0
            descriptionField.textContainerInset = UIEdgeInsets.zero
            descriptionField.dataDetectorTypes = .all
        }
    }
    @IBOutlet weak var descriptionViewHeight: NSLayoutConstraint!
    
    @IBOutlet weak var categoryStackView: UIStackView!
    @IBOutlet weak var categoryField: UITextField! {
        didSet {
            categoryField.isHidden = true
        }
    }
    @IBOutlet weak var scrollView: UIScrollView!
    @IBOutlet weak var locationLabel: UILabel!

    @IBOutlet weak var membersStackView: UIStackView!
    @IBOutlet weak var membersLabelButton: UIButton! {
        didSet {
            membersLabelButton.rx.tap.bind { [weak self] in self?.viewMembers() }.disposed(by: disposal)
        }
    }
    @IBOutlet weak var membersViewAllButton: UIButton! {
        didSet {
            membersViewAllButton.rx.tap.bind { [weak self] in
                self?.viewMembers() }.disposed(by: disposal)
        }
    }

    @IBOutlet weak var membersScroller: UserScroller! {
        didSet {
            membersScroller.delegate = self
            membersScroller.configureBadgePlaceholder(title: "Requests", image: #imageLiteral(resourceName: "RequestsIcon")) {
                [weak self] in self?.viewMembers()
            }
            membersScroller.contentInset = UIEdgeInsets(top: 0, left: 22, bottom: 0, right: 0)
        }
    }

    @IBOutlet weak var descriptionTarget: ThemeableButton! {
        didSet {
            descriptionTarget.hitTargetInset = .zero //  Don't expand the hit area next to our text fields
        }
    }
    @IBOutlet weak var scheduleSection: UIStackView!
    @IBOutlet weak var scheduleDate: UITextField!
    @IBOutlet weak var scheduleTime: UITextField!
    
    @IBOutlet weak var privacyTextField: UILabel!
    
    @IBOutlet weak var leaveButton: ThemeableButton! {
        didSet {
            leaveButton.addTarget { [weak self] _ in self?.delegate?.leave() }
        }
    }
    
    @IBOutlet weak var reportButton: ThemeableButton! {
        didSet {
            reportButton.addTarget { [weak self] _ in self?.delegate?.report() }
        }
    }
    
    @IBOutlet weak var deleteButton: ThemeableButton! {
        didSet {
            deleteButton.addTarget { [weak self] _ in
                self?.delegate?.delete { self?.goBack()
                }
            }
        }
    }
    
    private let categoriesViewController = CategoriesViewController()
    @IBOutlet weak var categoriesView: UIView!

    // MARK: UIViewController

    public override func viewDidLoad()
    {
        super.viewDidLoad()
        
        setMode() // historical, remove
        
        if let configureBlock = configureBlock {
            configureBlock(self)
        }

        if let group = group {
            configure(withGroup: group)
        } else {
            updateLayoutForContent()
        }
        
        addKeyboardShowHideObservers()
        
        installChild(viewController: categoriesViewController, in: categoriesView) {
            $0.constrainToFillSuperview()
        }
        // Height will be adjusted after categories are configured
        categoriesViewHeight = categoriesView.anchor(heightConstant: 8)[0]
    }
    
    var categoriesViewHeight: NSLayoutConstraint?
    
    public override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
    }

    public override func viewDidLayoutSubviews()
    {
        super.viewDidLayoutSubviews()

        membersStackView.isHidden = !membersScroller.hasUsers
        
        // Dynamically size the text view to fit the content.
        let size = descriptionField.sizeThatFits(CGSize(width:descriptionField.bounds.width, height:CGFloat.greatestFiniteMagnitude))
        descriptionViewHeight.constant = size.height
        
        categoriesViewHeight?.constant = categoriesViewController.sizeThatFits(size: self.view.bounds.size).height
    }
    
    // MARK: View configuration
    
    // TODO: remove
    private func setMode() {
        let editingEnabled = false
        descriptionField.isEditable = editingEnabled
    }
    
    public func configure(withGroup group:Group)
    {
        self.group = group
        
        if let locationName = group.locationName {
            selectedLocation = SelectedLocation(coordinate: group.location, locationName: locationName, locationSubtitle: "")
        }
        if let description = group.description {
            setGroupDescription(description)
        }
        
        navBar.applyGroupTitle(group: group)
        
        // TESTING
        /*
        let categories = [
                GroupCategory.Career,
                GroupCategory.Communities,
                GroupCategory.Events,
                GroupCategory.Family,
                GroupCategory.Fitness,
                GroupCategory.FoodDrink
            ]
         //categoriesViewController.delegate = self
        */
        let categories = GroupCategories.from(strings: group.categories)
        //log("group: \(group.title), categories: \(categories)")
        setCategories(categories: categories)
        self.categoryStackView.isHidden = categories.isEmpty
        self.categoryStackView.isUserInteractionEnabled = false

        // Fetch the latest members
        service.getGroupMembers(withToken: group.groupToken) { [weak self] response in
            if case .success(let userResponses) = response {
                guard let sself = self else { return }

                //log("group: \(group.title) retrieved \(userResponses.count) members: \(userResponses.map { $0.name })")
                let members = sself.personManager
                    .getPeople(forProtos: userResponses)
                    .sorted { $0.name < $1.name }
                
                sself.configureMembers(group: group, members: members)
            }

            // TODO: Retry on error
        }
        
        // Enable the edit button when we are the owner or an admin
        navBar.rightButton.isShown = group.owner.isMe || userManager.userIsAdmin

        updateLayoutForContent()
    }
    
    public func configureMembers(group: Group, members: [Person])
    {
        let maxUsersShown = 50
        membersScroller.configureUsers(users: Array(members.prefix(maxUsersShown)) )
        membersScroller.showCreator(user: group.owner)
        let membersTitle = "\(group.joinedCount) " + "Member".pluralize(forNumber: members.count)
        membersLabelButton.setTitle(membersTitle, for: .normal)
        
        if group.owner.isMe {
            group.rx.joinRequests.onNext { [weak self] count in
                self?.membersScroller.setBadge(count: count)
            }.disposed(by: disposal)
        }
        
        view.setNeedsLayout() // TODO: Why doesn't setting this in the members scroller work?
    }
    
    public func configure(withBlock block: @escaping ((CircleDetailViewController)->Void)) {
        if isViewLoaded {
            block(self)
        } else {
            configureBlock = block
        }
    }

    public func setGroupDescription(_ text: String) {
        descriptionField.text = text
        updateDescriptionText()
    }

    // Update all content driven layout
    private func updateLayoutForContent()
    {
        updateDescriptionText()
        updateDoneButtonEnabledState()
        updateLabels()
        updateLocationText()
        updateSchedule()
        updatePrivacy()
        updateOptionButtons()
    }
    
    private func updateOptionButtons() {
        [leaveButton, reportButton, deleteButton].forEach { $0.isHidden = true }
        guard let group = group else { return }
        leaveButton.isShown = group.isJoined && !group.owner.isMe
        reportButton.isShown = !group.owner.isMe
        deleteButton.isShown = group.owner.isMe || userManager.userIsAdmin
    }
    
    private func updatePrivacy() {
        let discoverable = group?.discoverable ?? false
        let womenOnly = PresentSpace.isWomenOnly(space: group?.space)

        privacyTextField.text =
            (womenOnly ? "Women-only. " : "")
            + (PrivacyText.getPreApprovalDescription(forPreApproval: group?.preapproval, womenOnly: womenOnly) ?? "")
            + " "
            + PrivacyText.getDiscoverabilityAndGenderTextFor(discoverable: discoverable, womenOnly: womenOnly)
    }
    
    private func updateSchedule()
    {
        // Note: We are currently hiding the schedule
        let forceHideSchedule = true
        // Hide the schedule if nothing to show.
        if let schedule = group?.schedule, !forceHideSchedule {
            scheduleSection.isHidden = false
            scheduleDate.text = formatScheduleDateLine(schedule: schedule)
            scheduleTime.text = formatScheduleTimeLine(schedule: schedule)
        } else {
            scheduleSection.isHidden = true
            view.setNeedsLayout()
        }
    }
    
    private func updateLabels()
    {
        // Localize
        let editingEnabled = false // TODO: Remove
        locationLabel.text = "Location" + (editingEnabled && selectedLocation == nil ? "*" : "")
    }
    
    // MARK: Text handling
    
    @objc public func topicTextChanged() {
        updateLabels()
        updateDoneButtonEnabledState()
    }

    @objc public func descriptionTextChanged() {
        updateDoneButtonEnabledState()
    }
    
    // TODO: Encapsulate the description text placeholder functionality
    
    // MARK: UITextViewDelegate
    
    public func textViewDidChange(_ textView: UITextView) {
        updateDescriptionText()
        updateDescriptionPlaceholderText()
    }
    public func textViewDidBeginEditing(_ textView: UITextView) {
        updateDescriptionPlaceholderText()
    }
    public func textViewDidEndEditing(_ textView: UITextView) {
        updateDescriptionPlaceholderText()
    }
    public func textView(_ textView: UITextView, shouldInteractWith URL: URL, in characterRange: NSRange, interaction: UITextItemInteraction) -> Bool {
        logDebug("CreateGroupViewController: should interact with url = \(URL)")
        //activitySpinner.startAnimating()
        URLManager.shared.dispatchUrl(url: URL) {
            //self.activitySpinner.stopAnimating()
        }
        return false
    }

    private func updateDescriptionText()
    {
        // For Non-owner view with empty description, default description the text
        if descriptionField.text.isEmpty, let group = group, !group.owner.isMe {
            descriptionField.text = "Check out the chat for info!" // Localize
        }
        updateDescriptionPlaceholderText()
        
        view.setNeedsLayout()
        view.layoutIfVisible()
        //descriptionField.setNeedsLayout()
        //descriptionField.layoutIfVisible()
    }
    
    private func updateDescriptionPlaceholderText() {
        descriptionPlaceholderField.isHidden = !descriptionField.text.isEmpty
    }
    
    private func updateLocationText() {
        locationNameField.isHidden = locationNameField.text.isEmpty
        locationField.isHidden = !locationNameField.isHidden && locationField.text.isEmpty
    }
    
    // TODO: We can replace this with simply view.endEditing()
    @discardableResult
    public override func resignFirstResponder() -> Bool {
        if descriptionField.isFirstResponder {
            descriptionField.resignFirstResponder()
        }

        super.resignFirstResponder()
        return true
    }
    
    // MARK: Button Handling
    
    @objc private func goBack() {
        screenPresenter.goBack()
    }
    
    @IBAction func locationPressed() {
        showLocation()
    }
    @IBAction func descriptionPressed(_ sender: Any) {
        descriptionField.becomeFirstResponder()
    }
    
    private func chooseLocation() {
        screenPresenter.presentCreateGroupLocation(with: self, allowRetreat: true, selectedLocation: selectedLocation)
        descriptionField.resignFirstResponder()
    }
    
    private func showLocation() {
        guard let location = selectedLocation else {
            logError("Missing location for viewing map")
            return
        }
        guard let url = MapsUtil.appleMapsUrl(forLocation: location) else { return }
        UIApplication.shared.open(url, options: [:]) { success in }
    }
    
    @IBAction func categoryPressed() { }
    
    fileprivate func updateDoneButtonEnabledState()
    {
        doneButton.isEnabled = selectedLocation != nil
    }
    
    // view members screen
    func viewMembers() {
        guard let group = group else { return }
        screenPresenter.presentCircleMembers(for: group)
    }

    // MARK: UserScrollerDelegate
    
    // TODO: Factor this out with the same usage in MyProfileViewController
    public func selectedUser(user: PersonData)
    {
        guard let user = user as? Person else { return }
        screenPresenter.presentPersonProfile(forPerson: user)
    }
    
    // MARK: - CreateGroupLocationDelegate
    
    public func setGroupLocation(location:SelectedLocation) {
        selectedLocation = location
        updateDoneButtonEnabledState()
    }
    
    // MARK: - CreateGroupCategoriesDelegate
    
    // An ordered list of categories
    public func setCategories(categories: [String]) {
        setCategories(categories: GroupCategories.from(strings: categories))
    }
    // An ordered list of categories
    public func setCategories(categories: [GroupCategory]) {
        categoriesViewController.configure(categories: categories, picked: true)
    }

    // MARK: Schedule formatting
    
    // TODO: Anything to factor out with the formatting in CardViewController?
    // "Monday, February 19"
    func formatScheduleDateLine(schedule: Present.Schedule) -> String
    {
        let start = schedule.startDate
        //        let start = Date(millisecondsSince1970: schedule.startTime)
        
        let format = DateFormatter()
        format.dateFormat = "EEEE, MMMM dd"
        return format.string(from: start)
    }
    
    // TODO: Anything to factor out with the formatting in CardViewController?
    // "9:00AM - 12:00PM"
    func formatScheduleTimeLine(schedule: Present.Schedule) -> String
    {
        //        let start = Date(millisecondsSince1970: schedule.startTime)
        //        let end: Date? = (schedule.hasEndTime && schedule.endTime != schedule.startTime) ? Date(millisecondsSince1970: schedule.endTime) : nil
        let start = schedule.startDate
        let end = schedule.endDate
        
        let format = DateFormatter()
        format.dateFormat = "h:mma"
        let startStr = format.string(from: start)
        if let end = end {
            let endStr = format.string(from: end)
            return "\(startStr) - \(endStr)"
        } else {
            return startStr
        }
    }
}

// MARK: KeyboardWillShowHideListener

extension CircleDetailViewController: KeyboardWillShowHideListener
{
    public func keyboardWillShow(with animation: KeyboardAnimation)
    {
        main {
            // Add content insets to teh scroll view to make space for the keyboard
            let insets = UIEdgeInsets(top: 0, left: 0, bottom: animation.endFrame.height, right: 0)
            self.scrollView.contentInset = insets
            self.scrollView.scrollIndicatorInsets = insets
            
            if self.descriptionField.isFirstResponder {
                self.scrollView.scrollRectToVisible(
                    self.descriptionField.convert(self.descriptionField.frame, to: self.scrollView), animated: true)
            }
        }
    }
    
    public func keyboardWillHide(with animation: KeyboardAnimation) {
        main {
            self.scrollView.contentInset = UIEdgeInsets.zero
            self.scrollView.scrollIndicatorInsets = UIEdgeInsets.zero
        }
    }
}

extension Present.Schedule {
    var startDate: Date {
        return Date(millisecondsSince1970: self.startTime)
    }
    var endDate: Date? {
        return (self.hasEndTime && self.endTime != self.startTime) ? Date(millisecondsSince1970: self.endTime) : nil
    }
}

// MARK: - Theming

extension ThemeableButton {
    public func applyCreateGroupTheme() {
        setTitleColor(.black, for: .normal)
        setTitleColor(.lightGray, for: .disabled)
        titleLabel?.font = .presentFont(ofSize: 14.0, weight: .semibold)
    }
}

