#!/bin/bash

# Script updated from git://github.com/tangentlabs/django-oscar.gitmaster
#
# Generate and push the source language file to Transifex.
# This script is called after every successful build on Travis CI.

if [ "$TRAVIS_REPO_SLUG" == "Electrical-Age/ElectricalAge" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_BRANCH" == "develop" ]
then
  echo "Installing Transifex client"
  pip install virtualenv
  virtualenv ~/env
  source ~/env/bin/activate
  pip install transifex-client

  echo "Generating the latest language source file from the develop branch"
  ./gradlew updateMasterLanguageFile

  echo "Submitting the generated translation source file to Transifex"
  # Write .transifexrc file
  echo "[https://www.transifex.com]
hostname = https://www.transifex.com
token = $TRANSIFEX_API_TOKEN
username = $TRANSIFEX_USER" > ~/.transifexrc
  
  tx push --source --translations -l en_us --no-interactive --skip

else
  echo "Ignore Transifex update..."
fi
