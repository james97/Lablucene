#!/bin/bash
########################################################################
#				read me
########################################################################
#on haze server:
#needed files: [train] train.fold1.txt, train.fold2.txt, etc.
#	       [qrels] qrels.all.rel
#          [jars]compareEvaluationFiles.jar, getResult.jar, RankLib.jar
#		   [test] test.fold1.txt, test.fold2.txt, etc.
#needed folders:models, scores
#				runs{and sub folders:r,r.i,r.i.t,r.i.t.s}
#				results{and sub folders:r,r.i,r.i.t,r.i.t.s}
#
#
#
#
#
########################################################################



########################################################################
#				train
########################################################################
for fold in 1 2
do
for r in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20
do

java -jar ../RankLib.jar -train train.fold$fold.txt -ranker 4 -silent -metric2t NDCG -save models/train.fold$fold.r.$r.model -r $r
#if try to optimize MAP, change NDCG to MAP here

done
done

########################################################################
#				test
########################################################################
for fold in 1 2
do
for r in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20
do 

java -jar ../RankLib.jar -rank test.fold$fold.txt -metric2T NDCG -load models/train.fold$fold.r.$r.model -score scores/test.fold$fold.r.$r.s
#if try to optimize MAP, change NDCG to MAP here
done
done


########################################################################
#				get score & run
########################################################################
for fold in 1 2
do
for r in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20
do
java -jar ../getResult.jar test.fold$fold.info scores/test.fold$fold.r.$r.s runs/train.fold$fold.r.$r.run
done
done


for r in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20
do
cat runs/train.fold1.r.$r.run runs/train.fold2.r.$r.run > runs/r/train.r.$r.run
done

