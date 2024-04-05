//
//  JsonUtil.swift
//  Present
//
//  Created by Patrick Niemeyer on 1/23/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation

public extension String {
    public static let emptyJson: String = "{}"
}

public protocol JsonConvertible : Codable {
    init?(jsonString: String)
    func toJson()->String?
}

public extension JsonConvertible {
    init?(jsonString: String) {
        guard let data = jsonString.data(using: .utf8) else { return nil }
        do {
            self = try JSONDecoder().decode(Self.self, from: data)
        } catch {
            return nil
        }
    }
    func toJson()->String? {
        guard let data = try? JSONEncoder().encode(self) else { return nil }
        return String(data: data, encoding: .utf8)
    }
}

