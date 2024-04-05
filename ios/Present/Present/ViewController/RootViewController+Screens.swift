//
//  RootViewController+Screens.swift
//  Present
//
//  Created by Patrick Niemeyer on 4/17/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import PresentProtos

// MARK: Screens

public extension RootViewController
{
    // MARK: Invite and add screens
    
    public func presentAddFriends() {
        AddFriendsViewController().do {
            present(screen: $0, allowRetreat: true, with: .slideInVertical)
        }
    }
    
    public func presentAddFriendsOnboarding(workflowDelegate: WorkflowStepDelegate) {
        AddFriendsOnboardingViewController().do {
            $0.workflowDelegate = workflowDelegate
            present(screen: $0, allowRetreat: false, with: .slideInHorizontal)
        }
    }
    
    public func presentGetStartedOverlayViewController() {
        let vc = GetStartedOverlayViewController()
        //present(screen: vc, allowRetreat: true, with: .slideInVertical)
        self.currentViewController.present(vc, animated: true)
    }
    
    // MARK: Create / Edit Circle
    
    public func presentCreateCircle(setupBlock: ((CreateCircleViewController)->Void)? = nil)
    {
        GatedCreateGroupViewController()
            .configurePlaceholder {
                $0.isModal = true
            }.do {
                if let setupBlock = setupBlock {
                    $0.configure(withBlock: setupBlock)
                }
            present(screen: $0, allowRetreat: true, with: .slideInVertical)
        }
    }
    
    public func presentCircleMembers(for group: Group) {
        CircleMembersViewController().do {
            $0.configure {
                $0.apply(group: group)
            }
            self.present(screen: $0, allowRetreat: true, with: .slideInHorizontal)
        }
    }
    
    public func presentEditCircle(
        forEditing group: Group, withDelegate delegate: EditCircleDelegate? = nil)
    {
        CreateCircleViewController().do {
            $0.apply(group: group)
            $0.editCircleDelegate = delegate
            present(screen: $0, allowRetreat: true, with: .slideInVertical)
        }
    }

    // MARK: Profile
    
    public func presentProfile(forUserId userId: String)
    {
        userManager.whenUserAuthorizedToProceedToApp {
            self.personManager.getPerson(userId: userId).onSuccess {
                self.presentPersonProfile(forPerson: $0, refresh: false)
            }.neverDisposed()
        }
    }
    
    public func presentPersonProfile(forPerson person: Person) {
        presentPersonProfile(forPerson: person, refresh: true)
    }
    
    /// If refresh is true the profile will be displayed immediately but refreshed from the server in the background
    public func presentPersonProfile(forPerson person: Person, refresh: Bool = true)
    {
        let profileViewController = GatedProfileViewController()
            .configurePlaceholder {
                $0.titleText.text = "User Profile"
                $0.subtitleText.text = "Sign in to view other users' profiles!"
                $0.titleImage.image = #imageLiteral(resourceName: "ProfileImage")
                $0.verifyPhoneNumberButton.isShown = true
                $0.isModal = true
            }.then {
                $0.setBackButtonVisible(true)
            }

        // Apply what we have immediately
        profileViewController.apply(person: person)

        // Fetch the latest profile for the user from the user manager when ready
        if refresh {
            userManager.whenUserAuthorizedToProceedToApp {
                self.service.getUser(userId: person.id) { response in
                    if case let .success(user) = response {
                        profileViewController.apply(person: self.personManager.getPerson(forProto: user))
                    }
                }
            }
        }
        present(screen: profileViewController, allowRetreat: true, with: .slideInVertical)
    }
    
    public func presentEditProfile()
    {
        EditProfileViewController().do {
            present(screen: $0, allowRetreat: true, with: .slideInVertical)
        }
    }
    
    public func presentLocationPicker()
    {
        LocationPickerViewController().do {
            present(screen: $0, allowRetreat: true, with: .slideInVertical)
        }
    }
    
    // TESTING
    public func presentNewPersonProfile(forPerson person: Person) {
        ProfileViewController().do {
            $0.apply(person: person)
            present(screen: $0, allowRetreat: true, with: .slideInVertical)
        }
    }
    
    public func presentCategoryViewController(
        forCategory category: String, space: PresentSpace? = nil)
    {
        let categoryViewController = CategoryCirclesListViewController(locationProvider: locationProvider, screenPresenter: self, userManager: userManager, category: category)
        categoryViewController.space = space
        present(screen: categoryViewController, allowRetreat: true, with: .slideInHorizontal)
    }
    
