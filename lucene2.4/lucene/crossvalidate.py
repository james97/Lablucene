#!/usr/bin/env python

"""
print cross validation results optimized for all queries
not optimize for per query
"""

import sys
import numpy as np 
from numpy import array 


def cross_aver(train, test):
	train_sum = sum(train)/len(train)
	test_sum = sum(test)/len(test)
	# print train_sum
	# print test_sum

	maxtrain_id = train_sum.argmax()
	maxtest_id = test_sum.argmax()
	ave_test = test_sum[maxtrain_id]
	ave_train = train_sum[maxtest_id]
	return (ave_train + ave_test)/2



path = sys.argv[1]

queryid = []
paras = []
performdict = []
with open(path) as a_file:
	for line in a_file:
		pos = line.find(' ')
		queryid.append(line[:pos].strip())
		items = line[pos+1:].strip().split(' ')
		t_dict = []
		for item in items:
			# print 'item:', item
			split_ = item.split(':')
			t_dict.append(eval(split_[1]))
			paras.append(split_[0])
		performdict.append(t_dict)

ndperform = array(performdict)
ndperform.max

np.set_printoptions(threshold=np.nan)

length = len(performdict)
bestperf = []



# print ndperform
print 'best: ', ndperform.max(axis=1)/length
print 'best: ', sum(ndperform.max(axis=1))/length
print sum(ndperform)/length
sump = sum(ndperform)
print max(sump)/length
print length

print cross_aver( ndperform[ slice(0, len(ndperform), 2) ], ndperform[ slice(1, len(ndperform), 2)] )

