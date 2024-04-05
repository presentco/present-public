//
//  EventSupressor.swift
//  Present
//
//  Created by Dan Federman on 3/28/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation


public protocol EventSupressor {

    func beginIgnoringInteractionEvents()
    func endIgnoringInteractionEvents()
    
}

extension UIApplication: EventSupressor {
    
    // UIApplication already conforms.
    
}
