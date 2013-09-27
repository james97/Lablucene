#ant compile
#ant jar
#scp lib/LabLucene1.0.jar zheng@10.7.10.220:/home/yezheng/workspace/lucene2.4.1/lib
#scp lib/DUTLIB.jar zheng@10.7.10.220:/home/yezheng/workspace/lucene2.4.1/lib
#scp lib/commons-httpclient-3.1.jar zheng@10.7.10.220:/home/yezheng/workspace/lucene2.4.1/lib
#ant compile
if [ -n "$1" ]; then
 ./jar.sh
fi

#scp lib/LabLucene1.0.jar lib/DUTLIB.jar  benhe@haze.hprn.yorku.ca:/home/benhe/jeff/lucene2.4.1/lib

lftp -u yezheng,19810716 publish.yorku.ca/LabLucene
mput lib/LabLucene1.0.jar
exit
