#!/usr/bin/env python

# -*- coding: utf-8 -*-
"""
Created on Wed Nov 28 03:19:47 2012

@author: zheng
"""

import sys, os, glob
from sh import Command, cd, tail
import numpy as np

DROPBOX_HOME = os.environ['DROPBOX_HOME']
sys.path.append("%s/python" % DROPBOX_HOME)
sys.path.append("/data/Dropbox/python")
from ibash import bash, waitForJobs, bashBackground


terrier_home = '/home/zheng/workspace/terrier-3.5'
terrier_home = os.path.abspath('.')
cd(terrier_home)

topN = 2500
output_para = "-Dmatching.retrieved_set_size=%s -Dtrec.output.format.length=%s" % (topN, topN)

etcs = ['ap8889', 'trec8', 'wt2g', 'wt10g', 'blog08', 'robust04']
etc = etcs[4]
debug = False
last_topic = None

if len(sys.argv) ==1:
	pass
if len(sys.argv) >=2:
	if sys.argv[1] == '-d':
		debug = True
	else:
		etc = sys.argv[1]

if len(sys.argv) >=3 and sys.argv[2] =='-d':
    debug = True
elif len(sys.argv) >=3:
    	last_topic = sys.argv[2]



terrier_etc = './etc/%s' % etc
# print terrier_etc
# run = Command('export')
# run('TERRIER_ETC=%s' % terrier_etc)

os.environ['TERRIER_ETC'] = terrier_etc
## scan mu 
run = Command('./bin/trec_terrier.sh')

for mu in range(300, 3000, 50):
	#run('-r -Dtrec.model=DirichletLM -c %s' % str(mu) , _bg=True)
	#waitForJobs("java", num=5)
	a =0

# print run('-r -Dtrec.model=DirichletLM %s -c %s -Dtrec.results.file=myExperiment.res ' % (output_para, mu) )

opt_mu={'ap8889':600, 'wt2g':1850, 'wt10g':650, 'blog08':1350, 'trec8':300, 'robust04':300}
mu = opt_mu[etc]
if debug:
	print "export TERRIER_ETC=%s" % terrier_etc
	print './bin/trec_terrier.sh -r -Dtrec.model=DirichletLM %s -c %s -Dtrec.results.file=myExperiment.res ' % (output_para, mu)

def exist(result_path):
	if os.path.exists( result_path ):
		line = list(tail(result_path))[-1]
		if(line.split()[0]) == last_topic:
			return True
		print 'run ', result_path, line
	return False

#print run('-r -Dtrec.model=DirichletLM -c %s -Dtrec.results.file=myExperiment.res' % mu)
#default MRF with optimal mu
# run('-r -c %s -Dtrec.model=DirichletLM -Dmrf.mu=%s -Dmatching.dsms=MRFDependenceScoreModifier -Dtrec.results.file=DirichletLM_MRF_test.res' % (mu, 4000) )



def MRF():
	w_o = 1.0
	counter = 0
	type = 'FD'
	output_para = ""
	for ngram in [8]:
		for w_u in np.arange(0.1, 1.5, 0.1):
			fname = 'MRF_%s_mu%s_o%s_u%s_ngram%s_mrfmu%s.res' % (type, mu, w_o, w_u, ngram,mu)
			result_path = "var/results/%s/%s" % (etc, fname)
			cmd = '-r -c %s -Dtrec.model=DirichletLM -Dmrf.mu=%s -Dmatching.dsms=MRFDependenceScoreModifier -Dproximity.dependency.type=%s -Dtrec.results.file=%s -Dproximity.w_o=%s -Dproximity.w_u=%s -Dproximity.ngram.length=%s %s' % (mu, mu, type, fname, w_o, w_u, ngram, output_para)
			if debug:
				print './bin/trec_terrier.sh' , cmd
				sys.exit(0)
				
			if exist(result_path):
				print 'skip ', result_path
				continue
			waitForJobs("java", num=3, sleeps=10)
			p = run( cmd, _bg=True)
			p.wait()
			counter += 1
			print "run ", counter
MRF()
#print '-r -c %s -Dtrec.model=DirichletLM -Dmrf.mu=%s -Dmatching.dsms=MRFDependenceScoreModifier -Dtrec.results.file=DirichletLM_MRFdef_samemu.res' % (str(mu), str(4000))
#sys.exit() 

