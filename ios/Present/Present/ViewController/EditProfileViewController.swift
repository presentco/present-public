//
//  EditProfileViewController.swift
//  Present
//
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import MIBadgeButton_Swift
import PresentProtos
import RxSwift
import Then
import Tabman
import Pageboy

public class EditProfileViewController: PresentViewController
{
    // All content is within the scrollview to allow us to acccomodate the keyboard
    @IBOutlet weak var contentScrollView: UIScrollView!

    @IBOutlet weak var profileInfoContainer: UIView! 
        
    @IBOutlet weak var navBar: NavBar! {
        didSet {
            navBar.do {
                $0.title.text = "Edit Profile"
                $0.cancelButton.isShown = true
                $0.cancelButton.addTarget { [weak self] _ in self?.screenPresenter.goBack() }
                $0.saveButton.isShown = true
                $0.saveButton.addTarget { [weak self] _ in self?.saveProfile() }
            }
        }
    }
    
    @IBOutlet weak var profileImageView: ThemeableButton! {
        didSet {
            profileImageView.backgroundColor = Palette.avatarLoadingColor
            profileImageView.addTarget { [weak self] _ in self?.choosePhoto() }
            profileImageView.roundCornersToHeight()
            profileImageView.clipsToBounds = true
        }
    }
    
    @IBOutlet weak var bioTextView: ThemeableTextView! {
        didSet {
            bioTextView.isScrollEnabled = false
            bioTextView.dataDetectorTypes = .all
            bioTextView.autocapitalizationType = .sentences
            bioTextView.returnKeyType = .default
            bioTextView.delegate = self
            
            // Create the placeholder text
            let placeholderText  = UITextView().then {
                $0.text = "Bio" // Localize
                $0.font = bioTextView.font
                $0.textColor = UIColor(hex: 0xBDBDBD)
                $0.backgroundColor = .clear
                $0.isUserInteractionEnabled = false
                bioTextView.superview?.addSubview($0) // doesn't work as a subview?
                $0.constrainToFill(view: bioTextView)
            }
            bioTextView.rx.text.bind { text in
                placeholderText.alpha = text.isEmpty ? 1.0 : 0.0
            }.disposed(by: disposal)
        }
    }
    
    @IBOutlet weak var givenNameField: ThemeableTextField! {
        didSet {
            givenNameField.placeholder = "First Name"
            themeNameField(field: givenNameField)
        }
    }
    @IBOutlet weak var familyNameField: ThemeableTextField! {
        didSet {
            familyNameField.placeholder = "Last Name"
            themeNameField(field: familyNameField)
        }
    }
    
    private var user: Person?
    
    // MARK: ViewController
    
    override public func viewDidLoad()
    {
        super.viewDidLoad()
        
        addKeyboardShowHideObservers()
        
        // Note: these require other fields to be loaded so do them here
        givenNameField.rx.text.bind { text in self.updateSaveButton() }.disposed(by: disposal)
        familyNameField.rx.text.bind { text in self.updateSaveButton() }.disposed(by: disposal)

        // Only take one, we don't want our own updates fed back to us.
        userManager.me.observable.take(1).onNext { [weak self] user in
            self?.apply(user: user)
        }.disposed(by: disposal)
        
        updateSaveButton()
    }
    
