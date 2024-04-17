#!/bin/bash

set -e

CURRENT_DIR=/opt/payara/itrend-setup
FILES_DIR=scripts

cd $CURRENT_DIR

for f in $CURRENT_DIR/$FILES_DIR/*.sh; do
    chmod +x "$f"
    echo "Running $f"
    bash "$f" -H
done 