def crter(crter_mu):
	print 'mu: ', mu
	counter = 0
	for m in ['CTOffLine']:
		for s in [1, 2, 3, 4, 5, 6, 7, 8,9, 10, 20, 25, 50, 75, 100]:
			for k in ['gaussKernel', 'triangleKernel', 'circleKernel', 'cosKernel', 'quarKernel', 'epanKernel', 'triweightKernel']:
				for w in np.arange(0.1, 0.5, 0.1):
					fname = 'DirichletLM_mu%scmu_%s_w%s_k%s_s%s.res' %(mu, crter_mu, w, k, s)
					cmd = '-r -c %s -Dceter.mu=%s -Dtrec.model=DirichletLM -Dp.model=%s  -Dk.sigma=%s -Dkernel.name=%s -Dw.prox=%s -Dmatching.dsms=org.terrier.matching.dsms.BlockScoreModifier_js_comb  -Dtrec.results.file=%s %s' % (mu, crter_mu, m, s, k, w, fname, output_para)
					if debug:
						print cmd
						sys.exit(0)
					

					result_path = "var/results/%s/%s" % (etc, fname)
					# print result_path
					if os.path.exists( result_path ):
						line = list(tail(result_path))[-1]
						if(line.split()[0]) == last_topic:
							print 'skip : %s ' % result_path
							continue
						print 'run ', result_path, line
					print 'run ', result_path

					waitForJobs("java", num=1, sleeps=10)
					run(cmd, _bg=True)
					#p.wait()
					#sys.exit(0)
					counter += 1
					print counter
	print counter

#crter(mu)
#sys.exit(1)
#for cmu in np.arange(300, 2000, 50):
#	crter(cmu)

#BlockScoreModifier_js_comb.numberoftopdoc = 1000

topN = 1000
crter_mu = mu
def crter_runningtime():
	"""
	ap8889
	"""
	counter = 0
	for m in ['CTOffLine']:
		for s in [5]:
			for k in ['cosKernel']:
				for w in [0.1]:
					topN = 2500
					output_para = "-Dmatching.retrieved_set_size=%s -Dtrec.output.format.length=%s" % (topN, 1000)
					output_para = ""
					rerank_num = 20000
					fname = 'DirichletLM_mu%scmu_%s_w%s_k%s_s%s_rerank%s.res' %(mu, crter_mu, w, k, s, rerank_num)
					cmd = '-r -c %s -Dceter.mu=%s -Dtrec.model=DirichletLM -Dp.model=%s  -Dk.sigma=%s -Dkernel.name=%s -Dw.prox=%s -Dmatching.dsms=org.terrier.matching.dsms.BlockScoreModifier_js_comb  -Dtrec.results.file=%s %s -DBlockScoreModifier_js_comb.numberoftopdoc=%s' % (mu, crter_mu, m, s, k, w, fname, output_para, rerank_num)
					if debug:
						print './bin/trec_terrier.sh ' + cmd
						sys.exit(0)
					

					result_path = "var/results/%s/%s" % (etc, fname)
					# print result_path
					if os.path.exists( result_path ):
						line = list(tail(result_path))[-1]
						if(line.split()[0]) == last_topic:
							print 'skip : %s ' % result_path
							continue
						print 'run ', result_path, line
					print 'run ', result_path

					waitForJobs("java", num=2, sleeps=10)
					p = run(cmd, _bg=True)
					print p.stdout
					#p.wait()
					#sys.exit(0)
					counter += 1
					print counter
	print counter

def crter_runningtime_blog08():
	"""
	crter_runningtime_blog08
	"""
	counter = 0
	for m in ['CTOffLine']:
		for s in [2]:
			for k in ['triweightKernel']:
				for w in [0.5]:
					topN = 2500
					output_para = "-Dmatching.retrieved_set_size=%s -Dtrec.output.format.length=%s" % (topN, 1000)
					output_para = ""
					rerank_num = 2000
					fname = 'DirichletLM_mu%scmu_%s_w%s_k%s_s%s_rerank%s.res' %(mu, crter_mu, w, k, s, rerank_num)
					cmd = '-r -c %s -Dceter.mu=%s -Dtrec.model=DirichletLM -Dp.model=%s  -Dk.sigma=%s -Dkernel.name=%s -Dw.prox=%s -Dmatching.dsms=org.terrier.matching.dsms.BlockScoreModifier_js_comb  -Dtrec.results.file=%s %s -DBlockScoreModifier_js_comb.numberoftopdoc=%s' % (mu, crter_mu, m, s, k, w, fname, output_para, rerank_num)
					if debug:
						print './bin/trec_terrier.sh ' + cmd
						sys.exit(0)
					

					result_path = "var/results/%s/%s" % (etc, fname)
					# print result_path
					if os.path.exists( result_path ):
						line = list(tail(result_path))[-1]
						if(line.split()[0]) == last_topic:
							print 'skip : %s ' % result_path
							continue
						print 'run ', result_path, line
					print 'run ', result_path

					waitForJobs("java", num=2, sleeps=10)
					p = run(cmd, _bg=True)
					print p.stdout
					#p.wait()
					#sys.exit(0)
					counter += 1
					print counter
	print counter	
	
#crter_runningtime_blog08()
#waitForJobs("java", num=1)
#print run('-e')



