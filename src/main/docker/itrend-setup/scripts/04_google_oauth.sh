#!/bin/bash

if [ "${GOOGLE_CLIENT_ID}" ]; then
    json_file="${HOME_DIR}/itrend-setup/google.json"

    sed -i "s/GOOGLE_CLIENT_ID/${GOOGLE_CLIENT_ID}/g" ${json_file}
    sed -i "s/GOOGLE_CLIENT_SECRET/${GOOGLE_CLIENT_SECRET}/g" ${json_file}

    curl -X POST -H 'Content-type: application/json' --upload-file ${json_file} localhost:8080/api/admin/authenticationProviders?unblock-key=${BLOCKED_API_KEY}
fi