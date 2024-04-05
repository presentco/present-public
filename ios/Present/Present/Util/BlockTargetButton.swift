//
//  BlockTargetButton.swift
//  Present
//
//  Created by Patrick Niemeyer on 3/27/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation

// Deprecated: Let's just use Rx
public class BlockTargetButton: UIButton
{
    public typealias ButtonTarget = (BlockTargetButton)->Void
    
    required public override init(frame: CGRect = .zero) {
        super.init(frame: frame)
        self.initButton()
    }
    
    required public init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        self.initButton()
    }
    
    private func initButton() {
        self.addTarget(self, action: #selector(buttonPressed), for: .touchUpInside)
    }
    
    public class Target {
        var target: (BlockTargetButton)->Void
        public init(target: @escaping (BlockTargetButton)->Void) {
            self.target = target
        }
    }
    
    private var targets: [Target] = []
    
    /// Add a target action to the button, returning a Target wrapper that can be
    /// used with removeTarget() if needed.
    /// As always, be careful to use [weak self] if you do not wish to retain the
    /// items referenced by the target.
    @discardableResult
    public func addTarget(target: @escaping ButtonTarget) -> Target {
        let wrapper = Target(target: target)
        targets.append(wrapper)
        return wrapper
    }
    
    public func removeTarget(_ target: Target) {
        targets = targets.filter{ $0 !== target }
    }
    
    @objc func buttonPressed() {
        for wrapper in targets { wrapper.target(self) }
    }
}

