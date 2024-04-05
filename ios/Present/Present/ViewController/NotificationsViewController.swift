//
//  NotificationsViewController.swift
//  Present
//
//  Created by Dan Federman on 6/27/17.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import UIKit


public final class NotificationsViewController: UITableViewController, ActivityObserver, ActivityTableViewCellDelegate
{
    // MARK: Private Properties
    
    private let imageManager: ImageManager
    private let screenPresenter: RootViewController
    private let activityManager: ActivityManager
    private let activityIndicator = PresentLogoActivityIndicator()
    
    private let presentRefreshController = PresentRefreshController()
    
    private var isVisible: Bool = false
    private var showLoadingErrorWhenVisible = false
    
    private var cellModels: [DefaultActivityTableViewCellModel] = [] {
        didSet {
            if cellModels == oldValue {
                //log("activity unchanged, not reloading")
            } else {
                //log("activity changed, reloading")
                tableView.reloadData()
            }
            hasRetrievedContent = true
        }
    }
    private var hasRetrievedContent = false {
        didSet {
            activityIndicator.stopAnimating()
        }
    }
    
    // MARK: Initialization
    
    public required init(activityVendor: ActivityManager, imageManager: ImageManager, screenPresenter: RootViewController) {
        self.imageManager = imageManager
        self.screenPresenter = screenPresenter
        self.activityManager = activityVendor
        
        super.init(style: .plain)
        
        activityVendor.register(activityObserver: self, observerQueue: .main)
    }
    
    public required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    // MARK: ActivityObserver
    
    public func activityDidUpdate(to activity: [Activity]) {
        presentRefreshController.endRefreshing()
        let date = Date()
        cellModels = activity.map {
            DefaultActivityTableViewCellModel(
                activity: $0,
                imageManager: imageManager,
                referenceDate: date
            )
        }
    }
    
    public func activityDidFailToUpdate() {
        presentRefreshController.endRefreshing()
        if cellModels.count == 0 {
            showLoadingErrorWhenVisible = true
            showLoadingErrorIfNecessary()
        }
    }
    
    @objc private func refreshAllNotifications(){
        presentRefreshController.beginAnimatingPullToRefreshLogo()
        self.refreshNotifications()
    }
    
    public func refreshNotifications(){
        self.activityManager.fetchActivity()
    }
    
    // MARK: UITableViewDataSource
    
