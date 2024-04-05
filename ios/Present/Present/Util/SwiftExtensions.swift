//
//  SwiftExtensions.swift
//  Present
//
//  Created by Patrick Niemeyer on 7/30/16.
//  Copyright © 2016 Present Company. All rights reserved.
//

import Foundation

extension Equatable {
    func oneOf(_ other: Self...) -> Bool {
        return other.contains(self)
    }
}
