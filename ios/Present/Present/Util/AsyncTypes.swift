//
//  AsyncTypes.swift
//  Present
//
//  Created by Patrick Niemeyer on 11/30/17.
//  Copyright © 2017 Present Company. All rights reserved.
//
import Foundation

// TODO: Remove, the compiler can't handle this in conjunction with @escaping
/// A completion block with no arguments
public typealias Completion = ()->Void

/// A completion block accepting a bool indicating success
public typealias Success = (_ success:Bool)->Void

/// A completion block accepting a typed result
public typealias Result<T> = (_ result:T)->Void

/// A response enum parameterized on error and success types
// TODO: I would have put the success type first.
public enum Response<ErrorType, SuccessType>
{
    // MARK: Cases
    
    case error(ErrorType)
    case success(SuccessType)
    
    // MARK: Public Methods
    
    public func translate<UpdatedErrorType, UpdatedSuccessType>(error errorTranslator: (ErrorType) -> UpdatedErrorType, success successTranslator: (SuccessType) -> UpdatedSuccessType) -> Response<UpdatedErrorType, UpdatedSuccessType> {
        switch self {
        case let .error(errorValue):
            return .error(errorTranslator(errorValue))
            
        case let .success(successValue):
            return .success(successTranslator(successValue))
        }
    }
    
    public func translateSuccess<UpdatedSuccessType>(with translator: (SuccessType) -> UpdatedSuccessType) -> Response<ErrorType, UpdatedSuccessType> {
        switch self {
        case let .error(errorValue):
            return .error(errorValue)
            
        case let .success(successValue):
            return .success(translator(successValue))
        }
    }
    
    public func translateError<UpdatedErrorType>(with translator: (ErrorType) -> UpdatedErrorType) -> Response<UpdatedErrorType, SuccessType> {
        switch self {
        case let .error(errorValue):
            return .error(translator(errorValue))
            
        case let .success(successValue):
            return .success(successValue)
        }
    }
}