    public override func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }
    
    public override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return cellModels.count
    }
    
    public override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: ActivityTableViewCell.reuseIdentifier) as! ActivityTableViewCell
        
        cell.configure(with: cellModels[indexPath.row])
        cell.delegate = self
        
        return cell
    }
    
    // MARK: UITableViewDelegate
    
    public override func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        let expectedHeight: CGFloat = 80.0
        return expectedHeight
    }
    
    public override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        
        screenPresenter.presentScreen(for: cellModels[indexPath.row].target)
    }
    
    // MARK: UIViewController
    
    public override func viewDidLoad() {
        super.viewDidLoad()
        
        tableView.register(ActivityTableViewCell.self, forCellReuseIdentifier: ActivityTableViewCell.reuseIdentifier)
        
        tableView.delegate = self
        tableView.dataSource = self
        
        tableView.separatorInset = .zero
        tableView.separatorColor = Palette.whiteToWhiteSeparatorColor
        
        //presentRefreshController.addTarget(self, action: #selector(refreshAllNotifications))
        //self.refreshControl = presentRefreshController.refreshControl
        
        edgesForExtendedLayout = []
        
        // Set the footer view to avoid having separators below the last row.
        tableView.tableFooterView = UIView()
        tableView.backgroundColor = UIColor(red: 0.99, green: 0.99, blue: 0.99, alpha: 1.0)
    }
    
    public override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)

        isVisible = true
        showLoadingErrorIfNecessary()
        
        if !hasRetrievedContent {
            installChild(viewController: activityIndicator, in: tableView)
            activityIndicator.startAnimating()
        }
        
        // HACK: The tab manager is causing viewDidAppear to be called before the view is actually visible on first load.
        // HACK: We will skip the first view before marking as read.  Alternatively we could get the cooperation of the
        // HACK: tab view controller to tell us when our tab is selected.
        if viewDidAppearCount > 0 {
            self.activityManager.markAsRead()
        }
        viewDidAppearCount += 1
    }
    private var viewDidAppearCount = 0
    
    override public func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        refreshNotifications()
    }
    
    public override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        
        isVisible = false
    }
    
    public override func scrollViewDidScroll(_ scrollView: UIScrollView) {
        presentRefreshController.updatePullToRefresh()
    }
    
    // MARK: Private Methods
    
    private func showLoadingErrorIfNecessary() {
        guard showLoadingErrorWhenVisible && isVisible else {
            return
        }
        
        showLoadingErrorWhenVisible = false
        
        let activityFailedToLoadTitle = NSLocalizedString(
            "ActivityTableViewControllerCouldNotLoadActivityAlertTitle",
            tableName: nil,
            bundle: .main,
            value: "There was an issue loading your Activity",
            comment: "Title of the alert informing the user there was an issue loading activity"
        )
        
        let alertController = UIAlertController(title: nil, message: activityFailedToLoadTitle, preferredStyle: .alert)
        let retryButtonTitle = NSLocalizedString(
            "ActivityTableViewControllerReloadActivityAlertButtonTitle",
            tableName: nil,
            bundle: .main,
            value: "Ok",
            comment: "Title of the alert button prompting user to re-fetch activity"
        )
        alertController.addAction(withTitle: retryButtonTitle) { /*[weak self]*/ (_) in
            //self?.activityVendor.fetchActivity()
        }
        
        present(alertController, animated: true)
    }
    
    // MARK: DefaultActivityTableViewCellModel
    
    private class DefaultActivityTableViewCellModel: ActivityTableViewCellModel, Equatable
    {
        public weak var delegate: ActivityTableViewCellModelDelegate?
        public let target: Presentable
        
        // MARK: Initialization
        
        public init(activity: Activity, imageManager: ImageManager, referenceDate: Date) {
            timestamp = referenceDate.relativeTime(since: activity.date, maxGranularity: .second)
            text = activity.summary
            target = activity.target
            
            if let photo = activity.photo {
                imageManager.getImage(atURL: photo, successHandler: { [weak self] (image) in
                    self?.profileImage = image
                })
            }
        }
        
        // MARK: ActivityTableViewCellModel
        
        public private(set) var profileImage: UIImage? {
            didSet {
                delegate?.didUpdate(model: self)
            }
        }
        public private(set) var timestamp: String = "" {
            didSet {
                delegate?.didUpdate(model: self)
            }
        }
        public private(set) var text: String = "" {
            didSet {
                delegate?.didUpdate(model: self)
            }
        }
        
        public static func ==(lhs: DefaultActivityTableViewCellModel, rhs: DefaultActivityTableViewCellModel) -> Bool {
            return lhs.profileImage == rhs.profileImage &&
                   lhs.timestamp == rhs.timestamp &&
                   lhs.text == rhs.text
                   // TODO: Make Presentable equatable
                   //lhs.target == rhs.target
        }

    }
    
    public func didSelectProfileImage(person: Person) {
        screenPresenter.presentPersonProfile(forPerson: person)
    }
}

//
//  ActivityTableViewCell.swift
//
//  Created by Dan Federman on 6/26/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Relativity

public protocol ActivityTableViewCellModel
{
    var profileImage: UIImage? { get }
    var timestamp: String { get }
    var text: String { get }
    var target: Presentable { get }
    
    var delegate: ActivityTableViewCellModelDelegate? { get set }
}

public protocol ActivityTableViewCellModelDelegate: class {
    func didUpdate(model: ActivityTableViewCellModel)
}

public protocol ActivityTableViewCellDelegate: class {
    func didSelectProfileImage(person: Person)
}

public final class ActivityTableViewCell: UITableViewCell, ActivityTableViewCellModelDelegate
{
    // MARK: Private Properties
    
    private let profileImageView = CircularImageBorderView()
    private let summaryLabel = UILabel()
    private let timestampLabel = UILabel()
    
    private var viewModel: ActivityTableViewCellModel? {
        didSet {
            viewModel?.delegate = self
        }
    }
    
    // MARK: Public Static Properties
    
