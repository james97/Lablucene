#!/bin/bash

dirs=`find etc/* -type d`

for dir in $dirs; do
        cp etc/lucene-log.xml $dir
done   
