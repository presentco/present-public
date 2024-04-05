#!/bin/sh

if [ "$#" -lt 2 ]; then
    echo "Usage: ./datastore-backup-create gcloud_project_id storage_bucket [backup_folder]"
    echo "Note: backup_folder is optional. Timestamp used as default folder name."
    exit 1
fi

PROJECT_ID=$1
BUCKET=$2

if [ "$3" = "" ]; then 
	FOLDER="$(date +"%Y-%m-%d-T%H-%M-%S")"
else
	FOLDER=$3
fi

echo "Backup datastore for ${PROJECT_ID} to storage bucket ${BUCKET}/${FOLDER}? (y/n)"
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
echo "Initating backup for ${PROJECT_ID} to cloud storage bucket ${BUCKET}/${FOLDER}..."

gcloud beta datastore export gs://$BUCKET/$FOLDER

# Alternative curl command if gcloud command line tool does not work.
# curl \
# -H "Authorization: Bearer $(gcloud auth print-access-token)" \
# -H "Content-Type: application/json" \
# https://datastore.googleapis.com/v1beta1/projects/${PROJECT_ID}:export \
# -d '{
#   "outputUrlPrefix": "gs://'${BUCKET}/${FOLDER}'",
#   "entityFilter": {
#     "namespaceIds": [""],
#   },
# }'

if [ "$?" -ne "0" ]; then
	echo
	echo "There was a problem initiating the datastore backup. Please try again."
else
	echo
	echo "Datastore backup successfully initiated."
	echo "You can monitor progress with the following command: "
	echo "'gcloud beta datastore operations list'."
	echo "See also: https://cloud.google.com/datastore/docs/export-import-entities"
fi

exit 0
