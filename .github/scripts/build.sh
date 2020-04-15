#!/usr/bin/env bash
./gradlew --no-daemon --version
./gradlew --no-daemon build "$@"
