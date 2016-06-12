#!/bin/bash

# Script updated from git://github.com/tangentlabs/django-oscar.gitmaster
#
# Generate and push the source language file to Transifex.
# This script is called after every successful build on Travis CI.
# It relies on $TRANSIFEX_PASSWORD being set in .travis.yml

if [ "$TRAVIS_REPO_SLUG" == "Electrical-Age/ElectricalAge" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_BRANCH" == "1.7.10-MNA" ]
then

  echo "Installing the transifex client"
  python --version
  pip install --upgrade pip
  pip --version
  
  # Transifex-client version 0.12.1 is required
  # See https://github.com/transifex/transifex-client/issues/113  
  pip install transifex-client --upgrade

  echo "Generating the latest language source file"
  ./gradlew updateMasterLanguageFile

  echo "Submitting the generated translation source file to Transifex"
  # Write .transifexrc file
  echo "[https://www.transifex.com]
hostname = https://www.transifex.com
password = $TRANSIFEX_PASSWORD
token =
username = metc" > ~/.transifexrc
  
  tx push --source --translations -l en_us --no-interactive --skip
fi
