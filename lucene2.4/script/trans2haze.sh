#!/bin/bash
#ant compile
if [ -n "$1" ]; then
 ./jar.sh
fi

if [ "$1" == "-c1" ]; then
 scp lib/LabLucene1.0.jar benhe@haze.hprn.yorku.ca:/home/benhe/jeff/lucene2.4.1/lib
else 
 scp lib/LabLucene1.0.jar lib/DUTLIB.jar  benhe@haze.hprn.yorku.ca:/home/benhe/jeff/lucene2.4.1/lib
fi
