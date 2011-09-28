#!/bin/sh
git checkout gh-pages || exit $?
./publish.sh # That's not this script, since we just checked out a different branch.
git checkout master    || exit $?
git push github master || exit $?
