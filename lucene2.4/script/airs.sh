#!/bin/bash

#export ETC="robust"
#export ETC="trec123Disk12T51-200"
#export ETC="WT10GT451-550"
export ETC="GOV2"

resultpath="--trec.results results/airs10/${ETC}"
weigtmodel="--Lucene.Search.WeightingModel Dirichlet_LM"
General="$resultpath --Lucene.TRECQuerying.outputformat TRECDocidOutputFormat $weigtmodel --Lucene.Search.LanguageModel true --Lucene.PostProcess QueryExpansionLM --term.selector.name DFRTermSelector --Lucene.QueryExpansion.Model ModelBasedTermSelector --QueryExpansion.RelevanceFeedback false --parameter.free.expansion false"

###################scan basic weighting model paras##########################

{
:<<Block 
#Best: WT10G (mu=1500),Gov2(mu =1000, b=0.4) 
for mu in 300 400 500 600 700 800 900 1000 1200 1300 1400 1500; do
    bin/waitForJobs.sh  trec_lucene.sh 4;
    bin/trec_lucene.sh -r $General --dlm.mu $mu &
done

weigtmodel="--Lucene.Search.WeightingModel BM25"
General="$resultpath --Lucene.TRECQuerying.outputformat TRECDocidOutputFormat $weigtmodel --Lucene.Search.LanguageModel false --Lucene.PostProcess QueryExpansionLM --term.selector.name DFRTermSelector --Lucene.QueryExpansion.Model ModelBasedTermSelector --QueryExpansion.RelevanceFeedback false --parameter.free.expansion false"
for b in 0.1 0.2 0.3 0.35 0.4 0.5 0.55 0.6 0.7 0.8 0.9; do
  bin/waitForJobs.sh  trec_lucene.sh 4;
  bin/trec_lucene.sh -r $General --bm25.b $b &
done
Block
}


##############################################################################

################DPH BASE#####################
weigtmodel="--Lucene.Search.WeightingModel DPH"
General="$resultpath --Lucene.TRECQuerying.outputformat TRECDocidOutputFormat $weigtmodel --Lucene.Search.LanguageModel false --Lucene.PostProcess QueryExpansionAdap --term.selector.name DFRTermSelector --Lucene.QueryExpansion.Model KL --QueryExpansion.RelevanceFeedback false --parameter.free.expansion true"
#bin/trec_lucene.sh -r $General &

##baseline DFR

:<<Block
for terms in 50; do
 for docs in 3 5 10 15 20 30 50; do
    bin/waitForJobs.sh  trec_lucene.sh 4;
    bin/trec_lucene.sh -r -q $General --expansion.mindocuments 1 --rocchio.weighted false --expansion.documents $docs --expansion.terms $terms --rocchio.beta $beta &
   done
 done
Block




weigtmodel="--Lucene.Search.WeightingModel DPH"
General="$resultpath --Lucene.TRECQuerying.outputformat TRECDocidOutputFormat $weigtmodel --Lucene.Search.LanguageModel false --Lucene.PostProcess QueryExpansionAdap --term.selector.name RocchioTermSelector --Lucene.QueryExpansion.Model KL --QueryExpansion.RelevanceFeedback false --parameter.free.expansion false"

##baseline rocchio
:<<Block
for beta in 0.3 0.4 0.5  0.7; do
  for terms in 50; do
   for docs in 3 5 10 15 20 30 50; do
    bin/waitForJobs.sh  trec_lucene.sh 5;
    bin/trec_lucene.sh -r -q $General --expansion.mindocuments 1 --rocchio.weighted false --expansion.documents $docs --expansion.terms $terms --rocchio.beta $beta &
   done
  done
done
Block


##weighted rocchio
:<<Block
for type in 3; do
 for beta in  0.6 0.8 1; do
  for terms in 50; do
   for docs in  10 30; do
    bin/waitForJobs.sh  trec_lucene.sh 5;
    bin/trec_lucene.sh -r -q $General --expansion.mindocuments 1 --rocchio.weighted true --expansion.documents $docs --expansion.terms $terms --rocchio.beta $beta --rocchio.weighteType $type --trec.query.partial true --trec.query.startid 751 --trec.query.endid 800 &
   done
  done
 done
done
Block

###############################################

##########Evaluate the control para
:<<Block
weigtmodel="--Lucene.Search.WeightingModel DPH"
General="$resultpath --Lucene.TRECQuerying.outputformat TRECDocidOutputFormat $weigtmodel --Lucene.Search.LanguageModel false --Lucene.PostProcess QueryExpansionAdap --term.selector.name RocchioTermSelector --Lucene.QueryExpansion.Model KL --QueryExpansion.RelevanceFeedback false --parameter.free.expansion false"
#0.1 0.2   0.6 0.8 0.9
for type in 3; do
 for beta in 0.2 0.4 0.6  2 5; do
  for terms in 50; do
   for docs in 15 30; do
    bin/waitForJobs.sh  trec_lucene.sh 5;
    bin/trec_lucene.sh -r -q $General --expansion.mindocuments 1 --rocchio.weighted true --expansion.documents $docs --expansion.terms $terms --rocchio.beta $beta --rocchio.weighteType $type &
   done
  done
 done
