#!/bin/bash
set -e
set -u

SCRIPT_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
IMAGE_NAME="registry.gitlab.com/fcavalieri/jobrunr/ci"
IMAGE_VERSION="v0.0.4"
TAG="$IMAGE_NAME:$IMAGE_VERSION"

(
  cd "$SCRIPT_DIR"
  docker build --no-cache -t $TAG .
  docker push $TAG
)
