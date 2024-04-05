//
//  PresentListCell.swift
//  Present
//
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import StringStylizer
import RxSwift
import PresentProtos

public let PresentListCellReuseIdentifier = "present-list-reuse-id"

public class PresentListCell<T> : UITableViewCell, TableViewModelCell
{
    //deinit { log("deinit: \(type(of: self))") }
    
    public typealias M = T
    public var model: T?
    
    var disposal = DisposeBag()
    let id = Sequence()
    

    // An equivalent image view backing the imageButton
    @IBOutlet weak var leftImageView: UIImageView! {
        didSet {
            //leftImageView.isHidden = true
            leftImageView.roundedCorners = 30;
            leftImageView.clipsToBounds = true
            leftImageView.isUserInteractionEnabled = false
        }
    }
    
    @IBOutlet weak var leftButton: UIButton! {
        didSet {
            leftButton.isUserInteractionEnabled = false
        }
    }
    
    @IBOutlet weak var imageHeight: NSLayoutConstraint!
    
    @IBOutlet weak var titleLabel: UILabel!
    
    @IBOutlet weak var subtitleLabel: UILabel!
    
    @IBOutlet weak var titleDetailLabel: UILabel! {
        didSet {
            titleDetailLabel.isHidden = true
        }
    }

    @IBOutlet weak var subtitleBadgeLabel: UILabel! {
        didSet {
            /*
            // Make it a dot for now
            subtitleBadgeLabel.text = nil
            UIView().do {
                $0.backgroundColor = subtitleBadgeLabel.backgroundColor
                $0.frame = CGRect(x:0, y:0, width: 14, height:14)
                $0.roundCornersToHeight()
                $0.clipsToBounds = true
                subtitleBadgeLabel.addSubview($0)
            }
            subtitleBadgeLabel.backgroundColor = .clear
            */
            subtitleBadgeLabel.isHidden = true
        }
    }
    
    @IBOutlet weak var rightButton: UIButton! {
        didSet {
            rightButton.isHidden = true
            rightButton.roundCornersToHeight()
        }
    }
    @IBOutlet weak var rightButtonWidth: NSLayoutConstraint!

    public var showRightButton: Bool = false {
        didSet {
            rightButtonWidth.constant = showRightButton ? 90 : 0
            rightButton.isShown = showRightButton
        }
    }
    
    private var spinner: PresentLogoActivityIndicator?

    override public required init(style: UITableViewCellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        initCell()
    }
    
    public init() {
        let reuseIdentifier = String(describing: type(of: self))
        super.init(style: .default, reuseIdentifier: reuseIdentifier);
        initCell()
    }
    
    required public init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        initCell()
    }
    
    func initCell() {
        //log("init: \(type(of: self))")
        initViewFromNib(classType: PresentListCell.self) // children use our xib
        showRightButton = false
        selectionStyle = .none
    }
    
    // Reset non-content related state here
    public override func prepareForReuse() {
        super.prepareForReuse()
        self.disposal = DisposeBag() // close our subscriptions
        self.removeSpinner()
    }

    public func apply(model: T) {
        self.model = model
    }
    
    override public func layoutSubviews() {
        super.layoutSubviews()
        leftImageView.roundCornersToHeight()
        subtitleBadgeLabel.roundCornersToHeight()
        subtitleBadgeLabel.clipsToBounds = true
    }
    
    // Set the left image, with a default fade in from a nil image
    func setImage(_ image: UIImage, fadeIn: Double = 0.2) {
        if leftImageView.image == nil {
            leftImageView.alpha = 0
            leftImageView.image = image
            UIView.animate(withDuration: fadeIn) {
                self.leftImageView.alpha = 1.0
            }
        } else {
            leftImageView.image = image
        }
    }
    
    func clearImage() {
        leftImageView.image = nil
    }
    
    public var hasImage: Bool {
        return leftImageView.image != nil
    }

    func setButtonLightColor() {
        rightButton.do {
            $0.setTitleColor(UIColor.hex(0x333333), for: .normal)
            $0.setTitleColor(UIColor.hex(0x999999), for: .disabled)
            $0.backgroundColor = .white
            $0.layer.borderWidth = 1.0
            $0.layer.borderColor = UIColor.hex(0xe0e0e0).cgColor
        }
    }
    
    func setButtonDarkColor() {
        rightButton.do {
            $0.setTitleColor(.white, for: .normal)
            $0.backgroundColor = UIColor.hex(0x743CCE)
            $0.layer.borderWidth = 0
        }
    }
    
    func addSpinner() {
        guard self.spinner == nil else { return }
        self.spinner = PresentLogoActivityIndicator().then {
            self.rightButton.addSubview($0.view)
            $0.view.anchorCenter(to: self.rightButton)
            $0.logoSize = 20
            $0.backSize = 26
            $0.startAnimating()
        }
    }
    
    func removeSpinner() {
        guard let spinner = spinner else { return }
        spinner.stopAnimating()
        spinner.view.removeFromSuperview()
        self.spinner = nil
    }


    func nextId() -> Int {
        return id.next()
    }
    func currentId() -> Int {
        return id.current()
    }
}


