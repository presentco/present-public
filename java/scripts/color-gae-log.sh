#!/bin/sh

# 0.1.0.1 - - [13/Apr/2017:00:21:00 -0700] "GET /hourly HTTP/1.1" 200 0 - "AppEngine-Google; (+http://code.google.com/appengine)" "api.xxx-staging.appspot.com" ms=361 cpu_ms=1090 cpm_usd=0.000252 queue_name=__cron task_name=ccb10560ed8e9cd9c7f5da927c188c3b instance=00c61b117cd35a5b45ce34eaf6ebdfa8d107baa4f7b51d110828eecfa705d0f620b7667c28abaac0758f410355 app_engine_release=1.9.48
#208.66.28.150 - - [13/Apr/2017:10:52:02 -0700] "POST /api/GroupService/getNearbyGroups HTTP/1.1" 200 25274 - "Present/8 CFNetwork/811.4.18 Darwin/16.5.0" "api-dot-xxx-staging.appspot.com" ms=178 cpu_ms=348 cpm_usd=0.002825 instance=00c61b117cd35a5b45ce34eaf6ebdfa8d107baa4f7b51d110828eecfa705d0f620b7667c28abaac0758f410355 app_engine_release=1.9.48

ACK=ack 
if which -s $ACK 
then
    ack --flush --passthru --color --color-match=green "^.*\"GET /.*" \
    | ack --flush --passthru --color --color-match=green "^.*\"POST /.*"
else
    cat
fi

