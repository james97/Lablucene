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
import numpy as np
import logging, math

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


def add_features(files):
	d1 = {}
	d2 = {}

	drel = {}
	dnon = {}

	with closing(gen_open(sys.argv[2])) as f:
		data = np.loadtxt(f, dtype=np.str, comments=None, usecols=[0,2,4])
		for a, b, c in data:
			d1[a+b] = c
			
	with closing(gen_open(sys.argv[3])) as f:
		data = np.loadtxt(f, dtype=np.str, comments=None, usecols=[0,2,4])
		for a, b, c in data:
			d2[a+b] = c
			

	with closing(gen_open(sys.argv[1])) as f:
		data = np.loadtxt(f, dtype=np.str, comments=None, usecols=[0,2,3])
		for a, b, c in data:
			key = a + b
			
			if key in d1 and key in d2:
				
				f1 = float (d1[key]) - float (d2[key])
				if float(c) > 0:
					if drel.has_key(a):
						drel[a].append(f1)
					else:
						drel[a] = [f1]
				else:
					if dnon.has_key(a):
						dnon[a].append(f1)
					else:
						dnon[a] = [f1]
						
	for key in drel.keys():
		rel_list = drel[key]
		non_list = dnon[key]
		print key, np.average(rel_list), len(rel_list), np.average(non_list), len(non_list)

def aver_crter(files):
	d1 = {}
	drel = {}
	dnon = {}
	with closing(gen_open(sys.argv[2])) as f:
		data = np.loadtxt(f, dtype=np.str, comments=None, usecols=[0,1,2])
		for a, b, c in data:
			d1[a+b] = c

	with closing(gen_open(sys.argv[1])) as f:
		data = np.loadtxt(f, dtype=np.str, comments=None, usecols=[0,2,3])
		for a, b, c in data:
			key = a + b
			if key in d1:
				f1 = float(d1[key])
				if math.isnan(f1):
				    print a,b,c,f1
				if float(c) > 0:
					if drel.has_key(a):
						drel[a].append(f1)
					else:
						drel[a] = [f1]
				else:
					if dnon.has_key(a):
						dnon[a].append(f1)
					else:
						dnon[a] = [f1]
				
	for key in drel.keys():
		rel_list = drel[key]
		#print np.average(rel_list)
		i = 0 
		length = len(dnon[key])
		while i < length:
		    if math.isnan(dnon[key][i]):
			print i, dnon[key][i]
		    i = i +1
		if key in dnon:
			non_list = dnon[key]
			print key, np.average(rel_list), len(rel_list), np.average(non_list), len(non_list)	
		else:
			non_list =[]
			print key, np.average(rel_list), len(rel_list), 0, len(non_list)	

def rd(l1,l2):
    return map( lambda x, y : tuple(map(sum,zip(x,y))),  l1,l2)

def find_matched(files):
    dict_list = []
    for file in files:
	
	with closing(gen_open(file)) as f:
	    d = {}
	    data = np.loadtxt(f, dtype=np.str, comments=None, usecols=[0,1,3])
	    for a, b, c in data:
		if not math.isnan(float(b)) and not math.isnan( float(c)) > 0:
		    d[a] = (float(b), float(c))
	    print len(d)
	    dict_list.append(d)
	    
    d_len = len(dict_list)
    matched =[]
    for key in dict_list[0].keys():
	i = 0
	tag = True
	try:
	    while i < d_len:
			#print key, dict_list[i+1][key], dict_list[i][key]
			#if dict_list[i+1][key][2] < dict_list[i][key][2]:
				#tag = False
				#pass
				#break
			i = i+2
	    if tag:
			matched.append( (key, [d[key] for d in dict_list]) )
	except:
	    #print i, key
	    i = i +2
    print 'len:', len(matched)
    for item in matched:
		print item
    sec_item = [item[1] for item in matched]
    #print sec_item
    total_list = reduce(rd, sec_item)
    length = float(len(sec_item))
    print 'average:', map(lambda x: tuple(np.array(x)/length), total_list)

		
	
	
			
#add_features(sys.argv)
#aver_crter(sys.argv)
find_matched(sys.argv[1:])
