#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Created on Mon Nov 19 17:06:28 2012

@author: zheng
"""


import sys, os, thread
sys.path.append("/data/Dropbox/python")
from ibash import bash, waitForJobs, bashBackground
from setup import LUCENE_HOME, basicRetrieve, ppRetrieve, toLuceneParas, evalall
from setup import merge_dict, run

print "LabLucene in:%s" % LUCENE_HOME
os.chdir(LUCENE_HOME)

fdict = {}

ETC = "WT10GT451-550"
ETC = 'robustT301-450'
dataset = 'GOV2'
debugpara=False
###############################Feedback from the following file ##############################################
fdfiles = ['/data/Dropbox/workspace/experiment/wt10g/WT10GT451-550.train.quality.2.ipr5level.scaled.f2.r.13.i.45.t.0.007.reg.0.09.run', 
           '/data/Dropbox/workspace/experiment/wt10g/WT10GT451-550.train.quality.2.ipr5level.scaled.f2.run',
            '/data/Dropbox/workspace/experiment/wt10g/WT10GT451-550.train.quality.1.scaled.f2.run', 
            '/data/Dropbox/workspace/experiment/wt10g/WT10GT451-550.train.quality.2.iprlevel.scaled.f2.run.new',
            '/data/Dropbox/workspace/experiment/wt10g/WT10GT451-550.train.quality.2.ipr5level.p_30.r.13.i.65.t.0.0001.reg.0.009.run',  
            '/data/Dropbox/workspace/experiment/wt10g/WT10GT451-550.train.quality.2.ipr5level.ndcg.r.18.i.45.t.0.003.reg.0.003.run',
            '/data/Dropbox/workspace/experiment/wt10g/WT10GT451-550.train.quality.2.ipr5level.scaled.f2._ndcg_train.r.19.i.25.t.0.005.reg.0.001.run',
            '/data/Dropbox/workspace/experiment/wt11g/WT10GT451-550.train.quality.2.ipr5level.scaled.f2._p30_train.r.17.i.10.t.0.01.reg.0.007.run', 
            '/data/Dropbox/workspace/experiment/wt10g/WT10GT451-550.train.quality.2.ipr3level.scaled.f2._ndcg_train.r.14.i.35.t.0.001.reg.0.003.run',
            '/data/Dropbox/workspace/experiment/wt10g/WT10GT451-550.train.quality.2.ipr3level.scaled.f2._p30_train.r.19.i.55.t.0.01.reg.0.0001.run']
            
fdfiles1 = ['/data/Dropbox/workspace/experiment/GOV2/gov2.quality.1.p15.train.r.4.i.75.t.0.001.reg.0.0003.run', 
            '/data/Dropbox/workspace/experiment/GOV2/gov2.quality.2.p15.train.r.12.i.55.t.0.0007.reg.0.0005.run']


paradict = {"bm25.b": [0.39], 
            'QueryExpansion.FeedbackfromFile': fdfiles1, 
            "expansion.documents": [3, 5, 10,15, 20, 30, 50], 
            'expansion.terms': [35], 
            'rocchio.weighted': ['true'], 'rocchio.weighteType': [1,3],
            'rocchio.exp': [1.5], 
            'rocchio.beta': [0.4, 0.5],
            'trec.shortFirsPass': ['true']}
print reduce( lambda x,y: x*y, map(len, paradict.values() ))
run(paradict, type='PP', pnum=5, ETC = dataset, resultpath = "results/test", debug=debugpara)
###############################################################################



############################## topk unweighted-- AdpaQE_WRocchio_KL ####################
paradict = {'QueryExpansion.FeedbackfromFile': ['false'], 
            "expansion.documents": [3, 5, 10,15, 20, 30, 50], 
            'expansion.terms': [35, 50], 
            'rocchio.weighted': ['false'], 'rocchio.weighteType': [0],
            'rocchio.exp': [1.5], 
            'rocchio.beta': [0.3, 0.4, 0.5, 0.7],
	    'trec.shortFirsPass': ['true']}
print reduce( lambda x,y: x*y, map(len, paradict.values() ))
# run(paradict, type='PP', pnum=5, ETC = dataset, resultpath = "results/test", debug=False)
#################################################################

########################## basicRet ################################
# run({"bm25.b": ["0.344"]}, type='basic', pnum=4)
# run({"bm25.b": ["0.39"]}, type='basic', pnum=4)

########################## Proximity Retrieval ################################
proximity = {"proximity.enable": "true", "proximity.model": "non", "proximity.weight": "0.3", 
    "proximity.type": "FD", "proximity.slop": "15"}

#run(merge_dict(proximity, {"bm25.b": [0.3, 0.39]} ), type='basic', pnum=4, ETC = "GOV2", resultpath = "results/test", debug=False)

###############################QE Features######################################
paradict = {"bm25.b": ["0.39"], 'rocchio.termSelector': ['ProxTermSelector'], 'ProxTermSelector.winSize': ['50'],  'ProxTermSelector.proxType': ['4'], 'ProxTermSelector.sd': ['50'], 
        'ProxTermSelector.normPow': ['0.6'], 'expansion.terms': ['50'], 'trec.shortFirsPass': ['true']}
# run(paradict, type='PP', pnum=10, ETC = dataset, resultpath = "results/test", debug=False)

############################# quality_trainPPRet #######################
# def quality_trainPPRet():
#     fdict = {}
#     fdict.update(defaultdict)
#     fdict.update(ppdict)
#     pdict = {"bm25.b": "0.39","Lucene.PostProcess": "FeatureExtract13PP", "PerQueryRegModelTraining.lasttopic": "850", "TRECQuerying.end": "100", 'expansion.terms': '35'}
#     fdict.update(pdict)
#     waitForJobs("TrecLucene", num=1)
#     cmd = ppRetrieve(para=toLuceneParas(fdict))
#     bashBackground(cmd)
# quality_trainPPRet()
#########################################################################
#QERet()

#trainPPRet()
# adaptivePPRet()

# QERet_fromFile()

# waitForJobs("TrecLucene", num = 1)
# bashBackground( evalall( para = toLuceneParas(defaultdict) ) )
# bash(evalall(para="var/%s/%s" % (resultpath, ETC)))


####################################################################
# os.environ['ETC'] = ETC
# resultpath = "results/test"
# if not os.path.isdir("var/%s/%s" % (resultpath, ETC)):
#     os.makedirs("var/%s/%s" % (resultpath, ETC))
# defaultdict = {"Lucene.Search.WeightingModel": "BM25",
#               "bm25.b": "0.3",
#               "Lucene.Search.LanguageModel": "false",
#               "trec.results": "%s/%s" % (resultpath, ETC),
#               "Lucene.TRECQuerying.outputformat": "TRECDocidOutputFormat"
# }

# ppdict = { #for postprocessing
#     "Lucene.PostProcess": "QueryExpansionAdap", 
#     "term.selector.name": "RocchioTermSelector",
#     "Lucene.QueryExpansion.Model": "KL", 
#     "QueryExpansion.RelevanceFeedback": "false", 
#     "parameter.free.expansion": "false"
# }

# proximity = {"proximity.enable": "false", "proximity.model": "non", "proximity.weight": "0.3", "proximity.type": "FD", "proximity.slop": "15"}



# merge_dict = (lambda a, b: (lambda a_copy: a_copy.update(b) or a_copy)(a.copy())) # merge multiple dict with reduce

# def run(paradict, type='basic', pnum=4):
#     fun = basicRetrieve
#     basedict = {}
#     if type == 'basic': #for basic retrieval 
#         fun = basicRetrieve 
#     elif type == 'PP': # for post-processing, including query expansion. 
#         fun = ppRetrieve
#         basedict.update(ppdict)
#     for pdict in IterGrid(paradict):
#         fdict = reduce(merge_dict, (defaultdict, basedict, proximity, ppdict, pdict))
#         waitForJobs("TrecLucene", num=pnum)
#         # print toLuceneParas(fdict)
#         cmd = fun(para=toLuceneParas(fdict))
#         # print cmd
#         bashBackground(cmd)
