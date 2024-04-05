//
//  Logging+Debug.swift
//  Present
//
//  Created by Patrick Niemeyer on 5/17/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation

private func baseName(ofFileName fileName: String) -> String {
    return "\((fileName as NSString).lastPathComponent.replacingOccurrences(of: ".swift", with: ""))"
}
// Log a debug string prefixing the caller's file name.
public func log(file: String = #file, _ string: String) {
    logDebug("\(baseName(ofFileName: file)): \(string)")
}

// Log a higlighted debug string prefixing the caller's file name,
public func logx(file: String = #file, _ string: String) {
    logDebug("XXX: \(baseName(ofFileName: file)): \(string)")
}

// Turn off the log statement temporarily
public func logn(file: String = #file, _ string: String) { }
