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


path = sys.argv[1]
outpath = path+'.tmp'
finalpath = path+'.scaled.f2'

scaler = MinMaxScaler(feature_range=(0.1, 1))
print path
X_train, y, qid = load_svmlight_file(path, query_id=True)
X_train = X_train.toarray()

# X_T = X_train.transpose()
# normalizer = Normalizer().fit(X_T)
# X = normalizer.transform(X_T).transpose().astype(np.float32)

# X = scaler.fit_transform(X_T).transpose().astype(np.float32)
y = y.astype(int)

index = []
pre = qid[0]
start = 0
for i in range(1, len(qid)):
	if qid[i] != pre:
		index.append(i)
		pre = qid[i]
		# print len(qid[start:i]), (qid[start:i])
		start = i

# print index
X = X_train

def add_features(files):
	global X_train
	print sys.argv[1]
	with closing(gen_open(sys.argv[1])) as f:
		data = np.loadtxt(f, dtype=np.str, comments=None, usecols=[1,2,10])
		# print data
		ls = [(a[4:] + '#' +c[7:], b[2:]) for a, b, c in data]
		docs = np.array(ls)[:,0]
		f0 = np.array(ls)[:,1].astype(np.float32)

	for f in files:
		with closing(gen_open(f)) as f:
			data = np.loadtxt(f, dtype=np.str, usecols=[0,2,4])
			dicta={}
			for a, b, c in data:
				dicta[a+'#'+b] = float(c)
			f1 = np.zeros(f0.shape)
			for i in range(len(docs)):
				if docs[i] in dicta:
					f1[i] = dicta[docs[i]]
				else:
					logging.warning("doc %s is not retrieved" % docs[i]) # will print a message to the console
					# raise TypeError("doc %s is not retrieved" % docs[i])
			# np.set_printoptions(threshold=np.nan)
			print f1.shape, f0.shape
			f2 = f1 - f0
			# X_train=np.append(X_train, f1.reshape((len(f1), 1)), axis=1)
			X_train=np.append(X_train, f2.reshape((len(f2), 1)), axis=1)
			print X_train.shape
			print X_train[0,:]


add_features(sys.argv[2:])


def scale():
	global X
	X_sliced = np.split(X_train, index)
	X_f =[]
	for X_i in X_sliced:
		# print len(X_i),
		try:
			min_max_scaler = MinMaxScaler(feature_range=(0, 1))
			xtrans = min_max_scaler.fit_transform(X_i);
			mask = np.isnan(xtrans)
			xtrans[mask] = 0.00001
			mask = xtrans <= 0 
			xtrans[mask] = 0.00001
			# print len(xtrans), xtrans
			X_f.append( xtrans.astype(np.float32) )
		except Exception, e:
			print len(X_i)
			raise e

		else:
			pass


	X= np.concatenate(X_f)
	print X[0:2]

scale()

removed_feature_index=[2, 3, 4]
def remove_feature(index):
	global X
	X = np.delete(X, removed_feature_index, axis=1)

# remove_feature(removed_feature_index)

def dump():

	dump_svmlight_file(X, y, outpath, zero_based=False,comment=None, query_id=qid)
	f1lines = filter(lambda a: a[0] != '#', open(outpath).readlines())
	f2lines = map(lambda a: a[a.find('#'):], open(path).readlines())
	output = map(lambda a, b: a.rstrip('\n') + ' ' + b, f1lines, f2lines)
	# print output[1:10]
	fout = open(finalpath, 'w')
	for line in output:
		fout.write(line)

	fout.close();
	os.remove(outpath)

dump()

#////////////////////////////////////////////////////
def feature_selection():
	from sklearn.datasets import make_classification
	from sklearn.ensemble import ExtraTreesClassifier

	# Build a forest and compute the feature importances
	forest = ExtraTreesClassifier(n_estimators=250,
	                              compute_importances=True,
	                              random_state=0)

	forest.fit(X, y)
	importances = forest.feature_importances_
	std = np.std([tree.feature_importances_ for tree in forest.estimators_],
	             axis=0)
	indices = np.argsort(importances)[::-1]

	# Print the feature ranking
	print "Feature ranking:"

	for f in xrange(len(indices)):
		print "%d. feature %d (%f)" % (f + 1, indices[f], importances[indices[f]])

	# Plot the feature importances of the forest
	import pylab as pl
	pl.figure()
	pl.title("Feature importances")
	pl.bar(xrange(len(indices)), importances[indices],
	       color="r", yerr=std[indices], align="center")
	pl.xticks(xrange(len(indices)), indices)
	pl.xlim([-1, 11])
	pl.show()

#feature_selection()