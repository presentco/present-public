//
//  MessagesViewController.swift
//  Present
//
//  Copyright © 2016 Present Company. All rights reserved.
//

import AVFoundation
import AVKit
import ImagePickerSheetController
import JSQMessages
import NYTPhotoViewer
import Photos
import Relativity
import Then
import UIKit
import PresentProtos

/**
 This is the scrolling message list component used in group chat and direct messaging.
 */
public final class MessagesViewController: JSQMessagesViewController, JSQMessagesCollectionViewCellDelegate, JSQMessagesCellURLHandlingDelegate, GroupMessagesViewControllerModelDelegate, NYTPhotosViewControllerDelegate, UIImagePickerControllerDelegate, UINavigationControllerDelegate, KeyboardWillShowHideListener, LegacyApplicationServices
{
    
    // MARK: Private Properties
    
    private let userManager: UserManager
    //private let urlResolvingService: URLResolvingService
    
    /// The JSQMessages view model.
    private var jsqMessages = [JSQMessageModel]()
    
    /// A logical model that wraps the group messages.
    // This class updates the jsq view model via the delegate.
    public var model: GroupMessagesViewControllerModel? {
        didSet {
            model?.delegate = self
            initReportMenuItem()
        }
    }

    // JSQMessages avatar image models. ImageManager caches the underlying URL data.
    private var userPhotoURLToAvatarImageMap = [URL : JSQMessagesAvatarImage]()
    
    /// A map of photo URLs to completion handlers to execute once the photo at that URL is loaded.
    private var userPhotoURLToAvatarImageLoadingCompletionHandlersMap = [URL : [() -> Void]]()
    
    // The original height of the toolbar
    private var inputToolbarDefaultHeight: CGFloat?
    
    // Maps of media items within the currently composed message text.
    // This currently holds UIImage and VideoItem types
    private var textAttachmentToUploadableMessageMap = [NSTextAttachment: UploadableMessage]()
    
    private let joinToCommentButton = ThemeableButton()
    private let joinToCommentGradientView = GradientView.clearToWhite(endY: 0.5)
    private let joinToCommentContainerView = ClearView()
    
    var emptyChatText = UITextView() // Empty chat placeholder text
    var emptyChatShareLinkButton = UIButton() // Empty chat placeholder share button
    
    private let group: Group
    
    // MARK: Private Static Properties
    
    private static let avatarImageSize: UInt = 35
    private static let messagesLeftRightMargin: CGFloat = 12
    private static let messagesTopBottomMargin: CGFloat = 12
    private static let inlineImageMaxDimension: CGFloat = 64 // aspect fit
    
    private static let incomingMessageBubble = JSQMessagesBubbleImageFactory().incomingMessagesBubbleImage(with: Palette.incomingBalloonColor)
    private static let outgoingMessageBubble = JSQMessagesBubbleImageFactory().outgoingMessagesBubbleImage(with: Palette.outgoingBalloonColor)
    private static let placeholderAvatarImage = JSQMessagesAvatarImage(placeholder: #imageLiteral(resourceName: "user-woman-512"))
    
    // MARK: Initialization
    
    public required init(userManager: UserManager, group: Group)
    {
        self.userManager = userManager
        self.group = group
        super.init()
    }
    
    public required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    deinit {
        if isViewLoaded {
            collectionView.removeObserver(self, forKeyPath: "contentSize", context: nil)
        }
    }
    
    // MARK: Public Properties
    
    public weak var delegate: MessagesViewControllerDelegate?
    public weak var scrollDelegate: MessagesViewControllerScrollDelegate?
    
    public var hasContent: Bool {
        return jsqMessages.count > 0
    }
    
    public var membershipStatus: Present.GroupMembershipState = .none {
        didSet {
            updateJoinButtonAppearance()
        }
    }
    
    public var inputField: JSQMessagesComposerTextView {
        return inputToolbar.contentView.textView
    }
    
    // MARK: JSQMessagesCollectionViewDataSource
    
    public override func collectionView(_ collectionView: JSQMessagesCollectionView?, messageDataForItemAt indexPath: IndexPath!) -> JSQMessageData? {
        return messageForIndexPath(indexPath)
    }
    
    public override func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        // Invoke the JSQ default functionality.
        let cell = super.collectionView(collectionView, cellForItemAt: indexPath) as! JSQMessagesCollectionViewCell
        
        if let textView = cell.textView {
            textView.textColor = Palette.messageTextColor
            textView.linkTextAttributes = [ NSAttributedStringKey.foregroundColor.rawValue : Palette.messageTextColor,
                                            NSAttributedStringKey.underlineStyle.rawValue : NSUnderlineStyle.styleSingle.rawValue | NSUnderlineStyle.patternSolid.rawValue ]
            
            textView.urlHandlingDelegate = self
        }
        
        cell.delegate = self
        
        return cell
    }
    
    public override func collectionView(_ collectionView: JSQMessagesCollectionView?, messageBubbleImageDataForItemAt indexPath: IndexPath!) -> JSQMessageBubbleImageDataSource? {
        let message = messageForIndexPath(indexPath)
        
        return message.isMine ? MessagesViewController.outgoingMessageBubble : MessagesViewController.incomingMessageBubble
    }
    
    /// cell top label (centered by default)
    public override func collectionView(_ collectionView: JSQMessagesCollectionView?, attributedTextForCellTopLabelAt indexPath: IndexPath!) -> NSAttributedString? {
        // Show date
        let message = messageForIndexPath(indexPath)
        guard message.showDate else {
            return nil // minor dup of logic with height getter
        }
        
        return NSMutableAttributedString(string: Date.shortMonthFullDayYearDateFormatter.string(from: message.date))
    }
    
    /// avatar image for index
    // This is a little awkward: JSQMessages associates avatar images via this delegate method.
    // There does not seem to be a way to update the image asynchronously other than to reload the
    // entire view, so we do our best to pre-cache them on startup.
    public override func collectionView(_ collectionView: JSQMessagesCollectionView?, avatarImageDataForItemAt indexPath: IndexPath!) -> JSQMessageAvatarImageDataSource? {
        let message = messageForIndexPath(indexPath)
        if message.failedToSend {
            let avatarImage = MessagesViewController.placeholderAvatarImage?.copy() as! JSQMessagesAvatarImage
            avatarImage.avatarImage = #imageLiteral(resourceName: "errorGlyph")
            return avatarImage
            
        } else if let avatarImageDataSource = userPhotoURLToAvatarImageMap[message.author.photoURL] {
            if avatarImageDataSource.avatarImage == nil {
                loadAvatarImage(at: message.author.photoURL) { [weak collectionView] in
                    collectionView?.reloadItems(at: [indexPath])
                }
            }
            
            return avatarImageDataSource
            
        } else {
            loadAvatarImage(at: message.author.photoURL) { [weak collectionView] in
                collectionView?.reloadItems(at: [indexPath])
            }
            
            return MessagesViewController.placeholderAvatarImage
        }
    }
    
