//
//  ViewStateManager.swift
//  Created by Patrick Niemeyer on 9/14/16.
//

import Foundation

/// Maintain per-entity persistent local state for views using UserDefaults storage.
/// Use a ViewState subclass to implement a typed view for the data and retrieve it async 
/// with a ViewStatePromise.
public class ViewStateManager
{
    static var shared = ViewStateManager()

    func saveViewState(data: String, forId id: String) {
        logDebug("saved view state key:\(id) = value:\(data)")
        UserDefaults.standard.set(data, forKey: id)
    }
    
    /// Async call to get the local view state.
    func getViewStateData( forId id: String, complete : (String?)->() ) {
        complete(UserDefaults.standard.string(forKey: id))
    }

    /// Get or create a JsonConvertible (Codable) persisted view state type
    public func viewState<T>(forInstance instanceId: String, block: @escaping (T)->()) where T:ViewState & JsonConvertible
    {
        let id = "\(type(of: self)):\(ViewState.version):"+instanceId
        getViewStateData(forId: id) { stringData in
            //logDebug("ViewStateManager: loaded data: \(stringData), for id: \(id)")
            let viewState: T
            let emptyJson = "{}"
            if let jsonViewState = T(jsonString: stringData ?? emptyJson) {
                viewState = jsonViewState
            } else {
                logError("ViewStateManager: unable to restore view state, resetting");
                viewState = T(jsonString: emptyJson)!
            }
            viewState.viewStateManager = self
            viewState.id = id
            block(viewState)
        }
    }
    
    public func save(_ viewState: ViewState) {
        guard let id = viewState.id else { return }
        var str: String?
        if let jsonViewState = viewState as? JsonConvertible {
            str = jsonViewState.toJson()
        } else {
            fatalError("unknown view state type")
        }
        guard let stringData = str else {
            logError("ViewStateManager: no data to save")
            return
        }
        //logDebug("ViewStateManager: saving data: \(stringData), for id: \(id)")
        saveViewState(data: stringData, forId: id)
    }
}

/// Base class for view state implementations
/// Subclasses should adopt the JsonConvertible protocol to expose their state properties.
public class ViewState
{
    static let version = 1

    var viewStateManager: ViewStateManager?

    /// Unique id for this view; automatically prefixed with version and class type
    var id: String?

    // TODO: Remove / migrate from these old string view states to the new JsonConvertible version
    /// Set after initial values loaded by the view state promise.
    var initialized = false
    
    func save() {
        viewStateManager?.save(self)
    }
}


