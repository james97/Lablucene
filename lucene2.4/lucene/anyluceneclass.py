#!/bin/env python
from setup import LUCENE_HEAP_MEM, LUCENE_HOME, ETC, CLASSPATH


PARA = [ '-Xmx%s' % LUCENE_HEAP_MEM, 
          '-Dlucene.etc=%s/%s' % (LUCENE_HOME, ETC),
          '-Dlucene.home=% (LUCENE_HOME, ETC)',
          '-Dlucene.setup=/home/zheng/workspace/java/lucene2.4/etc/WT2G/lucene.properties',
          '-cp',
          CLASSPATH]