    public static let reuseIdentifier = "ActivityTableViewCell"
    public static let profileImageDimension: CGFloat = 60.0
    
    // MARK: Private Static Properties
    
    private static let standardTimestampTheme = UILabel.Theme(textColor: UIColor(hex: 0xb6_b9_bf), font: UIFont.presentFont(ofSize: 14.0, weight: .medium), numberOfLines: 1)
    private static let standardSummaryTheme = UILabel.Theme(textColor: UIColor(hex: 0x7d_7e_80), font: UIFont.presentFont(ofSize: 16.0, weight: .regular), numberOfLines: 2)
    
    public weak var delegate: ActivityTableViewCellDelegate?
    
    // MARK: Initialization
    
    public override init(style: UITableViewCellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        
        contentView.addSubview(profileImageView)
        let tr = UITapGestureRecognizer( target: self, action: #selector(tapOnProfile(rec:)) )
        tr.delegate = self
        profileImageView.isUserInteractionEnabled = true
        profileImageView.addGestureRecognizer(tr)
        
        contentView.addSubview(timestampLabel)
        contentView.addSubview(summaryLabel)
        
        selectionStyle = .none
        
        applyThemes()
    }
    
    public required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    // MARK: Public Properties
    
    public var timestampTheme = ActivityTableViewCell.standardTimestampTheme {
        didSet {
            applyThemes()
        }
    }
    public var summaryTextTheme = ActivityTableViewCell.standardSummaryTheme {
        didSet {
            applyThemes()
        }
    }
    
    // MARK: ActivityTableViewCellModelDelegate
    
    public func didUpdate(model: ActivityTableViewCellModel) {
        configure(with: model)
    }
    
    // MARK: UITableViewCell
    
    public override func prepareForReuse() {
        super.prepareForReuse()
        
        clearProfilePicture()
    }
    
    // MARK: UIView
    
    public override func layoutSubviews() {
        super.layoutSubviews()
        
        let leadingMargin = 15.horizontalOffset
        let textLabelMargin = 15.horizontalOffset
        let summaryTimestampMargin = 14.horizontalOffset
        let trailingMargin = 15.horizontalOffset
        
        profileImageView.imageDiameter = ActivityTableViewCell.profileImageDimension
        profileImageView.sizeToFit()
        ViewPosition.Anchor.left + leadingMargin - (profileImageView.borderOutset / 2.0).horizontalOffset <-- profileImageView.left
        
        timestampLabel.sizeToFit()
        timestampLabel.right + trailingMargin --> .right
        summaryLabel.sizeToFit(fixedWidth: ((profileImageView.right + textLabelMargin) |--| (timestampLabel.left - summaryTimestampMargin)).width)
        
        profileImageView.right + textLabelMargin <-- summaryLabel.left
    }
    
    // MARK: Public Methods
    
    public func configure(with model: ActivityTableViewCellModel) {
        viewModel = model
        
        if let profileImage = model.profileImage {
            profileImageView.image = profileImage
            profileImageView.backgroundColor = nil
            profileImageView.borderOutset = 3.0
            profileImageView.borderWidth = 2.0
            
        } else {
            clearProfilePicture()
        }
        
        timestampLabel.text = model.timestamp
        summaryLabel.text = model.text
        
        profileImageView.borderColor = .clear
        
        setNeedsLayout()
    }
    
    // MARK: Private Methods
    
    private func applyThemes() {
        timestampLabel.apply(theme: timestampTheme)
        summaryLabel.apply(theme: summaryTextTheme)
        
        setNeedsLayout()
    }
    
    private func clearProfilePicture() {
        profileImageView.image = nil
        profileImageView.backgroundColor = Palette.avatarLoadingColor
        profileImageView.borderOutset = 0.0
        profileImageView.borderWidth = 0.0
        
        setNeedsLayout()
    }
    
    @objc private func tapOnProfile( rec : UITapGestureRecognizer ) {
        guard let viewModel = viewModel else { return }
        var person: Person? = nil
        switch (viewModel.target) {
        case let .showGroupMessage(commentResponse):
            person = Person(userProto: commentResponse.author, isMe: false)
        default:
            break
        }
        if let person = person {
            delegate?.didSelectProfileImage(person: person)
        }
    }
}

