#!/usr/bin/env python

import os
import sys
import commands
import re
from querycutter import QueryCutter
#from termcolor import colored

if (len(sys.argv) != 5):
    print "Help:the format is: python nFoldcrossValid.py collectionPath strategy " +\
            "queryIdRange idgap" 

collection = sys.argv[1]
allresults = collection 
docNums = ["5", "10", "20", "30", "50"]
betas = [u"0.*"]
strategy = sys.argv[2]
cutter = QueryCutter(sys.argv[3], int(sys.argv[4]))
#parities = ["Odd", "Even"]
#parities = ["451-475","476-500"]
CVIdPairs = {}
oneFold, nFold = cutter.generate_range_pairs()
for i in range(len(oneFold)):
    if nFold[i] == "151-195":
        nFold[i] = "151-195,201-200"
    CVIdPairs[nFold[i].replace(",", "")] = oneFold[i]




cmds =[]
for docNum in docNums:
    for beta in betas:
        for parity in CVIdPairs.keys():
		#DPH_QEAdap_RocDFRPow=1.5FromTop_10_20_TFIDF0.2
		if strategy == "Roc":
		    cmd  = u"./listMAP.py " + allresults +u"/|grep Q" + parity +u"DPH_QEAdap_RocDFRPow=1.5FromTop_" + docNum + u"_20_TFIDF|cut -d: -f3|sort"
		else:
		    cmd = u"./listMAP.py " + allresults +u"/|grep Q" + parity +u"DPH_QEAdap_TopicSel_" + strategy + ".*" + beta + "=trueFromTop_" + docNum + u"_20_TFIDF|cut -d: -f3|sort"
		cmds.append(cmd)

#print len(cmds)
bestScores = []
accordings = []
for cmd in cmds:
    val = commands.getoutput(cmd).split("\n") 
   # print cmd
   #print val[-1]    
    bestScores.append(val[-1])
    cmd = cmd.replace(u"cut -d: -f3|sort", u"grep " + val[-1])
    item = commands.getoutput(cmd).split("\n") 
    #print item[0]
    for ids in CVIdPairs.keys():
        if re.search(ids, item[0]):
            according = item[0].replace(ids, CVIdPairs[ids])
            break
    

    #print according
    m = re.search("TFIDF\d\.\d", according)
    according  = according[:m.end()]
    newcmd = "./listMAP.py " +  allresults + "|grep " + according
    accordingResult =  commands.getoutput(newcmd).split("\n")
    accordings.append(accordingResult[0])

#print bestScores
pattern = u"FromTop_.*_20_TFIDF"
#pattern = u"DPH_QEAdap_TopicSel_s=[0-9]{1,2}t=[0-9]{1,2}beta=.*expTag=truewithOrgScore=trueFromTop_.*_30_TFIDF0\.[0-9]"
pairs = {}
for according in accordings:
    m = re.search(pattern, according)
    #put all the results for the same docNum in a group
    if m.group() not in pairs.keys():
        pairs[m.group()] = []
        pairs[m.group()].append(according)
    else:
         pairs[m.group()].append(according)

#Get the average of all the one-fold results
for key in pairs.keys():
    print key + "\n"
    org = [x for x in pairs[key]] 
    #noneorg = list(set(pairs[key]).difference(set(org))) 
    #print org
    values = [x.split(":")[2] for x in org]
    avg = 0.0
    for value in values:
       avg = avg + float(value)
    print str(avg/len(values)) + "\n"
    #print noneorg
    #values = [x.split(":")[2] for x in noneorg]
    #avg = 0.0
    #for value in values:
    #   avg = avg + float(value)
    #print str(avg/2.0) + "\n"

#    print  "\n\n"


