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

debugpara = False
dataset = 'GOV2'
#dataset = 'robustT301-450'
# dataset = "WT10GT451-550"
#dataset = 'WT2G'
###############################Feedback from the following file ##############################################
fdfiles = [ './experiment/test_gov2_train+wt10g.quality.1_p15r.19.i.10.t.0.0003.reg.0.07.run',
            './experiment/.test_gov2_train+wt10g.quality.2_p15_r.9.i.45.t.0.0001.reg.0.01.run',
            './experiment/wt10g/WT10GT451-550.train.quality.2.ipr3level.scaled.f2_p15_r.20.i.45.t.0.005.reg.0.007.run',
            './experiment/wt10g/WT10GT451-550.train.quality.1.scaled.f2._p15_.r.1.i.45.t.0.005.reg.0.009.run']

fdfiles1 = ['/data/Dropbox/workspace/experiment/disk45/disk4and5T301-450.train.quality.2.ipr3level.scaled.f2_p15_r.6.i.25.t.0.0005.reg.0.01.run',
            '/data/Dropbox/workspace/experiment/disk45/disk4and5T301-450.train.quality.1.scaled.f2_p15_r.13.i.45.t.0.001.reg.0.0007.run',
            '/data/Dropbox/workspace/experiment/disk45/disk4and5T301-450.train.quality.2.ipr3level.scaled.f2_ndcg_r.15.i.45.t.0.005.reg.0.0007.run',
            '/data/Dropbox/workspace/experiment/disk45/disk4and5T301-450.train.quality.1.scaled.f2_ndcg_r.17.i.45.t.0.01.reg.0.005 .run',
            '/data/Dropbox/workspace/experiment/test_disk45_train+wt10g.quality.1_ndcg_r.9.i.10.t.0.007.reg.0.009.run',
            '/data/Dropbox/workspace/experiment/test_disk45_train+wt10g.quality.2_ndcg_r.4.i.10.t.0.01.reg.0.03.run', 
            '/data/Dropbox/workspace/experiment/disk45/robustT301-450.train.quality.1_p15_r.11.i.45.t.0.001.reg.0.0003.run',
            '/data/Dropbox/workspace/experiment/disk45/robustT301-450.train.quality.2_p15_.r.7.i.10.t.0.001.reg.0.0005.run']

fdfiles2 = ['/data/Dropbox/workspace/experiment/GOV2/gov2.quality.1.p15.train.r.4.i.75.t.0.001.reg.0.0003.run', 
            '/data/Dropbox/workspace/experiment/GOV2/gov2.quality.2.p15.train.r.12.i.55.t.0.0007.reg.0.0005.run',
            './experiment/test_gov2_train+wt10g.quality.1_p15r.19.i.10.t.0.0003.reg.0.07.run',
            './experiment/.test_gov2_train+wt10g.quality.2_p15_r.9.i.45.t.0.0001.reg.0.01.run']

fdfiles3 = ['./experiment/wt2g/WT2G.train.quality.2_p15_r.3.i.65.t.0.003.reg.0.005.run',
            './experiment/wt2g/WT2G.train.quality.1_p15_r.9.i.65.t.0.003.reg.0.003.run']

data_specific_para = {
    'GOV2': {"bm25.b": [0.39], 'QueryExpansion.FeedbackfromFile': fdfiles2[:-2], "dlm.mu": [1000]
    },
    'robustT301-450': {"bm25.b": [0.3444], 'QueryExpansion.FeedbackfromFile': fdfiles1[2:4], "dlm.mu": [900], 
    "PerQueryRegModelTraining.lasttopic": "450"
    },
    "WT10GT451-550": {"bm25.b": [0.2505], 'QueryExpansion.FeedbackfromFile': fdfiles1[-2:], "dlm.mu": [1000]    
    },
    "WT2G": {"bm25.b": [0.2381], 'QueryExpansion.FeedbackfromFile': 'false', "PerQueryRegModelTraining.lasttopic": "450", 
    'QueryExpansion.FeedbackfromFile': fdfiles3, "dlm.mu": [1150]
    }
}

paradict = {"expansion.documents": [3, 5, 10,15, 20, 30, 50], 
            'expansion.terms': [35], 
            'rocchio.weighted': ['true'], 'rocchio.weighteType': [1, 3],
            'rocchio.exp': [1.5], 
            'rocchio.beta': [0.3],
            'trec.shortFirsPass': ['true']}

para_new = merge_dict(paradict, data_specific_para[dataset])
print reduce( lambda x,y: x*y, map(len, paradict.values() ))
run(para_new, type='PP', pnum=5, ETC = dataset, resultpath = "results/test", debug=debugpara)
###############################################################################