    /// bubble top label (aligned with bubble text or image start by default)
    public override func collectionView(_ collectionView: JSQMessagesCollectionView?, attributedTextForMessageBubbleTopLabelAt indexPath: IndexPath!) -> NSAttributedString?
    {
        // Show user name
        let message = messageForIndexPath(indexPath)
        // minor dup of logic with height getter
        if message.isMine || !message.showUsername { return nil }
        
        return NSAttributedString(string: message.senderDisplayName ?? "")
    }
    
    /// cell bottom label (fully justified left/right e.g. with avatar image)
    public override func collectionView(_ collectionView: JSQMessagesCollectionView?, attributedTextForCellBottomLabelAt indexPath: IndexPath!) -> NSAttributedString? {
        let message = messageForIndexPath(indexPath)
        if message.failedToSend {
            return NSMutableAttributedString(string: NSLocalizedString("MessagesViewControllerNotDeliveredAccessoryText",
                                                                       tableName: nil, bundle: .main,
                                                                       value: "Not Delivered",
                                                                       comment: "Accessory text for a message that the user attempted to send but was not delivered."),
                                             attributes: [
                                                NSAttributedStringKey.foregroundColor : UIColor(red: 0xe3, green: 0x4d, blue: 0x52),
                                                NSAttributedStringKey.font : UIFont.presentFont(ofSize: 11.0, weight: .medium)
                ])
        
        } else if message.showTimestamp {
            return NSMutableAttributedString(string: Date.hourMinuteTimeFormatter.string(from: message.date))
            
        } else {
            return nil
        }
    }
    
    public override func collectionView(_ collectionView: JSQMessagesCollectionView?, didDeleteMessageAt indexPath: IndexPath?) {
        guard let indexPath = indexPath else {
            return
        }
        
        let message = messageForIndexPath(indexPath)
        
        // Note: We have to remove the item from messages before returning from this call.
        // TODO: If we want to block completion of the operation until the server responds
        // TODO: we could avoid the built-in delete operation here and implement our own operation
        // TODO: as we do for report abuse. My only concern with that is that we would lose any
        // TODO: built-in GUI feedback such as animating the delete and perhaps built-in bulk delete
        // TODO: operations that we may want in the future.
        jsqMessages.remove(at: indexPath.item)
        
        // TODO:(dan) Error/Completion handling?
        model?.delete(message: message, errorHandler: {_ in}, successHandler: {})
        
        // If it was failed, remove the persisted version
        userManager.groupManager.removeFromPersistedFailedMessages(messageIds: [message.messageId])

        collectionView?.reloadData()
    }
    
    // MARK: JSQMessagesCollectionViewDelegateFlowLayout
    
    // cell top label height
    public override func collectionView(_ collectionView: JSQMessagesCollectionView?, layout collectionViewLayout: JSQMessagesCollectionViewFlowLayout!, heightForCellTopLabelAt indexPath: IndexPath!) -> CGFloat {
        // Show date?
        let message = messageForIndexPath(indexPath)
        return message.showDate ? kJSQMessagesCollectionViewCellLabelHeightDefault : 0
    }
    
    // bubble top label height
    public override func collectionView(_ collectionView: JSQMessagesCollectionView?, layout collectionViewLayout: JSQMessagesCollectionViewFlowLayout?, heightForMessageBubbleTopLabelAt indexPath: IndexPath!) -> CGFloat {
        // Show user name?
        let message = messageForIndexPath(indexPath)
        return !message.isMine && message.showUsername ? kJSQMessagesCollectionViewCellLabelHeightDefault : 0
    }
    
    // cell bottom label height
    public override func collectionView(_ collectionView: JSQMessagesCollectionView?, layout collectionViewLayout: JSQMessagesCollectionViewFlowLayout?, heightForCellBottomLabelAt indexPath: IndexPath!) -> CGFloat {
        if self.collectionView(collectionView, attributedTextForCellBottomLabelAt: indexPath) != nil {
            return kJSQMessagesCollectionViewCellLabelHeightDefault
        } else {
            return 0.0
        }
    }
    
    // MARK: JSQMessagesCollectionViewCellDelegate
    
    public func messagesCollectionViewCellDidTapAvatar(_ cell: JSQMessagesCollectionViewCell!) {
        guard let cell = cell, let indexPathOfCell = collectionView.indexPath(for: cell), !showErrorSheetIfNecessary(forMessageAt: indexPathOfCell) else {
            return
        }
        guard let model = model, let screenPresenter = model.activityScreenPresenter else { return }
        
        let messageForCell = messageForIndexPath(indexPathOfCell)
        guard !messageForCell.author.isMe else { return }

        logEvent(.circle_chat_tap_user)
        let person = messageForCell.author
        screenPresenter.presentPersonProfile(forPerson: person)
    }
    
    public func messagesCollectionViewCellDidTapMessageBubble(_ cell: JSQMessagesCollectionViewCell!) {
        guard let indexPath = collectionView?.indexPath(for: cell) else {
            return
        }
        
        showErrorSheetIfNecessary(forMessageAt: indexPath)
        
        let tappedMessage = messageForIndexPath(indexPath)
        
        // Pass the view of the message cell for use by the photo viewer zoom in/out
        // TODO: I think we should pass this in for all visible media messages on the screen
        // TODO: or alt, provide a callback for looking them up.
        //let cell = super.collectionView(collectionView, cellForItemAtIndexPath: indexPath) as! JSQMessagesCollectionViewCell
        //let tappedMediaView = cell.mediaView
        let tappedMediaView : UIView? = nil
        
        guard let tappedMessageAttachment = tappedMessage.attachment else {
            return
        }
        
        if let tappedPhotoAttachment = tappedMessageAttachment.asPhoto {
            // Show the images for comments that we have loaded.
            var allPhotos = [PhotoModel]()
            let initialPhoto = PhotoModel(photoAttachment: tappedPhotoAttachment, uploader: tappedMessage.author)
            for message in jsqMessages {
                if let photoAttachment = message.attachment?.asPhoto {
                    if photoAttachment == tappedPhotoAttachment {
                        allPhotos.append(initialPhoto)
                    } else {
                        allPhotos.append(PhotoModel(photoAttachment: photoAttachment, uploader: message.author))
                    }
                }
            }
            
            show(photos: allPhotos, initialPhoto: initialPhoto, tappedMediaView: tappedMediaView)
            
        } else if let tappedMovie = tappedMessageAttachment.asMovie {
            let videoPlayer = AVPlayer(url: tappedMovie.source)
            let videoPlayerViewController = AVPlayerViewController()
            videoPlayerViewController.player = videoPlayer
            present(videoPlayerViewController, animated: true) {
                videoPlayer.play()
            }
        }
    }
    
