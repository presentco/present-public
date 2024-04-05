//
//  CategoriesViewController.swift
//  Present
//
//  Created by Patrick Niemeyer on 9/14/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import Then
import RxSwift
import RxCocoa

public protocol CategoriesViewControllerDelegate: class {
    /// Should allow selection of the category
    func shouldSelect(category: GroupCategory)->Bool
}

/// Presents a list of categories as buttons with a flow layout
public class CategoriesViewController : PresentViewController
{
    public var allowsSelection: Bool = false
    public var delegate: CategoriesViewControllerDelegate?

    // If allowsSelection is true these are the selected categories
    public var selectedCategories: Set<GroupCategory> {
        get {
            return Set(categoryViews.filter { $0.isPicked }.map { $0.category })
        }
        set {
            guard newValue != selectedCategories else { return }
            categoryViews.forEach {
                $0.isPicked = newValue.contains($0.category)
            }
            rx.selectedCategories.onNext(newValue)
        }
    }
    public struct Rx {
        let selectedCategories = ReplaySubject<Set<GroupCategory>>.create()
    }
    public let rx = Rx()

    private let colors: [UIColor] = [
        UIColor(hex: 0x743CCE), UIColor(hex: 0xFF0AA6), UIColor(hex: 0xFDD61C), UIColor(hex: 0x25D8F1)
    ]
    private var colorIndex = 0
    private var categoryViews = [CategoryButton]()
    
    // TODO: We should interoperate with sizeThatFits / intrisic content size
    override public func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        layoutCategoryViews(width: view.bounds.width)
    }
    
    // Note: This is not a UIViewController method.
    // I'm not sure what the right thing to do here is other than provide
    // this and have the composing VC call it to set the constraints.
    public func sizeThatFits(size: CGSize) -> CGSize {
        let height = layoutCategoryViews(width: size.width)
        return CGSize(width: size.width, height: height)
    }

    // Return the height
    @discardableResult
    private func layoutCategoryViews(width: CGFloat) -> CGFloat
    {
        let height: CGFloat = 25
        let marginX: CGFloat = 0, marginY: CGFloat = 5
        let padX: CGFloat = 12, padY: CGFloat = 11
        var x: CGFloat  = marginX, y: CGFloat  = marginY
        // Lay out the views, wrapping when needed.
        for view in categoryViews {
            let size = view.sizeThatFits(CGSize(width: 999, height: height))
            if x > 0 && x + size.width > width {
                x = marginX; y += height + padY
            }
            view.frame = CGRect(origin: CGPoint(x: x, y: y), size: size)
            view.roundedCorners = height/2.0
            x += size.width + padX
        }
        
        return y + height
    }
    
    /// @param categories is a list to support ordering
    public func configure(categories: [GroupCategory], picked: Bool = true) {
        if categoryViews.isNotEmpty {
            categoryViews.forEach { $0.removeFromSuperview() }
            categoryViews = []
        }
        colorIndex = 0
        categories.forEach {
            createCategoryView(category: $0).do {
                categoryViews.append($0)
                $0.isPicked = picked
                view.addSubview($0)
            }
        }
    }
    
    private func createCategoryView(category: GroupCategory) -> CategoryButton
    {
        return CategoryButton().then { button in
            let color = colors[colorIndex % colors.count]
            button.configure(category: category, color: color)
            button.delegate = delegate
            button.rx.isPicked.onNext { [weak self] _ in
                guard let sself = self else { return }
                sself.rx.selectedCategories.onNext(sself.selectedCategories)
            }.disposed(by: disposal)
            colorIndex += 1
        }
    }
    
    private class CategoryButton: UIButton
    {
        public var category: GroupCategory = .attend
        public var delegate: CategoriesViewControllerDelegate?
        
        private let disposal = DisposeBag()
        private var selectedColor = UIColor.green
        private var selectedTextColor = UIColor.white
        private var deselectedColor = UIColor.lightGray
        private var deselectedTextColor = UIColor.white
        
        // Note: avoiding overlap with isSelected terminology here
        public var isPicked = false {
            didSet {
                guard isPicked != oldValue else { return }
                rx.isPicked.onNext(isPicked)
                applyStyle()
            }
        }
        
        public struct Rx {
            let isPicked = ReplaySubject<Bool>.create()
        }
        public let rx = Rx()
        
        required public override init(frame: CGRect = .zero) {
            super.init(frame: frame)
            self.rx.tap.bind { [weak self] _ in
                guard let sself = self else { return }
                if !sself.isPicked && !(sself.delegate?.shouldSelect(category: sself.category) ?? true) {
                    return
                }
                sself.isPicked = !sself.isPicked
                
                // This messes with the text...
                // I think it has to do with our layout by frame fighting with the transform.
                // (view layout is called twice during the animation)
                //if sself.isPicked { sself.pulse(1.2) }
                //sself.transform = CGAffineTransform(scaleX: 2.0, y:2.0)
            }.disposed(by: disposal)
        }
        
        required public init?(coder aDecoder: NSCoder) {
            super.init(coder: aDecoder)
        }
        
        public func configure(category: GroupCategory, color: UIColor) {
            self.category = category
            self.selectedColor = color
            setTitle(category.rawValue, for: .normal)
            titleLabel?.font = UIFont.systemFont(ofSize: 16)
            titleLabel?.minimumScaleFactor = 0.5
            titleLabel?.allowsDefaultTighteningForTruncation = true
            clipsToBounds = true
            showsTouchWhenHighlighted = false
            contentEdgeInsets = UIEdgeInsets(top: 3, left: 13, bottom: 3, right: 13)
            applyStyle()
        }
        
        private func applyStyle(animated: Bool = false) {
            backgroundColor = isPicked ? selectedColor : deselectedColor
            setTitleColor(
                isPicked ? selectedTextColor : deselectedTextColor, for: .normal)
        }
    }
}
