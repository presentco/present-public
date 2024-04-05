Cast Placeholder Server
=======================

An App Engine server for our simple placeholder app

The following commands should be run from the `java` directory, one level up.

Make just the placeholder server:

    $ mvn -am -pl cast-placeholder package

Run the placeholder server locally:

    $ ~/appengine-java-sdk/bin/dev_appserver.sh cast-placeholder/target/cast-placeholder-1.0

Deploy:

    $ ~/appengine-java-sdk/bin/appcfg.sh update cast-placeholder/target/cast-placeholder-1.0
