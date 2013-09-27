#!/usr/bin/python
"""
scale the train file from lucene 
for sigir13: quality estimation
"""

import os, sys
from bz2 import BZ2File
from contextlib import closing
import gzip
import io
import os.path
from sklearn.datasets import load_svmlight_file, dump_svmlight_file
from sklearn.preprocessing import StandardScaler, MinMaxScaler, Normalizer
from setup import LUCENE_HOME
import numpy as np
import logging

def gen_open(f):
    if isinstance(f, int):  # file descriptor
        return io.open(f, "rb", closefd=False)
    elif not isinstance(f, basestring):
        raise TypeError("expected {str, int, file-like}, got %s" % type(f))
    _, ext = os.path.splitext(f)
    if ext == ".gz":
        return gzip.open(f, "rb")
    elif ext == ".bz2":
        return BZ2File(f, "rb")
    else:
        return open(f, "rb")


# index = []
# pre = qid[0]
# start = 0
# for i in range(1, len(qid)):
# 	if qid[i] != pre:
# 		index.append(i)
# 		pre = qid[i]
# 		# print len(qid[start:i]), (qid[start:i])
# 		start = i

def add_features(files):
	print sys.argv[1]
	d1 = {}
	d2 = {}
	with closing(gen_open(sys.argv[2])) as f:
		data = np.loadtxt(f, dtype=np.str, comments=None, usecols=[0,2,4])
		for a, b, c in data:
			d1[a+b] = c
	with closing(gen_open(sys.argv[3])) as f:
		data = np.loadtxt(f, dtype=np.str, comments=None, usecols=[0,2,4])
		for a, b, c in data:
			d2[a+b] = c

	with closing(gen_open(sys.argv[1])) as f:
		data = np.loadtxt(f, dtype=np.str, comments=None, usecols=[0,2,4,5])
		for x in data:
			key = x[0]+x[1]
			if key in d1:
				f1 = float (d1[key]) - float (x[2])
			else:
				f1 = 0.01
			if key in d2:
				f2 = float (d2[key]) - float (x[2])
			else:
				f2 = 0.01
			print x[0], x[1], f1, f2		
		# ls = [(a[4:] + '#' +c[7:], b[2:]) for a, b, c in data]
		# docs = np.array(ls)[:,0]
		# f0 = np.array(ls)[:,1].astype(np.float32)

	# for f in files:
	# 	with closing(gen_open(f)) as f:
	# 		data = np.loadtxt(f, dtype=np.str, usecols=[0,2,4])
	# 		dicta={}
	# 		for a, b, c in data:
	# 			dicta[a+'#'+b] = float(c)
	# 		f1 = np.zeros(f0.shape)
	# 		for i in range(len(docs)):
	# 			if docs[i] in dicta:
	# 				f1[i] = dicta[docs[i]]
	# 			else:
	# 				logging.warning("doc %s is not retrieved" % docs[i]) # will print a message to the console
	# 				# raise TypeError("doc %s is not retrieved" % docs[i])
	# 		# np.set_printoptions(threshold=np.nan)
	# 		print f1.shape, f0.shape
	# 		f2 = f1 - f0
	# 		# X_train=np.append(X_train, f1.reshape((len(f1), 1)), axis=1)
	# 		X_train=np.append(X_train, f2.reshape((len(f2), 1)), axis=1)
	# 		print X_train.shape
	# 		print X_train[0,:]

add_features(sys.argv)