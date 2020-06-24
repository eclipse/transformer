#!/usr/bin/env bash
./mvnw --batch-mode --version
./mvnw --batch-mode --no-transfer-progress install "$@"
