//
//  UserScroller.swift
//  Present
//
//  Created by Patrick Niemeyer on 7/28/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import RxSwift

public protocol UserScrollerDelegate : class {
    func selectedUser(user: PersonData)
}

// TODO: Replace this with a collection view based scroller like CircleCarouselViewController
public class UserScroller : UIView, UserScrollerCellDelegate
{
    @IBOutlet weak var scrollView: UIScrollView!
    @IBOutlet weak var stackView: UIStackView!
    
    public weak var delegate: UserScrollerDelegate?
    public var imageHeight: CGFloat = 39
    public var users: [PersonData] = []

    public var contentInset: UIEdgeInsets {
        get {
            return scrollView.contentInset
        }
        set {
            scrollView.contentInset = newValue
        }
    }
    
    var showBadgeCell = false
    var badgeCell: UserScrollerCell? // configured badge cell
    let disposal = DisposeBag()

    convenience init() {
        self.init(frame: CGRect.zero)
    }
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        initView()
    }
    
    required public init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        initView()
    }
    
    private func initView() {
        let view = viewFromNibForClass()
        view.frame = bounds
        configureUsers(users: [])
        addSubview(view)
    }
    
    public func configureUsers(users usersIn: [PersonData])
    {
        // If nothing that we show has changed shortcut setup.
        if users.count > 0 && usersIn.count == users.count && !zip(usersIn, users).contains(where: {
            $0.0.nameData != $0.1.nameData || $0.0.photoData != $0.1.photoData
        }) {
            logDebug("configureUsers: No changes in users, skipping.")
            return
        }

        for view in stackView.arrangedSubviews {
            if view is UserScrollerCell {
                stackView.removeArrangedSubview(view)
                view.removeFromSuperview()
            }
        }
        
        for user in usersIn.prefix(6) { self.add(user: user) }
        for user in usersIn.dropFirst(6) {
            DispatchQueue.main.async { [weak self] in
                guard let sself = self else { return }
                sself.add(user: user)
            }
        }

        self.users = usersIn
        
        showBadgePlaceholderIfNeeded()
    }
    
    public func add(placeHolder image: UIImage) {
        let cell = initPlaceHolder(image: image)
        stackView.addArrangedSubview(cell)
    }
    
    private func initPlaceHolder(image: UIImage) -> UserScrollerCell {
        let cell = UserScrollerCell()
        cell.creatorLabel.isHidden = true
        cell.delegate = self
        cell.imageButtonHeight.constant = imageHeight
        cell.imageButton.setImage(image, for: .normal)
        cell.label.text = nil
        return cell
    }
    
    public func add(user: PersonData)
    {
        let cell = UserScrollerCell(user: user, imageHeight: imageHeight)
        cell.creatorLabel.isHidden = true
        cell.delegate = self
        cell.imageButtonHeight.constant = imageHeight
        stackView.addArrangedSubview(cell)
    }
    
    // Add the "creator" adornment to the specified users view and move that view to the first spot.
    public func showCreator(user: PersonData) {
        for case let view as UserScrollerCell in stackView.arrangedSubviews {
            if let viewUser = view.user, viewUser.id == user.id {
                view.creatorLabel.isHidden = false
                stackView.removeArrangedSubview(view)
                let index = badgeCellIsShowing ? 1 : 0
                stackView.insertArrangedSubview(view, at: index)
            }
        }
    }
    
    /// If count is non-zero we show the placeholer image with a badge count in the first
    /// user position.  If zero the placeholder is removed.
    // Note: when rewriting this we should just add an API for badging any user
    public func configureBadgePlaceholder(title: String?, image: UIImage, target: @escaping ()->Void)
    {
        let badgeCell = initPlaceHolder(image: image)
        badgeCell.imageButton.clipsToBounds = false
        badgeCell.imageButton.badgeEdgeInsets = UIEdgeInsets(top: 10, left: 0, bottom: 0, right: 4)
        badgeCell.imageButton.rx.tap.bind { _ in target() }.disposed(by: disposal)
        badgeCell.label.text = title
        self.badgeCell = badgeCell
    }
    
    /// If the badge count is non-zero show the badged placeholder.
    /// (the badge placeholder must be configured before setting this value.)
    public func setBadge(count: Int)
    {
        guard let badgeCell = badgeCell else { fatalError("no badge cell configured") }
        badgeCell.imageButton.badgeCount  = count
        showBadgePlaceholderIfNeeded()
    }
    
    private func showBadgePlaceholderIfNeeded() {
        guard let badgeCell = badgeCell else { return }
        let count = badgeCell.imageButton.badgeCount 
        if count > 0 {
            showBadgePlaceholder()
        } else {
            removeBadgePlaceholder()
        }
    }
    
    private var badgeCellIsShowing: Bool {
        if let badgeCell = badgeCell, !stackView.arrangedSubviews.isEmpty {
            return stackView.arrangedSubviews[0] == badgeCell
        }
        return false
    }
    private func showBadgePlaceholder() {
        guard let badgeCell = badgeCell else { fatalError("no badge cell configured") }
        if !badgeCellIsShowing {
            stackView.insertArrangedSubview(badgeCell, at: 0)
        }
    }
    private func removeBadgePlaceholder() {
        guard let badgeCell = badgeCell else { fatalError("no badge cell configured") }
        if badgeCellIsShowing {
            badgeCell.removeFromSuperview()
            stackView.removeArrangedSubview(badgeCell)
        }
    }
    
    public var hasUsers : Bool {
        return stackView.arrangedSubviews.count > 0
    }
    
    public func selectedUser(user: PersonData) {
        delegate?.selectedUser(user: user)
    }
}

