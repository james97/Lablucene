#!/bin/bash


cd ../dutir-lib/
ant jar;
cd ../lucene2.4/
ant jar;

echo 'input 1 to yezheng@irlab, 2 to jeff@okapi01, 3 to both'
read choose

if [ $choose == "1" ]; then
 scp lib/LabLucene1.0.jar yezheng@10.7.10.220:~/workspace/lucene2.4.1/lib
 scp lib/DUTLIB.jar yezheng@10.7.10.220:~/workspace/lucene2.4.1/lib  
elif [ $choose == "2" ]; then
 scp lib/LabLucene1.0.jar jeff@10.7.11.37:~/workspace/lucene2.4/lib
 scp lib/DUTLIB.jar jeff@10.7.11.37:~/workspace/lucene2.4/lib
elif [ $choose == "3" ]; then
 scp lib/LabLucene1.0.jar yezheng@10.7.10.220:~/workspace/lucene2.4.1/lib
 scp lib/DUTLIB.jar yezheng@10.7.10.220:~/workspace/lucene2.4.1/lib 
 scp lib/LabLucene1.0.jar jeff@10.7.11.37:~/workspace/lucene2.4/lib
 scp lib/DUTLIB.jar jeff@10.7.11.37:~/workspace/lucene2.4/lib
fi




