
/// Simple async caching
/// Author: Pat Niemeyer

import Foundation
import RxSwift

// DEPRECATED: Migrate to CachedSingle observable
///
/// Cache a Single<T> result for a period of time.
/// This class uses the provided Single producer to create a Single result and cache the value T
/// for the specified period. Subsequent requests return Single.just(T) until expired, after which a
/// new Single is produced. This class ensures that concurrent requests during the production of a new
/// Single value return the same Single instance.
///
// Note: In theory we can do this with Rx directly with .concat().take(1).
// Note: Concats is supposed to shortcut when completed and so we can use this to alternate
// Note: the service call with a cached source based on expiration.
// Note: However this may be simpler since it wraps all of the logic in one place.
public final class CacheableSingle<T : Any>
{
    public typealias SingleProducer = ()->Single<T>
    
    private let period: TimeInterval
    private let producer : SingleProducer
    private var value : T? {
        didSet {
            lastValueTime = Date()
        }
    }
    private var lastValueTime : Date?
    
    private var currentProducer: Single<T>?
    
    private let name: String // for logging
    
    public init( name: String, period : TimeInterval, producer: @escaping SingleProducer) {
        self.name = name
        self.period = period
        self.producer = producer
    }
    
    public convenience init( _ period : TimeInterval, producer: @escaping SingleProducer ) {
        self.init(name: "Cacheable", period: period, producer: producer)
    }
    
    /// An identifier that increments with each value production, used to ignore stale results
    private var generation: Int = 0
    
    public func get(override: Bool = false)->Single<T>
    {
        // synchronized work
        objc_sync_enter(self); defer { objc_sync_exit(self) }
        
        // If we have a valid value return it
        if let value = self.value, !isExpired, !override {
            //log("Cache hit for: \(name)")
            return Single.just(value)
        } else {
            // Generate a new single and share it among clients until a value is produced.
            //log("Cache miss for: \(name)")
            if let currentProducer = currentProducer {
                //log("Sharing current producer: \(name)")
                return currentProducer
            } else {
                //log("Creating producer: \(name)")
                self.generation += 1
                self.currentProducer = producer()
                    .asObservable().share().asSingle()
                    .onSuccess { [generation] value in
                        self.updateValue(value, generation: generation)
                    }
                    .onError { [generation] error in
                        self.updateValue(nil, generation: generation)
                    }
                return self.currentProducer.unwrappedOrFatal()
            }
        }
    }
    
    private func updateValue(_ value: T?, generation: Int) {
        // synchronized work
        objc_sync_enter(self); defer { objc_sync_exit(self) }
        // If the value is for an old request ignore it
        guard generation == self.generation else { return }
        // If the current producer already produced a value we're done
        guard self.currentProducer != nil else { return }
        // Apply the value and clear the current producer
        self.value = value
        self.currentProducer = nil
    }

    /// Clear the cached value so that it will be evaluated on the next call.
    public func invalidate() {
        lastValueTime = nil
    }
    
    /// Is this instance expired?
    public var isExpired : Bool {
        guard let lastValueTime = lastValueTime else { return true }
        return Date().timeIntervalSince(lastValueTime) > period
    }
}

/*
public class CachingObservableValue<E>: ObservableValue<E>
{
    let cache: CacheableSingle<E>
    
    public init( _ period : TimeInterval, producer: @escaping CacheableSingle<E>.SingleProducer ) {
        cache = CacheableSingle(period, producer: producer)
        super.init()
    }
}*/

