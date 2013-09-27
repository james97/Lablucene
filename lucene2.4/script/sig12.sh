#!/bin/bash


if [ -n "$1" ];
then
 export ETC="$1";
elif [ ! -n "$ETC" ]; then
 #export ETC="WT2G"
 #export ETC="robust"
 #export ETC="trec123Disk12T51-200"
 #export ETC="WT10GT451-550"
 #export ETC="GOV2"
 export ETC="robustT301-450"
 #export ETC="GOV2RF08"
 #export ETC="TREC05GOV2"/
 #export ETC="WT2G"
fi

f [ -e "etc/$ETC" ]; then
 echo "using: ETC= $ETC"
else
 echo "there is not ETC= $ETC"
 exit
fi




resultpath="--trec.results results/sig12/${ETC}"
if [ ! -d "$resultpath" ]; then
 mkdir -p var/results/sig12/${ETC}
fi

weightmodel="--Lucene.Search.WeightingModel DLM --bm25.b 0.3 --wm.c 8 --dlm.mu 1000 --Lucene.Search.LanguageModel false"
output="--Lucene.TRECQuerying.outputformat TRECDocidOutputFormat "
#proximity="--proximity.enable false --proximity.model non --proximity.weight 0.3 --proximity.type FD --proximity.slop 15"
#part="--trec.query.partial true --trec.query.startid 751 --trec.query.endid 800"
part="--trec.query.partial false --trec.query.parity 1"
General="$resultpath $output --Lucene.Search.WeightingModel DLM --Lucene.Search.LanguageModel true"


#####################BM25+PRoc###################################################

#:<<Block                                                                                                                                            
weigtmodel="--Lucene.Search.WeightingModel BM25 --bm25.b 0.3 "
#part="--trec.query.partial true --trec.query.startid 751 --trec.query.endid 800"
General="$resultpath $output $weigtmodel  --Lucene.Search.LanguageModel false --Lucene.PostProcess QueryExpansionAdap  --term.selector.name RocchioTermSelector --rocchio.termSelector ProxTermSelector --Lucene.QueryExpansion.Model KL --QueryExpansion.RelevanceFeedback false --parameter.free.expansion false"
#for type in 3; do 
#for parity in 0 1; do
for winSize in 10 20 30 50 80 100 200 500 700 1000 1200 1500; do
#   for winSize in 10000; do
 #for beta in  0.1 0.3 0.5 0.7 0.9 1.2 1.4; do
 for beta in 1.0; do
 for terms in 10 20 30 50; do
     for docs in 20; do
      for proxtype in 1 2 4; do
    bin/waitForJobs.sh  trec_lucene.sh 4;
    bin/trec_lucene.sh -r -q $General  --expansion.mindocuments 1 --rocchio.weighted false --expansion.documents $docs --expansion.terms $terms --rocchio.beta $beta --ProxTermSelector.winSize $winSize  --ProxTermSelector.sd $winSize --ProxTermSelector.proxType $proxtype  --ProxTermSelector.normPow 0.6 &
   done
  done 
  done                                                                                                                                             
 done 
done


#Block

###############Eval############################
bin/waitForJobs.sh  trec_lucene.sh 1;
bin/trec_lucene.sh -e $resultpath $output $General --Lucene.TRECQuerying.outputformat TRECDocidOutputFormat

