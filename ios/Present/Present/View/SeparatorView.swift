import CoreGraphics
import Foundation
import UIKit

// @Deprecated 
/// Stroke a line specified by width and color, vertically centered and the width of the view component.
@IBDesignable
open class SeparatorView : UIView
{
    @IBInspectable var color : UIColor = UIColor.white
    @IBInspectable var width : CGFloat = 1.0
    
    override open func draw(_ rect: CGRect)
    {
        let ctx = UIGraphicsGetCurrentContext()!
        ctx.setLineWidth(width)
        ctx.beginPath()
        ctx.setLineCap(.round)
        ctx.move(to: CGPoint(x: 0+width, y: bounds.origin.y+rect.height/2))
        ctx.addLine(to: CGPoint(x: bounds.origin.x+rect.width-width, y: bounds.origin.y+rect.height/2))
        ctx.setStrokeColor(color.cgColor)
        ctx.strokePath()
    }
}
