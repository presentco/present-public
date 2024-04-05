//
//  ExploreViewController.swift
//  Present
//
//  Created by Patrick Niemeyer on 01/29/18
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import UIKit
import RxSwift
import RxCocoa

import Relativity // For legacy search bar

/// The main view controller for the Feed tab on the home screen, including the
/// present logo and search button, filling the content area above the home view bottom tabs.
/// HomeViewController --> FeedViewController, ...
///     --> GatedNearbyFeedWebViewController
public final class FeedViewController: PresentViewController, FeedContentViewControllerDelegate
{
    @IBOutlet weak var searchButton: ThemeableButton! {
        didSet {
            locationManager.locationAuthorized.observable.onNext { status in
                self.searchButton.isEnabled = status
            }.disposed(by: disposal)
            
            searchButton.addTarget { [weak self] button in
                guard let sself = self else { return }
                logEvent(.home_tap_search)
                sself.searchBarShowing = !sself.searchBarShowing
            }
        }
    }
    
    @IBOutlet weak var contentAreaSeparator: UIView!
    
    /// The current value of search
    public let feedSearch = ObservableValue<SearchText>(initialValue: .none)
    
    private var searchBarShowing = false {
        didSet {
            updateSearchBarVisibility()
        }
    }

    // The paged content including the tab bar control
    @IBOutlet weak var contentView: UIView!
    
    // The controller populating the content view
    let nearbyFeedController: GatedNearbyFeedWebViewController = GatedNearbyFeedWebViewController()

    //
    // Legacy Search bar
    //
    
    let searchBar = UIView()
    private let searchCancelButton = ThemeableButton()
    
    lazy private var searchBarTextField = ThemeableTextField().then {
        $0.layer.borderWidth = 1.0
        $0.layer.borderColor = UIColor(hex: 0xF2F2F2).cgColor
        $0.rx.text
            .throttle(0.3, scheduler: MainScheduler.instance)
            .distinctUntilChanged()
            .skip(1) // Ignore the initial empty string value
            .map { text in
                self.searchTextDidChange(to: text)
            }
            .disposed(by: disposal)
        $0.leftView = UIImageView(image: #imageLiteral(resourceName: "SearchIcon")).then {
            $0.contentMode = .center
            $0.sizeToFit()
            $0.bounds = $0.bounds.with {
                $0.size.width = $0.size.width + 2 * 12
            }
        }
        $0.leftViewMode = .always
    }
    
    public override func viewDidLoad()
    {
        super.viewDidLoad()
        
        nearbyFeedController.configure {
            $0.delegate = self
        }

        // Install the feed tabs view
        installChild(viewController: nearbyFeedController, in: contentView) {
            $0.constrainToFillSuperview()
        }
        
        initSearchBar()
        view.addSubview(searchBar)
        
        searchCancelButton.anchorCenter(to: searchButton)
        
        // TODO: This is not working: the content never reports positive scroll position (only negative ones)!?
        // TODO: Always show the separator for now.
        // Configure monitoring the webview scroll position for the content separator
        //nearbyFeedController.configure {
            //$0.feedViewController.webView.contentOffset.onNext { [weak self] offset in
                //log("offset = \(offset)")
                //self?.updateContentAreaSeparator(y: offset, maxY: 32)
            //}.disposed(by: $0.disposal)
        //}
        self.contentAreaSeparator.alpha = 1.0
    }
    
    // TODO: We've used this twice, next time make a util
    /// show the content area separator when info area is in partial scroll
    private func updateContentAreaSeparator(y: CGFloat, maxY: CGFloat) {
        let steps: CGFloat = 50
        let frac = min(steps, min(y, abs(maxY-min(y,maxY))))/steps // normalized 0-1 distance from edges
        self.contentAreaSeparator.alpha = frac
    }
}

// MARK: Legacy search bar

