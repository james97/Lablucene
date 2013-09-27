#!/usr/bin/env python

# -*- coding: utf-8 -*-
"""
Created on Wed Nov 28 03:19:47 2012

@author: zheng
"""

import sys, os, glob, logging, sh

DROPBOX_HOME = os.environ['DROPBOX_HOME']
LUCENE_HOME = os.environ['LUCENE_HOME']
sys.path.append("%s/python" % DROPBOX_HOME)
sys.path.append("/data/Dropbox/python")
from ibash import bash, waitForJobs, bashBackground
LUCENE_HEAP_MEM = "1024M"
from itertools import product
# from sklearn.grid_search import IterGrid
# LUCENE_HOME="/home/zheng/workspace/java/lucene2.4"
class LogFile(object):
    """File-like object to log text using the `logging` module."""

    def __init__(self, name=None):
        self.logger = logging.getLogger(name)

    def write(self, msg, level=logging.INFO):
        self.logger.log(level, msg)

    def flush(self):
        for handler in self.logger.handlers:
            handler.flush()

logging.basicConfig(level=logging.DEBUG, filename='mylog.log')



class IterGrid(object):
    """Generators on the combination of the various parameter lists given

    Parameters
    ----------
    param_grid: dict of string to sequence
        The parameter grid to explore, as a dictionary mapping estimator
        parameters to sequences of allowed values.

    Returns
    -------
    params: dict of string to any
        **Yields** dictionaries mapping each estimator parameter to one of its
        allowed values.

    Examples
    --------
    >>> from sklearn.grid_search import IterGrid
    >>> param_grid = {'a':[1, 2], 'b':[True, False]}
    >>> list(IterGrid(param_grid)) #doctest: +NORMALIZE_WHITESPACE
    [{'a': 1, 'b': True}, {'a': 1, 'b': False},
     {'a': 2, 'b': True}, {'a': 2, 'b': False}]

    See also
    --------
    :class:`GridSearchCV`:
        uses ``IterGrid`` to perform a full parallelized grid search.
    """

    def __init__(self, param_grid):
        self.param_grid = param_grid

    def __iter__(self):
        param_grid = self.param_grid
        if hasattr(param_grid, 'items'):
            # wrap dictionary in a singleton list
            param_grid = [param_grid]
        for p in param_grid:
            # Always sort the keys of a dictionary, for reproducibility
            items = sorted(p.items())
            keys, values = zip(*items)
            for v in product(*map(lambda x: x if isinstance(x, list) else [x], values)):  # avoid failure when the dict values are not list
                params = dict(zip(keys, v))
                yield params


jars = glob.glob(LUCENE_HOME + "/lib/*.jar")
CLASSPATH = ":".join(jars)
#jarsful = map(lambda x: LUCENE_HOME +"/lib/%s" %x, jars)
datasets=["robustT301-450",                     #0
          "WT2G",                               #1
          "WT10GT451-550",                      #2
          "GOV2",                               #3
          "robust04",                           #4
          "trec8Disk45-CR-T401-450",            #5
          "trec7Disk45-CR-T351-400",            #6
          "genomic06",                          #7 
          "trec6Disk45T301-350",
          "trec5Disk24T251-300",
          "trec9WebAdhocT451-500",
          "trec10WebAdhocT501-500",
          "trec123Disk12T51-200",
          "robust",
          "disk4and5T301-450",
          "disk12NEWST51-200",
          "socialTag",
          "Chemiscal",
          "trecDisk1to5",
          "Microblog11",
          "genomic04",
          "clueweb09"
]

ETC = "WT10GT451-550"
os.environ['ETC'] = ETC
resultpath = "results/test"
if not os.path.isdir("var/%s/%s" % (resultpath, ETC)):
    os.makedirs("var/%s/%s" % (resultpath, ETC))
defaultdict = {"Lucene.Search.WeightingModel": "BM25",
              "bm25.b": "0.3",
              "Lucene.Search.LanguageModel": "false",
              "trec.results": "%s/%s" % (resultpath, ETC),
              "Lucene.TRECQuerying.outputformat": "TRECDocidOutputFormat"
}

def get_defaultpara(ETC = "WT10GT451-550", resultpath = "results"):
  if not os.path.isdir("var/%s/%s" % (resultpath, ETC)):
    os.makedirs("var/%s/%s" % (resultpath, ETC))
  os.environ['ETC'] = ETC
  defaultdict = {"Lucene.Search.WeightingModel": "BM25",
              "bm25.b": "0.3",
              "Lucene.Search.LanguageModel": "false",
              "trec.results": "%s/%s" % (resultpath, ETC),
              "Lucene.TRECQuerying.outputformat": "TRECDocidOutputFormat"}
  return defaultdict

ppdict = { #for postprocessing
    "Lucene.PostProcess": "QueryExpansionAdap", 
    "term.selector.name": "RocchioTermSelector",
    "Lucene.QueryExpansion.Model": "KL", 
    "QueryExpansion.RelevanceFeedback": "false", 
    "parameter.free.expansion": "false"
}


def setEnviron():
    """set default etc"""
    if os.environ.has_key('ETC'):
        LUCENE_ETC = LUCENE_HOME + "/etc/" + os.environ['ETC']
    else:
        LUCENE_ETC = LUCENE_HOME + "/etc/"
    return LUCENE_ETC


def toLuceneParas(dicts):
    str_list = []
    for (k, v) in dicts.items():
        str_list.append("--%s %s" %( k, str(v) ))
    return ' '.join(str_list)

