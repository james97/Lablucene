import os, sys
#import pylab as pl
import numpy as np
#from sklearn import cluster, datasets
import matplotlib.pyplot as pl

"""
input quality.2
"""

fontsize = 12
f = open(sys.argv[1])
data = np.loadtxt(f, comments=None, dtype=np.str)
length = len(data[:,1])
noise = np.random.uniform(-0.4, 0.4, length)

if len(sys.argv)==3:
	f = open(sys.argv[2])
	data1 = np.loadtxt(f, comments=None, dtype=np.str)
	length1 = len(data1[:,1])
	noise1 = np.random.uniform(-0.4, 0.4, length1)




def scatter_level():
	pl.figure()
	pl.title("Improvement scatter")
	#print data[:, 1].shape, noise.shape
	#X = data[:,1].astype(np.float32)
	#msk = X > 0
	#print len(X[msk])
	X = data[:,1].astype(np.float32) - noise
	Y = data[:,0].astype(np.float32)
	# k_means = cluster.KMeans(n_clusters=3)
	# k_means.fit(X)
	# mask1 =  k_means.labels_ == 2

	pl.hlines(0, -1., 3., colors='r')
	#print 'xlen:', len(X[msk])
	pl.scatter(X, Y)
	#print Y[msk]
	pl.xlim(X.min()*1.1, X.max()*1.1)
	#pl.ylim(Y.min()*1.1, Y.max()*1.1)
	pl.xticks([0, 1, 2])
	pl.xlabel('Relevance Level')
	pl.ylabel('Improvement by PRF')
	pl.show()

def scatter2_level():
	pl.figure(figsize=(6, 4))
	pl.subplot(1, 2, 1)
	pl.title("Improvement scatter on WT10G", fontsize=fontsize)
	X = data[:,1].astype(np.float32) - noise
	Y = data[:,0].astype(np.float32)
	pl.hlines(0, -1., 3., colors='r')
	pl.scatter(X, Y)
	pl.xlim(X.min()*1.1, X.max()*1.1)
	#pl.ylim(Y.min()*1.1, Y.max()*1.1)
	pl.xticks([0, 1, 2])
	pl.xlabel('Relevance Level', fontsize=fontsize)
	pl.ylabel('Improvement by PRF', fontsize=fontsize)
	
	pl.subplot(1, 2, 2)
	pl.title("Improvement scatter ON GOV2", fontsize=fontsize)
	X = data1[:,1].astype(np.float32) - noise1
	Y = data1[:,0].astype(np.float32)
	pl.hlines(0, -1., 3., colors='r')
	pl.scatter(X, Y)
	pl.xlim(X.min()*1.1, X.max()*1.1)
	#pl.ylim(Y.min()*1.1, Y.max()*1.1)
	pl.xticks([0, 1, 2])
	pl.xlabel('Relevance Level', fontsize=fontsize)
	pl.ylabel('Improvement by PRF', fontsize=fontsize)	
	
	#pl.tight_layout()
	pl.show()

def transformX(x):
	if x <=0:
		return 0
	elif x < 0.1:
		return 1
	else:
		return 2

def transformX1(x):
	if x <=0:
		return 0
	elif x < 0.1:
		return 1
	else:
		return 2		

def transform_data():
	b = np.array(map(transformX, data[:,0].astype(np.float32)))
	b1 = data[:,1].astype(np.int).reshape((length, 1))
	# b = b.reshape((length, 1)) + b1
	b = np.array( map( max, zip(b.reshape((length, 1)), b1) ) )
	b = b.reshape((length, 1))
	print b
	print b.shape, b1.shape

	data_trans = np.append(b.astype(np.str), data[:,2:], axis=1)
	# print data_trans
	# data_trans.tofile(sys.argv[1] + '.ipr3level', sep=" ")
	np.savetxt(sys.argv[1] + '.ipr3level', data_trans, fmt='%s', delimiter=' ', newline='\n')

	## output new qrel file based on improvement Level
	z = np.zeros(b.shape, dtype=np.int).astype(np.str)
	# data_trans = reduce( np.append(axis=1), (data[:,2], z, data[:,-1], b.astype(np.str) ) )

	data_trans = np.append( np.array( map(lambda x:x[x.index(':')+1:], data[:,2]) ).reshape((length, 1)), z, axis=1)
	data_trans = np.append(data_trans, np.array( map(lambda x:x[x.index('=')+1:], data[:,-1]) ).reshape((length, 1)), axis=1)
	data_trans = np.append(data_trans, b.astype(np.str), axis=1)
	np.savetxt(sys.argv[1] + '.ipr3level.qrel', data_trans, fmt='%s', delimiter=' ', newline='\n')




#scatter_level()
scatter2_level()
#transform_data()