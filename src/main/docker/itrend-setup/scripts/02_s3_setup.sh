#!/bin/bash

if [ "${AWS_BUCKET_NAME}" ]; then
    echo "Setting up S3 bucket"
    aws configure set aws_access_key_id "$AWS_ACCESS_KEY_ID" --profile $AWS_S3_PROFILE
    aws configure set aws_secret_access_key "$AWS_SECRET_KEY" --profile $AWS_S3_PROFILE
    aws configure set region "$AWS_REGION" --profile $AWS_S3_PROFILE
    aws configure set output "json" --profile $AWS_S3_PROFILE
fi