#!/bin/sh
git checkout gh-pages && ./publish.sh; ERROR=$?; git checkout master || exit $?
# That publish.sh isn't this script; it's a script in the gh-pages branch.
[ $ERROR -eq 0 ] || exit $ERROR
git push github master || exit $?