############################## topk unweighted-- AdpaQE_WRocchio_KL ####################
paradict = {'QueryExpansion.FeedbackfromFile': ['false'],
            "expansion.documents": [3, 5, 10,15, 20, 30, 50], 
            'expansion.terms': [50], 
            'rocchio.weighted': ['false'], 'rocchio.weighteType': [0],
            'rocchio.exp': [1.5], 'rocchio.beta': [0.3, 0.4, 0.5, 0.6, 0.7, 0.8],
	        'trec.shortFirsPass': ['true']}

para_new = reduce( merge_dict, (paradict, data_specific_para[dataset], {'QueryExpansion.FeedbackfromFile': 'false'}) )
print reduce( lambda x,y: x*y, map(len, paradict.values() ))
#run(para_new, type='PP', pnum=6, ETC = dataset, resultpath = "results/test", debug=debugpara)
#################################################################

############################## Language Model sweep mu ####################
paradict = {'QueryExpansion.FeedbackfromFile': ['false'], 
            "Lucene.Search.WeightingModel": "DLM", "Lucene.Search.LanguageModel": "true",
            "dlm.mu": range(500, 1200, 50)}

para_new = reduce( merge_dict, (paradict, data_specific_para[dataset], {'QueryExpansion.FeedbackfromFile': 'false'}) )
print reduce( lambda x,y: x*y, map(len, paradict.values() ))
# run(paradict, type='basic', pnum=6, ETC = dataset, resultpath = "results/test", debug=debugpara)
# fun1()
#################################################################

############################## Language Model QueryExpansionLM_RM3 ####################
paradict = {'QueryExpansion.FeedbackfromFile': ['false'], 
            "Lucene.Search.WeightingModel": "DLM", "dlm.mu": "1000", "Lucene.Search.LanguageModel": "true",
            "Lucene.PostProcess": "QueryExpansionLM", "term.selector.name": "RM3TermSelector",
            "expansion.documents": [3, 5, 10,15, 20, 30, 50],
            'expansion.terms': [50], 'lm.alpha': [0.3, 0.4, 0.5, 0.6, 0.7, 0.8],
            'trec.shortFirsPass': ['true']}

para_new = reduce( merge_dict, (paradict, data_specific_para[dataset], {'QueryExpansion.FeedbackfromFile': 'false'}) )
print reduce( lambda x,y: x*y, map(len, para_new.values() ))
#run(para_new, type='PP', pnum=6, ETC = dataset, resultpath = "results/test", debug=debugpara)
#################################################################


########################## basicRet BM25 ################################
para_new = merge_dict(data_specific_para[dataset], {'QueryExpansion.FeedbackfromFile': 'false'})
# run(para_new, type='basic', pnum=6, ETC = dataset, resultpath = "results/test", debug=debugpara)

########################## Proximity Retrieval ################################
proximity = {"proximity.enable": "true", "proximity.model": "non", "proximity.weight": "0.3", 
    "proximity.type": "FD", "proximity.slop": "15"}
para_new = reduce( merge_dict, (proximity, data_specific_para[dataset], {'QueryExpansion.FeedbackfromFile': 'false'}) )
#run(para_new, type='basic', pnum=4, ETC = dataset, resultpath = "results/test", debug=debugpara)

###############################QE Features######################################
paradict = {'rocchio.termSelector': ['ProxTermSelector'], 'ProxTermSelector.winSize': ['50'],  'ProxTermSelector.proxType': ['4'], 'ProxTermSelector.sd': ['50'], 
        'ProxTermSelector.normPow': ['0.6'], 'expansion.terms': ['35'], 'trec.shortFirsPass': ['true']}
para_new = reduce( merge_dict, (paradict, data_specific_para[dataset], {'QueryExpansion.FeedbackfromFile': 'false'}) )
#run(para_new, type='PP', pnum=4, ETC = dataset, resultpath = "results/test", debug=debugpara)

############################# quality_trainPPRet #######################
# PerQueryRegModelTraining.lasttopic must be set. Or you can divide the query into multiple sections, see the source of FeatureExtract13PP
pdict = {"Lucene.PostProcess": "FeatureExtract13PP", "TRECQuerying.end": "100", 'expansion.terms': '35'}
para_new = reduce( merge_dict, (pdict, data_specific_para[dataset], {'QueryExpansion.FeedbackfromFile': 'false'}) )
#run(para_new, type='PP', pnum=4, ETC = dataset, resultpath = "results/test", debug=debugpara)

#########################################################################
