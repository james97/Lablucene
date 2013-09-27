#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Created on Mon Nov 19 17:06:28 2012

@author: zheng
"""


import os, thread

from ibash import bash, waitForJobs, bashBackground
import numpy as np
from setup import LUCENE_HOME, basicRetrieve, ppRetrieve, toLuceneParas, evalall

os.chdir(LUCENE_HOME)


def sysLabLucene():
	cmd = "scp %s/lib/LabLucene1.0.jar zheng@10.7.10.219:~/workspace/java/lucene2.4/lib" % LUCENE_HOME
	print cmd
	out = bash(cmd)
	print out

	cmd = "scp %s/lib/LabLucene1.0.jar zheng@10.7.10.252:~/workspace/LabLucene/lib" % LUCENE_HOME
	print cmd
	out = bash(cmd)
	print out



sysLabLucene()