LUCENE_ETC = setEnviron()
basicParaList=["-Xmx%s" % LUCENE_HEAP_MEM,
              "-cp", CLASSPATH,  "-Dlucene.etc=%s -Dlucene.home=%s -Dlucene.setup=%s/lucene.properties" % (LUCENE_ETC, LUCENE_HOME, LUCENE_ETC)
    ]

def toLuceneParasList(dicts, method='basic'):
    str_list = basicParaList[:] #copy
    for (k, v) in dicts.items():
        str_list.append("--%s" %k)
        str_list.append(str(v))
    if method=='basic':
        str_list.append('-r')
    elif method=='pp':
        str_list.append('-r -q')
    elif method == 'eval':
        str_list.append('-e')
    return str_list

def evalall(para = ''):
    LUCENE_ETC = setEnviron()
    execution = """java -Xmx%s -cp %s -Dlucene.etc=%s -Dlucene.home=%s -Dlucene.setup=%s/lucene.properties"""  % (LUCENE_HEAP_MEM, CLASSPATH, LUCENE_ETC, LUCENE_HOME, LUCENE_ETC)
    # print "%s org.dutir.lucene.TrecLucene -e %s" %(execution, para)
    return "%s org.dutir.lucene.TrecLucene -e %s" %(execution, para)

def basicRetrieve(para=''): #using all default paras in the property file
    LUCENE_ETC = setEnviron()
    execution = """java -Xmx%s -cp %s -Dlucene.etc=%s -Dlucene.home=%s -Dlucene.setup=%s/lucene.properties""" %(LUCENE_HEAP_MEM, CLASSPATH, LUCENE_ETC, LUCENE_HOME, LUCENE_ETC)
    return "%s org.dutir.lucene.TrecLucene -r %s" %(execution, para)

def ppRetrieve(para=''): #using all default paras in the property file
    LUCENE_ETC = setEnviron()
    execution = """java -Xmx%s -cp %s -Dlucene.etc=%s -Dlucene.home=%s -Dlucene.setup=%s/lucene.properties""" %(LUCENE_HEAP_MEM, CLASSPATH, LUCENE_ETC, LUCENE_HOME, LUCENE_ETC)
    return "%s org.dutir.lucene.TrecLucene -r -q %s" %(execution, para)


merge = (lambda a, b: (lambda a_copy: a_copy.update(b) or a_copy) (a.copy()) ) # merge multiple dict with reduce

def merge_dict(a,b):
  return merge(a,b)


def run(paradict, type='basic', pnum=4, ETC = "WT10GT451-550", resultpath = "results", eval=True, debug=False):
  import time
  begintime = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime())
  fun = basicRetrieve
  basedict = {}
  os.environ['ETC'] = ETC
  if type == 'basic': #for basic retrieval 
      fun = basicRetrieve 
  elif type == 'PP': # for post-processing, including query expansion. 
      fun = ppRetrieve
      basedict.update(ppdict)
  de_dict = get_defaultpara(ETC = ETC, resultpath = resultpath)
  for pdict in IterGrid(paradict):
      fdict = reduce(merge_dict, (de_dict, basedict, ppdict, pdict))
      # waitForJobs("TrecLucene", num=pnum)
      waitForJobs("java", num=pnum)
      # print toLuceneParas(fdict)
      if not debug:
        cmd = fun(para=toLuceneParas(fdict))
        bashBackground(cmd)
      else:
        print toLuceneParas(fdict)
  if eval:
    waitForJobs("java", num=1)
    cmd = evalall( para= toLuceneParas(get_defaultpara(ETC = ETC, resultpath = resultpath)) )
    bash(cmd)
    print 'evaled all file in %s' % (' ' + resultpath +'/' + ETC)
  endtime = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime())
  print begintime, endtime


def run1(paradict, type='basic', pnum=4, ETC = "WT10GT451-550", resultpath = "results", eval=True, debug=False):
  import time
  begintime = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime())
  fun = basicRetrieve
  basedict = {}
  os.environ['ETC'] = ETC
  if type == 'basic': #for basic retrieval 
      fun = basicRetrieve 
  elif type == 'PP': # for post-processing, including query expansion. 
      fun = ppRetrieve
      basedict.update(ppdict)
  de_dict = get_defaultpara(ETC = ETC, resultpath = resultpath)
  for pdict in IterGrid(paradict):
      fdict = reduce(merge_dict, (de_dict, basedict, ppdict, pdict))
      # waitForJobs("TrecLucene", num=pnum)
      waitForJobs("java", num=pnum)
      # print toLuceneParas(fdict)
      if not debug:
        cmd = fun(para=toLuceneParas(fdict))
        bashBackground(cmd)
      else:
        print toLuceneParas(fdict)
  if eval:
    waitForJobs("java", num=1)
    cmd = evalall( para= toLuceneParas(get_defaultpara(ETC = ETC, resultpath = resultpath)) )
    bash(cmd)
    print 'evaled all file in %s' % (' ' + resultpath +'/' + ETC)
  endtime = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime())
  print begintime, endtime




# Redirect stdout and stderr
#sys.stdout = LogFile('stdout')
#sys.stderr = LogFile('stderr')
sh.ls('/home', _out='log.txt', _err_to_out=True, _bg=True)
#sh.sleep('2')
#sh.java(_err_to_out=True, _out=sys.stdout)
#print 'sleep over'
