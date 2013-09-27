#!/usr/bin/env python
import subprocess
import os
import sys
from os import path
from optparse import OptionParser
from optparse import OptionGroup

base="/home/benhe/jeff/lucene2.4.1/lib"
tbase="/home/benhe/jeff/temp/"
des="""
default (no para) is to scp the jar lib from Haze.
"""

##############################
def getMulti(files, base='.'):
 files = [base+ "/" + x for x in files]
 return '"' + ' '.join(files) +'"'
#############################


def main():
 parser = OptionParser(usage="usage: %prog [options] [filename]", version="%prog 1.0", epilog="post")
 parser.add_option("-t", "--trans",
                  action="store_true", dest="trans", default=False,
                  help="transfer a local file to the Haze server")
 parser.add_option("-g", "--gets",
                  action="store_true", dest="gets", default=False,
                  help="get a file from the Haze Server")
 parser.set_description(des)
 (options, args) = parser.parse_args()
 alen = len(args)
 print "options:", options, " args:", args
 print base
##########################################################
 if not options.trans and not options.gets and alen >0:
  parser.print_help()
 elif options.trans and not options.gets:
  scpcommand='scp -r ' + ' '.join(args) + ' benhe@haze.hprn.yorku.ca:' + tbase
  print scpcommand
  os.system(scpcommand)
 elif options.gets and not options.trans:
  scpcommand='scp -r benhe@haze.hprn.yorku.ca:' + getMulti(args, base = tbase) + ' .'
  print scpcommand
  os.system(scpcommand)
 else:
  scpcommand='scp -r benhe@haze.hprn.yorku.ca:"' + base + '/DUTLIB.jar ' + base +'/LabLucene1.0.jar" lib/'
  print scpcommand
  os.system(scpcommand)



if __name__ == '__main__':
 main()
 #print getMulti(["b"]) 
