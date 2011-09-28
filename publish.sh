#!/bin/sh
rm -r *.html *.css com package-list resources
cp -pr build/docs/javadoc/* .
git commit -a -m "update javadoc"
git push github gh-pages
