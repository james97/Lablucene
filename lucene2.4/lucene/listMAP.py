#!/usr/bin/env python
import sys
import subprocess
import re
from optparse import OptionParser
import scriptutil as su

def runBash(cmd):
    p = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE)
    out = p.stdout.read().strip()
    return out  #This is the stdout from the shell command

spec="var/results/topicRF/GOV2/"

def main():
 parser = OptionParser(usage="usage: %prog [options] [result_file_dirname]",version="%prog 1.0")
 parser.add_option("-d",
                  action="store_true", dest="d", default=False,
                  help='display the map results from ./var/results')
 parser.add_option("-s", 
                  action="store_true", dest="s", default=False,
                  help='display the map results from internally specified') 
 parser.add_option("-m", 
                  action="store", dest="measure", default="Average",
                  help='specify the eval measure to grep')
 parser.add_option("-o",   ##deprecated, use -f
                  action="store_true", dest="o", default=False,
                  help='order the result by the specified measure') 
 parser.add_option("-f",
                  action="store", type='int', dest="f", default=0,
                  help='order the result by the specified measure')
 
 (options, args) = parser.parse_args()
 print "options:", options
 print "args:", args
 measure = options.measure
 
 l = len(args)
 if True:
     flist = su.ffindgrep(args[0], shellglobs=('*.eval',), regexl=("Average| 5:| 20:",), firstoccur=False, retlist=True )
     fout =[]
     from os import sep
     for key, value in flist.iteritems():
         fout.append(':'.join([v.split(':')[1].strip() for v in value])  + ":\t" + key.split(sep)[-1])
     fout.sort( key=lambda line: float(line.split(':')[options.f]) )
     print "\n".join(fout)
 elif l == 1 and options.o:
  grep = "ls -tr " +  args[0] + "/*.eval |xargs grep " + measure + "|uniq | sed 's/.*\///g'"
  grep1 = "ls -tr " +  args[0] + "/*.eval |xargs grep " + "' 5:'" + "|uniq | sed 's/.*\///g'"
  grep2 = "ls -tr " +  args[0] + "/*.eval |xargs grep " + "' 20:'" + "|uniq | sed 's/.*\///g'"
  out = runBash(grep)
  out1 = runBash(grep1)
  out2 = runBash(grep2)
  lout = out.split('\n')
  lout1 = [" :" + line.split(":")[2] for line in out1.split('\n')]
  lout2 = [" :" + line.split(":")[2] for line in out2.split('\n')]

  fout = [(a + b + c) for a, b, c in zip(lout, lout1, lout2)]
  fout.sort( key=lambda line: float(line.split(':')[2]) )
  #print "\n".join(fout)
  #sort_bash = "echo '%s'|sort -t':' -k2" % "\n".join(fout)
  ##print sort_bash
  #print runBash(sort_bash)
 elif l == 1 and options.o:
  grep = "ls -tr " +  args[0] + "/*.eval |xargs grep " + measure + "|uniq | sed 's/.*\///g' |sort -t':' -k3"
  out = runBash(grep)
  print out 
 elif l == 1:
  grep = "ls -tr " +  args[0] + "/*.eval |xargs grep " + measure + "|uniq | sed 's/.*\///g'"
  out = runBash(grep)
  print out
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
