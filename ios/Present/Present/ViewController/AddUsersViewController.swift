//
//  AddUsersViewController.swift
//  Present
//
//  Created by Patrick Niemeyer on 7/19/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import RxSwift

/// Base class for the add friends and add members to circles view controllers
/// A search view controller that does some minimal setup.
class AddUsersViewController: SearchViewController
{
    override public func viewDidLoad()
    {
        super.viewDidLoad()
        self.navBar.title.text = "Add Friends"
        self.searchBar.placeholder = "Search for friends"
        self.searchSeparator.isHidden = true

        self.searchBar.rx.searchButtonClicked.bind { [weak self] in
            self?.searchBar.resignFirstResponder()
            }.disposed(by: disposal)
        
        initContent()
    }
    
    /// Override to add the tabs
    func initContent() { }
}

/// This class manages two tabs shared by add friends and add members to group view controllers.
/// (The facebook and contacts tabs of add friends or add members to circle
/// This class is parameterized on the types of the table view controllers to be used
/// so that it can be used in both the add friends and add members to circle contexts.
class AddUserTabsViewController<
        FBTabType: AddUsersTableViewController,
        ContactsTabType: AddUsersTableViewController>
    : PresentTabsViewController
{
    var searchText: Observable<SearchText>?
    
    lazy var facebookViewController =
        ConditionalViewController<FBTabType>()
            .configurePlaceholder {
                $0.titleText.text = "Welcome to Present!"
                $0.subtitleText.text = "Sign up with Facebook to connect with your friends."
                $0.titleImage.image = #imageLiteral(resourceName: "ProfileImage")
                $0.connectWithFacebookButton.isShown = true
            }
            .initViewController(FBTabType.init)
            .configureViewController {
                $0.searchText = self.searchText
            }.configure { [weak self] cvc in
                var weakCvc = cvc // Unused but seems to work around compiler bug
                self?.userManager.me.observable.onNext { [weak cvc] me in
                        cvc?.showPlaceholder = !me.userProfile.facebookLinked
                    }.disposed(by: cvc.disposal)
            }
    
    lazy var contactsViewController =
        ConditionalViewController<ContactsTabType>()
            .configurePlaceholder {
                $0.titleText.text = "Welcome to Present!"
                $0.subtitleText.text = "Allow access to contacts to connect with your friends."
                $0.titleImage.image = #imageLiteral(resourceName: "ProfileImage")
                $0.allowContactsAccessButton.isShown = true
            }
            .initViewController(ContactsTabType.init)
            .configureViewController {
                $0.searchText = self.searchText
            }.configure { [weak self] cvc in
                var weakCvc = cvc // Unused but seems to work around compiler bug
                self?.contactsManager.rx.authorizationStatus
                    .observeOn(MainScheduler.instance)
                    .onNext { [weak cvc] status in
                        cvc?.showPlaceholder = status != .authorized
                    }.disposed(by: cvc.disposal)
            }
    
    /// Set the desired tabs and return the tab count
    @discardableResult
    override public func initTabs() -> Int
    {
        tabViewControllers = [contactsViewController, facebookViewController]
        bar.items = [Item(title: "CONTACTS"), Item(title: "FACEBOOK")]
        return tabViewControllers.count
    }
}

/// Base class for table views used in the add friends and add members to circles view controllers
/// Contains the table view and a reference to the enclosing container search text observable
public class AddUsersTableViewController: PresentViewController, UITableViewDelegate
{
    let tableView = UITableView()
    let tableViewModel = TableViewModel()
    let emptyView = EmptyView()
    private var loading = BehaviorSubject.init(value: false)

    var searchText: Observable<SearchText>? {
        didSet {
            tableViewModel.bind(searchTextSource: searchText)
            emptyView.bind(searchTextSource: searchText)
        }
    }
    
    override required public init() {
        super.init()
    }
    
    public required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override public func viewDidLoad()
    {
        let separatorColor = UIColor(hex: 0xF8F8F8)
        tableView.do {
            $0.separatorInset = UIEdgeInsets(top: 0, left: 9, bottom: 0, right: 9)
            $0.separatorColor = separatorColor
            $0.rowHeight = 41
            view.addSubview($0)
            $0.constrainToFillSuperview()
        }
        view.addBottomSeparator(color: separatorColor)
        initTable()
        
        super.viewDidLoad() // init the table before we run config
        
        self.tableView.rx.willBeginDragging.bind {
            UIResponder.resignAnyResponders()
            }.disposed(by: disposal)

        emptyView.do {
            view.addSubview($0)
            $0.constrainToFillSuperview()
            $0.isHidden = true
            $0.showButton = true
        }
       
        Observable.combineLatest(tableViewModel.changeObserver, loading)
            .bind { [weak self] isEmpty, loading in
                log("Change: isEmpty \(isEmpty) loading: \(loading)")
                self?.tableView.isHidden = isEmpty
                self?.emptyView.isShown = isEmpty && !loading
        }.disposed(by: disposal)
    }
    
    var lastFetch: Date?
    
    override public func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        if (lastFetch ?? Date(millisecondsSince1970: 0)).timeIntervalSinceNow > 30.0 {
            fetchData()
            lastFetch = Date()
        }
    }
    
    override public func startActivity() {
        loading.onNext(true)
        super.startActivity()
    }
    
    override public func endActivity() {
        loading.onNext(false)
        super.endActivity()
    }
    
    override public func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        // Note: This is a workaround for cells containing our logo spinner.
        // Note: (they need to be told to restart their animation when hidden and then made visible.
        // Note: We should probably add a viewDidAppear() to our base cell and delegate to visible cells.
        tableView.reloadData()
    }
    
    func initTable() { }
    func fetchData() { }
}