########################################################################
#				evaluate
########################################################################
rm results/r/*
for r in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20
do
trec_eval qrels.all.rel runs/r/train.r.$r.run -m ndcg> results/r/train.r.$r.result
done

########################################################################
#				find best model
########################################################################
java -jar compareEvaluationFiles.jar results/r/ ndcg > tmp/r.log
tail tmp/r.log -n 1 | cut -d . -f 3 | read bestr
# echo $bestr

##
# infile=tmp/r.log 
# while read a
# do 
# 	echo $a
# done < $infile


########################################################################
#				train iteration
########################################################################
for fold in 1 2
do
for i in 10 25 35 45 55 65 75
do
java -jar ../RankLib.jar -train train.fold$fold.txt -ranker 4 -silent -metric2t NDCG -save models/train.fold$fold.r.$bestr.i.$i.model -r $bestr -i $i
#if try to optimize MAP, change NDCG to MAP here
done
done

########################################################################
#				test iteration
########################################################################
for fold in 1 2
do
for i in 10 25  35 45 55 65 75
do 
java -jar ../RankLib.jar -rank test.fold$fold.txt -metric2T NDCG -load models/train.fold$fold.r.$bestr.i.$i.model -score scores/test.fold$fold.r.$bestr.i.$i.s
#if try to optimize MAP, change NDCG to MAP here
done
done

########################################################################
#				get score & run for iteration
########################################################################
# rm runs/r.i/*
for fold in 1 2
do
for i in 10 25  35 45 55 65 75
do
java -jar ../getResult.jar test.fold$fold.info scores/test.fold$fold.r.$bestr.i.$i.s runs/train.fold$fold.r.$bestr.i.$i.run
done
done


for i in 10 25  35 45 55 65 75
do
cat runs/train.fold1.r.$bestr.i.$i.run runs/train.fold2.r.$bestr.i.$i.run > runs/r.i/train.r.$bestr.i.$i.run
done

# ########################################################################
# #				evaluate for iteration
# ########################################################################
rm results/r.i/*
for i in 10 25  35 45 55 65 75
do
trec_eval qrels.all.rel runs/r.i/train.r.$bestr.i.$i.run -m ndcg > results/r.i/train.r.$bestr.i.$i.result
done

# ########################################################################
# #				find best model for iteration
# ########################################################################
# rm results/r.i/.*
java -jar compareEvaluationFiles.jar results/r.i/ ndcg > tmp/r.i.log
#infile=tmp/r.log 
#while read a
#do 
#	echo $a
#done < $infile
tail tmp/r.log -n 1 | cut -d . -f 3 | read bestr
tail tmp/r.i.log -n 1 | cut -d . -f 5 | read besti
echo $besti




########################################################################
#				train tolerance
########################################################################
for fold in 1 2
do
for t in 0.0001 0.0003 0.0005 0.0007 0.001 0.003 0.005 0.007 0.01
do
java -jar ../RankLib.jar -train train.fold$fold.txt -ranker 4 -silent -metric2t NDCG  -r $bestr -i $besti -tolerance $t -save models/train.fold$fold.r.$bestr.i.$besti.t.$t.model
#if try to optimize MAP, change NDCG to MAP here
done
done

########################################################################
#				test tolerance
########################################################################
for fold in 1 2
do
for t in 0.0001 0.0003 0.0005 0.0007 0.001 0.003 0.005 0.007 0.01
do 
java -jar ../RankLib.jar -rank test.fold$fold.txt -metric2T NDCG -load models/train.fold$fold.r.$bestr.i.$besti.t.$t.model -score scores/r.i.t/test.fold$fold.r.$bestr.i.$besti.t.$t.s
#if try to optimize MAP, change NDCG to MAP here
done
done

########################################################################
#				get score & run for tolerance
########################################################################
for fold in 1 2
do
for t in 0.0001 0.0003 0.0005 0.0007 0.001 0.003 0.005 0.007 0.01
do
java -jar ../getResult.jar test.fold$fold.info scores/r.i.t/test.fold$fold.r.$bestr.i.$besti.t.$t.s runs/r.i.t/train.fold$fold.r.$bestr.i.$besti.t.$t.run
done
done


for t in 0.0001 0.0003 0.0005 0.0007 0.001 0.003 0.005 0.007 0.01
do
cat runs/r.i.t/train.fold1.r.$bestr.i.$besti.t.$t.run runs/r.i.t/train.fold2.r.$bestr.i.$besti.t.$t.run > runs/r.i.t/train.r.$bestr.i.$besti.t.$t.run
done

########################################################################
#				evaluate for tolerance
########################################################################
rm results/r.i.t/*
for t in 0.0001 0.0003 0.0005 0.0007 0.001 0.003 0.005 0.007 0.01
do
trec_eval qrels.all.rel runs/r.i.t/train.r.$bestr.i.$besti.t.$t.run -m ndcg > results/r.i.t/train.r.$bestr.i.$besti.t-$t-result
done

########################################################################
#				find best model for tolerance
########################################################################
java -jar compareEvaluationFiles.jar results/r.i.t/ ndcg > tmp/r.i.t.log
#infile=tmp/r.log 
#while read a
#do 
#	echo $a
#done < $infile
tail tmp/r.log -n 1 | cut -d . -f 3 | read bestr
tail tmp/r.i.log -n 1 | cut -d . -f 5 | read besti
tail tmp/r.i.t.log -n 1 | cut -d - -f 2 | read bestt
# echo $bestt





########################################################################
#				train regularization
########################################################################
for fold in 1 2
do
for slack in 0.001 0.003 0.005 0.007 0.009 0.01 0.03 0.05 0.07 0.09 0.0001 0.0003 0.0005 0.0007 0.0009
do
java -jar ../RankLib.jar -train train.fold$fold.txt -ranker 4 -silent -metric2t NDCG  -r $bestr -i $besti -tolerance $bestt -save models/train.fold$fold.r.$bestr.i.$besti.t.$bestt.reg.$slack.model
#if try to optimize MAP, change NDCG to MAP here
done
done

########################################################################
#				test regularization
########################################################################
for fold in 1 2
do
for slack in 0.001 0.003 0.005 0.007 0.009 0.01 0.03 0.05 0.07 0.09 0.0001 0.0003 0.0005 0.0007 0.0009
do 
java -jar ../RankLib.jar -rank test.fold$fold.txt -metric2T NDCG -load models/train.fold$fold.r.$bestr.i.$besti.t.$bestt.reg.$slack.model -score scores/r.i.t.s/test.fold$fold.r.$bestr.i.$besti.t.$bestt.reg.$slack.s
#if try to optimize MAP, change NDCG to MAP here
done
done

########################################################################
#				get score & run for regularization
########################################################################
for fold in 1 2
do
for slack in 0.001 0.003 0.005 0.007 0.009 0.01 0.03 0.05 0.07 0.09 0.0001 0.0003 0.0005 0.0007 0.0009
do
java -jar ../getResult.jar test.fold$fold.info scores/r.i.t.s/test.fold$fold.r.$bestr.i.$besti.t.$bestt.reg.$slack.s runs/r.i.t.s/train.fold$fold.r.$bestr.i.$besti.t.$bestt.reg.$slack.run
done
done


for slack in 0.001 0.003 0.005 0.007 0.009 0.01 0.03 0.05 0.07 0.09 0.0001 0.0003 0.0005 0.0007 0.0009
do
cat runs/r.i.t.s/train.fold1.r.$bestr.i.$besti.t.$bestt.reg.$slack.run runs/r.i.t.s/train.fold2.r.$bestr.i.$besti.t.$bestt.reg.$slack.run > runs/r.i.t.s/train.r.$bestr.i.$besti.t.$bestt.reg.$slack.run
done

########################################################################
#				evaluate for regularization
########################################################################
rm results/r.i.t.s/*
for slack in 0.001 0.003 0.005 0.007 0.009 0.01 0.03 0.05 0.07 0.09 0.0001 0.0003 0.0005 0.0007 0.0009
do
trec_eval qrels.all.rel runs/r.i.t.s/train.r.$bestr.i.$besti.t.$bestt.reg.$slack.run -m ndcg > results/r.i.t.s/train.r.$bestr.i.$besti.t.$bestt.reg-$slack-result
done

########################################################################
#				find best model for regularization
########################################################################
java -jar compareEvaluationFiles.jar results/r.i.t.s/ ndcg > tmp/r.i.t.s.log
#infile=tmp/r.log 
#while read a
#do 
#	echo $a
#done < $infile
tail tmp/r.log -n 1 | cut -d . -f 3 | read bestr
tail tmp/r.i.log -n 1 | cut -d . -f 5 | read besti
tail tmp/r.i.t.log -n 1 | cut -d - -f 2 | read bestt
tail tmp/r.i.t.s.log -n 1 | cut -d - -f 2 | read bests
# echo $bests
tail tmp/r.i.t.s.log -n 1 | read result

# cp /home/justin/data/wt10g/ipr5level.qrel/results/r.i.t.s/$result /home/justin/Dropbox/experiment/wt10g





