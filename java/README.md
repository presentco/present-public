Present Server
==============

Deploying the API server
------------------------
This process aims to ensure changes are well tested before going to production and to minimize
conflicts between developers deploying to staging and production.

1. Test locally:
    1. `./bin/run-dev-server.sh`
    1. Run `DevelopmentTests` until they pass. Test manually. Check the logs.
1. Test in staging:
    1. In [#eng](https://present.slack.com/messages/C3MG3HYHM): “I’m deploying API to staging!”
    1. `./scripts/deploy-api-staging.sh`
    1. Run `StagingTests` until they pass. Test manually. Check the logs.
1. Deploy to production:
    1. Merge to master. *Production should always come from master.*
    1. In [#eng](https://present.slack.com/messages/C3MG3HYHM): “I’m deploying API to production!”
    1. `./scripts/deploy-api-production.sh`
    1. Run `ProductionTests`. Test manually. Check the logs.
    1. If anything looks fishy, re-deploy the [previous build](https://github.com/presentco/present/releases).
1. In [#eng](https://present.slack.com/messages/C3MG3HYHM): “My change is deployed to production. Your turn!"

Architecture
------------

Our backend is comprised of the following components:

- [`appengine-ear`](appengine-ear) - The umbrella App Engine application.
  - [`appengine-web`](appengine-web) - The *web* module. Contains [the static marketing
    site](appengine-web/src/main/webapp) and [the React-based client application](../present-webapp).
  - [`appengine-api`](appengine-api) - The *API* module. Implements the API used by our
    iOS, Android, and web-based client applications.
- [`proto`](../proto/present) - [Protocol Buffers](https://developers.google.com/protocol-buffers/docs/overview) used to define our API.
- [`live-server`](live-server) - Our real-time chat server. Runs on [GKE](https://cloud.google.com/container-engine/).
- [`wire-rpc`](wire-rpc-server) - The Java implementation of our in-house RPC framework.

Notable Dependencies
--------------------
- [Objectify](https://github.com/objectify/objectify/wiki) - Java API for Google Cloud Datastore
- [Wire](https://github.com/square/wire) - Protocol Buffers for Java
- [Guice](https://github.com/google/guice) - Dependency injection

Style
-----
We follow [Google's Java Style Guide](https://google.github.io/styleguide/javaguide.html).

Configure Your Environment (One Time)
-------------------------------------

### Install [Homebrew](https://brew.sh/)

    $ /usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"

### Install Java 8

    $ brew tap caskroom/versions
    $ brew cask install java8

### Install Maven

    $ brew install maven

### Install IntelliJ IDE (Optional)

    $ brew cask install intellij-idea-ce
    
Import our Java code style from `intellij/codestyle/Present.xml`.

### Install the App Engine SDK

    $ brew install app-engine-java
    
### Install npm

    $ brew install npm
    
### Install the web app

Go to the `present/present-webapp` subdirectory, and run: 

    $ npm install

###  Build the web app

    $ mvn -am -pl appengine-web install


Deploy and Run the Servers
--------------------------

### Run the App Engine Development Server

Use [run-dev-server.sh](https://github.com/presentco/present/blob/master/java/bin/run-dev-server.sh) build script from your local repo to deploy the server locally

    $ ./bin/run-dev-server.sh
    
This executes `mvn package` before launching App Engine with the `appengine-ear` module.

### Run all servers

Install ack for colorizing, tmux to run multiple servers:

    $ brew install ack tmux
    $ ./scripts/run-servers-tmux.sh

### Deploy Web to Staging

    $ mvn -am -pl appengine-web package
    $ ./scripts/deploy-web-staging.sh

### Deploying to Staging

    $ ./scripts/deploy-web-staging.sh
    $ ./scripts/deploy-api-staging.sh

### Deploying to Production

    $ ./scripts/deploy-web-production.sh
    $ ./scripts/deploy-api-production.sh

Send Example Emails
-------------------
The email template is in `appengine-api/src/main/resources/email.html`.

Once, before sending the example emails, you must build/install `appengine-api`:

    $ ./scripts/install-api.sh
    
Send example emails:

    $ ./scripts/send-example-emails.sh [email] [email]...
