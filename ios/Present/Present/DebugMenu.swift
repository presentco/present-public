//
//  DebugMenu.swift
//  Present
//
//  Created by Patrick Niemeyer on 1/8/18.
//  Copyright © 2018 Present Company. All rights reserved.
//

import Foundation
import Eureka
import StringStylizer

public class DebugMenu : OverlayViewController
{
    let tintColor = Palette.presentPurple
    let fvc = FormViewController()
    let applicationServices: ApplicationServices

    public init(applicationServices: ApplicationServices) {
        self.applicationServices = applicationServices
        super.init()
        self.backgroundType = .blur
    }
    
    required public init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    public override func viewDidLoad()
    {
        super.viewDidLoad()
        
        fvc.view.alpha = 1.0
        fvc.view.backgroundColor = .white
        fvc.view.roundedCorners = 16
        fvc.view.clipsToBounds = true
        fvc.tableView?.backgroundView = nil // needed for alpha on iPhone
        fvc.tableView?.backgroundColor = UIColor.clear
        fvc.tableView?.isScrollEnabled = false
        //fvc.tableView?.separatorStyle = .none
        fvc.tableView?.contentInset = .init(top: 8, left: 0, bottom: 0, right: 0)
        fvc.tableView.isScrollEnabled = true
        fvc.tableView.showsVerticalScrollIndicator = false

        // Set default styles
        // TODO: Is there a way to combine these as defaults on the base row type?
        SliderRow.defaultCellUpdate = { cell,row in
            cell.backgroundColor = .clear
            cell.textLabel?.textColor = .black
        }
        CheckRow.defaultCellUpdate = { cell,row in
            cell.backgroundColor = .clear
            cell.textLabel?.textColor = .black
            cell.tintColor = self.tintColor
        }
        ButtonRow.defaultCellUpdate = { cell,row in
            cell.backgroundColor = .clear
            cell.textLabel?.textColor = self.tintColor
            cell.textLabel?.textAlignment = .left
        }
        SegmentedRow<String>.defaultCellUpdate = { cell, row in
            cell.backgroundColor = .clear
            cell.textLabel?.textColor = .black
        }
        TextRow.defaultCellUpdate = { cell, row in
            cell.backgroundColor = .clear
            cell.textLabel?.textColor = .black
            cell.tintColor = self.tintColor
        }
        SegmentedRow<String>.defaultCellUpdate = { cell,row in
            cell.backgroundColor = .clear
            cell.textLabel?.textColor = .black
            cell.tintColor = self.tintColor
        }

        let section = Section()  { section in }
        
        section <<< ButtonRow()
            .cellUpdate { cell, row in
                cell.textLabel?.attributedText =
                    "Debug Menu".stylize()
                        .font(UIFont.systemFont(ofSize: 22))
                        .color(.black)
                        .paragraphAlignment(.center).attr

                cell.isUserInteractionEnabled = false
                //cell.separatorInset = UIEdgeInsets(top: 0, left: 100, bottom: 0, right: 0)
            }
            .onCellSelection{ cell, row in
                logDebug("debugMenu: button pushed")
            }

        section <<< LabelRow() {
            let appVersion = Bundle.main.applicationVersion
            $0.title = "App Version: \(appVersion)"
        }
        
        section <<< LabelRow() { _ in
            /*
            let admin = applicationServices.userManager.userIsAdmin ? "*admin*" : ""
            let user = applicationServices.userManager.me.value?.name ?? ""
            let phone = PhoneNumber(string: applicationServices.userManager.userSession.phoneNumber ?? "")
            if applicationServices.userManager.authorizedToProceedToApp {
                $0.title = "\(user) (\(phone?.displayString ?? "")) \(admin)"
            } else {
                $0.title = "User: Not logged in."
            }*/
        }
        .cellUpdate { cell, row in
            let user = (self.applicationServices.userManager.me.value?.name ?? "")
                .stylize()
                .font(UIFont.systemFont(ofSize: 16))
                .color(.black)
                .attr
            let phone =
                ("  ("+(PhoneNumber(string: self.applicationServices.userManager.userSession.phoneNumber ?? "")?
                    .displayString ?? "") + ")")
                .stylize()
                .font(UIFont.systemFont(ofSize: 14))
                .attr
            let admin = (self.applicationServices.userManager.userIsAdmin ? "  Admin" : "")
                .stylize()
                .font(UIFont.italicSystemFont(ofSize: 14))
                .color(.red)
                .attr
            if self.applicationServices.userManager.authorizedToProceedToApp {
                cell.textLabel?.attributedText = user + phone + admin
            } else {
                cell.textLabel?.attributedText =
                    "User not logged in.".stylize()
                        .font(UIFont.systemFont(ofSize: 18))
                        .color(.red)
                        .attr
            }
        }

        #if !LOCAL_SERVER

            section <<< SegmentedRow<String>(){
                let title = "Server"
                $0.title = title
                $0.options = [Server.Endpoint.Description.production.rawValue, Server.Endpoint.Description.staging.rawValue]
                $0.value = Server.Endpoint.current.rawValue
                //logDebug("DebugMenu: start value = \($0.value)")
                $0.onChange { row in
                    if row.value == Server.Endpoint.current.rawValue { return }
                    //logDebug("DebugMenu: server change: \(row.value)")
                    
                    let confirmChangeServerAlert =
                        UIAlertController(
                            title: "Switch to \(row.value!)?",
                            message: "Continuing will quit the app.",
                            preferredStyle: .actionSheet
                            //preferredStyle: .alert
                        )
                        .withDestructive(title: "Continue") { _ in
                            self.applicationServices.service.updateDeviceToken(deviceToken: "SWITCHED-ENDPOINTS-DO-NOT-PUSH", completionHandler: { _ in
                                Server.setDesiredEndpoint(to: Server.Endpoint.Description(rawValue: row.value!)!)
                                exit(0)
                            })
                        }
                        .withCancel { _ in
                            //logDebug("DebugMenu: cancel")
                            row.value = Server.Endpoint.current.rawValue
                            row.updateCell() // not sure why this is necessary
                        }
                    self.present(confirmChangeServerAlert, animated: true)
                }
            }

        #endif
        
        section <<< SwitchRow("Nearby Joined Circles Only"){
            $0.title = $0.tag
            $0.value = self.applicationServices.userManager.userSession.nearbyJoinedCirclesOnly
        }
        .onChange { row in
            self.applicationServices.userManager.userSession.nearbyJoinedCirclesOnly = row.value ?? false
            self.applicationServices.userManager.groupManager.refreshJoinedGroups()
        }
        
        section <<< SwitchRow("Show Events"){
                $0.title = $0.tag
                $0.value = EventLoggingOverlay.shared.showEvents
            }
            .onChange { row in
                EventLoggingOverlay.shared.showEvents = row.value ?? false
            }
        
        section <<< SwitchRow("Override Home URL"){
            $0.title = $0.tag
            let override = self.applicationServices.userManager.userSession.overrideHomeUrl
            $0.value = override != nil
            }
            .onChange { row in
                if !(row.value ?? false) {
                    self.applicationServices.userManager.userSession.overrideHomeUrl = nil
                    self.fvc.form.rowBy(tag: "Home URL")?.updateCell()
                }
        }
        section <<< URLRow("Home URL") {
            $0.title = $0.tag
            $0.placeholder = "https://present.co"
            // Note: It seems to be necessary to set a value here and in cellUpdate().
            // Note: I even tried calling updateCell manually from here.
            if let url = self.applicationServices.userManager.userSession.overrideHomeUrl {
                $0.value = URL(string: url)
            }
            let switchTag = "Override Home URL"
            $0.hidden = .function([switchTag], { form -> Bool in
                let row: RowOf<Bool>! = form.rowBy(tag: switchTag)
                return row.value ?? false == false
            })
            }
            .cellUpdate{ cell, row in
                cell.textField.textColor = self.tintColor
                if let url = self.applicationServices.userManager.userSession.overrideHomeUrl {
                    row.value = URL(string: url)
                } else {
                    row.value = nil
                }
            }
            .onChange { row in
                self.applicationServices.userManager.userSession.overrideHomeUrl = row.value?.absoluteString
        }

        section <<< ButtonRow() {
            $0.title = "Clear Block List"
        }
        .cellUpdate{ cell, row in
            cell.textLabel?.textAlignment = .left
        }
        .onCellSelection{ cell, row in
            self.applicationServices.service.unblockUser(withToken: "RESET") { _ in
                self.applicationServices.userManager.groupManager.purgeAndRefreshLoadedGroups()
            }
        }

        section <<< ButtonRow() {
            $0.title = "Clear Image Cache"
        }
        .onCellSelection{ cell, row in
            ImageManager.shared.clearCache()
        }
        
        section <<< ButtonRow() {
            $0.title = "Reset Hide Women-Only"
            }
            .onCellSelection{ cell, row in
                self.applicationServices.userManager.hideWomenOnly.value = nil
                self.dismissOverlay()
        }
        
        // Reset the prefs related to post-onboarding login experience
        // (show the space picker, tour, etc.)
        section <<< ButtonRow() {
            $0.title = "First Launch Experience"
            }
            .cellUpdate{ cell, row in
                cell.textLabel?.textAlignment = .left
            }
            .onCellSelection{ cell, row in
                self.applicationServices.userManager.userSession.isFirstLaunch = true
                self.applicationServices.userManager.userSession.selectedSpace = nil
                self.applicationServices.userManager.userSession.selectedLocation = nil
                self.dismissOverlay()
            }

        section <<< ButtonRow() {
                $0.title = "Log Out"
            }
            .onCellSelection{ cell, row in
                let confirmRequireOnboardingAlert = UIAlertController(title: "Are you sure?", message: "Continuing will log you out and quit the app", preferredStyle: .actionSheet)
                    .withDestructive(title: "Log Out") { _ in
                        self.applicationServices.service.updateDeviceToken(deviceToken: "SWITCHED-ENDPOINTS-DO-NOT-PUSH", completionHandler: { _ in
                            self.applicationServices.userManager.resetLogin()
                            exit(0)
                        })
                    }
                    .withCancel()
                self.present(confirmRequireOnboardingAlert, animated: true)
            }
        
        /*
        section <<< ButtonRow()
            .cellUpdate{ cell, row in
                cell.textLabel?.attributedText = "Dismiss".stylize().paragraphAlignment(.center).color(.darkGray).attr
            }
            .onCellSelection{ cell, row in
                self.dismiss()
            }
        */

        fvc.form +++ section
        
        installChild(viewController: fvc, in: view)
        self.dismissableContentView = fvc.view
    }
    
    override func dismissOverlay() {
        super.dismiss(animated: true) {
            // Note: Dismissing the transparent (over current context) debug view controller
            // Note: will not trigger viewWillAppear on the underlying view, so we trigger the
            // Note: action here.
            // TODO: We should call the appearance transition methods here, right?
            self.applicationServices.screenPresenter.currentViewController.viewWillAppear(false)
            self.applicationServices.screenPresenter.currentViewController.viewDidAppear(false)
        }
    }
    
    public override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        let height = view.bounds.height * 5.0/8.0
        let bounds = CGRect(x:0, y:0, width: view.bounds.width - 16*2, height: height)
        fvc.view.bounds = bounds
        fvc.view.center = view.center
    }
    
}


