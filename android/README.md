Present Android Application
=============

## Configure your environment

The Android app currently depends on a local Maven repo for its shared libraries with the server, which must be built before the app. To set up a local server, see [these instructions](https://github.com/presentco/present/blob/master/java/README.md).

Now, you should be able to execute `./gradlew buildMavenDependencies` to populate the local Maven repo. (Ironically via gradle.)

## Service

The networking service is using a shared library [`RpcClient`](https://github.com/presentco/present/blob/master/java/wire-rpc-client/src/main/java/present/wire/rpc/client/RpcClient.java) via the app's `co.present.present.service.RpcManager` wrapper.

It is currently configured to default to the staging server on debug and production on release. You can change the environment at any time by swiping the debug drawer from the left edge of the app in any internal or debug build, or while signed in as an admin in the production build.

To set up a local server, see [these instructions](https://github.com/presentco/present/blob/master/java/README.md).

## Manage users on the staging server

It's hacky but you can manage users on the staging server by putting their info into Pat's testing tool.

To setup:
- `brew install maven`
- `brew install npm`
- `mvn package` (run this in the main `/present` folder, not `/present/android`)
- Download gcloud tools: https://cloud.google.com/sdk/docs/#install_the_latest_cloud_tools_version_cloudsdk_current_version
- Write application default credentials: Unpack google cloud download, go to `/bin`, and run: `./gcloud auth application-default login`.  
- Open `PatTester.java` and put in the information of the user you want to alter in staging.  


## Publish the app internally

The main point of the internal build is to have a different package name (co.present.present.internal) than the production build, so we can have a test version and prod version installed at the same time. Right now the package name is pretty much the only difference.

Our publishing process is just a gradle wrapper that runs our unit tests, builds the app, and runs a tiny shell script and uploads it to a Google Cloud bucket.  You'll need to do the same Google Cloud authorization as for managing the staging server. 

- Increment the build number and build version name (e.g. `109` and `2.7.0-alpha1`) and check in your changes.  This is used to generate the build tag.
- Run `./gradlew publishInternal`
- The build script will tag the commit with the build number, and post a link to the build, as well as a summary of changes since the last build, to the `#android` Slack channel.  You'll probably need to update the build script (`publish-apk.sh` since it's hardcoded to look for my (Lisa) commits.

## Publish the app to the Play Store

We are enrolled in [Google Play App Signing](https://support.google.com/googleplay/android-developer/answer/7384423). The key saved in our repo is our upload key. Google holds our release signing key; when we upload a bundle signed with our upload key to the Play Store, it unwraps the bundle and then re-signs it with the release key.  If we need to, we can re-download our upload key from the Play Store or get a new one.  

- Increment the build number and build version name (e.g. `110` and `2.8.0`) and check in your changes.  
- [Optional] Run `./gradlew publishInternal` so we have a copy of the internal build in our cloud bucket; it also tags the commit

- Pick `Build > Generate Signed Bundle/APK` from the menu in Android Studio
- Pick `Android App Bundle`
- The upload key signing info (passwords, key name, and keystore path) is directly in the gradle file under 
```
android 
    signingConfigs {
        production
    }
```
You may have to manually fill them in.
- Pick Build Type: release, Flavor: production
- The bundle will be generated under `android/app/release/app.aab`.
- Go to https://play.google.com/apps/publish.  and choose Present, then `Release Management > App Releases` then "Manage" in `Production Track`.  At the top go to the big blue button "Create Release".
- Drag the app bundle in to upload it and follow the instructions. MAKE SURE YOU PRESS "REVIEW" AND THEN "PUBLISH" ON THE NEXT PAGE.  It's a really confusing UI and if you don't make sure, you'll have a "draft" release that doesn't get published. 
- There's no way to be notified when the app goes live.  It should be within an hour, but you just have to refresh the page.

```
