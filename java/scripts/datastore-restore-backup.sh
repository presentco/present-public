#!/bin/sh

if [ $# -lt 3 ]; then
    echo "Usage: ./datastore-restore-backup gcloud_project_id datastore_bucket backup_folder_name"
    exit 1
fi

PROJECT_ID=$1
BUCKET=$2
FOLDER=$3

echo "WARNING: This will overwrite any existing entity with the same ID" 
echo "         and can NOT be undone."
echo "See: https://cloud.google.com/datastore/docs/export-import-entities"
echo "Restore datastore for ${PROJECT_ID} from backup in storage bucket ${BUCKET}/${FOLDER}? (y/n)" 

read answer
if echo "$answer" | grep -iq "^y" ;then
    echo "Backing up..."
else
    echo "Exiting."; exit
fi

echo "Setting ${PROJECT_ID} as default project..."

gcloud config set project $PROJECT_ID

echo
echo "Authenticating with Google..."
echo "You may be prompted to login."

gcloud auth login

echo
echo "Initating restore to ${PROJECT_ID} from backup ${BUCKET}/${FOLDER}..."

gcloud beta datastore import gs://$BUCKET/$FOLDER/$FOLDER.overall_export_metadata

# Alternative curl command if gcloud command line tool does not work.
# curl \
# -H "Content-Type: application/json" \
# -d '{
#   "inputUrl": "gs://$BUCKET/$FOLDER/$FOLDER.overall_export_metadata",
# }'

if [ "$?" -ne "0" ]; then
	echo
	echo "There was a problem initiating the datastore restore. Please try again."
else
	echo
	echo "Datastore restore successfully initiated."
	echo "You can monitor progress with the following command: "
	echo "'gcloud beta datastore operations list'."
	echo "See also: https://cloud.google.com/datastore/docs/export-import-entities"
fi

exit 0
