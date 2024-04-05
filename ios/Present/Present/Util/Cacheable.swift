
/// Simple async caching
/// Author: Pat Niemeyer

import Foundation

/*
 Example:
    lazy var myString = CacheableAsync<String>(.seconds(10.0)) { completion, error in
        let value = self.service()
        completion(value)
    }
    ...
    myString.get { value in ... }
    myString.get(override: true) { value in ... }
 */
public final class CacheableAsync<T : Any>
{
    public typealias CacheableBlock = (_ completion: @escaping (T)->(), _ error: ((String)->())?)->()
    
    private let name: String // for logging
    private let period: TimeInterval
    private let block : CacheableBlock
    private var value : T?
    private var lastTime : Date?
    
    public init( name: String, period : TimeInterval, block: @escaping CacheableBlock ) {
        self.name = name
        self.period = period
        self.block = block
    }
    
    public convenience init( _ period : TimeInterval, block: @escaping CacheableBlock ) {
        self.init(name: "Cacheable", period: period, block: block)
    }
    
    public func get(override: Bool = false, complete: @escaping (T)->())
    {
        get(override: override, complete: complete, error: nil)
    }
    
    public func get(override: Bool = false, complete: @escaping (T)->(), error: ((String)->())?)
    {
        let value = self.value
        if ( value == nil || isExpired || override ) {
            logn("Cacheable: cache miss for: \(name)")
            block( { result in
                self.value = result
                self.lastTime = Date()
                complete( result )
            },
               error
            )
        } else {
            logn("Cacheable: cache hit for: \(name)")
            complete( value! ) // never nil
        }
    }

    /// Clear the cached value so that it will be evaluated on the next call.
    public func invalidate() {
        lastTime = nil
    }
    
    /// Is this instance expired?
    public var isExpired : Bool {
        guard let lastTime = lastTime else { return true }
        return Date().timeIntervalSince(lastTime) > period
    }
}

