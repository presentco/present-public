//
//  OnboardingTextEntryContentView.swift
//  Present
//
//  Created by Dan Federman on 4/7/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import Relativity


public protocol OnboardingTextEntryContentViewDelegate: class, TextFieldTransformDelegate {
    
    func textDidChange(to text: String, in onboardingTextEntryContentView: OnboardingTextEntryContentView)
    func didPressReturnKey(in onboardingTextEntryContentView: OnboardingTextEntryContentView)
    
}


public final class OnboardingTextEntryContentView: UIView, AnimatedSubmissionCapable, KeyboardWillShowHideListener, UITextFieldDelegate, UITextViewDelegate {
    
    // MARK: Initialization
    
    public required init() {
        super.init(frame: .zero)
        
        addSubview(gradientBackgroundView)
        addSubview(headerLabel)
        addSubview(textView)
        addSubview(textField)
        addSubview(underlineView)
        addSubview(accessoryButton)
        addSubview(spinner)
        addSubview(continueButton)
        addSubview(dummyTextView)
        addSubview(skipButton)
        
        addKeyboardShowHideObservers()
        
        textView.delegate = self
        textView.isScrollEnabled = false
        textField.delegate = self
        textField.addTarget(self, action: #selector(textDidChange), for: .editingChanged)
        
        continueButton.isEnabled = false
        submissionAnimator.animatedSubmissionCapable = self
        skipButton.isHidden = true
    }
    
    public required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    // MARK: Public Properties
    
    public weak var delegate: OnboardingTextEntryContentViewDelegate?
    
    public let gradientBackgroundView = GradientView()
    
    public var inputViewType = InputViewType.textField {
        didSet {
            updateInputViewType()
        }
    }
    
    public let textView = ThemeableTextView()
    public let textField = ThemeableTextField()
    public let underlineView = UIView()
    
    public let accessoryButton = ThemeableButton()
    
    public let continueButton = ThemeableButton()
    
    public let skipButton = ThemeableButton()
    
    /// The content inset cooresponding to the current keyboard height (height is zero if keyboard is not showing).
    public var bottomContentInset: CGFloat = 0 {
        didSet {
            if oldValue != bottomContentInset {
                layoutIfVisible()
            }
        }
    }
    
    public var visibleInputView: TextInputView {
        switch inputViewType {
        case .textField:
            return textField
        case .textView:
            return  textView
        }
    }
    
    // MARK: AnimatedSubmissionCapable
    
    public let headerLabel = UILabel()
    
    public var submissionViewsToAnimate: [UIView] {
        return [textView, textField, underlineView, accessoryButton, skipButton]
    }
    
    public let spinner = UIActivityIndicatorView(activityIndicatorStyle: .whiteLarge)
    
    public var navigationButton: UIButton {
        return continueButton
    }
    
    // MARK: KeyboardWillShowHideListener
    
    public func keyboardWillShow(with animation: KeyboardAnimation) {
        animation.animate(animations: {
            self.bottomContentInset = animation.endFrame.height
        }, completion: nil)
    }
    
    public func keyboardWillHide(with animation: KeyboardAnimation) {
        animation.animate(animations: {
            self.bottomContentInset = 0.0
        }, completion: nil)
    }
    
    // MARK: UITextFieldDelegate
    
    public func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        if continueButton.isEnabled {
            delegate?.didPressReturnKey(in: self)
            
        } else {
            textField.shake()
            underlineView.shake()
        }
        
        return false
    }
    
    public func textField(_ textField: UITextField, shouldChangeCharactersIn range: NSRange, replacementString string: String) -> Bool {
        guard let delegate = delegate else {
            return true
        }
        
        let transform = delegate.transformFor(proposedReplacement: string, forCharactersIn: range, in: textField)
        
        switch transform {
        case .proposedReplacementAllowed:
            return true
            
        case .proposedReplacementDisallowed:
            textField.shake()
            return false
            
        case let .reformat(text, selectedStartOffset):
            let originalSelectionRange = textField.selectedTextRange
            textField.text = text
            textField.sendActions(for: .editingChanged)
            if let selectedTextRange = originalSelectionRange, let selectionRangePosition = textField.position(from: selectedTextRange.start, offset: selectedStartOffset) {
                let selectionRange = textField.textRange(from: selectionRangePosition, to: selectionRangePosition)
                textField.selectedTextRange = selectionRange
            }
            
            return false
        }
    }
    
