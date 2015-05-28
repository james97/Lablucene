#!/usr/bin/env python
import sys
import subprocess
import re
from optparse import OptionParser

def runBash(cmd):
    p = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE)
    out = p.stdout.read().strip()
    return out  #This is the stdout from the shell command

spec="var/results/topicRF/GOV2/"
spec="var/results/topicRF/GOV2RF08/"

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
  grep = "find " +  args[0] + "/. -name *.eval |xargs grep " + measure + "|uniq | sed 's/.*\///g'"
  out = runBash(grep)
  print out 
 elif options.d :
   grep1 = "find " +  args[0] + "/. -name *.eval ||xargs grep Average |uniq"
   out = runBash(grep1)
   print out
 elif options.s or l == 0:
  grep = "find " +  args[0] + "/. -name *.eval ||xargs grep " + measure +" |uniq | sed 's/.*\///g'"
  out = runBash(grep)
  print out


if __name__ == '__main__':
    main()
