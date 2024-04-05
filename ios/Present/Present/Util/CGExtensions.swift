//
//  CGExtensions.swift
//  Present
//
//  Created by Patrick Niemeyer on 1/6/16.
//  Copyright © 2016 Present Company. All rights reserved.
//

import CoreGraphics

func + (left: CGPoint, right: CGPoint) -> CGPoint {
    return CGPoint(x: left.x + right.x, y: left.y + right.y)
}
func - (left: CGPoint, right: CGPoint) -> CGPoint {
    return CGPoint(x: left.x - right.x, y: left.y - right.y)
}

func * (point: CGPoint, mult: CGFloat) -> CGPoint {
    return CGPoint(x: point.x * mult, y: point.y * mult )
}

func += (left: inout CGPoint, right: CGPoint) {
    left = left + right
}
func -= (left: inout CGPoint, right: CGPoint) {
    left = left - right
}

func *= (point: inout CGPoint, mult: CGFloat) {
    point.x *= mult
    point.y *= mult
}

extension CGPoint {
    func offsetBy(dx: CGFloat, dy: CGFloat) -> CGPoint {
        return CGPoint( x: self.x+dx, y: self.y+dy)
    }
}

extension CGSize {
    static func square(_ side: CGFloat) -> CGSize {
        return CGSize(width: side, height: side)
    }
}
