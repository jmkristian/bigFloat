#!/bin/sh
mkdir javadoc
rm -r javadoc/*
cp -pr build/docs/javadoc/* javadoc
git commit -a -m "update javadoc"
git push github gh-pages
