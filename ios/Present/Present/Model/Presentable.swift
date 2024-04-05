//
//  Presentable.swift
//  Present
//
//  Created by Dan Federman on 7/7/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import PresentProtos

public protocol PresentablePresenter: ScreenPresenter {
    func presentScreen(for presentable: Presentable)
}

public enum Presentable
{
    // MARK: Cases

    // TODO: Rethink how these are named
    case showGroupMessage(Present.CommentResponse)
    case showGroup(Present.GroupResponse)
    case showGroupByGroup(Group)
    case showGroupMembershipRequests(Group)
    case showProfile(Present.UserResponse)
    //case showProfileById(String)
    case showCategory(Present.CategoryResponse,PresentSpace) // TODO: Remove space
    case showCategoryById(String,PresentSpace?) // TODO: Remove space

    // MARK: Initialization

    // A reference response from e.g. an activity event
    public init?(proto: Present.ReferenceResponse.Response) {
        switch proto {
            case .OneOfResponseNotSet:
                 return nil
                
            case .UserId, .GroupId, .CommentId/*, .MessageId*/:
                return nil
                
            case let .User(userResponse):
                self = .showProfile(userResponse)
            
            case let .Group(groupResponse):
                self = .showGroup(groupResponse)
                
            case let .Comment(commentResponse):
                self = .showGroupMessage(commentResponse)
        }
    }

    // A url resolution response from a link activation
    public init?(proto: Present.ResolveUrlResponse.Result) {
        switch proto {
            case .OneOfResultNotSet, .App:
                return nil
                
            case let .User(userResponse):
                self = .showProfile(userResponse)
                
            case let .Group(groupResponse):
                self = .showGroup(groupResponse)
                
            case let .Comment(commentResponse):
                self = .showGroupMessage(commentResponse)
            
            case let .Category(categoryResponse):
                self = .showCategory(categoryResponse, PresentSpace.everyone)
        }
        
    }
    
}
