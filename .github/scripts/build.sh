#!/usr/bin/env bash
./mvnw --version --settings settings.xml
./mvnw --no-transfer-progress --settings settings.xml clean install "$@"
