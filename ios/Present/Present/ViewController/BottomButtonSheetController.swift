//
//  BottomButtonSheetController.swift
//  Present
//
//  Created by Dan Federman on 4/26/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import Relativity


public final class BottomButtonSheetController: UIViewController, UIGestureRecognizerDelegate {
    
    // MARK: Actions
    
    public enum Action {
        case primary
        case secondary
        case cancel
    }
    
    // MARK: ViewModel
    
    public struct ViewModel {
        
        public let title: String
        public let primaryButtonTitle: String
        public let secondaryButtonTitle: String
        
    }
    
    // MARK: Public Static Methods
    
    public static func show(with viewModel: ViewModel, from viewController: UIViewController, animated: Bool = true, completion: @escaping () -> Void = {}, actionHandler: @escaping (Action) -> Void = { _ in }) {
        
        let bottomButtonSheetController = BottomButtonSheetController(actionHandler: actionHandler)
        bottomButtonSheetController.apply(viewModel: viewModel)
        bottomButtonSheetController.show(from: viewController, completion: completion)
    }
    
    // MARK: Initialization
    
    public required init(actionHandler: @escaping (Action) -> Void) {
        self.actionHandler = actionHandler
        
        super.init(nibName: nil, bundle: nil)
    }
    
    public required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    // MARK: Public Properties
    
    public var topToTitlePadding: CGFloat {
        get {
            return contentView.topToTitlePadding
        }
        set {
            contentView.topToTitlePadding = newValue
        }
    }
    
    public var titleToPrimaryButtonPadding: CGFloat {
        get {
            return contentView.titleToPrimaryButtonPadding
        }
        set {
            contentView.titleToPrimaryButtonPadding = newValue
        }
    }
    
    public var primaryButtonToSecondaryButtonPadding: CGFloat {
        get {
            return contentView.primaryButtonToSecondaryButtonPadding
        }
        set {
            contentView.primaryButtonToSecondaryButtonPadding = newValue
        }
    }
    
    public var secondaryButtonToBottomPadding: CGFloat {
        get {
            return contentView.secondaryButtonToBottomPadding
        }
        set {
            contentView.secondaryButtonToBottomPadding = newValue
        }
    }
    
    // MARK: UIGestureRecognizerDelegate
    
