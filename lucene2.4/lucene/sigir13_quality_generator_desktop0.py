#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Created on Mon Nov 19 17:06:28 2012

@author: zheng
"""

import sys, os
sys.path.append("/data/Dropbox/python")
from setup import LUCENE_HOME
from setup import merge_dict, run

print "LabLucene in:%s" % LUCENE_HOME
os.chdir(LUCENE_HOME)

pdict = {"bm25.b": "0.39","Lucene.PostProcess": "FeatureExtract13PP", 
		# "PerQueryRegModelTraining.lasttopic": "850", 
		"TRECQuerying.end": "70", 'expansion.terms': '35', 
		'trec.query.partial': 'true'}

a = range(805,850, 15) # [700, 715, 730, 745]
b =  map(lambda x: x+14, a) #[714, 729, 744, 759]
a = [805, 820, 835]
b = [819, 834, 850]

print a,b

splitdict = map(lambda (x,y):{'trec.query.startid':x, 'trec.query.endid': y}, zip(a,b))

for split in splitdict:
	paradict = merge_dict(pdict, split)
	print paradict
	run(paradict, type='PP', pnum=2, ETC = "GOV2", resultpath = "results/test", debug=False)
