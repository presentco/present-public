//
//  URLOpener.swift
//  Present
//
//  Created by Dan Federman on 3/8/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation


public protocol URLOpener {
    
    func open(_ url: URL, options: [String : Any], completionHandler completion: ((Bool) -> Void)?)
    
}

public extension URLOpener {
    
    public func open(_ url: URL, options: [String : Any] = [:], completionHandler completion: ((Bool) -> Void)? = nil) {
        open(url, options: options, completionHandler: completion)
    }
    
}


extension UIApplication: URLOpener {
    
    // UIApplication already conforms.
    
}
