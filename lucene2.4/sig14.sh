#!/bin/bash

#export ETC="robust04"
#export ETC="WT2G"
#export ETC="trec123Disk12T51-200"


#export ETC="trec1Disk12T51-100"
#export ETC="trec2Disk12T101-150"
#export ETC="trec3Disk12T151-200"
export ETC="trec6Disk45T301-350"
#export ETC="trec7Disk45T351-400"
#export ETC="trec8Disk45T401-450"
#export ETC="trec9WT10GT451-500"
#export ETC="trec10WT10G501-550"
#export ETC="trec04GOV2T701-750"
#export ETC="trec05GOV2T751-800"
#export ETC="trec06GOV6T801-850"


resultpath="--trec.results results/tmp-sig14/${ETC}"
if [ ! -d "$resultpath" ]; then
 mkdir -p var/results/tmp-sig14/${ETC}
fi

weightmodel="--Lucene.Search.WeightingModel DLM --bm25.b 0.3 --wm.c 8 --dlm.mu 1000 --Lucene.Search.LanguageModel false"
output="--Lucene.TRECQuerying.outputformat TRECDocidOutputFormat "
#proximity="--proximity.enable false --proximity.model non --proximity.weight 0.3 --proximity.type FD --proximity.slop 15"
part="--trec.query.partial true --trec.query.startid 301 --trec.query.endid 305"
#part="--trec.query.partial false"
#part="--trec.query.partial true" 
#range=(151-160,166-180,186-200 150-155 156-200)
#range=(151-155 156-160 161-165 166-170 171-175 176-180 181-185 186-190 191-195 196-200 156-200 151-155,161-200 151-160,166-200 151-165,171-200 151-170,176-200 151-175,181-200 151-180,186-200 151-185,191-200 151-190,196-200 151-195)
General="$resultpath $output $weightmodel $part --Lucene.Search.LanguageModel false"



###################scan basic weighting model paras##########################

{
:<<Block 
#Best: WT10G (mu=1500),GOV2(mu =1000, b=0.4) 
  for mu in 1500; do
    bin/waitForJobs.sh  trec_lucene.sh 1;
    bin/trec_lucene.sh -r $General --dlm.mu $mu &
  done
Block

:<<Block

#weightmodel="--Lucene.Search.WeightingModel BM25 --bm25.b 0.3 --wm.c 6"
weightmodel="--Lucene.Search.WeightingModel BM25"

#for b in 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.9; do
  for b in 0.3; do
  #for ids in "${range[@]}" ;do
 # for parity in 0 1; do
  bin/waitForJobs.sh  trec_lucene.sh 6;
  bin/trec_lucene.sh -r $General  $weightmodel --bm25.b $b &
  #bin/trec_lucene.sh -r $General --proximity.enable false &
 # done 
done

Block

#######################Proximity model################################

#bin/trec_lucene.sh -r $General --bm25.b 0.75 &
:<<Block

#bin/trec_lucene.sh -r $General  --trec.query.partial true &
General="$resultpath $output $weightmodel $proximity"
for b in 0.3; do 
 for slop in 15 20; do
  bin/waitForJobs.sh  trec_lucene.sh 6;
  bin/trec_lucene.sh -r $General  --proximity.weight $b --proximity.slop $slop &
 done
done

Block
#########################################
#bin/trec_lucene.sh -r $General --Lucene.Search.WeightingModel PL2 --wm.c 6 &
}


##############################################################################:
:<<Block

weigtmodel="--Lucene.Search.WeightingModel DLM --lm.mu 1000"
General="$resultpath $output $weigtmodel $additional --Lucene.Search.LanguageModel true --Lucene.PostProcess QueryExpansionLM --Lucene.PostProcess QueryExpansionLM --term.selector.name TopicTermSelector --Lucene.QueryExpansion.Model KL --TopicTermSelector.expTag true --QueryExpansion.RelevanceFeedback false"
for alpha in 0.95 1; do
  for terms in 50; do
   for docs in 30; do
    for topic in 2 3 10; do
     for gama in 0.2 0.3; do
      for mix in false; do
   bin/waitForJobs.sh  trec_lucene.sh 4;
   bin/trec_lucene.sh -r -q $General --expansion.terms $terms --expansion.documents $docs --lm.alpha $alpha --TopicTermSelector.strategy 6  --TopicTermSelector.NUM_TOPICS $topic --FileFeedbackSelector.gama $gama --FileFeedbackSelector.mixTag $mix&
     done
     done
    done
   done
  done
done

Block

