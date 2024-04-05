//
//  ObservableValue.swift
//  Present
//
//  Created by Patrick Niemeyer on 8/2/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import RxSwift
import Then

/**
 ObservableValue composes a ReplaySubject observable with a synchronized read/write copy of the current value.
 This is similar to RxSwift.Variable but does not require an initital value at init time and does not allow
 nils in the sequence. The exposed current value is nil until the initial value has been set and attempts to
 set it back to nil are ignored.
 
 Note: We could allow this to be used with nillable types, but the issue is that it is not easy to differentiate
 the initial 'no value' from explicitly setting a nil in the stream.  The caller would have to do this
 with e.g. `value = Optional<String>.none` as opposed to `value = nil`.
 
 @See OptionalObservableValue for an observable value that allows nils in the sequence.
 */
public class ObservableValue<E>
{
    // A block for applying a new value.
    public typealias Setter = (_ value:E, _ apply: (E)->Void)->Void
    
    private let subject = ReplaySubject<E>.create(bufferSize: 1)
    private var _value = SynchronizedValue<E>()
    private var setter: Setter?
    private var didSetBlock: ((E)->Void)?
    
    /// Synchronized read/write copy of the current value, initially nil but thereafter will always have a value.
    public var value: E? {
        get {
            return _value.value
        }
        set {
            if newValue == nil { log("observablevalue explicitly setting to nil") }
            //log("observable value setter: \(newValue)")
            guard let newValue = newValue else { return }
            if let setter = setter {
                setter(newValue) { appliedValue in
                    _value.value = appliedValue
                    subject.onNext(newValue)
                }
            } else {
                _value.value = newValue
                subject.onNext(newValue)
            }
            didSetBlock?(newValue)
        }
    }
    
    public var observable: RxSwift.Observable<E> {
        return subject
    }
    
    /// - Parameter setter: An optional block to be called for applying new values. If the setter
    ///     does not apply the value it is not set and no observers are notified.
    public init(setter: Setter? = nil) {
        self.setter = setter
    }
    
    public convenience init(setter: Setter? = nil, initialValue: E? = nil) {
        self.init(setter: setter)
        if let initialValue = initialValue {
            self.value = initialValue
        }
    }
    
    /// - initializationBlock : A block to be called for initializing the value.
    public convenience init(setter: Setter? = nil, initializationBlock: (()->E?)? = nil) {
        self.init(setter: setter)
        if let initializationBlock = initializationBlock {
            self.value = initializationBlock()
        }
    }
    
    public func set(_ newValue: E) {
        value = newValue
    }
    
    public func initialValue(_ value: E?) -> Self {
        self.value = value
        return self
    }
    
    public func setter(_ setter: @escaping Setter) -> Self {
        self.setter = setter
        return self
    }
    
    public func didSet(block: (_ newValue: E)->Void) -> Self {
        return self
    }
    
    public func onNext(onNext: @escaping (E) throws -> Swift.Void) -> RxSwift.Observable<E> {
        return self.observable.onNext(onNext: onNext)
    }
}
extension ObservableValue: Then { }

/**
 ObservableValue composes a ReplaySubject observable with a synchronized read/write copy of the current value.
 This is similar to RxSwift.Variable but does not require an initital value at init time.
 Nils are allowed in the sequence.

 See the notes on ObservableValue for an explanation of why we have this explicitly optional version.
 */
public class OptionalObservableValue<E>
{
    // A block for applying a new value.
    public typealias Setter = (_ value:E?, _ apply: (E?)->Void)->Void
    
    private let subject = ReplaySubject<E?>.create(bufferSize: 1)
    private var _value = SynchronizedValue<E>()
    private var setter: Setter?
    private var didSetBlock: ((E?)->Void)?
    
    /// Synchronized copy of the current value, initially nil but thereafter will always have a value.
    public var value: E? {
        get {
            return _value.value
        }
        set {
            if let setter = setter {
                setter(newValue) { appliedValue in
                    _value.value = appliedValue
                    subject.onNext(newValue)
                }
            } else {
                _value.value = newValue
                subject.onNext(newValue)
            }
            didSetBlock?(newValue)
        }
    }
    
    public var observable: RxSwift.Observable<E?> {
        return subject
    }
    
    /// - Parameter setter: An optional block to be called for applying new values. If the setter
    ///     does not apply the value it is not set and no observers are notified.
    public init(setter: Setter? = nil) {
        self.setter = setter
    }
    
    public convenience init(setter: Setter? = nil, initialValue: E? = nil) {
        self.init(setter: setter)
        if let initialValue = initialValue {
            self.value = initialValue
        }
    }
    
    /// - initializationBlock : A block to be called for initializing the value.
    public convenience init(setter: Setter? = nil, initializationBlock: (()->E?)? = nil) {
        self.init(setter: setter)
        if let initializationBlock = initializationBlock {
            self.value = initializationBlock()
        }
    }
    
    public func set(_ newValue: E?) {
        value = newValue
    }
    
    public func initialValue(_ value: E?) -> Self {
        self.value = value
        return self
    }
    
    public func setter(_ setter: @escaping Setter) -> Self {
        self.setter = setter
        return self
    }
    
    public func didSet(block: (_ newValue: E?)->Void) -> Self {
        return self
    }
    
    public func onNext(onNext: @escaping (E?) throws -> Swift.Void) -> RxSwift.Observable<E?> {
        return self.observable.onNext(onNext: onNext)
    }
}

public class FixedObservableValue {
    public static func create<E>(with value: E) -> ObservableValue<E> {
        return ObservableValue<E>()
            .initialValue(value)
            .setter { newValue, apply in /* don't apply */ }
    }
}