    public func messagesCollectionViewCellDidTap(_ cell: JSQMessagesCollectionViewCell!, atPosition position: CGPoint) {
        // Nothing to do.
    }
    
    public func messagesCollectionViewCell(_ cell: JSQMessagesCollectionViewCell!, didPerformAction action: Selector!, withSender sender: Any!) {
        guard let indexPath = collectionView.indexPath(for: cell) else {
            logError("Could not find index path for cell to perform action \(action)")
            return
        }
        
        let message = messageForIndexPath(indexPath)
        logDebug("Perform action: \(action) \(message)")
        
        if action == #selector(delete(_:)) {
            self.collectionView(collectionView, didDeleteMessageAt: indexPath)
            
        } else if action == #selector(reportMessage(_:)) {
            func reportMessage(asBeing reason: Group.AbuseReason) {
                self.model?.reportAbusive(message: message, for: reason)
                
                let didReportAbuseAlert = UIAlertController(title: NSLocalizedString(
                    "MessagesViewControllerReportedAbusiveCommentAlertTitle",
                    tableName: nil,
                    bundle: .main,
                    value: "Thanks! We’ll look into it ASAP.",
                    comment: "Title for alert confirming that the user just reported a comment as abuse"
                ), message: nil, preferredStyle: .alert)
                
                didReportAbuseAlert.addAction(withTitle: NSLocalizedString(
                    "MessagesViewControllerReportedAbusiveCommentOkayButtonTitle",
                    tableName: nil,
                    bundle: .main,
                    value: "Close",
                    comment: "Title for button dismissing alert confirming that the user just reported a comment as abuse"
                ))
                
                present(didReportAbuseAlert, animated: true)
            }
            
            let reportMessageTitle = NSLocalizedString(
                "MessagesViewControllerReportAbusiveCommentSheetTitle",
                tableName: nil,
                bundle: .main,
                value: "Tell us why you’re reporting this comment so we can help",
                comment: "Title for sheet asking the user why they are reporting a comment as abusive"
            )
            
            let reportAbuseSheet = UIAlertController(title: reportMessageTitle, message: nil, preferredStyle: .actionSheet)
            
            let inappropriateButtonTitle = NSLocalizedString(
                "MessagesViewControllerReportAbusiveCommentSheetInapropriateButtonTitle",
                tableName: nil,
                bundle: .main,
                value: "It’s inappropriate",
                comment: "Title for button that will report a comment as abusive due to being inappropriate"
            )
            
            reportAbuseSheet.addAction(withTitle: inappropriateButtonTitle, style: .destructive, handler: { (_) in
                reportMessage(asBeing: .inappropriate)
            })
            
            let spamButtonTitle = NSLocalizedString(
                "MessagesViewControllerReportAbusiveCommentSheetSpamButtonTitle",
                tableName: nil,
                bundle: .main,
                value: "It’s spam",
                comment: "Title for button that will report a comment as abusive due to being spam"
            )
            
            reportAbuseSheet.addAction(withTitle: spamButtonTitle, style: .destructive, handler: { (_) in
                reportMessage(asBeing: .spam)
            })
            
            let cancelButtonTitle = NSLocalizedString(
                "MessagesViewControllerReportAbusiveCommentSheetCancelButtonTitle",
                tableName: nil,
                bundle: .main,
                value: "Cancel",
                comment: "Title for button that cancel reporting a comment as abusive"
            )
            
            reportAbuseSheet.addAction(withTitle: cancelButtonTitle, style: .cancel)
            