done

Block
################################LM Expansion######################################


weigtmodel="--Lucene.Search.WeightingModel Dirichlet_LM"
General="$resultpath --Lucene.TRECQuerying.outputformat TRECDocidOutputFormat $weigtmodel --Lucene.Search.LanguageModel true --Lucene.PostProcess QueryExpansionLM --term.selector.name RMTermSelector --Lucene.QueryExpansion.Model KL --QueryExpansion.RelevanceFeedback false --expansion.mindocuments 1 --parameter.free.expansion false"
bin/trec_lucene.sh -r $General --dlm.mu 500

:<<Block
 for alpha in 0.3 0.4 0.5 0.7; do
  for terms in 50; do
   for docs in 10; do
    bin/waitForJobs.sh  trec_lucene.sh 5;
    bin/trec_lucene.sh -r -q $General --dlm.mu 500 --lm.alpha $alpha --expansion.documents $docs --expansion.terms $terms --trec.query.partial true --trec.query.startid 751 --trec.query.endid 800 &
   done
  done
 done
Block

##################################################################################

###############Eval############################
bin/waitForJobs.sh  trec_lucene.sh 1;
bin/trec_lucene.sh -e $resultpath
##############################################
##############################################################################
exit

General="--Lucene.Search.WeightingModel Dirichlet_LM --Lucene.Search.LanguageModel true --dlm.mu 500 --Lucene.PostProcess QueryExpansionLM --term.selector.name TopicTermSelector --Lucene.QueryExpansion.Model KL --TopicTermSelector.expTag true --QueryExpansion.RelevanceFeedback true"
for alpha in 0.5 0.6 0.7; do
  for terms in 20 30 40; do
   for docs in 3 5 10 15 20; do
    for topic in 10 20; do
   bin/waitForJobs.sh  trec_lucene.sh 4;
   bin/trec_lucene.sh -r -q $General --expansion.terms $terms --expansion.documents $docs --lm.alpha $alpha --TopicTermSelector.strategy 5 --TopicTermSelector.NUM_TOPICS $topic&
   bin/trec_lucene.sh -r -q $General --expansion.terms $terms --expansion.documents $docs --lm.alpha $alpha --TopicTermSelector.strategy 6  --TopicTermSelector.NUM_TOPICS $topic &
   bin/trec_lucene.sh -r -q $General --expansion.terms $terms --expansion.documents $docs --lm.alpha $alpha --TopicTermSelector.strategy 7 --TopicTermSelector.NUM_TOPICS $topic &
    done
   done
  done
done





exit
interval=50;
for (( mu=$interval; mu<2000; mu=mu+interval ))  
do
   bin/waitForJobs.sh  trec_lucene.sh 6;
   bin/trec_lucene.sh -r --dlm.mu $mu &
   echo "$mu"
done



for alpha in 0.2 0.3 0.5 0.6; do
 for docs in 5 10 20 30 50; do
  for terms in 20  25 50 75; do
   bin/waitForJobs.sh  trec_lucene.sh 4;
   bin/trec_lucene.sh -r -q  --expansion.documents  $docs --expansion.terms $terms --Lucene.QueryExpansion.LMalpha $alpha --rocchio.beta $alpha&
  done
 done
done

exit
  for lambda in 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.9; do
   bin/waitForJobs.sh  trec_lucene.sh 3;
   bin/trec_lucene.sh -r --Lucene.WeightingModel.JelinekMercer_LM.lambda $lambda &
  done


exit;
for alpha in 0.2 0.3 0.4 0.5; do
  for terms in 20 30 40 50 70 100; do
   bin/waitForJobs.sh  trec_lucene.sh 2;
   bin/trec_lucene.sh -r -q  --expansion.terms $terms --Lucene.QueryExpansion.LMalpha $alpha &
  done
done

exit

for fbfile in YUIR.1 FDU.1 ugTr.1 UMas.2 ;do
 for process in CBRRocchioQueryExpansion; do
  for beta in 0.4; do
   bin/waitForJobs.sh  trec_lucene.sh 2;
   trec_lucene.sh -Dlucene.pDir=clueweb09 -r -q --Lucene.PostProcess $process --Rocchio.Feedback.filename ${fbase}$fbfile --rocchio.beta $beta &
  done 
 done
done
