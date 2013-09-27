#!/usr/bin/python
"""
extract_original_content.py docno
print out the url and content of doc with docno
"""
import gzip, sys, re

key = sys.argv[1]
print type(key)
keys = key.split('-')
#fname = sys.argv[2]
path = '%s/permalinks-%s.gz' % (keys[1], keys[2])


#----------------------------------------------------------------------
def extract(content):
    """"""

#key = 'BLOG06-20051206-026-0000000000'  
pat = '(<DOC>.*?<DOCNO>%s</DOCNO>.*?<PERMALINK>(.*?)</PERMALINK>.*?</DOCHDR>(.*?)</DOC>)' % key

with gzip.open(path, 'rb') as f:
    content = f.read()    
    matchObj = re.search(pat, content, re.S)
    print matchObj.group(2)
    print matchObj.group(3)