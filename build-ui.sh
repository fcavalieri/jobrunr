#!/bin/bash
set -x
set -e
set -u
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
RELEASE_DIR="$SCRIPT_DIR/release"
rm -rf "$RELEASE_DIR"
mkdir -p "$RELEASE_DIR"

(
  cd "$SCRIPT_DIR/core/src/main/resources/org/jobrunr/dashboard/frontend"
  . ~/.nvm/nvm.sh
  nvm use 16
  npm install
  npm run build
)
