#!/bin/sh
# Populate BigQuery from a Datastore backup.

if [ "$#" != "1" ]
then
  echo "Usage: load-bq.sh [backup ID]"
  exit
fi

BACKUP_ID=$1

TYPES="Group
Client
Comment
User
VerificationRequest
SavedGroups
Chat
BlockedUsers
Contact
CommentContainerView
Content
WhitelistedUser
WhitelistGeofences
Event"

for TYPE in $TYPES
do
  bq load --nosync --replace --autodetect --source_format DATASTORE_BACKUP \
    present-production:present.$TYPE gs://present-production-backups/$BACKUP_ID.$TYPE.backup_info
done