    // MARK: UITextViewDelegate
    
    public func textViewShouldEndEditing(_ textView: UITextView) -> Bool {
        if !continueButton.isEnabled {
            textView.shake()
            underlineView.shake()
            
            return false
            
        } else {
            return true
        }
    }
    
    public func textViewDidEndEditing(_ textView: UITextView) {
        if continueButton.isEnabled {
            delegate?.didPressReturnKey(in: self)
        }
    }
    
    public func textView(_ textView: UITextView, shouldChangeTextIn range: NSRange, replacementText text: String) -> Bool {
        guard let delegate = delegate else {
            return true
        }
        
        let transform = delegate.transformFor(proposedReplacement: text, forCharactersIn: range, in: textView)
        
        switch transform {
        case .proposedReplacementAllowed:
            return true
            
        case .proposedReplacementDisallowed:
            textView.shake()
            return false
            
        case let .reformat(text, selectedStartOffset):
            let originalSelectionRange = textView.selectedTextRange
            textView.text = text
            if let selectedTextRange = originalSelectionRange, let selectionRangePosition = textView.position(from: selectedTextRange.start, offset: selectedStartOffset) {
                let selectionRange = textView.textRange(from: selectionRangePosition, to: selectionRangePosition)
                textView.selectedTextRange = selectionRange
            }
            
            return false
        }
    }
    
    public func textViewDidChange(_ textView: UITextView) {
        delegate?.textDidChange(to: textView.text, in: self)
    }
    
    // MARK: UIView
    
    public override func layoutSubviews() {
        super.layoutSubviews()
        
        guard bounds.size != .zero else {
            // Don't try to lay out if we haven't been sized yet.
            return
        }
        
        gradientBackgroundView.bounds = bounds
        gradientBackgroundView.middle --> middle
        
        let pixelRounder = PixelRounder(for: self)
        
        
        let textEntryWidth = bounds.width - pixelRounder.convert(2.0 * 44.0, to: self)
        headerLabel.sizeToFit(fixedWidth: textEntryWidth)
        textInputView.sizeToFit(fixedWidth: textEntryWidth)
        underlineView.bounds.size = CGSize(width: textEntryWidth, height: ScreenSize(view: self).underlineThickness)
        
        continueButton.setNavigationButtonSize(boundsWidth: bounds.width, with: pixelRounder)
        continueButton.roundedCorners = continueButton.bounds.height / 2.0
        
        skipButton.sizeToFit()
        
        headerLabel.top --> pixelRounder.convert(66.0, to: self).verticalOffset + top
        
        let inputFieldToUnderlineViewDistance: CGFloat
        switch inputViewType {
        case .textField:
            inputFieldToUnderlineViewDistance = pixelRounder.convert(26.0, to: self)
            
        case .textView:
            inputFieldToUnderlineViewDistance = pixelRounder.convert(12.0, to: self)
        }
        
        // Distribute the input field, underline, and continue button separately to prevent
        // vairable header text length (and line count) from affecting these elements.
        distributeSubviewsVertically(within: CGRect(origin: .zero, size: CGSize(width: bounds.width, height: bounds.height - bottomContentInset))) { () -> [DistributionItem] in
            ~5~ <> textInputView <> inputFieldToUnderlineViewDistance <> underlineView <> ~2~ <> continueButton <> 46.0
        }
        
        distributeSubviewsVertically(within: CGRect(x: 0.0, y: (.top |--| continueButton.bottom).height, width: bounds.width, height: (continueButton.bottom |--| .bottom).height - bottomContentInset)) { () -> [DistributionItem] in
            ~2~ <> skipButton <> ~1~
        }
        
        let rectBetweenUnderlineAndKeyboard = CGRect(x: 0.0,
                                                     y: (top |--| underlineView.bottom).height,
                                                     width: bounds.width,
                                                     height: (underlineView.bottom |--| continueButton.top).height)
        accessoryButton.sizeToFit(fixedWidth: textEntryWidth)
        distributeSubviewsVertically(within: rectBetweenUnderlineAndKeyboard) { () -> [DistributionItem] in
            ~2~ <> accessoryButton <> ~1~
        }
        
        spinner.middle --> textInputView.middle
        dummyTextView.bottom --> bottom
    }
    