    override public func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
    }
    
    // MARK: ProfileViewControllerConfig
    
    // Note: In the isMe case this may be called before the view appears.
    public func apply(user: Person)
    {
        //log("edit apply user: \(user.name), \(user.bio)")
        self.user = user

        imageManager.getImage(atURL: user.photoURL).onSuccess { [weak self] urlimage in
            self?.profileImageView.setImage(urlimage.image, for: .normal)
        }.disposed(by: disposal)

        self.givenNameField.text = user.nameData.givenName
        self.familyNameField.text = user.nameData.familyName
        self.bioTextView.text = user.bio
        updateSaveButton()
    }
    
    private var nameValid: Bool {
        return !givenNameField.text.isEmptyOrWhitespace
            && !familyNameField.text.isEmptyOrWhitespace
    }
    
    private var nameChanged: Bool {
        return userManager.fullName?.givenName != givenNameField.text
            || userManager.fullName?.familyName != familyNameField.text
    }
    private var bioChanged: Bool {
        return bioTextView.text != userManager.personalDescription
    }
    
    // enable the save button on change
    private func updateSaveButton() {
        guard navBar?.saveButton != nil else { return }
        navBar.saveButton.isEnabled = (nameChanged || bioChanged) && nameValid
    }
    
    private func themeNameField(field: ThemeableTextField) {
        field.autocapitalizationType = .words
        field.keyboardType = .asciiCapable
        field.autocorrectionType = .no
        field.returnKeyType = .done
        field.delegate = self
    }
    
    private func saveProfile()
    {
        // TODO: Activity spinner
        let previousName = userManager.fullName
        let previousBio = userManager.personalDescription
        
        let fullName = FullName(
            givenName: givenNameField.text?.trimmingWhitespace() ?? "",
            familyName: familyNameField.text?.trimmingWhitespace() ?? "")
        userManager.fullName = fullName
        userManager.personalDescription = bioTextView.text.trimmingWhitespace()
        
        startActivity()
        userManager.putUserProfile { [weak self] (response) in
            switch response {
                case .error:
                    self?.endActivity()
                    logError("error: \(response)")
                    self?.userManager.fullName = previousName
                    self?.userManager.personalDescription = previousBio
                    if let sself = self {
                        UIAlertController.showAcknowledgeAlert(
                            fromViewController: sself,
                            title: "Error Saving Profile",
                            message: "Unable to update your profile")
                    }
                case .success:
                    self?.endActivity()
                    self?.screenPresenter.goBack()
                }
        }
        
    }
    
}

// MARK: Photo
extension EditProfileViewController
{
    private func choosePhoto()
    {
        PhotoPicker.showSelfiePhotoPrompt(from: self) { [weak self] (result) in
            guard let sself = self else { return }
            
            switch result {
                case let .chosePhoto(image):
                    let previousImage = sself.profileImageView.image(for: .normal)
                    
                    self?.startActivity()
                    sself.userManager.setProfilePhoto(
                        to: image,
                        errorHandler: { userDisplayableError in
                            self?.endActivity()
                            let errorTitle = "We couldn’t save your profile photo"
                            let errorDismissButtonTitle = "Ok"
                            let errorAlert = UIAlertController(title: errorTitle, message: userDisplayableError, preferredStyle: .alert)
                            errorAlert.addAction(withTitle: errorDismissButtonTitle) { _ in
                                guard let sself = self else { return }
                                sself.profileImageView.apply(viewModel: ThemeableButton.ViewModel(title: .none, image: previousImage))
                            }
                            self?.present(errorAlert, animated: true, completion: nil)
                        },
                        successHandler: {
                            self?.endActivity()
                        }
                    )
    
                    sself.profileImageView.apply(viewModel: ThemeableButton.ViewModel(title: .none, image: image))

                case .choseToRemovePhoto:
                    logError("No photo could have previously been selected.")
    
                case .cancelled:
                    break
            }
        }
    }
}

// MARK: UITextFieldDelegate (name fields)

extension EditProfileViewController: UITextFieldDelegate
{
    public func textField(_ textField: UITextField, shouldChangeCharactersIn range: NSRange, replacementString string: String) -> Bool
    {
        // Prevent the user from deleting a name fully
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
    
    public func textFieldDidEndEditing(_ textField: UITextField)
    {
        textField.text = textField.text?.trimmingWhitespace()
        updateSaveButton()
    }
}

// MARK: UITextViewDelegate (bio field)

extension EditProfileViewController: UITextViewDelegate
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
        if textView == bioTextView && textView.text.isEmpty {
            textView.text = userManager.personalDescription
        }
        
        return true
    }
    
    public func textViewDidEndEditing(_ textView: UITextView)
    {
        if textView == bioTextView {
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

extension EditProfileViewController: KeyboardWillShowHideListener
{
    public func keyboardWillShow(with animation: KeyboardAnimation)
    {
        main {
            // Add content insets to teh scroll view to make space for the keyboard
            let insets = UIEdgeInsets(top: 0, left: 0, bottom: animation.endFrame.height, right: 0)
            self.contentScrollView.contentInset = insets
            self.contentScrollView.scrollIndicatorInsets = insets
            
            // TODO: Scroll the focused text field into view
            /*
            if self.descriptionField.isFirstResponder {
                self.contentScrollView.scrollRectToVisible(
                    self.descriptionField.convert(self.descriptionField.frame, to: self.contentScrollView), animated: true)
            }*/
        }
    }
    
    public func keyboardWillHide(with animation: KeyboardAnimation) {
        main {
            self.contentScrollView.contentInset = UIEdgeInsets.zero
            self.contentScrollView.scrollIndicatorInsets = UIEdgeInsets.zero
        }
    }
}

