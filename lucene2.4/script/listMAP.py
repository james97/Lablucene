#!/usr/bin/env python
import sys
import subprocess
import re
import os
from optparse import OptionParser

def runBash(cmd):
    p = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE)
    out = p.stdout.read().strip()
    return out  #This is the stdout from the shell command

spec="var/results/topicRF/GOV2/"

map_str="Average Precision"
Rpre_str="R Precision"
pat10_str="Precision at   10"

def grepDir(dirname):
    fileList = os.listdir(dirname)
    rList = []
    for file in fileList:
        if file.endswith(".eval"):
            path = os.path.join(dirname, file)
            rList.append(file.replace(".gz.eval", "") +"\t" +grep3(path))
    rList.sort()
    return '\n'.join(rList)

def grep3(fname):
    file = open(fname)  
    lines = file.readlines()
    for line in lines:
        if line.lower().find(map_str.lower()) != -1:
            Map = line.split(":")[1].strip()
        elif line.lower().find(Rpre_str.lower()) != -1:
            Rpre = line.split(":")[1].strip()
        elif line.lower().find(pat10_str.lower()) != -1:
            Pat10 = line.split(":")[1].strip()
    return "MAP:" + Map + " P@10:" + Pat10 +" RPre:" + Rpre

def grep1(fname, measure="Average    \t Precision"):
    p= re.compile('\s+')
    fmeasure = p.sub("\s+", measure)
    pres= re.compile(fmeasure)
    file = open(fname)
    lines = file.readlines()
    for line in lines:
        if pres.match(line):
            return line.strip()
    return "Measure " + '"' +measure +'"'+ " does not exist"

def grep(fname, measure='Average'):
    file = open(fname)
    lines = file.readlines()
    for line in lines:
        pos = line.lower().find(measure.lower())
        if pos != -1:
            return line.strip()
    return "Measure " + '"' +measure +'"'+ " does not exist"

def main():
 parser = OptionParser(usage="usage: %prog [options] [dirname]",version="%prog 1.0")
 parser.add_option("-d",
                  action="store_true", dest="d", default=False,
                  help='display the map results from ./var/results')
 parser.add_option("-s", 
                  action="store_true", dest="s", default=False,
                  help='display the map results from internally specified') 
 parser.add_option("-m", 
                  action="store", dest="measure", default="Average",
                  help='specify the eval measure to grep')

 (options, args) = parser.parse_args()
 print "options:", options
 print "args:", args
 measure = options.measure
 
 l = len(args)
 if l == 1:
  grep = "ls -tr " +  args[0] + "/*.eval |xargs grep " + measure + "|uniq | sed 's/.*\///g'"
  #out = runBash(grep)
  #print out
  print grepDir(args[0])
 elif options.d :
   grep1 = "ls -tr var/results/*.eval |xargs grep Average |uniq"
   out = runBash(grep1)
   print out
 elif options.s or l == 0:
  grep = "ls -tr " +  spec + "/*.eval |xargs grep " + measure +" |uniq | sed 's/.*\///g'"
  out = runBash(grep)
  print out


if __name__ == '__main__':
    main()