    public func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldReceive touch: UITouch) -> Bool {
        guard gestureRecognizer == tapGestureRecognizer else {
            return true
        }
        
        // Tapping outside of the content view should dismiss the profile view.
        return !contentView.point(inside: touch.location(in: contentView), with: nil)
    }
    
    // MARK: UIViewController
    
    public override func viewDidLoad() {
        super.viewDidLoad()
        
        view.addSubview(contentView)
        
        contentView.backgroundColor = .white
        contentView.apply_defaultTheme()
        
        contentView.primaryButton.addTarget(self, action: #selector(primaryButtonTapped), for: .touchUpInside)
        contentView.secondaryButton.addTarget(self, action: #selector(secondaryButtonTapped), for: .touchUpInside)
        
        view.addGestureRecognizer(tapGestureRecognizer)
        tapGestureRecognizer.delegate = self
        tapGestureRecognizer.addTarget(self, action: #selector(tapGestureRecognizerDidFire))
    }
    
    public override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        // Hide our view before we show ourselves.
        contentView.isHidden = true
    }
    
    public override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        
        // Content view should start off screen.
        contentView.transform = CGAffineTransform(translationX: 0.0, y: contentView.bounds.size.height)
        contentView.isHidden = false
        
        // Content view should then animate into view.
        UIView.animate(withDuration: 0.4, delay: 0.0, usingSpringWithDamping: 0.65, initialSpringVelocity: 0.2, options: .curveEaseIn, animations: {
            self.contentView.transform = .identity
        }, completion: nil)
    }
    
    public override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        
        contentView.sizeToFit()
        contentView.bottom --> .bottom
    }
    
    // MARK: Public Methods
    
    public func apply(viewModel: ViewModel) {
        contentView.titleLabel.text = viewModel.title
        contentView.primaryButton.setTitle(viewModel.primaryButtonTitle, for: .normal)
        
        contentView.secondaryButton.setTitle(viewModel.secondaryButtonTitle, for: .normal)
        contentView.secondaryButton.isHidden = viewModel.secondaryButtonTitle.count == 0
    }
    
    public func show(from viewController: UIViewController, animated: Bool = true, completion: @escaping () -> Void = {}) {
        modalTransitionStyle = .crossDissolve
        modalPresentationStyle = .overFullScreen
        viewController.present(self, animated: animated, completion: completion)
    }
    
    // MARK: Private Properties
    
    private let contentView = ContentView()
    private let tapGestureRecognizer = UITapGestureRecognizer()
    private let actionHandler: (Action) -> Void
    
    // MARK: Private Methods
    
    @objc
    private func tapGestureRecognizerDidFire() {
        dismiss(dueTo: .cancel)
    }
    
    @objc
    private func primaryButtonTapped() {
        dismiss(dueTo: .primary)
    }
    
    @objc
    private func secondaryButtonTapped() {
        dismiss(dueTo: .secondary)
    }
    
    private func dismiss(dueTo action: Action) {
        func sendContentOffscreen(completion: @escaping (Bool) -> Void) {
            // Send content view should be off screen.
            UIView.animate(withDuration: 0.2, delay: 0.0, options: .curveEaseOut, animations: {
                self.contentView.transform = CGAffineTransform(translationX: 0.0, y: self.contentView.bounds.size.height)
            }, completion: completion)
        }
        
        if isBeingPresented {
            sendContentOffscreen() { _ in
                self.dismiss(animated: false) {
                    self.actionHandler(action)
                }
            }
            
        } else {
            sendContentOffscreen() { _ in
                self.parent?.uninstallChild(viewController: self)
                self.actionHandler(action)
            }
        }
    }
    
    // MARK: ContentView
    
    private final class ContentView: UIView {
        
        // MARK: Initialization
        
        public required override init(frame: CGRect) {
            super.init(frame: frame)
            
            addSubview(titleLabel)
            addSubview(primaryButton)
            addSubview(secondaryButton)
        }
        
        public convenience init() {
            self.init(frame: .zero)
        }
        
        public required init?(coder aDecoder: NSCoder) {
            fatalError("init(coder:) has not been implemented")
        }
        
        // MARK: Public Properties
        
        public let titleLabel = UILabel()
        public let primaryButton = ThemeableButton()
        public let secondaryButton = ThemeableButton()
        
        public var topToTitlePadding: CGFloat = 32.0 {
            didSet {
                setNeedsLayout()
            }
        }
        
        public var titleToPrimaryButtonPadding: CGFloat = 20.0 {
            didSet {
                setNeedsLayout()
            }
        }
        
        public var primaryButtonToSecondaryButtonPadding: CGFloat = 15.0 {
            didSet {
                setNeedsLayout()
            }
        }
        
        public var secondaryButtonToBottomPadding: CGFloat = 25.0 {
            didSet {
                setNeedsLayout()
            }
        }
        
        // MARK: UIView
        
        public override func layoutSubviews() {
            super.layoutSubviews()
            
            titleLabel.sizeToFit(fixedWidth: labelWidth)
            primaryButton.sizeToFit()
            primaryButton.layer.cornerRadius = primaryButton.bounds.height / 2.0
            secondaryButton.sizeToFit(fixedWidth: labelWidth)
            
            if secondaryButton.isHidden {
                distributeSubviewsVertically() {
                    topToTitlePadding <> titleLabel <> titleToPrimaryButtonPadding <> primaryButton <> secondaryButtonToBottomPadding
                }
                
            } else {
                distributeSubviewsVertically() {
                    topToTitlePadding <> titleLabel <> titleToPrimaryButtonPadding <> primaryButton <> primaryButtonToSecondaryButtonPadding <> secondaryButton <> secondaryButtonToBottomPadding
                }
            }
        }
        
        public override func sizeThatFits(_ size: CGSize) -> CGSize {
            guard let superview = superview else {
                return .zero
            }
            
            let titleLabelHeight = CGRect(origin: .zero, size: titleLabel.sizeThatFits(CGSize(width: labelWidth, height: .greatestFiniteMagnitude))).insetBy(capAndBaselineOf: titleLabel.font).height
            let primaryButtonHeight = primaryButton.sizeThatFits(CGSize(width: labelWidth, height: .greatestFiniteMagnitude)).height
            let secondaryButtonHeight = secondaryButton.sizeThatFits(CGSize(width: labelWidth, height: .greatestFiniteMagnitude)).height
            
            return CGSize(
                width: superview.bounds.width,
                height: topToTitlePadding
                    + titleLabelHeight
                    + titleToPrimaryButtonPadding
                    + primaryButtonHeight
                    + (secondaryButton.isHidden ? 0.0 : primaryButtonToSecondaryButtonPadding)
                    + (secondaryButton.isHidden ? 0.0 : secondaryButtonHeight)
                    + secondaryButtonToBottomPadding
            )
        }
        
        // MARK: Theming
        
        public func apply_defaultTheme() {
            let primaryColor = UIColor(hex: 0x74_3C_CE)
            backgroundColor = primaryColor
            
            titleLabel.apply(theme: UILabel.Theme(textColor: .white, font: .presentFont(ofSize: 17, weight: .regular), textAlignment: .center))
            primaryButton.contentEdgeInsets = UIEdgeInsets(top: -15.0, left: -44.0, bottom: -15.0, right: -44.0)
            primaryButton.apply(theme: ThemeableButton.Theme(textColor: primaryColor,
                                                          highlightedTextColor: primaryColor.withAlphaComponent(Palette.defaultHighlightedButtonTitleAlpha),
                                                          backgroundColor: .white,
                                                          font: .presentFont(ofSize: 17.0, weight: .semibold)))
            let secondaryButtonTextColor = UIColor(red: 0x7D, green: 0x7E, blue: 0x80)
            secondaryButton.apply(theme: ThemeableButton.Theme(textColor: secondaryButtonTextColor,
                                                          highlightedTextColor: secondaryButtonTextColor.withAlphaComponent(Palette.defaultHighlightedButtonTitleAlpha),
                                                          font: .presentFont(ofSize: 14.0, weight: .regular)))
        }
        
        // MARK: Private Properties
        
        private var labelWidth: CGFloat {
            guard let superview = superview else {
                return 0.0
            }
            
            let inset: CGFloat = 40.0
            
            guard superview.bounds.width > inset else {
                return superview.bounds.width
            }
            
            return superview.bounds.width - inset
        }
        
    }
}