    /// Show a set groups
    public func presentGroupsViewController(groups: [Group], title: String) {
        let vc = GroupsViewController(locationProvider: locationProvider, userManager: userManager, screenPresenter: self, title: title, groups: groups)
        present(screen: vc, allowRetreat: true, with: .slideInHorizontal)
    }
    
    public func presentCreateGroupLocation(
        with delegate: CreateGroupLocationDelegate, allowRetreat: Bool, selectedLocation: SelectedLocation? = nil) {
        let createGroupLocationViewController = CreateGroupLocationViewController(
            delegate : delegate, screenPresenter: self, locationProvider: locationProvider, selectedLocation: selectedLocation)
        present(screen: createGroupLocationViewController, allowRetreat: allowRetreat, with: .slideInVertical)
    }
    
    public func presentMapView(withDelegate delegate: GroupMapViewDelegate) {
        presentMapView(group: nil, withDelegate: delegate)
    }
    public func presentMapView(withSelectedGroup group: Group, withDelegate delegate: GroupMapViewDelegate) {
        presentMapView(group: group, withDelegate: delegate)
    }
    public func presentMapView(group: Group? = nil, withDelegate delegate: GroupMapViewDelegate)
    {
        let groupMapViewController = GroupMapViewController()
        groupMapViewController.groupManager = userManager.groupManager
        groupMapViewController.selectedGroup = group
        groupMapViewController.delegate = delegate
        present(screen: groupMapViewController, allowRetreat: true, with: .slideInHorizontal)
    }
    
    // MARK: GroupScreenPresenter
    
    public func pushGroup(for group: Group) {
        let groupViewController = CircleViewController()
        groupViewController.configure {
            $0.configure(withGroup: group)
        }
        present(screen: groupViewController, allowRetreat: true, with: .slideInHorizontal)
    }
    
    // TODO: Remove
    public func pushGroup(for group: Group, showChat: Bool) {
        let groupViewController = CircleViewController()
        groupViewController.configure {
            $0.configure(withGroup: group)
        }
        present(screen: groupViewController, allowRetreat: true, with: .slideInHorizontal)
    }
    
    public func presentNotificationSettingsViewController() {
        let notificationSettingsVC = NotificationSettingsViewController(userManager: userManager, screenPresenter: self)
        
        present(screen: notificationSettingsVC, allowRetreat: true, with: .slideInHorizontal)
    }
    
    // MARK: Facebook
    
    public func presentWaitForApproval(allowRetreat: Bool, authorization: Present.Authorization?) {
        let waitForApprovalVC = WaitForApprovalViewController(urlOpener: urlOpener, screenPresenter: self) {
            if let authorization = authorization, authorization.hasBlockScreen {
                $0.setWaitText(stringValue: authorization.blockScreen.text)
            }
            // TESTING
            //$0.setWaitText(stringValue: "Testing wait screen message...")
        }
        present(screen: waitForApprovalVC, allowRetreat: allowRetreat, with: .slideInHorizontal)
    }
    
    public func presentWaitingIndicator() {
        let determiningWaitingIndicatingViewController = WaitingIndicatorViewController()
        
        present(screen: determiningWaitingIndicatingViewController, allowRetreat: true, with: .slideInHorizontal)
    }
    
    // MARK: Phone Signup
    
    public func presentPhoneSignupFlow()
    {
        let flow = PhoneSignupFlow()

        flow.completionHandler = { [weak flow] in
            // Note: be careful not to retain the flow
            guard let flow = flow else { return }
            logDebug("PresentPhoneSignupFlow: completion handler")
            self.didEnd(flow: flow, shouldPopScreens: true)
        }
        begin(flow: flow)
    }
    
    /// Show the phone number signup screen
    public func presentPhoneEntry(workflowDelegate: WorkflowStepDelegate) {
        let vc = SignupEnterPhoneViewController()
        vc.workflowDelegate = workflowDelegate
        present(screen: vc, allowRetreat: true, with: .slideInVertical)
    }
    
    /// Show the signup photo & name screen
    public func presentSignupPhotoAndName(workflowDelegate: WorkflowStepDelegate) {
        let vc = SignupPhotoAndNameViewController()
        vc.workflowDelegate = workflowDelegate
        present(screen: vc, allowRetreat: true, with: .slideInHorizontal)
    }
    
