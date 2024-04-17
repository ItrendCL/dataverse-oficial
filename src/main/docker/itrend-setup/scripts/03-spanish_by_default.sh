#!/bin/bash

# Based on:
# https://lattesdata.cnpq.br/config/index.php/home/dataverse/customize/Languages
# https://github.com/IQSS/dataverse/issues/9537

# This code replace the default language "en" to "es" and define the "us" as "en"
# The reason is that the default language is "en" and Dataverse dont allow to change it
WORKDIR=/opt/payara
DATAVERSE_DIR=$WORKDIR/dataverse-langs

TEMP_ALL_LANGUAGES=$DATAVERSE_DIR/langTmp
BUNDLE_DIR=$DATAVERSE_DIR/langBundles

mkdir -p $WORKDIR
mkdir $DATAVERSE_DIR
mkdir $BUNDLE_DIR

mkdir $TEMP_ALL_LANGUAGES


cd $TEMP_ALL_LANGUAGES/
wget https://github.com/GlobalDataverseCommunityConsortium/dataverse-language-packs/archive/refs/heads/dataverse-v$APP_VERSION.zip
unzip dataverse-v$APP_VERSION.zip

echo "copy files"

FILES_DIRECTORY=dataverse-language-packs-dataverse-v$APP_VERSION

FILES=(
    "astrophysics"
    "biomedical"
    "BuiltInRoles"
    "Bundle"
    "citation"
    "codeMeta20"
    "computationalworkflow"
    "customARCS"
    "customCHIA"
    "customDigaai"
    "customGSD"
    "custom_hbgdki"
    "customMRA"
    "customPSI"
    "customPSRI"
    "geospatial"
    "journal"
    "License"
    "MimeTypeDetectionByFileExtension"
    "MimeTypeDetectionByFileName"
    "MimeTypeDisplay"
    "MimeTypeFacets"
    "socialscience"
    "staticSearchFields"
    "ValidationMessages"
)

echo "======================== Copy files =us--en_US"

for file in "${FILES[@]}"; do
    cp "$TEMP_ALL_LANGUAGES/$FILES_DIRECTORY/en_US/$file.properties" "$BUNDLE_DIR/${file}_us.properties"
    echo "copying $file to $BUNDLE_DIR/${file}_us.properties"
done

echo "======================== Copy default files ="

for file in "${FILES[@]}"; do
    if [ -f "$TEMP_ALL_LANGUAGES/$FILES_DIRECTORY/es_ES/${file}_es.properties" ]; then
        cp "$TEMP_ALL_LANGUAGES/$FILES_DIRECTORY/es_ES/${file}_es.properties" "$BUNDLE_DIR/${file}_en.properties"
    else
        echo "File $file not found in es_ES directory."
    fi
done

echo "===>Preparing ZIP FILE"
cd $BUNDLE_DIR
zip languages.zip *.properties
asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} create-jvm-options "-Ddataverse.lang.directory=$BUNDLE_DIR"
curl http://localhost:8080/api/admin/datasetfield/loadpropertyfiles?unblock-key=${BLOCKED_API_KEY} -X POST --upload-file languages.zip -H "Content-Type: application/zip"

echo "===>Defining the default language"
curl http://localhost:8080/api/admin/settings/:Languages?unblock-key=${BLOCKED_API_KEY} -X PUT -d '[{"locale":"en","title":"Español"}, {"locale":"us","title":"English"}]'