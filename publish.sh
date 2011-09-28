#!/bin/sh
git checkout gh-pages || exit $?
./publish.sh
git checkout master    || exit $?
git push github master || exit $?
