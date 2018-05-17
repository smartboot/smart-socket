#!/usr/bin/env bash
set -e # halt script on error

bundle exec jekyll build
# bundle exec travis-lint
# bundle exec htmlproof ${HTML_FOLDER} --disable-external

cd ${HTML_FOLDER}

# config
git config --global user.email "nandomoreira.me@gmail.com"
git config --global user.name "Fernando Moreira"

# deploy
git init
git add --all
git commit -m "Deploy to GitHub Pages"
git push --force --quiet "https://${GH_TOKEN}@github.com/${GH_REF}" master:gh-pages