    /// Show the notifications prompt screen
    public func presentNotificationsPrompt(
        workflowDelegate: WorkflowStepDelegate,
        transition: ScreenTransitionAnimator.TransitionStyle = .slideInHorizontal
    ) {
        let vc = NotificationAccessViewController()
        vc.workflowDelegate = workflowDelegate
        present(screen: vc, allowRetreat: true, with: transition)
    }
    
    /// Show the notifications prompt screen outside of the signup workflow.
    public func presentNotificationsPromptStandalone() {
        let workflowDelegate = StandaloneWorkflowStepDelegate {
            self.goBack()
        }
        self.retainStandaloneWorkflowDelegate = workflowDelegate // someone has to own it
        presentNotificationsPrompt(workflowDelegate: workflowDelegate, transition: .slideInVertical)
    }
    
    // MARK: Home screen
    
    public func presentHomeViewFeedTab(completion: ((FeedViewController)->Void)? = nil) {
        presentTabOnHomeViewController(.feed) { homeViewController in
            completion?(homeViewController.feedViewController)
        }
    }
    public func presentHomeViewCreateTab(completion: ((GatedCreateGroupViewController)->Void)? = nil) {
        presentTabOnHomeViewController(.create) { homeViewController in
            completion?(homeViewController.createViewController)
        }
    }
    public func presentHomeViewProfileTab(completion: ((GatedProfileViewController)->Void)? = nil) {
        presentTabOnHomeViewController(.profile) { homeViewController in
            guard let profileViewController = homeViewController.profileViewController as? GatedProfileViewController else { fatalError("wrong view controller")
            }
            completion?(profileViewController)
        }
    }
    
    // Show a particular tab on the home view controller
    private func presentTabOnHomeViewController(
        _ tab: HomeViewController.NavigationTab,
        completion: ((HomeViewController)->Void)? = nil)
    {
        func pulse(tab: HomeViewController.NavigationTab, homeViewController: HomeViewController)
        {
            mainAfter(milliseconds: 200) {
                switch tab {
                case .create:
                    homeViewController.createButton.pulse(2.5)
                case .feed:
                    homeViewController.feedButton.pulse(2.5)
                case .profile:
                    homeViewController.profileButton.pulse(2.5)
                }
            }
        }
        // Note: We are double and triple chaining the dispatching to main here to try to avoid any possible display issues.
        // Note: We had a very weird crashing bug on selection of MyCircles tab without this.
        if currentScreen.viewController != homeViewController
        {
            DispatchQueue.main.async {
                logDebug("presentTabOnHomeViewController: popping to home view controller")
                self.popToRootViewController()
                DispatchQueue.main.async {
                    guard let homeViewController = self.homeViewController else { return }
                    homeViewController.navigationTab = tab
                    DispatchQueue.main.async {
                        completion?(homeViewController)
                        pulse(tab: tab, homeViewController: homeViewController)
                    }
                }
            }
        } else {
            DispatchQueue.main.async {
                logDebug("presentTabOnHomeViewController: home view controller currently showing")
                guard let homeViewController = self.homeViewController else { return }
                homeViewController.navigationTab = tab
                DispatchQueue.main.async {
                    completion?(homeViewController)
                    pulse(tab: tab, homeViewController: homeViewController)
                }
            }
        }
    }

    // TODO: Pass the comment id here to allow highlighting it in the chat
    /// - param showChat: if true show the chat tab, overriding default tab selection logic
    public func transitionToGroup(withToken groupToken: PresentUUID, showChat: Bool = false)
    {
        self.userManager.groupManager.getGroup(withToken: groupToken) { (result: Response<UserDisplayableError, Group>) in
            switch result {
            case .error:
                // Nothing to do here.
                break

            case let .success(group):
                let groupViewController = CircleViewController().then {
                    $0.configure {
                        $0.configure(withGroup: group)
                    }
                }

                // Clear the flow stack.
                self.flowStack = []

                let homeViewController = self.getOrCreateHomeViewController()
                homeViewController.navigationTab = .feed

                self.executeAfterAnyScreenTransition {
                    // Set our screen stack.
                    self.screenStack = [
                        PresentedScreen(viewController: homeViewController, transition: .fade),
                        PresentedScreen(viewController: groupViewController, transition: .slideInHorizontal)
                    ]
                }
            }
        }
    }
    
}
