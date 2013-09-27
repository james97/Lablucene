# for m in CTOffLine; do for w in  0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.9 1.0; do for s in 1 2 3 4 5 6 7 8 9 10 20 25 50 75 100; do for k in gaussKernel triangleKernel circleKernel cosKernel quarKernel epanKernel triweightKernel;do TERRIER_OPTIONS=" -Dtrec.model=Dirichelet_LM -Dp.model=$m  -Dk.sigma=$s -Dkernel.name=$k -Dw.prox=$w " ./trec_terrier.sh -r; done;done;done;done
# DirichletLM
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


terrier_home = '/media/disk1/terrier-3.5'
cd(terrier_home)

etcs = ['ap8889', 'trec8', 'wt2g', 'wt10g', 'blog08']
etc = etcs[4]
debug = False
last_topic = None

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
# run = Command('export')
# run('TERRIER_ETC=%s' % terrier_etc)

os.environ['TERRIER_ETC'] = terrier_etc
## scan mu 
run = Command('./bin/trec_terrier.sh')

for mu in range(300, 3000, 50):
	#run('-r -Dtrec.model=DirichletLM -c %s' % str(mu) , _bg=True)
	#waitForJobs("java", num=5)
	a =0

# print run('-r -Dtrec.model=DirichletLM -c %s -Dtrec.results.file=myExperiment.res' % str(600))

opt_mu={'ap8889':600, 'wt2g':1850, 'wt10g':650, 'blog08':1350, 'trec8':300}
mu = opt_mu[etc]

#default MRF with optimal mu
#run('-r -c %s -Dtrec.model=DirichletLM -Dmrf.mu=%s -Dmatching.dsms=MRFDependenceScoreModifier -Dtrec.results.file=DirichletLM_MRFdef_samemu.res' % (str(mu), str(4000)) )
#print '-r -c %s -Dtrec.model=DirichletLM -Dmrf.mu=%s -Dmatching.dsms=MRFDependenceScoreModifier -Dtrec.results.file=DirichletLM_MRFdef_samemu.res' % (str(mu), str(4000))
#sys.exit() 

counter = 0

for m in ['CTOffLine']:
	for s in [1, 2, 3, 4, 5, 6, 7, 8,9, 10, 20, 25, 50, 75, 100]:
		for k in ['gaussKernel', 'triangleKernel', 'circleKernel', 'cosKernel', 'quarKernel', 'epanKernel', 'triweightKernel']:
			for w in np.arange(0.1, 0.5, 0.1):
				# print ( './bin/trec_terrier.sh -r -Dtrec.model=DirichletLM -Dp.model=%s  -Dk.sigma=%s -Dkernel.name=%s -Dw.prox=%s' %
				# 	(m, str(s), k, str(w)) )
				fname = 'DirichletLM_mu%s_w%s_k%s_s%s.res' %(str(mu), str(w), k, str(s))
				if debug:
					print '-r -c %s -Dceter.mu=%s -Dtrec.model=DirichletLM -Dp.model=%s  -Dk.sigma=%s -Dkernel.name=%s -Dw.prox=%s -Dmatching.dsms=org.terrier.matching.dsms.BlockScoreModifier_js_comb  -Dtrec.results.file=%s' % (str(mu), str(mu), m, str(s), k, str(w), fname)
					sys.exit(0)
				

				result_path = "var/results/%s/%s" % (etc, fname)
				print result_path
				if os.path.exists( result_path ):
					line = list(tail(result_path))[-1]
					if(line.split()[0]) == last_topic:
						print 'skip : %s ' % result_path
						continue
					print 'run ', result_path, line

				waitForJobs("java", num=1, sleeps=10)
				p = run('-r -c %s -Dceter.mu=%s -Dtrec.model=DirichletLM -Dp.model=%s  -Dk.sigma=%s -Dkernel.name=%s -Dw.prox=%s -Dmatching.dsms=org.terrier.matching.dsms.BlockScoreModifier_js_comb  -Dtrec.results.file=%s' % (str(mu), str(mu), m, str(s), k, str(w), fname), _bg=True)
				p.wait()
				#print '-r -c %s -Dceter.mu=%s -Dtrec.model=DirichletLM -Dp.model=%s  -Dk.sigma=%s -Dkernel.name=%s -Dw.prox=%s -Dmatching.dsms=org.terrier.matching.dsms.BlockScoreModifier_js_comb  -Dtrec.results.file=%s' % (str(mu), str(mu), m, str(s), k, str(w), fname)
				#sys.exit(0)
				counter += 1
				print counter

print counter


#waitForJobs("java", num=1)
#print run('-e')



