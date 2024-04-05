Bubble iOS
======

Running Bubble
--------------

To run Bubble, open `Bubble.xcworkspace`, select your simulator or device target, and press the Build & Run button. Note that `Bubble.xcodeproj` does not include our dependencies, and can not be built & run outside of the workspace.

Cocoapods
---------

Since [we check the Pods directory into our repo](https://guides.cocoapods.org/using/using-cocoapods.html#should-i-check-the-pods-directory-into-source-control), day-to-day usage does not require wrangling Cocoapods.

### Updating Cocoapods
To update Cocoapods, change the cocoapods version in the Gemfile and then run `bundle exec pod install`

### Updating Dependencies
To update all dependencies, run `bundle exec pod update`.
To update an individual dependency, run `bundle exec pod update DependencyName`

### Modifying Dependencies
To add, remove, or change the version of a dependency, add, remove, or change the version of the desired dependency in the Podfile, then run `bundle exec pod install --repo-update`
