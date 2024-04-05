Local Maven Repository
----------------------

This directory houses dependencies that aren't available in remote repositories. What follows
are examples of how to add libraries to this repo.

Google S2 Geometry Library
--------------------------

    $ cd repo
    $ mvn deploy:deploy-file \
        -Durl=file://`pwd` \
        -Dfile=../../../s2-geometry-library-java/build/s2-geometry-java.jar \
        -Dsources=../../../s2-geometry-library-java/build/s2-geometry-java-sources.jar \
        -DgroupId=com.google.common \
        -DartifactId=s2-geometry \
        -Dpackaging=jar \
        -Dversion=1.0

Google Cloud Trace
------------------

Comes from https://github.com/crazybob/cloud-trace-java.

Installing 3rd Party Snapshots
-------------------------------

    $ cd [3rd party repo]

Set the version to BUBBLE-SNAPSHOT:
    
    $ mvn --batch-mode release:update-versions -DdevelopmentVersion=BUBBLE-SNAPSHOT
    
Deploy Wire to the Maven repo in Bubble's Git repo (after updating path to Bubble's git repo):
    
    $ mvn deploy -DaltDeploymentRepository=project.local::default::file:/Users/bob/Bubble/bubble/java/repo
