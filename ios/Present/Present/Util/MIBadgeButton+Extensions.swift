//
//  MIBadgeButton+Extensions.swift
//  Present
//
//  Created by Patrick Niemeyer on 7/24/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import MIBadgeButton_Swift

extension MIBadgeButton
{
    public var badgeCount: Int {
        set {
            self.badgeString = newValue > 0 ? "\(newValue)" : ""
        }
        get {
            return Int(self.badgeString ?? "0") ?? 0
        }
    }
}
