//
//  Workflow.swift
//  Present
//
//  Created by Patrick Niemeyer on 5/7/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation

/// An screen element that participates in a series of steps.
public protocol WorkflowStep {
    var workflowDelegate: WorkflowStepDelegate? { get set }
}

/// Simple workflow callback model.  Each step indicates when it is complete.
public protocol WorkflowStepDelegate: class {
    func workflowStepComplete() // This step is complete
    func workflowStepGoBack() // The user wants to go back to the previous step
    func workflowStepCancelWorkflow() // The user wants to cancel this entire workflow
}

public extension WorkflowStepDelegate {
    func workflowStepGoBack() { }
    func workflowStepCancelWorkflow() { }
}

/// A workflow delegate that executes a block when the step is complete.
/// This can be used to present an individual workflow screen outside of
/// its normal workflow.
public class StandaloneWorkflowStepDelegate: WorkflowStepDelegate, ApplicationServices
{
    let workflowCompleteBlock: ()->Void
    
    public init(withWorkflowCompleteBlock workflowCompleteBlock: @escaping ()->Void) {
        self.workflowCompleteBlock = workflowCompleteBlock
    }
    
    public func workflowStepComplete() {
        workflowCompleteBlock()
    }
}
