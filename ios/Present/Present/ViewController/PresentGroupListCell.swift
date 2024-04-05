//
//  PresentGroupListCell.swift
//  Present
//
//  Created by Patrick Niemeyer on 6/20/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import StringStylizer
import PresentProtos
import RxSwift

public class PresentGroupListCell: PresentListCell<Group>
{
    // A clone of the currently configued group data for change detection
    private var configuredGroup: Group?
    
    override public func apply(model group: Group)
    {
        super.apply(model: group)

        self.titleLabel.text = group.title
        self.titleDetailLabel.isShown = true
        
        let joinedIcon = String.attributedStringFromImage(image: #imageLiteral(resourceName: "MembersIcon"), xoffset: 0, yoffset: -0.9)
        let commentsIcon = String.attributedStringFromImage(image: #imageLiteral(resourceName: "CommentsIcon"), xoffset: 0, yoffset: -0.5)
        let distanceIcon = String.attributedStringFromImage(image: #imageLiteral(resourceName: "DistanceIcon"), xoffset: 0, yoffset: -0.5)
        let spacer9 = String.attributedStringSpacerImage(width: 9, height: 10)
        let spacer2 = String.attributedStringSpacerImage(width: 2.5, height: 10)
        self.subtitleLabel.attributedText =
            joinedIcon + spacer2 + "\(group.joinedCount)".stylize().attr +
            spacer9 + commentsIcon + spacer2 + "\(group.commentCount)".stylize().attr +
            spacer9 + distanceIcon + spacer2 + "\(group.locationName ?? "")".stylize().attr
        
        self.titleDetailLabel.text = Date().relativeTime(since: group.lastCommentTime, nowTimeSpan: 1.0, maxGranularity: .second)
        
        updateBadging(group: group)
        
        group.rx.modified
            .observeOn(MainScheduler.instance)
            .onNext { [weak self] in
                self?.updateBadging(group: group)
            }.disposed(by: disposal)

        
        // Update the cover photo or map if they have changed else shortcut/return
        if let coverPhotoUrl = group.coverPhoto {
            if group.coverPhoto == configuredGroup?.coverPhoto { return }
            clearImage()
            let requestId = nextId()
            ImageManager.shared.getImage(atURL: coverPhotoUrl)
                .onSuccess { [weak self] urlImage in
                    guard self?.currentId() == requestId else { return }
                    self?.setImage(urlImage.image)
                }.onError { _ in
                    logDebug("Error loading cover photo for group: \(group.groupToken)")
                }.disposed(by: disposal)
        } else {
            if let configuredGroup = configuredGroup, group.location == configuredGroup.location { return }
            clearImage()
            let requestId = nextId()
            MapSnapshotManager.shared.mapSnapshot(
            at: group.location, size: leftImageView.bounds.size, name: group.title, id: group.id) { [weak self] mapSnapshot, id in
                    guard self?.currentId() == requestId else { return }
                    self?.setImage(mapSnapshot)
            }
        }
        
        configuredGroup = group.clone()
    }
    
    private func updateBadging(group: Group)
    {
        // Apply any badging
        let readCount = group.lastRead + 1 // lastRead is the index
        let unreadCount = max(0, group.commentCount - readCount)
        //log("group: \(group.title), unread=\(unreadCount), joinRequests\(group.joinRequests)")
        
        // Total is unread plus join requests (join requests only if I am the owner)
        var totalCount = unreadCount + (group.owner.isMe ? group.joinRequests : 0)
        // Adding one for "new" groups for now if no other badging... could display a dot instead?
        if group.isNew && totalCount == 0 {
            totalCount += 1
        }
        
        self.subtitleBadgeLabel.isShown = totalCount > 0
        self.subtitleBadgeLabel.text = "\(totalCount)"
        //log("group: \(group.title) updated badge to: \(totalCount)")
    }
}
