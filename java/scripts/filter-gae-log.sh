#!/bin/sh
# remove warm db calls
# remove empty : lines
egrep -v 'warmdb|Warm DB|^[[:space:]]{1,}:[[:space:]]{1,}$'
