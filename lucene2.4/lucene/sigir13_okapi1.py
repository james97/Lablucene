#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Created on Mon Nov 19 17:06:28 2012

@author: zheng
"""

import sys, os
sys.path.append("/data/Dropbox/python")
from ibash import bash, waitForJobs, bashBackground
from setup import LUCENE_HOME
from setup import merge_dict, run

print "LabLucene in:%s" % LUCENE_HOME
os.chdir(LUCENE_HOME)


debugpara = False

# ETC = "WT10GT451-550"
# ETC = 'robustT301-450'
# dataset = 'GOV2'
#dataset = 'robustT301-450'
#dataset = "WT10GT451-550"
dataset = 'WT2G'
###############################Feedback from the following file ##############################################
pdict = {"bm25.b": "0.39","Lucene.PostProcess": "FeatureExtract13PP",
                # "PerQueryRegModelTraining.lasttopic": "850", 
                "TRECQuerying.end": "70", 'expansion.terms': '35',
                'trec.query.partial': 'true'}

fdfiles=['./experiment/GOV2/gov2.quality.1.p15.train.r.4.i.75.t.0.001.reg.0.0003.run',
        './experiment/GOV2/gov2.quality.2.p15.train.r.12.i.55.t.0.0007.reg.0.0005.run'
]
fdfiles1 = ['/data/Dropbox/workspace/experiment/disk45/disk4and5T301-450.train.quality.2.ipr3level.scaled.f2_p15_r.6.i.25.t.0.0005.reg.0.01.run',
            '/data/Dropbox/workspace/experiment/disk45/disk4and5T301-450.train.quality.2.ipr3level.scaled.f2_ndcg_r.15.i.45.t.0.005.reg.0.0007.run',
            '/data/Dropbox/workspace/experiment/disk45/disk4and5T301-450.train.quality.1.scaled.f2_p15_r.13.i.45.t.0.001.reg.0.0007.run',
            '/data/Dropbox/workspace/experiment/disk45/disk4and5T301-450.train.quality.1.scaled.f2_ndcg_r.17.i.45.t.0.01.reg.0.005 .run',
            '/data/Dropbox/workspace/experiment/test_disk45_train+wt10g.quality.1_ndcg_r.9.i.10.t.0.007.reg.0.009.run',
            '/data/Dropbox/workspace/experiment/test_disk45_train+wt10g.quality.2_ndcg_r.4.i.10.t.0.01.reg.0.03.run']

data_specific_para = {
    'GOV2': {"bm25.b": [0.39], 'QueryExpansion.FeedbackfromFile': fdfiles
    },
    'robustT301-450': {"bm25.b": [0.3444], 'QueryExpansion.FeedbackfromFile': fdfiles1[-2:]
    },
    "WT10GT451-550": {"bm25.b": [0.2505], 'QueryExpansion.FeedbackfromFile': fdfiles1[-2:]
    },
    "WT2G": {"bm25.b": [0.2381], 'QueryExpansion.FeedbackfromFile': 'unknownrightnow'
    }
}

paradict = {"bm25.b": [0.2505],
            "expansion.documents": [3, 5, 10,15, 20, 30, 50],
            'expansion.terms': [35],
            'rocchio.weighted': ['true'], 'rocchio.weighteType': [1,3],
            'rocchio.exp': [1.5, 2],
            'rocchio.beta': [0.6, 0.7],
            'trec.shortFirsPass': ['true']}

para_new = merge_dict(paradict, data_specific_para[dataset])
print reduce( lambda x,y: x*y, map(len, paradict.values() ))
# run(para_new, type='PP', pnum=12, ETC = dataset, resultpath = "results/test", debug=debugpara)




################################################################################################
pdict = {"bm25.b": "0.39","Lucene.PostProcess": "FeatureExtract13PP", 
		# "PerQueryRegModelTraining.lasttopic": "850", 
		"TRECQuerying.end": "70", 'expansion.terms': '35', 
		'trec.query.partial': 'true'}

############################## AdpaQE_WRocchio_KL  #############################################
fdfiles = ['']

paradict = {'QueryExpansion.FeedbackfromFile': ['false'], 
            'bm25.b':[0.30, 0.39],
            "expansion.documents": [3, 5, 10,15, 20, 30, 50], 
            'expansion.terms': ['50'], 
            'rocchio.weighted': ['true'], 'rocchio.weighteType': [1, 3],
            'rocchio.exp': [1.5], 'rocchio.beta': [0.3, 0.4, 0.5, 0.6]}

print reduce( lambda x,y: x*y, map(len, paradict.values() ))
# run(paradict, type='PP', pnum=10, ETC = "GOV2", resultpath = "results/test", debug=False)
###############################################################################