extension FeedViewController
{
    func initSearchBar()
    {
        searchBar.backgroundColor = .white
        searchCancelButton.apply(viewModel: ThemeableButton.ViewModel(title: .none, image: #imageLiteral(resourceName:"CancelSearchGlyph")))
        searchCancelButton.addTarget { [weak self] button in
            self?.searchBarShowing = false
        }
        searchBar.addSubview(searchCancelButton)
        searchBarTextField.attributedPlaceholder = NSAttributedString(string: NSLocalizedString(
            "CardHomeViewControllerSearchPlaceholderText",
            tableName: nil, bundle: .main,
            value: "Whatchya lookin' for?",
            comment: "Search placeholder text"), attributes: [ NSAttributedStringKey.font: UIFont.presentFont(ofSize: 14.0, weight: .regular), NSAttributedStringKey.foregroundColor: UIColor(hex: 0xA4_AA_B3) ]
        )
        searchBarTextField.tintColor = UIColor(hex: 0x71_41_DB)
        searchBarTextField.font = .presentFont(ofSize: 14.0, weight: .medium)
        searchBarTextField.addTarget(self, action: #selector(searchBarDidExit), for: .editingDidEndOnExit)
        //searchBarTextField.addTarget(self, action: #selector(showSearchBar), for: .editingDidBegin)
        searchBarTextField.returnKeyType = .search
        searchBar.addSubview(searchBarTextField)
        updateSearchBarAlpha(showBar: false)
    }
    
    public override func viewDidLayoutSubviews()
    {
        super.viewDidLayoutSubviews()

        let searchBarHeight: CGFloat = searchBar.isHidden ? 0 : 36.0
        searchBar.bounds.size = CGSize(width: view.bounds.width, height: searchBarHeight)

        if #available(iOS 11.0, *) {
            searchBar.topLeft --> .topLeft + view.safeAreaInsets.top.verticalOffset
        } else {
            searchBar.topLeft --> .topLeft + 20.verticalOffset
        }
        
        searchCancelButton.sizeToFit()
        searchBarTextField.bounds.size = CGSize(
            width: view.bounds.width - 20*3 - searchCancelButton.bounds.width,
            height: searchBar.bounds.size.height
        )
        searchBarTextField.right + 20.horizontalOffset + searchCancelButton.bounds.width.horizontalOffset + 20.horizontalOffset --> .right
        
        searchBarTextField.roundCornersToHeight()
    }
    
    private func searchTextDidChange(to text: String?) {
        //logDebug("FeedViewController: search text changed to: \(String(describing: text))")
        if searchBarShowing {
            self.feedSearch.value = .forText(text)
        }
    }
    
    @objc private func searchBarDidExit(sender: AnyObject) {
        guard let searchBar = sender as? ThemeableTextField, searchBar == self.searchBarTextField else {
            return
        }
        
        if searchBar.text == nil || searchBar.text?.count == 0 {
            searchBarShowing = false
        }
        
        searchBar.resignFirstResponder()
    }
    
    private func updateSearchBarAlpha(showBar: Bool) {
        let searchAlpha: CGFloat = showBar ? 1.0 : 0.0
        searchBar.alpha = searchAlpha
        searchCancelButton.alpha = searchAlpha
        searchBarTextField.alpha = searchAlpha
    }
    
    private func updateSearchBarVisibility()
    {
        if searchBarShowing {
            UIView.animate(withDuration: 0.2, animations: {
                self.updateSearchBarAlpha(showBar: true)
            }) { _ in
                //self.searchBarTextField.becomeFirstResponder()
            }
            feedSearch.value = .empty
        } else {
            searchBarTextField.resignFirstResponder()
            searchBarTextField.text = nil
            searchBarTextField.sendActions(for: .editingChanged)
            UIView.animate(withDuration: 0.2) {
                self.updateSearchBarAlpha(showBar: false)
            }
            feedSearch.value = .noSearch
        }
        view.setNeedsLayout()
    }
    
}

