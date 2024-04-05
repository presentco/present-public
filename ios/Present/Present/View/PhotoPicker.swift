//
//  PhotoPicker.swift
//  Present
//
//  Created by Dan Federman on 4/13/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import CropViewController

/// The single photo picker - used in create/edit circle, user profile/signup, and chat view for the case
/// of a single photo pick. The show method prompts with photo source choices and then displays the picker.
/// Currently utilizes the standard UIImagePickerController followed by TOCropViewController for the
/// case where cropping is required.
public final class PhotoPicker: NSObject, UINavigationControllerDelegate
{
    // MARK: Private Properties
    
    public enum CropMode {
        case none, square
    }
    
    private weak var presenter: UIViewController?
    public var completionHandler: ((PhotoPicker.Result) -> Void)?
    private var strongSelf: PhotoPicker?
    
    private let picker = UIImagePickerController()
    private let cropMode: CropMode
    private let defaultCameraDevice: UIImagePickerControllerCameraDevice
    private let allowRemoval: Bool
    
    // MARK: Public Static Methods

    public static func showSelfiePhotoPrompt(from presenter: UIViewController, allowRemoval: Bool = false, completionHandler: @escaping (PhotoPicker.Result) -> Void)
    {
        PhotoPicker(presenter: presenter, defaultCameraDevice: .front, cropMode: .square, allowRemoval: allowRemoval).show(completionHandler: completionHandler)
    }
    
    public static func showPhotoPrompt(
        from presenter: UIViewController, allowRemoval: Bool = false,
        cropMode: CropMode = .none, completionHandler: @escaping (PhotoPicker.Result) -> Void)
    {
        PhotoPicker(presenter: presenter, defaultCameraDevice: .rear, cropMode: cropMode, allowRemoval: allowRemoval).show(completionHandler: completionHandler)
    }
    
    // MARK: Initialization
    
    public required init(presenter: UIViewController, defaultCameraDevice: UIImagePickerControllerCameraDevice, cropMode: CropMode, allowRemoval: Bool) {
        self.presenter = presenter
        self.defaultCameraDevice = defaultCameraDevice
        self.cropMode = cropMode
        self.allowRemoval = allowRemoval
        
        super.init()
    }
    
    // MARK: Public Methods
    
    public func show(completionHandler: @escaping (PhotoPicker.Result) -> Void)
    {
        log("show photo picker")
        guard self.completionHandler == nil else {
            logError("Attempting to show add proto prompt when it is already showing")
            completionHandler(.cancelled)
            return
        }
        
        /// Keep our instance around until the completion.
        strongSelf = self
        self.completionHandler = completionHandler
        let prompt = UIAlertController(title: nil, message: nil, preferredStyle: .actionSheet)
        
        if UIImagePickerController.isCameraDeviceAvailable(.front) {
            prompt.addAction(withTitle: NSLocalizedString("TakePhotoPromptButtonTitle",
                                                          tableName: nil, bundle: .main,
                                                          value: "Take Photo",
                                                          comment: "Button title prompting the user to take a photo with the front-facing camera."),
                             handler: { _ in
                                logEvent(type: .tap, "Take Photo")
                                self.presentImagePicker(with: .camera)
            })
        }
        
        prompt.addAction(withTitle: NSLocalizedString("ChoosePhotoPromptButtonTitle",
                                                      tableName: nil, bundle: .main,
                                                      value: "Choose Photo",
                                                      comment: "Button title prompting the user to choose a photo from their camera roll."),
                         handler: { _ in
                            logEvent(type: .tap, "Choose Photo")
                            self.presentImagePicker(with: .photoLibrary)
        })
        
        if allowRemoval {
            let removeTitle = NSLocalizedString(
                "ChoosePhotoPromptRemoveExistingPhotoButtonTitle",
                tableName: nil,
                bundle: .main,
                value: "Remove Photo",
                comment: "Title of the button in the sheet appearing when a photo was previously selected allowing the user to clear it."
            )
            prompt.addAction(withTitle: removeTitle, style: .destructive) { _ in
                logEvent(type: .tap, "Remove Photo")
                self.dismiss(with: .choseToRemovePhoto)
            }
        }
        
        prompt.addAction(withTitle: NSLocalizedString("CancelAddPhotoPromptButtonTitle",
                                                      tableName: nil, bundle: .main,
                                                      value: "Cancel",
                                                      comment: "Button cancelling adding a photo."),
                         style: .cancel,
                         handler: { _ in
                            logEvent(type: .tap, "Cancel Adding Photo")
                            self.presenter?.dismiss(animated: true, completion: nil)
        })
        
        presenter?.present(prompt, animated: true, completion: nil)
    }
    
    // MARK: Private Methods
    
    public func presentImagePicker(with sourceType: UIImagePickerControllerSourceType)
    {
        picker.sourceType = sourceType
        picker.allowsEditing = false

        switch sourceType {
        case .camera:
            picker.cameraDevice = defaultCameraDevice
        case .photoLibrary, .savedPhotosAlbum:
            // Nothing to do here.
            break
        }
        
        picker.delegate = self
        presenter?.present(picker, animated: true, completion: nil)
    }
    
    private func dismiss(with result: Result) {
        completionHandler?(result)
        
        presenter?.dismiss(animated: true, completion: {
            self.presenter?.setNeedsStatusBarAppearanceUpdate()
            self.completionHandler = nil
            self.strongSelf = nil
        })
    }
    
    // MARK: Public Classes
    
    public enum Result {
        case chosePhoto(UIImage)
        case choseToRemovePhoto
        case cancelled
    }
}

extension PhotoPicker: UIImagePickerControllerDelegate
{
    // MARK: UIImagePickerControllerDelegate
    
    public func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [String : Any])
    {
        guard let image = info[UIImagePickerControllerOriginalImage] as? UIImage else {
            imagePickerControllerDidCancel(picker)
            return
        }
        
        switch cropMode {
            case .none:
                return dismiss(with: .chosePhoto(image))
            case .square:
                break
        }
        
        // Crop the picked photo
        let cropViewController = CropViewController(image: image)
        cropViewController.delegate = self
        cropViewController.aspectRatioPreset = .presetSquare;
        cropViewController.aspectRatioLockEnabled = true
        //cropViewController.resetAspectRatioEnabled = false
        cropViewController.aspectRatioPickerButtonHidden = true
        picker.present(cropViewController, animated: true, completion: nil)
    }
    
    public func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
        dismiss(with: .cancelled)
    }
}

extension PhotoPicker: CropViewControllerDelegate
{
    public func cropViewController(_ cropViewController: CropViewController, didCropToImage image: UIImage, withRect cropRect: CGRect, angle: Int)
    {
        dismiss(with: .chosePhoto(image))
    }
    public func cropViewController(_ cropViewController: CropViewController, didFinishCancelled cancelled: Bool) {
        dismiss(with: .cancelled)
    }
}

