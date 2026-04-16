#!/bin/sh

BITBUCKET_URL=ssh://git@bitbucket.org/itba/paw-2026a-14.git

git remote add bitbucket "$BITBUCKET_URL" 2>/dev/null || \
git remote set-url bitbucket "$BITBUCKET_URL"
