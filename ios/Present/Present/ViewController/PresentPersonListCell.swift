//
//  PresentPersonListCell.swift
//  Present
//
//  Created by Patrick Niemeyer on 6/20/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation

/// A Person cell with circular image, title/subtitle, and optional right side
/// adornments including an optional "right button".
/// Base class for friend list cell and related.
public class PresentPersonDataListCell<T: PersonData>: PresentListCell<T>
{
    override func initCell() {
        super.initCell()
        rightButton.addTarget(self, action: #selector(rightButtonPressed), for: .touchUpInside)
    }
    
    override public func apply(model user: T)
    {
        super.apply(model: user)
        initStyle()
        self.titleLabel.text = user.nameData.fullName
        setImage(personData: user)
    }
    
    public func initStyle() {
        self.selectionStyle = .none
        self.imageHeight.constant = 46
        self.subtitleLabel.isHidden = true
        self.showRightButton = false
    }
    
    public func setImage(personData user: PersonData)
    {
        if let image = user.photoData {
            self.setImage(image)
        }
        else if let photoUrl = user.photoURLData {
            setImage(url: photoUrl)
        }
        else {
            main {
                let image = Initials.generateInitialsImage(
                    name: user.nameData, height: self.imageHeight.constant)
                if let image = image {
                    self.setImage(image)
                }
            }
        }
    }
    
    public func setImage(url: URL)
    {
        let requestId = nextId()
        clearImage()
        ImageManager.shared.getImage(atURL: url)
            .onSuccess { [weak self] urlImage in
                guard self?.currentId() == requestId else { return }
                self?.setImage(urlImage.image)
            }.onError { _ in
                logDebug("Error loading photo: \(url)")
            }.disposed(by: disposal)
    }
    
    @objc public func rightButtonPressed() { }
}

public class PresentPersonListCell: PresentPersonDataListCell<Person> { }

