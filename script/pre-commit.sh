#!/bin/bash

set -e

S_PRE=$(git stash list | wc -l)
git stash -q --keep-index
S_POST=$(git stash list | wc -l)
if [[ $S_PRE != $S_POST ]]; then
  trap 'git stash pop -q' EXIT
fi

# Tests...
gradle clean build
