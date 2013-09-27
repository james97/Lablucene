#!/usr/bin/env python
import subprocess
import os
import sys
from optparse import OptionParser
from optparse import OptionGroup

spec="var/results/topicRF/GOV2/"

def runBash(cmd):
    p = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE)
    out = p.stdout.read().strip()
    return out  #This is the stdout from the shell command


def main():
 parser = OptionParser(usage="usage: %prog [options] [dirname]",version="%prog 1.0")

 (options, args) = parser.parse_args()
 print runBash('less ' + spec + args[0])


if __name__ == '__main__':
    main()