    // MARK: Private Properties
    
    /// Text field used to hold onto first responder so we don't have to push the keyboard offscreen.
    private var dummyTextView = UITextView()
    
    private let submissionAnimator = SubmissionAnimator()
    
    private var textInputView: UIView {
        switch inputViewType {
        case .textField:
            return textField
        case .textView:
            return  textView
        }
    }
    
    // MARK: Public Methods
    
    public func animateUserInputSubmission(spinnerAnimatesFromRight: Bool = true) {
        // Copy textInputView attributes so the keyboard doesn't change.
        switch inputViewType {
        case .textField:
            dummyTextView.copyAttributes(from: textField)
        case .textView:
            dummyTextView.copyAttributes(from: textView)
        }
        
        // Make the dummy keyboard first responder to steal taps.
        dummyTextView.becomeFirstResponder()
        
        submissionAnimator.animateUserInputSubmission(spinnerAnimatesFromRight: spinnerAnimatesFromRight)
    }
    
    public func animateUserInputSubmissionError(errorText: String) {
        submissionAnimator.animateUserInputSubmissionError(withErrorText: errorText) {
            self.textInputView.becomeFirstResponder()
        }
    }
    
    public func showUserInputViews(animated: Bool, completion: @escaping () -> Void = {}) {
        submissionAnimator.showUserInputViews(animated: animated, completion: completion)
    }
    
    private func updateInputViewType() {
        switch inputViewType {
        case .textField:
            textView.isHidden = true
            textField.isHidden = false
            
        case .textView:
            textView.isHidden = false
            textField.isHidden = true
        }
        
        setNeedsLayout()
    }
    
    @objc
    private func textDidChange() {
        guard let text = textField.text else {
            return
        }
        
        delegate?.textDidChange(to: text, in: self)
    }
    
    // MARK: Enum
    
    public enum InputViewType {
        case textField
        case textView
    }
}


// MARK: – Theming


public extension OnboardingTextEntryContentView {
    
    public func apply_onboardingTheme() {
        gradientBackgroundView.colors = Palette.onboardingBackgroundGradient
        headerLabel.apply_onboardingHeaderTheme()
        textField.apply(theme: ThemeableTextField.Theme(textColor: Palette.blackTextColor,
                                                        placeholderTextColor: Palette.blackTextColor.withAlphaComponent(Palette.defaultLabelPlaceholderTextAlpha),
                                                        font: .presentFont(ofSize: 23.0, weight: .medium, in: self),
                                                        textAlignment: .natural))
        textView.apply(theme: ThemeableTextView.Theme(textColor: Palette.blackTextColor,
                                                      font: .presentFont(ofSize: 23.0, weight: .medium, in: self),
                                                      textAlignment: .natural))
        underlineView.backgroundColor = Palette.blackTextColor
        
        accessoryButton.apply(theme: ThemeableButton.Theme(textColor: Palette.blackTextColor,
                                                           highlightedTextColor: Palette.blackTextColor.withAlphaComponent(Palette.defaultHighlightedButtonTitleAlpha),
                                                           font: .presentFont(ofSize: 17.0, weight: .medium)))
        
        spinner.color = Palette.blackTextColor
        
        continueButton.apply_onboardingNavigationButtonTheme()
        skipButton.apply_onboardingNavigationSkipButtonTheme()
    }
    
}
