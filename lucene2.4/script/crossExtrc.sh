#!/bin/bash


extract ( )
{
  #QEvenBM25b=0.3_QEAdap_RocProx4w=20F=0.6_20_50_KLb0.4_15.gz.eval:Average Precision: 0.2200
  REPLY=`echo $1 |sed -rn '\|KLb| s|.*BM25b=([0-9.]+)_QEAdap_RocProx4w=([0-9.]+)F=0.6_([0-9.]+)_([0-9]+)_KLb([0-9.]+)_([0-9.]+).gz.eval:Average Precision: ([0-9.]+)|\4\t\3\t\2|p'`
}

#out="QEvenBM25b=0.3_QEAdap_RocProx4w=20F=0.6_20_50_KLb0.4_15.gz.eval:Average Precision: 0.2200"
#map=`echo $out| cut -d : -f 3`;
#echo $out |sed -rn '\|KLb| s|.*BM25b=([0-9.]+)_QEAdap_RocProx4w=([0-9]+)F=0.6_20_5_KLb([0-9.]+)_117.gz.eval:Average Precision: ([0-9.]+)|\4\t\3\t\2|p'

#extract "$out"
#echo $REPLY

#exit

Op="QEven"

etc="WT10GT451-550"

for gtext in 20_10 20_20 20_30 20_50;do
  Op="QEven"
  for p in QOdd QEven; do
	echo "begin= $p $Op"
	if [ "$p" = "QEven" ]; then
	  	Op="QOdd";
		echo "eqal= $ $Op"
	fi  
	echo "$p $Op"
	f=`./listMAP.py var/results/sig12/$etc/ |grep "$p" |grep "$gtext" |sort -t: -k3 | tail -n 1` 
	echo "	$f"
	f1=`echo $f|sed "s/_[^_]*$/*.gz.eval/" |sed "s/$p/$Op/"`
	#echo $f1
	#ls var/results/sig12/GOV2/$f1|sed "s/\/[^\/]*$//"
	f3=`ls var/results/sig12/$etc/$f1|xargs|sed "s/.*\///"`
	f2=`ls var/results/sig12/$etc/$f1|xargs grep Average|uniq | sed 's/.*\///g'`
	echo "	$f3 $f2"
	extract "$f" 
	echo "$REPLY"
	extract "$f3:$f2"
	echo "$REPLY"
  done
done

exit

f=`./listMAP.py var/results/sig12/GOV2/ |grep QOdd |grep 20_10 |sort -t: -k3 | tail -n 1` 
echo $f
f1=`echo $f|sed "s/_[^_]*$/*.gz.eval/" |sed "s/QOdd/QEven/"`
echo $f1
#ls var/results/sig12/GOV2/$f1|sed "s/\/[^\/]*$//"
f3=`ls var/results/sig12/GOV2/$f1|xargs`
f2=`ls var/results/sig12/GOV2/$f1|xargs grep Average|uniq | sed 's/.*\///g'`
echo $f3 $f2


