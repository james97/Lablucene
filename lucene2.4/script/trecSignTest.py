#!/usr/bin/env python

import subprocess
import os
import sys
import signtest
from optparse import OptionParser
from optparse import OptionGroup

base="/home/benhe/jeff/lucene2.4.1/lib"
tbase="/home/benhe/jeff/temp/"
des="""

default (no para) is to scp the jar lib from Haze.
"""

#This function takes Bash commands and returns them
def bash(cmd):
    p = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE)
    out = p.stdout.read().strip()
    return out  #This is the stdout from the shell command

##############################
def getAPs(file, base='.'):
 #file = base+ "/" + file
 cmdtext= "sed -n '38,$p' " + file + " |awk '{ printf $2 \" \\n\"}'"
 out = bash(cmdtext)
 x = [float(value) for value in out.split()]
 return x
#############################


def main():
 parser = OptionParser(usage="usage: %prog [options] filename1 filename2", version="%prog 1.0", epilog="post") 

 '''
 parser.add_option("-t", "--trans",
                  action="store_true", dest="trans", default=False,
                  help="transfer a local file to the Haze server")
 parser.add_option("-g", "--gets",
                  action="store_true", dest="gets", default=False,
                  help="get a file from the Haze Server") 
 '''
 parser.set_description(des)
 (options, args) = parser.parse_args()
 alen = len(args)
 print "options:", options
 print "args:", args
##########################################################
 if alen == 2:
  x = getAPs(args[0])
  y = getAPs(args[1])
  signtest.signtest_pvalue(x, y, sided = 0, verbose =1, html=False)
 elif alen ==1:
  print getAPs(args[0])
 else:
  parser.print_help()
  exit

if __name__ == '__main__':
    main()
