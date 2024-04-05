//
//  SignupPhotoAndNameViewController.swift
//  Present
//
//  Created by Kaming Li on 5/3/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import UIKit
import RxSwift

protocol SignupPhotoAndNameDelegate: class {
    func submit(photo: UIImage?, firstName: String, lastName: String)->Completable
}

class SignupPhotoAndNameViewController: PresentViewController, WorkflowStep, UITextFieldDelegate, UIScrollViewDelegate
{
    public weak var workflowDelegate: WorkflowStepDelegate?
    public weak var delegate: SignupPhotoAndNameDelegate?
    
    let requirePhoto = false
    
    var selectedImage: UIImage? {
        didSet {
            self.updatePhotoSelection()
        }
    }
    private var prefillImage: UIImage? {
        didSet {
            self.selectedImage = prefillImage
        }
    }
    
    @IBOutlet weak var scrollView: UIScrollView! {
        didSet {
            scrollView.delegate = self
        }
    }
    
    @IBOutlet weak var cameraButton: ThemeableButton! {
        didSet {
            self.cameraButton.clipsToBounds = true
            self.cameraButton.backgroundColor = UIColor.Present.Purple.Main
            self.cameraButton.roundCornersToHeight()
            self.cameraButton.addTarget { [weak self] button in
                self?.choosePhoto()
            }
        }
    }
    
    @IBOutlet weak var nextButton: ThemeableButton! {
        didSet {
            nextButton.isEnabled = false
            nextButton.backgroundColor = UIColor.Present.Button.Disabled
            nextButton.setTitleColor(.white, for: .normal)
            nextButton.roundCornersToHeight()
            nextButton.addTarget { [weak self] button in
                guard let sself = self else { return }
                sself.view.isUserInteractionEnabled = false
                sself.activityIndicator.startAnimating()
                
                if sself.requirePhoto && sself.selectedImage == nil {
                    logDebug("SignupPhoto: no photo")
                    sself.updateNextButton()
                    return
                }
                
                sself.delegate?.submit(photo: sself.selectedImage, firstName: sself.firstNameTextField.text!, lastName: sself.lastNameTextField.text!)
                    .do {
                        logDebug("SignupPhoto: nextButton: stop animating")
                        sself.activityIndicator.stopAnimating()
                    }
                    .do(onCompleted: {
                        logDebug("SignupPhoto: nextButton: wokflow step complete")
                        sself.workflowDelegate?.workflowStepComplete()
                    })
                    .do(onError: { error in
                        logDebug("SignupPhoto: nextButton: error")
                        // TODO: Indicate error here
                    })
                    .disposed(by: sself.disposal)
            }
        }
    }
    
    @IBOutlet weak var firstNameLabel: UILabel! {
        didSet {
            firstNameLabel.textColor = UIColor.Present.Purple.Main
        }
    }
    
    @IBOutlet weak var firstNameTextField: UITextField! {
        didSet {
            firstNameTextField.autocapitalizationType = .words
            firstNameTextField.autocorrectionType = .no
            firstNameTextField.delegate = self
        }
    }
    
    @IBOutlet weak var lastNameLabel: UILabel! {
        didSet {
            lastNameLabel.textColor = UIColor.Present.Purple.Main
        }
    }
    
    @IBOutlet weak var lastNameTextField: UITextField! {
        didSet {
            lastNameTextField.autocapitalizationType = .words
            lastNameTextField.autocorrectionType = .no
            lastNameTextField.delegate = self
        }
    }
    @IBOutlet weak var closeButton: ThemeableButton! {
        didSet {
            closeButton.addTarget { [weak self] _ in self?.close() }
        }
    }
    
    override func viewDidLoad()
    {
        super.viewDidLoad()
        self.delegate = self
        
        addTapToDismissKeyboard()
        installChild(viewController: activityIndicator, in: view) {
            $0.sizeToFitSuperview()
        }
        view.bringSubview(toFront: activityIndicator.view)
        
        loadProfileData()
    }
    
        // TODO: Update to Rx
    private func loadProfileData()
    {
        userManager.whenUserProfileAvailable { profile in
            logDebug("SignupPhoto: got user profile")
            if profile.hasPhoto {
                logDebug("SignupPhoto: profile photo = \(profile.photo)")
            } else {
                logDebug("SignupPhoto: No profile photo")
            }
            if profile.hasPhoto && profile.photo != "" {
                // TODO: Update image manager to Rx
                self.imageManager.getImage(
                    atURLString: profile.photo,
                    errorHandler: {
                        self.prefillImage = nil // trigger update
                        self.updateNextButton()
                },
                    successHandler: { image in
                        //self.prefillImage = nil // trigger update // TEST: no image
                        self.prefillImage = image
                        self.updateNextButton()
                })
            } else {
                self.prefillImage = nil  // trigger update
            }
            
            self.firstNameTextField.text = profile.name.first
            self.lastNameTextField.text = profile.name.last
            self.updateNextButton()
        }
    }
    
