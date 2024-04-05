//
//  ActivityVendor.swift
//  Present
//
//  Created by Dan Federman on 7/6/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import RxSwift

// DEPRECATED: Use the Rx API
public protocol ActivityObserver: class {
    func activityDidUpdate(to activity: [Activity])
    func activityDidFailToUpdate()
}

public final class ActivityManager
{
    // MARK: Private Properties
    
    private var activityList: [Activity]? {
        didSet {
            legacyActivityObservers.set(value: activityList)
            activity.value = activityList
        }
    }
    
    private let optimisticActivityChanges = ActualizableQueue<Activity>()
    private let synchronizationQueue = DispatchQueue(label: "DefaultActivityVendor Work Queue")
    private let service: PresentService
    
    // Note: Dealing with the mutual dependency of activity manager and user manager here.
    public var userManager: UserManager?

    private let legacyActivityObservers = LegacyObservable<[Activity]?, ActivityObserver>() { value, observer in
        if let value = value {
            observer.activityDidUpdate(to: value)
        } else {
            observer.activityDidFailToUpdate()
        }
    }
    
    //  The public Rx observable activity list
    public lazy var activity = ObservableValue<[Activity]>()
    
    public lazy var unreadActivityCount = Observable.combineLatest(
        activity.observable,
        userManager.unwrappedOrFatal().lastReadActivity.observable
        )
        .observeOn(MainScheduler.instance)
        .map { (activity:[Activity], lastRead: Date?)->Int in
            //log("activity unread count: activity:\(activity.count), lastRead: \(lastRead)")
            guard let lastRead = lastRead else { return activity.count }
            let unreadActivity = activity.filter { $0.date > lastRead }
            //log("activity unread count: \(unreadActivity.count)")
            return unreadActivity.count
        }
    
    public func markAsRead() {
        userManager.unwrappedOrFatal().lastReadActivity.value = Date()
    }
    
    // MARK: Initialization
    
    public init(service: PresentService) {
        self.service = service
    }
    
    // MARK: ActivityVendor
    
    public func fetchActivity()
    {
        UserManager.shared.whenUserAuthorizedToProceedToApp {
            self.synchronizationQueue.async {
                let synchronization = Synchronization()
                self.optimisticActivityChanges.append(synchronization: synchronization)
                let earliestDate = self.activityList?.last?.date
                self.service.getPastActivity(after: earliestDate) { [weak self] (response) in
                    guard let strongSelf = self else {
                        return
                    }

                    strongSelf.synchronizationQueue.async {
                        strongSelf.optimisticActivityChanges.actualize(synchronization: synchronization)

                        switch response {
                            case let .success(activity):
                                strongSelf.insert_inWorkQueue(activity: activity)

                            case .error:
                                // TODO:(dan) Exponential backoff? Error handling! In the meantime inform our observers.
                                strongSelf.legacyActivityObservers.set(value: strongSelf.activityList)
                                strongSelf.activity.value = strongSelf.activityList
                        }
                    }
                }

            }
        }
    }
    
    public func insert(activity: [Activity]) {
        synchronizationQueue.async {
            activity.forEach {
                self.optimisticActivityChanges.append(transform: OptimisticTransform<Activity>(insert: $0, withIdentifier: $0.activityToken.uuidString))
            }
            
            self.insert_inWorkQueue(activity: activity)
        }
    }
    
    // DEPRECATED: Use the Rx API
    public func register(activityObserver: ActivityObserver, observerQueue: DispatchQueue) {
        _ = legacyActivityObservers.register(observer: activityObserver, on: observerQueue)
    }
    
    // MARK: Private Methods
    
    private func insert_inWorkQueue(activity: [Activity]) {
        let allActivity: [Activity]
        if let existingActivity = self.activityList {
            allActivity = existingActivity + activity
            
        } else {
            allActivity = activity
        }
        
        let activityTokenToActivityMap = allActivity.mapToDictionary({ [ $0.activityToken.uuidString : $0 ] })
        self.activityList = optimisticActivityChanges.applyTransforms(toIdentifierToValueMap: activityTokenToActivityMap).values.sorted().reversed()
    }
}

