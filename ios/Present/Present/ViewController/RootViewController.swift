//
//  RootViewController.swift
//  Present
//
//  Created by Dan Federman on 3/7/17.
//  Copyright © 2017 Present Company. All rights reserved.
//

import Foundation
import PresentProtos

public final class RootViewController: UINavigationController, PresentablePresenter, ScreenPresenter, ScreenTransitionAnimatorObserver,
        UIGestureRecognizerDelegate, UINavigationControllerDelegate
{
    // MARK: Properties
    public var currentViewController: UIViewController {
        return currentScreen.viewController
    }

    // TODO: get this private again
    var currentScreen: PresentedScreen {
        //logDebug("currentScreen: screenStack = \(screenStack), size = \(screenStack.count)")
        // We should always have a current screen.
        guard let lastScreen = screenStack.last else {
            fatalError("currentScreen: screenStack.last is nil!")
        }
        return lastScreen
    }

    // TODO: Get this private again
    var screenStack: [PresentedScreen] = [PresentedScreen(viewController: LaunchScreenViewController(), transition: .none)]
    {
        didSet
        {
            // Last ditch check for duplicate controller references in the screen stack which would blow up the navigation controller.
            // This could happen if the navigation stack is changed externally while we are in a transition and we end up pushing the
            // same VC twice. Use the executeAfterAnyTransitions() queue to avoid this.
            var set = Set<UIViewController>()
            for screen in screenStack {
                if set.contains(screen.viewController) {
                    logError("Screen stack contains the same screen more than once: \(screenStack). Bailing to previous value: \(oldValue)")
                    screenStack = oldValue
                } else {
                    set.insert(screen.viewController)
                }
            }
            //logDebug("screenStack didSet: here")
            
            syncViewControllerStack()
        }
    }
    private var showingLaunchScreen: Bool {
        return currentScreen.viewController is LaunchScreenViewController
    }

    private var currentScreenTransitionContext: ScreenTransitionContext?
    private var screenTransitionQueue = [() -> Void]()
    private var viewHasAppeared = false

    // TODO: get this private again
    /// The current stack of flows.
    var flowStack = [Flow]()
    
    /// The indicies of the first screen in the screenStack for a particular flow. The same length as flowStack.
    private var indiciesInScreenStackOfRootScreenInFlow = [Int]()

    let locationProvider: LocationProvider
    let notificationAuthorizationManager: NotificationAuthorizationManager
    let userManager: UserManager
    let personManager: PersonManager
    let urlOpener: URLOpener
    let eventSupressor: EventSupressor
    let imageManager: ImageManager
    let service: PresentService
    let applicationIconBadgeManager: ApplicationIconBadgeManager

    // TODO: get rid of these?
    var homeViewController: HomeViewController?

    var retainStandaloneWorkflowDelegate: StandaloneWorkflowStepDelegate?
    
    // MARK: Initialization
    
    public init(locationProvider: LocationProvider, notificationAuthorizationManager: NotificationAuthorizationManager, userManager: UserManager, personManager: PersonManager, urlOpener: URLOpener, eventSupressor: EventSupressor, imageManager: ImageManager, applicationIconBadgeManager: ApplicationIconBadgeManager, service: PresentService)
    {
        self.locationProvider = locationProvider
        self.notificationAuthorizationManager = notificationAuthorizationManager
        self.userManager = userManager
        self.personManager = personManager
        self.urlOpener = urlOpener
        self.eventSupressor = eventSupressor
        self.imageManager = imageManager
        self.service = service
        self.applicationIconBadgeManager = applicationIconBadgeManager

        super.init(nibName: nil, bundle: nil)
        
        isNavigationBarHidden = true
        delegate = self
    }
    
    public required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    // MARK: NotificationSupressionCapable
    
    public func shouldSuppress(notification: RemoteNotification) -> Bool {
        guard let topViewController = topViewController as? NotificationSupressionCapable else {
            return false
        }
        
        return topViewController.shouldSuppress(notification: notification)
    }
    
    // MARK: PresentablePresenter

    // Load any required data and show the appropriate screen for the presentable
    public func presentScreen(for presentable: Presentable)
    {
        switch presentable
        {
            case let .showGroupMessage(commentResponse):
                let groupId = PresentUUID(uuidString: commentResponse.groupId)
                userManager.groupManager.getGroup(withToken: groupId, completionHandler:
                { [weak self] (response: Response<UserDisplayableError, Group>) in
                    switch response {
                    case .error:
                        // TODO:(dan) Error handling!
                        break
                        
                    case let .success(group):
                        self?.pushGroup(for: group, showChat: true)
                    }
                })
                
            case let .showGroup(groupResponse):
                logDebug("RootViewController: presentable show group")
                let groupToken = PresentUUID(uuidString: groupResponse.uuid)
                userManager.groupManager.getGroup(withToken: groupToken, groupProto: groupResponse, completionHandler: { [weak self] (response) in
                    logDebug("RootViewController: presentable show group get group response")
                    switch response {
                    case .error:
                        // TODO:(dan) Error handling!
                        break
                        
                    case let .success(group):
                        logDebug("RootViewController: presentable push group")
                        self?.pushGroup(for: group)
                    }
                })
            
            case let .showGroupByGroup(group):
                pushGroup(for: group)
            
            case let .showGroupMembershipRequests(group):
                presentCircleMembers(for: group)

            case let .showProfile(userResponse):
                let person = personManager.getPerson(forProto: userResponse)
                presentPersonProfile(forPerson: person)

            case let .showCategory(categoryResponse, space):
                let category = categoryResponse.name
                presentHomeViewFeedTab { feedViewController in
                    DispatchQueue.main.async {
                        self.presentCategoryViewController(forCategory: category, space: space)
                    }
                }
            
            case let .showCategoryById(category, space):
                presentHomeViewFeedTab { feedViewController in
                    DispatchQueue.main.async {
                        self.presentCategoryViewController(forCategory: category, space: space)
                    }
                }
        }
    }
    
    // MARK: ScreenPresenter
    
    public func present(screen: UIViewController, allowRetreat: Bool, with desiredTransitionStyle: ScreenTransitionAnimator.TransitionStyle)
    {
        // Use no transition if we are still showing the launch screen (the fade adds 0.2s)
        let transitionStyle = showingLaunchScreen ? .none: desiredTransitionStyle

        let screenTransitionContext = ScreenTransitionContext(type: ((allowRetreat && !showingLaunchScreen) ? .push(screen) : .swap(screen)),
                                                              animator: ScreenTransitionAnimator(transitionStyle: transitionStyle))
        
        
        if !allowRetreat && flowStack.last != nil,
            let indexOfRootScreenInStackForCurrentFlow = indiciesInScreenStackOfRootScreenInFlow.last,
            indexOfRootScreenInStackForCurrentFlow != NSNotFound,
            let currentScreen = screenStack.last
        {
            // Squash all prior screens in this flow, since we can no longer retreat.
            screenStack.replaceSubrange(indexOfRootScreenInStackForCurrentFlow..<screenStack.endIndex, with: [currentScreen])
        }

        // Sanity check: Don't even attempt to present a screen that is already on the view stack.
        if viewControllers.contains(where: { $0 === screen }) {
            logError("Attempting to present same viewcontroller twice: \(screen).  Ignoring.")
        } else {
            present(screen: screen, screenTransitionContext: screenTransitionContext)
        }
    }
    
    public func presentModal(_ viewController: UIViewController) {
        self.present(screen: viewController, allowRetreat: true, with: .slideInVertical)
    }

    // TODO: This really needs a completion callback.
    public func goBack() {
        popScreen()
    }
    
    // MARK: ScreenTransitionAnimatorObserver
    
    public func transitionDidFinish(for screenTransitionAnimator: ScreenTransitionAnimator, transitionDidComplete: Bool) {
        eventSupressor.endIgnoringInteractionEvents()
        
        guard let finishedScreenTransitionContext = currentScreenTransitionContext, screenTransitionAnimator == finishedScreenTransitionContext.animator else {
            fatalError("Received finished transition for a screen animator \(screenTransitionAnimator) that is not the current screen animator \(String(describing: currentScreenTransitionContext?.animator))")
        }
        
        
        guard transitionDidComplete else {
            // We don't need to make any changes to our stack.
            return
        }
        
        switch finishedScreenTransitionContext.type {
        case let .push(newScreen):
            screenStack.append(PresentedScreen(viewController: newScreen, transition: finishedScreenTransitionContext.animator.transitionStyle))
            
        case let .swap(newScreen):
            screenStack.replaceSubrange((screenStack.endIndex - 1)..<screenStack.endIndex, with: [PresentedScreen(viewController: newScreen, transition: finishedScreenTransitionContext.animator.transitionStyle)])
            
        case .pop:
            guard screenStack.count > 1 else {
                fatalError("Attempting to pop screen but screen stack has only \(screenStack.count) screens!")
            }
            
            screenStack.removeLast()
            endCurrentFlowIfNecessary()
        }
        
        let lastIndex = indiciesInScreenStackOfRootScreenInFlow.count - 1
        if lastIndex >= 0 && indiciesInScreenStackOfRootScreenInFlow[lastIndex] == NSNotFound {
            // Mark where this flow started in our screen stack.
            indiciesInScreenStackOfRootScreenInFlow[lastIndex] = screenStack.count - 1
        }
        
        // Make sure our new screen is using the proper orientation.
        RootViewController.attemptRotationToDeviceOrientation()
        
        currentScreenTransitionContext = nil
        executeQueuedScreenTransition()
    }
    
    // MARK: UIGestureRecognizerDelegate
    
    public func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
        guard gestureRecognizer == interactivePopGestureRecognizer else {
            // We don't recognize this gesture recognizer!
            return false
        }
        
        return viewControllers.count > 1
    }
    
    // MARK: UINavigationControllerDelegate
    
    public func navigationController(_ navigationController: UINavigationController, animationControllerFor operation: UINavigationControllerOperation, from fromVC: UIViewController, to toVC: UIViewController) -> UIViewControllerAnimatedTransitioning? {
        return currentScreenTransitionContext?.animator
    }
    
    public func navigationController(_ navigationController: UINavigationController, interactionControllerFor animationController: UIViewControllerAnimatedTransitioning) -> UIViewControllerInteractiveTransitioning? {
        // We do not yet support any interactive presentations.
        return nil
    }
    
    public func navigationController(_ navigationController: UINavigationController, didShow viewController: UIViewController, animated: Bool) {
        guard currentScreen.viewController != viewController else {
            // We're already up to date!
            return
        }
        
        guard screenStack.count > viewControllers.count else {
            return
        }
        
        logEvent(type: .view, "Presented \(String(describing: type(of: viewController))), dismissed \(String(describing: type(of: currentScreen.viewController))) via pop gesture recognizer")
        
        // Make sure our screen stack is up to date.
        screenStack.removeLast()
        
        endCurrentFlowIfNecessary()
    }
    
    // MARK: UIViewController
    
    public override func viewDidLoad() {
        super.viewDidLoad()
        
        // Set our screen stack.
        syncViewControllerStack()
        
        interactivePopGestureRecognizer?.delegate = self
    }
    
    public override func viewDidAppear(_ animated: Bool) {
        logDebug("RootViewController: viewDidAppear")
        super.viewDidAppear(animated)
        
        viewHasAppeared = true
        
        executeQueuedScreenTransition()
    }
    
    public override var shouldAutorotate: Bool {
        return showingLaunchScreen ? false : currentScreen.viewController.shouldAutorotate
    }
    
    public override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
        return showingLaunchScreen ? .portrait : currentScreen.viewController.supportedInterfaceOrientations
    }
    
    public override var childViewControllerForStatusBarHidden: UIViewController? {
        return viewHasAppeared ? visibleViewController : nil
    }
    
    public override var prefersStatusBarHidden: Bool {
        guard showingLaunchScreen else {
            return currentScreen.viewController.prefersStatusBarHidden
        }
        
        return true
    }
    
    // MARK: Public Methods
    
    /// Adds a flow to the flow stack, sets the receiver as the flow's delegate, and calls `begin`. The next screen presentation after beginning a flow will force a screen push (rather than swap).
    /// - parameter flow: The flow to begin.
    public func begin(flow: Flow) {
        logEvent(type: .action, "Begin flow \(flow)")
        flowStack.append(flow)
        indiciesInScreenStackOfRootScreenInFlow.append(NSNotFound)
        flow.beginFlow()
    }

    // TODO: Convert to a Workflow, simplify
    public func pushFacebookOnboardingFlow()
    {
        let onboardingFlow = FacebookOnboardingFlow(service: service, screenPresenter: self, facebookLoginService: DefaultFacebookLoginService(), userManager: userManager)

        onboardingFlow.completionHandler =
        { [weak self, unowned onboardingFlow] in
            //logDebug("FacebookOnboardingFlow: Ending onboarding flow, screenStack = \(String(describing: self?.screenStack)), flowStack=\(String(describing: self?.flowStack))")
            guard let strongSelf = self else { return }
            
            strongSelf.didEnd(flow: onboardingFlow, shouldPopScreens: true)
        }

        begin(flow: onboardingFlow)
    }
    
    /// Unwinds the stack to go back to the root view controller.
    public func popToRootViewController()
    {
        guard let rootViewController = screenStack.first, rootViewController.viewController != currentScreen.viewController else {
            logError("Attempting to pop to root view controller when we are already on the root view controller!")
            return
        }
        
        logDebug("popToRootViewController: setting screen stack.")
        // Remove intermediary views from the stack.
        screenStack = [rootViewController, currentScreen]

        // Now pop back to the root view controller.
        logDebug("popToRootViewController: about to pop screen.")
        popScreen()
    }
    
    public func presentModal(alert: UIAlertController) {
        if !showingLaunchScreen, let topViewController = topViewController {
            topViewController.present(alert, animated: true)
            
        } else {
            screenTransitionQueue.append {
                self.presentModal(alert: alert)
            }
        }
    }

    public func executeAfterAnyScreenTransition(work: @escaping ()->Void)
    {
        self.screenTransitionQueue.append(work)
       
        if currentScreenTransitionContext == nil {
            // No current screen transition, prompt execution
            executeQueuedScreenTransition()
        } else {
            logDebug("Attempting to present screen during a transition! Enqueuing for later presentation")
        }
    }

    // MARK: Private Methods
    
    /// Force the navigation stack to match our screen stack.
    private func syncViewControllerStack() {
        //logDebug("setting nav stack to: \( screenStack.map { $0.viewController }) ")
        viewControllers = screenStack.map { $0.viewController }
        //logDebug("Post setting nav stack")
    }
    
    /// Executes a screen transition that was requested before the we view appeared on screen, or during the previous transition.
    private func executeQueuedScreenTransition() {
        if screenTransitionQueue.count > 0 {
            let screenTransition = screenTransitionQueue.removeFirst()
            log("executing screen transition: \(screenTransition)")
            screenTransition()
        }
    }

    /// Helper method for push(screen:transitionStyle:), swap(screen:transitionStyle:), and popScreen(). Do not call directly outside of those methods.
    /// Presents the specified screen using a screen transition context.
    private func present(screen: UIViewController, screenTransitionContext: ScreenTransitionContext) {
        guard viewHasAppeared else {
            // We can not yet present a screen.
            logDebug("Attempting to present \(String(describing: type(of: screen))) before the root view controller has appeared onscreen! Enqueing for later presentation")
            screenTransitionQueue.append {
                self.present(screen: screen, screenTransitionContext: screenTransitionContext)
            }
            return
        }
        
        guard currentScreenTransitionContext == nil else {
            // We are currently in the middle of a transition.
            logDebug("Attempting to present \(String(describing: type(of: screen))) during a transition! Enqueing for later presentation")
            screenTransitionQueue.append {
                self.present(screen: screen, screenTransitionContext: screenTransitionContext)
            }
            return
        }
        
        screenTransitionContext.animator.observer = self
        currentScreenTransitionContext = screenTransitionContext
        
        eventSupressor.beginIgnoringInteractionEvents()
        switch screenTransitionContext.type {
        case .push, .swap:
            pushViewController(screen, animated: true)
            
        case .pop:
            popViewController(animated: true)
        }
        //logDebug("nav stack = \(viewControllers)")
    }
    
    /// Retreat to the previous screen in the screen stack.
    private func popScreen() {
        // We are popping to the screen right before the last/current screen in the stack.
        let screenIndexToPopTo = screenStack.count - 2
        guard screenIndexToPopTo >= 0 && screenIndexToPopTo < screenStack.count else {
            fatalError("Attempting to dismissScreen when there is no valid screen to pop to")
        }
        
        let priorScreen = screenStack[screenIndexToPopTo].viewController
        let screenTransitionContext = ScreenTransitionContext(type: .pop, animator: ScreenTransitionAnimator(transitionStyle: currentScreen.transition.reversed))
        present(screen: priorScreen, screenTransitionContext: screenTransitionContext)
    }

    func didEnd(flow: Flow, shouldPopScreens: Bool = true) {
        guard flow === flowStack.popLast(), let indexOfFirstScreenInStack = indiciesInScreenStackOfRootScreenInFlow.popLast() else {
            logError("\(flow) ended when flow stack was \(flowStack)")
            return
        }

        if flowStack.count != indiciesInScreenStackOfRootScreenInFlow.count {
            logError("Ending \(flow) but flowStack count != indiciesInScreenStackOfRootScreenInFlow count: \(flowStack.count) != \(indiciesInScreenStackOfRootScreenInFlow.count)")
        }
        
        logEvent(type: .action, "Ending flow \(flow)")
        
        if shouldPopScreens, indexOfFirstScreenInStack != NSNotFound
        {
            // Remove necessary screens if they haven't already been removed for us (e.g. via a swipe).
            if screenStack.count > indexOfFirstScreenInStack {
                // Remove all prior screens in this flow from the stack.
                let rangeToRemove = indexOfFirstScreenInStack..<(screenStack.count - 1)
                if rangeToRemove.distance(from: rangeToRemove.startIndex, to: rangeToRemove.endIndex) > 0 {
                    screenStack.replaceSubrange(indexOfFirstScreenInStack..<(screenStack.count - 1), with: [])
                }
                
                // Pop to the screen that began this flow.
                popScreen()
            }
        }
    }
    
    private func endCurrentFlowIfNecessary() {
        guard let currentFlow = flowStack.last,
            let indexOfRootScreenInStackForCurrentFlow = indiciesInScreenStackOfRootScreenInFlow.last,
            indexOfRootScreenInStackForCurrentFlow > 0 && indexOfRootScreenInStackForCurrentFlow == screenStack.count - 2 else {
                // Nothing more to do here.
                return
        }
        
        // Popping this view popped the flow!
        currentFlow.completionHandler?()
    }
    
    func getOrCreateHomeViewController() -> HomeViewController
    {
        let homeViewController: HomeViewController
        
        if let existingHomeViewController = self.homeViewController {
            homeViewController = existingHomeViewController
            
        } else {
            homeViewController = HomeViewController()
            self.homeViewController = homeViewController
            
            logDebug("Created HomeViewController")
        }
        
        return homeViewController
    }
    
    public func presentHomeViewController()
    {
        logDebug("RootViewController: presentHomeViewController, sinceLaunch=\(AppDelegate.sinceLaunch)")
        guard self.homeViewController == nil else {
            // This can happen when a push notification made us show content before we had the customer's location.
            // It's an annoying race condition, but it isn't harmful to the user.
            logDebug("Attempting to presenting HomeViewController when one is already on the stack")
            return
        }
        
        let homeViewController = getOrCreateHomeViewController()
        
        let transitionStyle: ScreenTransitionAnimator.TransitionStyle
        
        if showingLaunchScreen || currentScreen.viewController is WaitingIndicatorViewController {
            transitionStyle = .fade
            
        } else {
            transitionStyle = .slideInHorizontal
        }
        
        present(screen: homeViewController, allowRetreat: false, with: transitionStyle)
    }
    
    // MARK: Private Structs
    
    public struct PresentedScreen {
        public let viewController: UIViewController
        public let transition: ScreenTransitionAnimator.TransitionStyle
    }
    
    private struct ScreenTransitionContext {
        public enum TransitionType {
            /// Adds a screen to the stack.
            case push(UIViewController)
            /// Removes the current screen, replacing it with the top screen on the stack.
            case pop
            /// Swaps the current screen with the new screen. Does not modify the stack.
            case swap(UIViewController)
        }
        
        public let type: TransitionType
        public let animator: ScreenTransitionAnimator
    }
}

public protocol ScreenPresenter: class {
    /// Presents a screen. Pushes a screen to the stack if retreat is allowed, otherwise swaps to the screen.
    func present(screen: UIViewController, allowRetreat: Bool, with desiredTransitionStyle: ScreenTransitionAnimator.TransitionStyle)
    
    /// Pops a screen from the stack.
    func goBack()
}

public protocol Flow: class, CustomStringConvertible {
    
    // TODO: Remove
    var completionHandler: (() -> Void)? { get set }
    
    /// Called to start a flow.
    /// When called, a flow should tell its presenter to present a screen.
    func beginFlow()
    
    /// Must be called by the flow when the flow completes.
    func completeFlow()
}

public extension Flow {
    
    func completeFlow() {
        self.completionHandler?()
    }
    
}

