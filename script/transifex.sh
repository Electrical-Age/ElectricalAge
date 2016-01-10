#!/bin/bash

# Script updated from git://github.com/tangentlabs/django-oscar.gitmaster
#
# Push source and translation files to Transifex.
# This script is called after every successful build on Travis CI.
# It relies on $TRANSIFEX_PASSWORD being set in .travis.yml

if [ "$TRAVIS_REPO_SLUG" == "Electrical-Age/ElectricalAge" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_BRANCH" == "1.7.10-MNA" ]
then
    echo "Submitting translation files to Transifex"
    pip install transifex-client
    # Write .transifexrc file
    echo "[https://www.transifex.com]
hostname = https://www.transifex.com
password = $TRANSIFEX_PASSWORD
token =
username = metc" > ~/.transifexrc
    tx push --source --translations --no-interactive --skip
fi