#!/bin/sh
git checkout gh-pages || exit $?
rm -rf javadoc/*
cp -pr build/docs/javadoc/* javadoc \
&& git add javadoc \
&& git commit -a -m "update javadoc"
ERROR=$?
git checkout master || exit $?
[ $ERROR -eq 0 ] || exit $ERROR
git push github master || exit $?
git push github gh-pages || exit $?
