#!/bin/bash
(
  cd core/src/main/resources/org/jobrunr/dashboard/frontend
  npm install
  npm run build
)
./gradlew build --no-daemon -i clean build