            present(reportAbuseSheet, animated: true)
        }
    }
    
    // MARK: JSQMessagesCellURLHandlingDelegate
    
    public func messagesCellTextView(_ textView: JSQMessagesCellTextView!, shouldInteractWith URL: URL!, in characterRange: NSRange, interaction: UITextItemInteraction) -> Bool
    {
        switch interaction {
            case .invokeDefaultAction:
                URLManager.shared.dispatchUrl(url: URL)
                return false
            
            case .preview, .presentActions:
                return false
            }
    }
    
    // MARK: JSQMessagesViewController
    
    public override func didPressSend(
        _ button: UIButton?,
        withMessageText rawText: String?,
        senderId: String?,
        senderDisplayName: String?,
        date: Date?)
    {
        guard let rawText = rawText else { return }

        send(message: .text(rawText))
    }
    
    public override func didPressAccessoryButton(_ sender: UIButton?) {
        guard let sender = sender else {
            return
        }
        
        presentImagePickerSheet(sender)
    }
    
    // MARK: MessageListener
    
    /// Set message model and reload the view.
    public func setMessages(_ messages: [JSQMessageModel])
    {
        logx("setMessages: \(messages)")
        
        guard messages != self.jsqMessages, let collectionView = self.collectionView else {
            updateShowEmptyChatPlaceholder()
            return
        }
        
        let oldMessages = self.jsqMessages
        self.jsqMessages = messages
        MessageMetadataDisplayStrategy().annotateMessages(messages)
        loadAvatarImages()
        updateShowEmptyChatPlaceholder()
        
        guard isViewLoaded else { return }  // ug
        
        let topOfVisibleContent = collectionView.contentSize.height - collectionView.contentOffset.y
        let visibleContentHeight = (collectionView.bounds.height - collectionView.contentInset.top - collectionView.contentInset.bottom)
        let isPinnedToBottom = (topOfVisibleContent - visibleContentHeight) == 0
        
        collectionView.reloadData()
        collectionView.layoutIfVisible()
        
        if isPinnedToBottom {
            scrollToBottom(animated: false)
        }
        
        if oldMessages.isEmpty {
            // We're setting content for the first time. Make sure that we're scrolled to the bottom.
            let collectionViewVisibleHeight = collectionView.bounds.height - collectionView.contentInset.top - collectionView.contentInset.bottom
            if collectionView.contentSize.height > collectionViewVisibleHeight {
                let topOfVisibleArea = collectionView.contentSize.height - collectionView.contentInset.top - collectionViewVisibleHeight
                collectionView.contentOffset = CGPoint(x: 0.0, y: topOfVisibleArea)
            }
        }
    }
    
    /// Add the new message to the model and animate it into the view at the end.
    public func appendIncomingMessage(_ message: JSQMessageModel)
    {
        logx("appendIncomingMessage didReceive: \(jsqMessages)")
        jsqMessages.append(message)
        MessageMetadataDisplayStrategy().annotateMessages(jsqMessages)
        loadAvatarImage(at: message.author.photoURL)
        finishReceivingMessage(animated: true)
        JSQSystemSoundPlayer.jsq_playMessageReceivedSound()
        model?.markAsRead()
        
        updateShowEmptyChatPlaceholder()
    }
    
    /// Called after sending an outoing message, this animates comment into the collection view locally.
    private func appendOutgoingMessage(sentMessage: JSQMessageModel) {
        loadAvatarImage(at: sentMessage.author.photoURL)
        logx("appendOutgoingMessage: \(sentMessage)")
        
        main {
            self.jsqMessages.append(sentMessage)
            self.finishSendingMessage(animated: true)
            
            main {
                JSQSystemSoundPlayer.jsq_playMessageSentSound()
                //self.collectionView?.reloadData()
                self.scrollToBottom(animated: true)
                self.updateShowEmptyChatPlaceholder()
            }
        }
    }
    
    
    // MARK: NYTPhotosViewControllerDelegate
    
    public func photosViewController(_ photosViewController: NYTPhotosViewController, handleActionButtonTappedFor photo: NYTPhoto) -> Bool {
        if UIDevice.current.userInterfaceIdiom == .pad {
            guard let photoImage = photo.image else {
                return false
            }
            
            let shareActivityViewController = UIActivityViewController(activityItems: [photoImage], applicationActivities: nil)
            
            shareActivityViewController.completionWithItemsHandler = {(activityType: UIActivityType?, completed:Bool, returnedItems:[Any]?, error) in
            }
            
            shareActivityViewController.completionWithItemsHandler = {
                (activityType: UIActivityType?, completed: Bool, returnedItems:[Any]?, error : Error?) in
                if completed {
                    photosViewController.delegate?.photosViewController!(photosViewController, actionCompletedWithActivityType: activityType?.rawValue)
                }
            }
            
            shareActivityViewController.popoverPresentationController?.barButtonItem = photosViewController.rightBarButtonItem
            photosViewController.present(shareActivityViewController, animated: true, completion: nil)
            
            return true
        }
        
        return false
    }
    
    public func photosViewController(_ photosViewController: NYTPhotosViewController, referenceViewFor photo: NYTPhoto) -> UIView? {
        // TODO: need the photo message bubble uiimageview here
        let photo = photo as! PhotoModel
        return photo.mediaView // May be nil. Currently set only for the tapped message.
    }
    
    public func photosViewController(_ photosViewController: NYTPhotosViewController, loadingViewFor photo: NYTPhoto) -> UIView? {
        // TODO: choose a real loading indicator
        let label = UILabel()
        label.text = "Loading\u{2026}"
        label.textColor = UIColor.green
        return label
    }
    
    public func photosViewController(_ photosViewController: NYTPhotosViewController, didNavigateTo photo: NYTPhoto, at photoIndex: UInt) {
        logDebug("Did Navigate To Photo: \(photo) identifier: \(photoIndex)")
        /// Load photos lazily
        (photo as! PhotoModel).load(into: photosViewController)
    }
    
    public func photosViewController(_ photosViewController: NYTPhotosViewController, actionCompletedWithActivityType activityType: String?) {
        logDebug("Action Completed With Activity Type: \(String(describing: activityType))")
    }

    public func photosViewControllerDidDismiss(_ photosViewController: NYTPhotosViewController)
    {
        logDebug("MessagesViewController: Did dismiss Photo Viewer: \(photosViewController)")
        
        /*
        if #available(iOS 11.0, *) {
            logDebug("MessagesViewController: safe areas = \(view.safeAreaInsets), \(view.safeAreaLayoutGuide)")
        }*/
        //self.view.setNeedsUpdateConstraints()
        //self.view.updateConstraintsIfNeeded()
        //self.view.setNeedsLayout()
        //self.view.layoutIfNeeded()
        
        // TODO: Issue: After dismissing the NYTPhotoViewer the iOS safeAreaInsets are broken
        // TODO: on iPhone 7/8 size classes.  They lose the 20px safe area for the status bar.
        // TODO: Attempting to refresh the layout and constraints does not help.
        // TODO: However it seems that showing another view controller and dismissing it does
        // TODO: cause the safe areas to be reset.  So as a workaround we are showing and
        // TODO: dismissing a non-animated, non-visible view controller.
        // TODO: Try removing this and changing the presentation style for NYTPhotoViewer back
        // TODO: to animated after updating to NYTPhotoViewer 2.0 (which currently has unrelated
        // TODO: crashing bugs preventing us from adopting it.)
        let vc = UIViewController()
        vc.view.alpha = 0.0
        vc.modalTransitionStyle = .crossDissolve
        vc.modalPresentationStyle = .overCurrentContext
        self.present(vc, animated: false) {
            vc.dismiss(animated: false)
        }
    }
    
    // MARK: UICollectionViewDataSource
    
    public override func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return jsqMessages.count
    }
    
    // MARK: UICollectionViewDelegate
    
    // Show menu on/off for message
    public override func collectionView(_ collectionView: UICollectionView, shouldShowMenuForItemAt indexPath: IndexPath) -> Bool {
        // super disables text selection temporarily
        super.collectionView(collectionView, shouldShowMenuForItemAt: indexPath)
        return true
    }
    
    // Can perform specific action for message
    public override func collectionView(_ collectionView: UICollectionView, canPerformAction action: Selector, forItemAt indexPath: IndexPath, withSender sender: Any?) -> Bool {
        let message = messageForIndexPath(indexPath)
        
        if action == #selector(delete(_:)) {
            return model?.canDelete(message: message) ?? false
            
        } else if action == #selector(reportMessage(_:)) {
            // We can't report our own comments.
            return !message.isMine
            
        } else {
            return super.collectionView(collectionView, canPerformAction: action, forItemAt: indexPath, withSender: sender)
        }
    }
    
    // MARK: UIScrollViewDelegate
    
    public override func scrollViewWillBeginDragging(_ scrollView: UIScrollView) {
        hideJoinToCommentButton()
        scrollDelegate?.willBeginDragging(messagesViewController: self, from: scrollView.contentOffset)
    }
    
    public override func scrollViewDidScroll(_ scrollView: UIScrollView) {
        scrollDelegate?.didScroll(messagesViewController: self, to: scrollView.contentOffset)
    }
    
    private func hideJoinToCommentButton() {
        UIView.animate(
            withDuration: 0.3,
            animations: {
                self.joinToCommentContainerView.alpha = 0
                //self.joinToCommentContainerView.transform = CGAffineTransform(scaleX: 0.2, y: 0.2)
            },
            completion: { complete in
                self.joinToCommentContainerView.isHidden = true
            }
        )
    }

    // MARK: UITextViewDelegate
    
    public override func textViewShouldBeginEditing(_ textView: UITextView) -> Bool {
        return true
    }
    
    // MARK: UIViewController
    
    public override func viewDidLoad()
    {
        super.viewDidLoad()
        
        // Enable message delete.
        JSQMessagesCollectionViewCell.registerMenuAction(#selector(delete))
        
        collectionView?.collectionViewLayout.springinessEnabled = false
        collectionView?.collectionViewLayout.sectionInset = UIEdgeInsets(top: MessagesViewController.messagesTopBottomMargin, left: MessagesViewController.messagesLeftRightMargin, bottom: MessagesViewController.messagesTopBottomMargin, right: MessagesViewController.messagesLeftRightMargin)
        
        inputToolbar.contentView.leftBarButtonItemWidth = 32
        
        senderId = userManager.me.value?.userToken ?? ""
        senderDisplayName = userManager.me.value?.name ?? ""
        userManager.whenUserAvailable { [weak self] (me) in
            self?.senderId = me.userToken
            self?.senderDisplayName = me.name
        }
        
        automaticallyScrollsToMostRecentMessage = true
        
        // Observe content size changes. Part of workaround for JSQ scroll to bottom issues.
        collectionView.addObserver(self, forKeyPath: "contentSize", options: .new, context: nil)
        

        let inputFieldFont = UIFont.presentFont(ofSize: 16.0, weight: .regular)
        inputField.layer.borderWidth = 0.0
        inputField.font = inputFieldFont
        
        inputField.textColor = .black
        inputField.tintColor = .black
        //inputField.placeHolderTextColor = .black // per new design
        
        // Remove the default background and border on the input toolbar
        // Note: setting to an empty image (vs nil) hides the (inaccessible) image view background
        inputToolbar.setShadowImage(UIImage(), forToolbarPosition: .bottom)
        inputToolbar.setBackgroundImage(UIImage(), forToolbarPosition: .bottom, barMetrics: .default)
        inputToolbar.addTopSeparator(color: .lightGray) // Add our own separator

        inputField.placeHolder = NSLocalizedString("MessagesViewControllerInputFieldPlaceholderText", tableName: nil, bundle: .main, value: "Say something!", comment: "Placeholder text for the chat input field.")
        inputField.showsVerticalScrollIndicator = false
        inputField.bounces = false
        inputToolbar.contentView.backgroundColor = .white

        inputToolbar.contentView.leftBarButtonItem = ThemeableButton().then {
            $0.setImage(#imageLiteral(resourceName: "CirclePlusIcon"), for: .normal)
            $0.setImage(nil, for: .highlighted)
        }
        
        inputToolbar.contentView.rightBarButtonItem = ThemeableButton().then {
            //let sendButtonColor = UIColor(red: 0x9D, green: 0x67, blue: 0xEA)
            let sendButtonColor = UIColor.Present.Purple.Main
            $0.apply(theme: ThemeableButton.Theme(textColor: sendButtonColor,
                                                  highlightedTextColor: sendButtonColor.withAlphaComponent(Palette.defaultHighlightedButtonTitleAlpha),
                                                  font: inputFieldFont))
            $0.setTitle(NSLocalizedString("MessagesViewControllerSendButtonTitle",
                                          tableName: nil, bundle: .main,
                                          value: "Send",
                                          comment: "Title for button to send a message."), for: .normal)
        }
        
        joinToCommentButton.apply_joinToCommentAppearance()
        joinToCommentButton.layer.borderWidth = 2.0
        joinToCommentButton.layer.borderColor = UIColor.black.cgColor
        
        joinToCommentButton.addTarget { [weak self] button in
            self?.join(button: button)
        }
        joinToCommentGradientView.isUserInteractionEnabled = false
        joinToCommentContainerView.addSubview(joinToCommentGradientView)
        joinToCommentContainerView.addSubview(joinToCommentButton)
        view.addSubview(joinToCommentContainerView)
        
        updateJoinButtonAppearance()
        
        collectionView.collectionViewLayout.messageBubbleFont = .presentFont(ofSize: 16.0, weight: .regular)
        
        // Empty chat placeholder text
        view.addSubview(emptyChatText)
        let stringValue = "*Crickets*\nGet the conversation started and\ninvite friends to the circle!" // Localize
        let attrString = NSMutableAttributedString(string: stringValue)
        let style = NSMutableParagraphStyle()
        style.alignment = .center
        let range = NSRange(location: 0, length: stringValue.count)
        attrString.addAttribute(NSAttributedStringKey.paragraphStyle, value: style, range: range)
        attrString.addAttribute(NSAttributedStringKey.font, value: UIFont.presentFont(ofSize: 20, weight: .medium), range: range)
        attrString.addAttribute(NSAttributedStringKey.foregroundColor, value: UIColor.black, range: range)
        emptyChatText.attributedText = attrString
        emptyChatText.isUserInteractionEnabled = false
        
        emptyChatText.textContainerInset = UIEdgeInsets.zero
        emptyChatText.textContainer.lineFragmentPadding = 0
    
        // Empty chat share link button
        view.addSubview(emptyChatShareLinkButton)
        emptyChatShareLinkButton.applyPurpleGradientButtonStyle()
        emptyChatShareLinkButton.titleLabel?.font = UIFont.presentFont(ofSize: 17, weight: .semibold)
        emptyChatShareLinkButton.setTitle("Let’s Do It!", for: .normal) // Localize
        emptyChatShareLinkButton.setTitleColor(.white, for: .normal)
        emptyChatShareLinkButton.addTarget(self, action: #selector(doEmptyChatShareLink), for: .touchUpInside)
    
        showEmptyChatPlaceholder = false // initially hidden
        
        addKeyboardShowHideObservers()
    }
    
    public override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        
        model?.markAsRead()
        
        if let model = model, model.shouldRevealKeyboardWhenPresented {
            inputField.becomeFirstResponder()
        }
    }
    
    public override func viewDidLayoutSubviews()
    {
        super.viewDidLayoutSubviews()
        
        joinToCommentContainerView.frame.size = CGSize(width: view.bounds.width, height: 100.0)
        joinToCommentContainerView.bottom --> inputToolbar.top - 30.verticalOffset
        
        joinToCommentGradientView.sizeToFitSuperview()

        // Ug. We need to switch all of this to autolayout.
        let newLines = joinToCommentButton.titleLabel?.text?.filter{ $0=="\n" }.count ?? 0
        if newLines > 0 {
            joinToCommentButton.layout = .fixed(size: CGSize(width: 330.0, height: 66.0))
        } else {
            joinToCommentButton.layout = .fixed(size: CGSize(width: 300.0, height: 46.0))
        }
        joinToCommentButton.sizeToFit()
        joinToCommentButton.roundCornersToHeight()
        joinToCommentButton.bottom --> joinToCommentContainerView.bottom

        emptyChatText.bounds = CGRect(x:0, y:0, width: 326, height: 72)
        emptyChatText.center = CGPoint(x: view.center.x, y: view.center.y - 50.0)
        emptyChatShareLinkButton.bounds = CGRect(x:0, y:0, width: 267, height: 50)
        emptyChatShareLinkButton.top --> emptyChatText.bottom + 30.verticalOffset
        
        emptyChatShareLinkButton.roundedCorners = emptyChatShareLinkButton.bounds.height/2.0
    }
    
    // MARK: Private Methods
    
    private func messageForIndexPath(_ indexPath: IndexPath) -> JSQMessageModel {
        return jsqMessages[indexPath.item]
    }
    
    @discardableResult
    private func showErrorSheetIfNecessary(forMessageAt indexPath: IndexPath) -> Bool {
        guard let model = model else {
            logError("Attempting to retry sending message when no model exists")
            return false
        }
        
        let message = messageForIndexPath(indexPath)
        
        guard message.failedToSend else {
            return false
        }
        
        let promptTitle = NSLocalizedString("MessagesViewControllerResendMessageSheetTitle",
                                            tableName: nil, bundle: .main,
                                            value: "Message was not delivered",
                                            comment: "Title for alert sheet prompting to retry sending a message that had previously failed to send.")
        let retrySendButtonTitle = NSLocalizedString("MessagesViewControllerResendMessageButtonTitle",
                                                  tableName: nil, bundle: .main,
                                                  value: "Try Again",
                                                  comment: "Title for button that will retry sending a message that had previously failed to send.")
        let cancelButtonTitle = NSLocalizedString("MessagesViewControllerNevermindButtonTitle",
                                                  tableName: nil, bundle: .main,
                                                  value: "Cancel",
                                                  comment: "Title for button that dismiss a prompt to retry sending a previousy failed message.")
        
        
        let alertController = UIAlertController(title: nil, message: promptTitle, preferredStyle: .actionSheet)
        alertController.addAction(withTitle: retrySendButtonTitle) { _ in
            self.retrySend(messageAtIndex: indexPath)
        }
        
        alertController.addAction(withTitle: cancelButtonTitle, style: .cancel)
        
        // WORKAROUND: Resign first responder before the sheet comes up to preserve scrolling.
        inputField.resignFirstResponder()
        
        present(alertController, animated: true)
        
        return true
    }
    
    private func initReportMenuItem() {
        if let model = model, model.canReportAbuse() {
            let reportMenuItemTitle = NSLocalizedString("MessagesViewControllerReportAbuseMenuItemTitle",
                                                        tableName: nil, bundle: .main,
                                                        value: "Report…",
                                                        comment: "Title text for menu item that allows a customer to report abusive content.")
            
            // Add "report" menu item to the system.
            let reportMenuItem = UIMenuItem(title: reportMenuItemTitle, action: #selector(reportMessage(_:)))
            UIMenuController.shared.menuItems = [reportMenuItem]
            JSQMessagesCollectionViewCell.registerMenuAction(#selector(reportMessage(_:)))
            
        } else {
            // Remove "report" menu item from the system.
            UIMenuController.shared.menuItems = []
        }
    }
    
    
    private func join(button: UIButton) {
        // TODO: Get the service Single back to here and participate
        joinToCommentButton.isEnabled = false
        joinToCommentButton.layer.borderColor = UIColor.lightGray.cgColor
        delegate?.handleJoin(from: self)
    }
    
    // MARK Appearance setup
    
    private func updateJoinButtonAppearance()
    {
        if group.owner.isMe {
            joinToCommentContainerView.alpha = 0.0
            return
        }
        joinToCommentContainerView.alpha = 1.0
        joinToCommentButton.layer.borderColor = UIColor.black.cgColor
        joinToCommentButton.isEnabled = true
        joinToCommentButton.titleLabel?.numberOfLines = 0
        inputToolbarEnabled = group.userAuthorizedToParticipate
        switch membershipStatus {
            case .none, .unjoined, .invited, .rejected:
                let title = group.userAuthorizedToParticipate ?
                    "Join & Receive Updates" :
                    "This circle’s chats are protected.\nJoin this circle to see posts!" 
                joinToCommentButton.setTitle(title, for: .normal)
            case .requestedGroupMembershipState:
                joinToCommentButton.setTitle("Join Requested", for: .normal)
                joinToCommentButton.layer.borderColor = UIColor.lightGray.cgColor
                joinToCommentButton.isEnabled = false
            case .active:
                joinToCommentContainerView.alpha = 0.0
        }
        view.setNeedsLayout()
    }
    
    private var inputToolbarEnabled = true {
        didSet {
            inputToolbar.isUserInteractionEnabled = inputToolbarEnabled
            inputToolbar.contentView.leftBarButtonItem.isEnabled = inputToolbarEnabled
            // Note: The right button is managed by JSQ based on whether text is in the
            // Note: input field.
            //inputToolbar.contentView.rightBarButtonItem.isEnabled = inputToolbarEnabled
        }
    }
    
    private func updateShowEmptyChatPlaceholder() {
        showEmptyChatPlaceholder = jsqMessages.isEmpty && group.userAuthorizedToParticipate
    }
    
    private var showEmptyChatPlaceholder: Bool = true {
        didSet {
            emptyChatText.isHidden = !showEmptyChatPlaceholder
            emptyChatShareLinkButton.isHidden = !showEmptyChatPlaceholder
        }
    }

    // MARK: Private Methods – Sending Messages
    
    private func send(message: UploadableMessage)
    {
        guard userManager.authorizedToProceedToApp else {
            return applicationServices.screenPresenter.presentGetStartedOverlayViewController()
        }
        guard let model = model else { return }
        
        let progressListener = { /*[weak self]*/ (progress: Normalized<Float>) in
            log("progress: \(progress.value)")
        }
        
        // Begin sending the message and return the jsq view model for it.
        guard let sentMessage = model.send(message: message, progressListener: progressListener) else {
            log("failed to construct message for sending")
            return
        }

        // Add the new message to the model and animate it into the collection view (optimistically)
        appendOutgoingMessage(sentMessage: sentMessage)
    }
    
    private func send(uploadableAttachment: Attachment.Uploadable) {
        send(message: .attachment(uploadableAttachment))
    }
    
    private func retrySend(messageAtIndex indexPath: IndexPath)
    {
        guard let model = model else { return }
        let failedMessage = messageForIndexPath(indexPath)
        guard failedMessage.failedToSend else {
            log("attempt to resend message not in failed state");
            return
        }
        model.resend(message: failedMessage, progressListener: nil)
    }

    // MARK: Private Methods – Avatar Loading
    
    /// Async pre-cache any avatar images that have not yet been loaded.
    private func loadAvatarImages() {
        jsqMessages.forEach {
            loadAvatarImage(at: $0.author.photoURL)
        }
    }
    
    /// Load and style the regular and highlighted avatar images for the id at URL
    private func loadAvatarImage(at avatarImageURL: URL, completionHandler: @escaping () -> Void = {}) {
        guard userPhotoURLToAvatarImageMap[avatarImageURL]?.avatarImage == nil else {
            completionHandler()
            return
        }
        
        let avatarImageCacheKey = "circular:\(MessagesViewController.avatarImageSize):\(avatarImageURL.absoluteString)"
        
        if let cachedAvatarImage = ImageManager.shared.getStoredImage(forCacheKey: avatarImageCacheKey) {
            userPhotoURLToAvatarImageMap[avatarImageURL] = (MessagesViewController.placeholderAvatarImage?.copy() as! JSQMessagesAvatarImage).then {
                $0.avatarImage = cachedAvatarImage
                $0.avatarHighlightedImage = cachedAvatarImage
            }
            completionHandler()
            
        } else if let completionHandlers = userPhotoURLToAvatarImageLoadingCompletionHandlersMap[avatarImageURL] {
            userPhotoURLToAvatarImageLoadingCompletionHandlersMap[avatarImageURL] = completionHandlers + [completionHandler]
            
        } else {
            userPhotoURLToAvatarImageLoadingCompletionHandlersMap[avatarImageURL] = [completionHandler]
            ImageManager.shared.getImage(atURLString: avatarImageURL.absoluteString, successHandler: { [weak self] image in
                // Get off the main queue for image manipulation.
                DispatchQueue.global().async {
                    guard let circularImage = JSQMessagesAvatarImageFactory.circularAvatarImage(image, withDiameter: MessagesViewController.avatarImageSize) else {
                        logError("Could not create circular image for avatar image")
                        return
                    }
                    
                    // Get back on the main queue to set our image.
                    DispatchQueue.main.async {
                        ImageManager.shared.cacheImage(circularImage, forCacheKey: avatarImageCacheKey)
                        
                        self?.userPhotoURLToAvatarImageMap[avatarImageURL] = (MessagesViewController.placeholderAvatarImage?.copy() as! JSQMessagesAvatarImage).then {
                            $0.avatarImage = circularImage
                            $0.avatarHighlightedImage = circularImage
                        }
                        self?.userPhotoURLToAvatarImageLoadingCompletionHandlersMap[avatarImageURL]?.forEach { $0() }
                        self?.userPhotoURLToAvatarImageLoadingCompletionHandlersMap.removeValue(forKey: avatarImageURL)
                    }
                }
            })
        }
    }
    
    // MARK: Private Methods – Abuse Reporting
    
    // Registered as the selector for the report abuse menu option but this is not
    // caled directly. See collectionView:performAction:.
    @objc
    private func reportMessage(_ menuController: UIMenuController) {
        logDebug("\(#function) called with \(menuController)")
    }
    
    // MARK: Private Methods – Image Viewer
    
    /// - parameter photos: Photos to show.
    /// - parmeter initialPhoto: The initial photo shown (allowing scrolling forward and back as needed).
    /// - parameter tappedMediaView: The media view that was tapped.
    private func show(photos: [PhotoModel], initialPhoto: PhotoModel, tappedMediaView: UIView?) {
        // TODO: This isn't working as expected, turning off for now.
        initialPhoto.mediaView = tappedMediaView
        
        let photosViewController = NYTPhotosViewController(photos: photos, initialPhoto: initialPhoto)
        photosViewController.delegate = self
        
        // Kick off loading at least the first image.
        photos.first?.load(into: photosViewController)
        
        // For the rest, if they are already in cache start grabbing them now.
        for photo in photos.dropFirst() {
            photo.load(onDiskImageInto: photosViewController)
        }
        
        /*
        if #available(iOS 11.0, *) {
            logDebug("MessagesViewController: safe areas = \(view.safeAreaInsets), \(view.safeAreaLayoutGuide)")
        }*/
        self.present(photosViewController, animated: false) {
            logDebug("present photo viewer complete")
        }
    }
    
    // MARK: Private Methods – Image Picker
    
    var singlePhotoPickerStrongRefs = [PhotoPicker]()
    
    /// Present the image picker choice sheet (recents, camera, photo lib).
    private func presentImagePickerSheet(_ sender: AnyObject) {
        PHPhotoLibrary.requestAuthorization { (status) in
            DispatchQueue.main.async {
                switch status {
                case .authorized, .notDetermined:
                    let controller = ImagePickerSheetController(mediaType: .image)
                    controller.maximumSelection = 5
                    
                    // Note the "secondary" title and handler here are used when the user picks from the "recent" images in the list. (Why this action?)
                    controller.addAction(ImagePickerAction(
                        title: "Take Photo", // localize
                        secondaryTitle: { count in "Send \(count) Image\(count > 1 ? "s" : "")" }, // localize (and localize pluralization)
                        handler: { _ in
                            // Add option to take photo or video if a camera is available
                            guard UIImagePickerController.isCameraDeviceAvailable(.rear) else { return }
                            // This single pick does not participate in the queuing behavior.
                            self.pickSingleImage(with: .camera)
                        }, secondaryHandler: { _ , count in
                            self.userPickedMultipleImages(controller.selectedAssets)
                        }
                    ))
                    
                    // TODO: This action does not participate in the UIImageSheetController queing behavior.  (The picker does have callbacks for individual select but I don't know how much we'd have to do to turn that into multi-select in the media picker.)
                    // TODO: Seems like we should at least be able to do multiple single selects with this by adding to the controller selected asssets and having it upadate the secondary handler text below...  Don't see how to do this though.
                    // TODO: Instead we currently use the DZN photo picker and rely on the crop page as a confirmation.
                    controller.addAction(
                        ImagePickerAction(
                            title: "Media Library", // localize
                            handler: {
                                action in self.pickSingleImage(with: .photoLibrary)
                        }/*, secondaryHandler: { _ , count in
                            // unused
                        }*/
                    ))
                    
                    // Add the cancel option.
                    controller.addAction(ImagePickerAction(cancelTitle: "Cancel")) // localize
                    
                    if UIDevice.current.userInterfaceIdiom == .pad {
                        controller.modalPresentationStyle = .popover
                        controller.popoverPresentationController?.sourceView = self.view
                        controller.popoverPresentationController?.sourceRect = CGRect(origin: self.view.center, size: CGSize())
                    }
                    
                    // Show the picker sheet
                    self.present(controller, animated: true, completion: nil)
                    
                case .denied, .restricted:
                    let allowPhotosAccessAlert = UIAlertController(title: NSLocalizedString( "MessagesViewControllerNoPhotoAccessAlertTitle", tableName: nil, bundle: .main, value: "Allow Photo Access", comment: "Title for alert telling the user to allow previously disallowed access to photos"), message: nil, preferredStyle: .alert)
                    
                    allowPhotosAccessAlert.addAction(withTitle: NSLocalizedString( "MessagesViewControllerNoPhotoAccessGoToSettingsButtonTitle", tableName: nil, bundle: .main, value: "Settings", comment: "Title for button to bring the user to Settings to change their photos permissions"
                    )) { _  in
                        logEvent(type: .tap, "Allow Photos Access")
                        UIApplication.shared.open(URL(string: UIApplicationOpenSettingsURLString)!)
                    }
                    
                    allowPhotosAccessAlert.addAction(
                        withTitle: NSLocalizedString( "MessagesViewControllerNoPhotoAccessNevermindButtonTitle", tableName: nil, bundle: .main, value: "Nevermind", comment: "Title for button to dismiss a prompt to change change the user's photos permissions"),
                        style: .cancel)
                    
                    self.present(allowPhotosAccessAlert, animated: true)
                }
            }
        }
    }
    
    private func pickSingleImage(with sourceType: UIImagePickerControllerSourceType) {
        // User DZN photo picker for the confirmation (crop) step.
        let singlePhotoPicker = PhotoPicker(presenter: self, defaultCameraDevice: .rear, cropMode: .none, allowRemoval: false)
        // Note: We must save a strong ref to the photo picker or this will fail.
        self.singlePhotoPickerStrongRefs.append(singlePhotoPicker)
        singlePhotoPicker.completionHandler = { result in
                if case let .chosePhoto(image) = result {
                    self.send(uploadableAttachment: .localImage(image: image))
                }
            }
        singlePhotoPicker.presentImagePicker(with: sourceType)
    }
    
    /// Invoked by the picker sheet when the user chooses one or more media items.
    /// This is to be differentiated from the didFinishPickingMediaWithInfo method above,
    /// which is called when the user chooses or takes a new single image via the (DZN enhanced)
    /// system media picker.
    private func userPickedMultipleImages(_ assets : [PHAsset]) {
        print("user picked multiple")
        var images = [UIImage]()
        let manager = PHImageManager.default()
        for asset in assets {
            // TODO: Calling this synchronously.
            // TODO: We can parallelize this with a dispatch group and do the fetch/scaling concurrently.
            let options = PHImageRequestOptions()
            options.isSynchronous = true
            manager.requestImage(
                for: asset,
                // Request the images at the max upload size
                targetSize: CGSize(width: ImageManager.uploadImageMaxDimension, height: ImageManager.uploadImageMaxDimension),
                contentMode: .aspectFit,
                options: options) { (image, info) in
                    if let image = image {
                        images.append(image)
                    }
            }
        }
        for image in images {
            send(uploadableAttachment: .localImage(image: image))
        }
    }
    
    private func userPickedSingleImage(image: UIImage) {
        let image = image.scaleAspectFitIfNeeded(ImageManager.uploadImageMaxDimension)
        send(uploadableAttachment: .localImage(image: image))
    }
    
    // MARK: Private method - sharing link
    
    @objc private func doEmptyChatShareLink() {
        delegate?.shareLink(from: self)
    }
    
    // MARK: KeyboardWillShowHideListener
    
    public func keyboardWillShow(with animation: KeyboardAnimation) {
        hideJoinToCommentButton()
        showEmptyChatPlaceholder = false
    }
    
    public func keyboardWillHide(with animation: KeyboardAnimation) {
        updateShowEmptyChatPlaceholder()
    }
    
}

// MARK: - Theming

public extension ThemeableButton {
    
    public func apply_joinToCommentAppearance() {
        let theme = ThemeableButton.Theme(
            textColor: .black,
            highlightedTextColor: Palette.joinToCommentButtonTitleColor.withAlphaComponent(Palette.defaultHighlightedButtonTitleAlpha),
            backgroundColor: .white,
            font: .presentFont(ofSize: 18.0, weight: .bold)
        )

        apply(theme: theme)
        
        // Ensure localized strings have the best shot at fitting entirely.
        titleLabel?.lineBreakMode = .byTruncatingMiddle
        titleLabel?.adjustsFontSizeToFitWidth = true
        titleLabel?.minimumScaleFactor = 0.7
    }
    
}

// MARK: - UIImagePickerControllerDelegate
extension MessagesViewController {
    
    public func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
        dismiss(animated: true, completion: nil)
    }
    
    /*
    public func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [String : Any])
    {
        print("did finish picking: ")
        
        defer {
            picker.dismiss(animated: true, completion: nil)
        }
        
        // Note: despite being named "OriginalImage" this image does reflect any user edits.
        guard let image = info[UIImagePickerControllerOriginalImage] as? UIImage else {
            logError("User picked an unsupported image")
            return
        }
        
        send(uploadableAttachment: .localImage(image: image))
    }
    
    public func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
        dismiss(animated: true, completion: nil)
    }*/
    
}

// MARK: - ImagePickerSheetControllerDelegate
extension MessagesViewController: ImagePickerSheetControllerDelegate {
    
    public func controllerWillEnlargePreview(_ controller: ImagePickerSheetController) {
        //print("Will enlarge the preview")
    }
    
    public func controllerDidEnlargePreview(_ controller: ImagePickerSheetController) {
        //print("Did enlarge the preview")
    }
    
    public func controller(_ controller: ImagePickerSheetController, willSelectAsset asset: PHAsset) {
        //print("Will select an asset")
    }
    
    public func controller(_ controller: ImagePickerSheetController, didSelectAsset asset: PHAsset) {
        //print("Did select an asset")
    }
    
    public func controller(_ controller: ImagePickerSheetController, willDeselectAsset asset: PHAsset) {
        //print("Will deselect an asset")
    }
    
    public func controller(_ controller: ImagePickerSheetController, didDeselectAsset asset: PHAsset) {
        //print("Did deselect an asset")
    }
    
}

public protocol MessagesViewControllerDelegate: class {
    func handleJoin(from: MessagesViewController)
    func shareLink(from: MessagesViewController)
}

public protocol MessagesViewControllerScrollDelegate: class {
    func didScroll(messagesViewController: MessagesViewController, to contentOffset: CGPoint)
    func willBeginDragging(messagesViewController: MessagesViewController, from contentOffset: CGPoint)
}

extension UIButton {
    public func applyPurpleGradientButtonStyle() {
        self.roundedCorners = self.bounds.height/2.0
        self.clipsToBounds = true  // no shadow, so ok
        
        // TODO: Move some of this into the gradient view
        let gradient = GradientView()
        gradient.colors = [/*UIColor(hex: 0x7141DB),*/ UIColor(hex: 0x3023AE), UIColor(hex: 0xC96DD8)]
        gradient.gradientLayer.startPoint = CGPoint(x:-0.25, y:0)
        gradient.gradientLayer.endPoint = CGPoint(x:1.5, y:0)
        self.insertSubview(gradient, at: 0)
        gradient.constrainToFill(view: self)
        
    }
}
