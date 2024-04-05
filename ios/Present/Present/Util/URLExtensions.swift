//
//  URLExtensions.swift
//  Present
//
//  Created by Patrick Niemeyer on 1/11/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation

extension URL {
    func isEquivalent(toUrl url: URL) -> Bool {
        return self.absoluteString.dropTrailingSlash() == url.absoluteString.dropTrailingSlash()
    }
}
