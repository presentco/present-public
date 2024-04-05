//
//  ShareSheetUtil.swift
//  Present
//
//  Created by Patrick Niemeyer on 10/17/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation

public class ShareSheetUtil
{
    public static func showShareSheet(from: UIViewController, withText shareText: String) {
        let shareViewController = UIActivityViewController(activityItems: [shareText], applicationActivities: nil)
        shareViewController.excludedActivityTypes = [.airDrop, .addToReadingList, .print, .openInIBooks]
        
        shareViewController.completionWithItemsHandler = { (activityType: UIActivityType?, completed: Bool, returnedItems: [Any]?, error: Error?) -> Void in
            
            if (completed) {
                let destination = (activityType?.rawValue ?? "none")
                logAction("User completed sharing", ["text": shareText, "destination": destination])
            } else {
                logAction("User cancelled share operation", ["text": shareText])
            }
            
        }
        
        from.present(shareViewController, animated: true, completion: nil)
    }
}
