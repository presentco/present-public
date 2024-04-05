#!/bin/sh
ACK=ack 
if which -s $ACK 
then
    ack --flush --passthru --color --color-match=green "^.*INFO:.*" \
    | ack --flush --passthru --color --color-match=yellow "^.*FINE:.*" \
    | ack --flush --passthru --color --color-match=red "^.*ERROR.*" \
    | ack --flush --passthru --color --color-match=yellow "^.*WARNING.*"
else
    cat
fi

