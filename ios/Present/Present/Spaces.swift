//
// Created by Patrick Niemeyer on 5/2/18.
// Copyright (c) 2018 Present Company. All rights reserved.
//

import Foundation
import PresentProtos

public enum PresentSpace: String
{
    // The enum values are the server side space ids for these spaces
    case womenOnly = "women-only"
    case everyone = "everyone"
    
    public var id: String { return rawValue }

    static func isEveryone(space: Present.SpaceResponse?) ->  Bool {
        return space?.id ?? "" == PresentSpace.everyone.id
    }
    static func isWomenOnly(space: Present.SpaceResponse?) ->  Bool {
        return space?.id ?? "" == PresentSpace.womenOnly.id
    }
}
