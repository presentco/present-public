//
//  Categories.swift
//  Present
//
//  Created by Patrick Niemeyer on 7/25/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation

// These are the fixed values that we store on the server (do not localize).
public enum GroupCategory : String {
    case attend = "Attend"
    case organize = "Organize"
    case eatAndDrink = "Eat & Drink"
    case exercise = "Exercise"
    case live = "Live"
    case learn = "Learn"
    case shop = "Shop"
    case volunteer = "Volunteer"
    case work = "Work"
    

    public static let all: [GroupCategory] = [
        .attend, .organize, .eatAndDrink, .exercise, .live, .learn, .shop, .volunteer, .work
    ]
}

public class GroupCategories
{
    /// Until we will move to a server, data-driven API
    public class func mapCategoryClientSide(string: String) -> String {
        let value = string.trimmingWhitespace()
        switch(value) {
            case "Hobbies":
                return "Lifestyle" // Map old Hobbies category to News
            default:
                return value
        }
    }

    public class func mapCategoriesClientSide(strings: Set<String>) -> Set<String> {
        return Set<String>(strings.map{ mapCategoryClientSide(string: $0) })
    }

    public class func from(strings: [String]) -> [GroupCategory] {
        let categories = strings
            .map{ mapCategoryClientSide(string: $0) }
            .compactMap { GroupCategory(rawValue:$0) }
        return Array(Set(categories)) // unique
    }
    
    public class func from(strings set: Set<String>) -> [GroupCategory] {
        return from(strings: Array<String>(set))
    }

}
