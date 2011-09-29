#!/bin/sh
rm -rf javadoc/*
cp -pr build/docs/javadoc/* javadoc || exit $?
git add javadoc || exit $?
git commit -a -m "update javadoc" || exit $?
git push github gh-pages || exit $?
