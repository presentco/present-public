//
//  ContactsMessageBatch.swift
//  Present
//
//  Created by Patrick Niemeyer on 10/17/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import MessageUI

/// Message a batch of Contacts
public class ContactsMessageBatch : NSObject, MFMessageComposeViewControllerDelegate
{
    private var contactsToSend: [Contact] = [] // Used in batch sending to contacts
    private let shareText: String
    private let fromViewController: UIViewController

    /// Callback before the send, e.g. for logging
    public var beforeSend: ((Contact)->())?
    
    public init(fromViewController: UIViewController, contactsToSend: [Contact], shareText: String) {
        self.fromViewController = fromViewController
        self.contactsToSend = contactsToSend
        self.shareText = shareText
    }

    public func send() {
        sendNextContact()
    }

    private func sendNextContact()
    {
        guard !contactsToSend.isEmpty else { return }
        
        let contact = contactsToSend.removeFirst()
        beforeSend?(contact)

        let sharedText = shareText

        logDebug("send next contact: \(contact.nameData.fullName)")

        guard MFMessageComposeViewController.canSendText() else {
            if UIDevice.current.isSimulator {
                logDebug("Can't send text in simulator, skipping.")
            } else {
                logError("Unable to send text message on device.")
            }
            return // shortcut all
        }

        // Contacts are split per phone, so there should be only one number
        guard let number = contact.phoneNumbers.first else {
            logDebug("no phone")
            sendNextContact()
            return
        }

        let composeVC = MFMessageComposeViewController()
        composeVC.messageComposeDelegate = self
        composeVC.recipients = [number.normalizedString]
        composeVC.body = sharedText

        fromViewController.present(composeVC, animated: true) { }
    }

    // MARK: MFMessageComposeViewControllerDelegate

    // We are messaging a contact with the app invite - handle the callback from the messaging UI
    public func messageComposeViewController(_ controller: MFMessageComposeViewController, didFinishWith result: MessageComposeResult)
    {
        logDebug("message compose result = \(result)")
        switch(result) {
            case .sent:
                logEvent(type: .action, "User sent invite contact.")
                controller.dismiss(animated: true) {
                    self.sendNextContact()
                }
                break
            case .cancelled:
                logEvent(type: .action, "User cancelled invite contact.")
                // Stop sending contacts
                controller.dismiss(animated: true)
                break
            case .failed:
                logEvent(type: .action, "Failed to send invite contact.")
                // TODO: What kind of failure do we see here?  Indicate to user?
                controller.dismiss(animated: true) {
                    self.sendNextContact()
                }
                break
        }
    }


}