#########BM25 + TopicTermSelector################################
#:<<Block
weigtmodel="--Lucene.Search.WeightingModel  DPH --bm25.b 0.35" 
#weightmodel="--Lucene.Search.WeightingModel DLM "
General="$resultpath $output $part $weigtmodel $additional --Lucene.Search.LanguageModel false --Lucene.PostProcess QueryExpansionAdap  --term.selector.name TopicBasedTermSelector --Lucene.QueryExpansion.Model TFIDF --TopicBasedTermSelector.expTag true --QueryExpansion.RelevanceFeedback false --parameter.free.expansion false  --dlm.mu 1500"
word2vec="./word2vec/pyword2vec_jeff/text8.model.txt"
#bin/trec_lucene.sh -r  $General --Lucene.TRECQuerying.firstRound true
#for parity in 0 1 ; do
#for beta in 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.9; do
  for beta in 0.9; do 
 for terms in 20; do
#    for docs in 50; do
   for docs in 5 10 20 30 50; do
 #   for topic in  10 20 50 100 ; do
    #for gama in 1; do
     #for mix in false; do
       #for topicBeta in 0 0.2 0.4 0.6 0.8 1.0; do
		for topicBeta in 0.0; do
#	for ids in "${range[@]}" ;do
     bin/waitForJobs.sh  trec_lucene.sh 1 ;
     bin/trec_lucene.sh -r -q $General $part --expansion.mindocuments 1 --TopicBasedTermSelector.expTag true --rocchio.weighted true --expansion.terms $terms --expansion.documents $docs --rocchio.beta $beta  --TopicBasedTermSelector.word2vecDataPath $word2vec --TopicBasedTermSelector.beta $topicBeta  --dlm.mu 1500 --TopicBasedTermSelector.strategy 2 --TopicBasedTermSelector.expNum $terms --TopicBasedTermSelector.expDoc $docs --rocchio.normweight false &

        done
      # done
     # done
     done
  #  done
   done
 # done
done
#--FileFeedbackSelector.gama $gama --FileFeedbackSelector.mixTag $mix  
#Block


###############################################################




########## Rocchio ###################
:<<Block
weigtmodel="--Lucene.Search.WeightingModel DPH --wm.c 6 --bm25.b 0.35"
General="$resultpath $output $part $weigtmodel $additional $proximity --Lucene.Search.LanguageModel false --Lucene.PostProcess QueryExpansionAdap --term.selector.name RocchioTermSelector --Lucene.QueryExpansion.Model TFIDF --QueryExpansion.RelevanceFeedback false --parameter.free.expansion false"
#0.1 0.2   0.6 0.8 0.9
for type in 3; do
 for beta in 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.9 ; do
# for beta in 0.9; do
  for terms in 20; do
   for docs in 5 10 20 30 50; do
 # for docs in 30; do
#    for parity in 0 1; do
	for ids in "${range[@]}" ;do
    bin/waitForJobs.sh  trec_lucene.sh 7;
    bin/trec_lucene.sh -r -q $General --trec.query.range $ids --expansion.mindocuments 1 --rocchio.weighted false --expansion.documents $docs --expansion.terms $terms --rocchio.beta $beta  &
    #bin/trec_lucene.sh -r -q --proximity.enable true --proximity.weight 0.3 --proximity.slop 10 $General --expansion.mindocuments 1 --rocchio.weighted true --expansion.documents $docs --expansion.terms $terms --rocchio.beta $beta --rocchio.weighteType $type &
   done
  done
 done
done
done
Block
################################RM3 Expansion######################################
:<<Block
weigtmodel="--Lucene.Search.WeightingModel DLM --dlm.mu 1500"
General="$resultpath $weigtmodel $output --Lucene.Search.LanguageModel true --Lucene.PostProcess QueryExpansionLM --term.selector.name RM3TermSelector --Lucene.QueryExpansion.Model KL --QueryExpansion.RelevanceFeedback false --expansion.mindocuments 1 --parameter.free.expansion false"
#bin/trec_lucene.sh -r $General --dlm.mu 1000

 for alpha in 0.2 0.3 0.4 0.5 0.6 0.7 0.8; do
  for terms in 20; do
   for docs in 20; do
    bin/waitForJobs.sh  trec_lucene.sh 4;
    bin/trec_lucene.sh -r -q $General $proximity $part --lm.alpha $alpha --expansion.documents $docs --expansion.terms $terms &
   done
  done
 done

Block

##################################################################################

###############Eval############################
#bin/waitForJobs.sh  trec_lucene.sh 1;
#bin/trec_lucene.sh -e $resultpath $output $General --Lucene.TRECQuerying.outputformat TRECDocidOutputFormat
##############################################
##############################################################################
exit