    func addTapToDismissKeyboard() {
        let tapGestureRecognizer = UITapGestureRecognizer(target: self, action: #selector(endAllEditing(_:)))
        self.view.addGestureRecognizer(tapGestureRecognizer)
    }
    
    @objc func endAllEditing(_ gestureRecognizer: UITapGestureRecognizer) {
        self.view.endEditing(true)
    }
    
    @objc private func choosePhoto()
    {
        //logEvent(.signup_view_select_photo)

        PhotoPicker.showSelfiePhotoPrompt(from: self) { [weak self] (result) in
            guard let sself = self else { return }
            
            switch result {
            case let .chosePhoto(image):
                logDebug("SignupPhoto: chose photo")
                sself.selectedImage = image
                
            case .choseToRemovePhoto:
                logError("Choosing to remove a photo is disallowed in onboarding.")
                
            case .cancelled:
                // Nothing to do.
                break
            }
        }
    }
    
    @objc func updateNextButton() {
        nextButton.isEnabled = shouldEnableNextButton()
        nextButton.backgroundColor = nextButton.isEnabled ? UIColor.Present.Button.Enabled : UIColor.Present.Button.Disabled
    }
    
    func shouldEnableNextButton() -> Bool {
        return
            // At least one name field
            !firstNameTextField.text.isEmptyOrWhitespace
            && !lastNameTextField.text.isEmptyOrWhitespace
            // and the photo, if required
            && (selectedImage != nil || !requirePhoto)
    }

    func textField(_ textField: UITextField, shouldChangeCharactersIn range: NSRange, replacementString string: String) -> Bool {
        NSObject.cancelPreviousPerformRequests(withTarget: self, selector: #selector(updateNextButton), object: nil)
        perform(#selector(updateNextButton), with: nil, afterDelay: 0.1)
        return true
    }
    
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        if textField == firstNameTextField {
            lastNameTextField.becomeFirstResponder()
        } else {
            textField.endEditing(true)
        }
        return true
    }
    
    func scrollViewDidScroll(_ scrollView: UIScrollView) {
        self.view.endEditing(true)
    }
    
    private func updatePhotoSelection()
    {
        // We have a selected image
        if let selectedImage = selectedImage {
            self.cameraButton.setImage(selectedImage, for: .normal)
        } else
        {
            self.cameraButton.setImage(#imageLiteral(resourceName: "signup-camera"), for: .normal)
        }
        updateNextButton()
    }
    
    private func close() {
        self.workflowDelegate?.workflowStepCancelWorkflow()
    }
}

extension SignupPhotoAndNameViewController: SignupPhotoAndNameDelegate
{
    func submit(photo: UIImage?, firstName: String, lastName: String)->Completable
    {
        logEvent(.signup_submit)
        
        // Update the profile info locally
        let fullName = FullName(givenName: firstName, familyName: lastName)
        userManager.fullName = fullName
        userManager.hasConfirmedFullName = true

        // TODO: Move this to PresentService
        // Wrap our put user profile call in a Completable
        return Single<Void>.create { observer in
            // Put the user profile
            logDebug("SignupPhoto: call putUserProfile")
            self.userManager.putUserProfile { response in
                switch(response) {
                case .error:
                    observer(.error(PresentServiceError.serverError))
                case .success:
                    if let photo = photo {
                        // After the profile update put the user photo
                        // TODO: Adopt Rx to clean up the chaining here
                        logDebug("SignupPhoto: putUserProfile success, call setProfilePhoto")
                        self.userManager.setProfilePhoto(
                            to: photo,
                            errorHandler:{ error in
                                observer(.error(PresentServiceError.serverError))
                            },
                            successHandler: {
                                logDebug("SignupPhoto: setProfilePhoto success")
                                observer(.success(()))
                            }
                        )
                    } else {
                        observer(.success(()))
                    }
                }
            }
            return Disposables.create()
        }.flatMap {
            // On success perform complete signup
            return self.userManager.completeSignup()
                .onSuccess { authResponse  in
                    logDebug("SignupPhoto: completeSignup returned")
                    self.userManager.setAuthorization(authResponse: authResponse)
                }
        }.asObservable().ignoreElements()
    }
}
