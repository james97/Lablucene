#!/usr/bin/env python
import subprocess
import os,sys,re,gzip
from optparse import OptionParser
from optparse import OptionGroup
import tempfile


qreldefault="TopicQrel/RF08/08.qrels.top10.txt.innerID"
base="var/results/topicRF/GOV2RF08/"
qrel="TopicQrel/RF08/08.qrels.top10.txt.innerID"
evalprog="trec_eval"
topxTag=False
reEval=False
top=1000
ext=[".gz", ".res"]

def bash(cmd):
    print cmd
    p = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE)
    out = p.stdout.read().strip()
    return out  #This is the stdout from the shell command


def evalf(filename):
 filename = base + filename
 name = None
 outfile = filename + ".eval"
 if os.path.exists(outfile) and not reEval:
  return
 if re.match('.+\.gz$', filename):
  global topxTag
  if topxTag :
   name = tempfile.gettempdir() +"/result.res"
   fw = open(name, 'w')
   f = gzip.open(filename, 'r')
   preid = "-1"
   pos = 0
   for line in f:
     dlist = line.split()
     if dlist[0] == preid:
        if pos < top: 
          fw.write(line)
          pos += 1 
     else:
        pos = 0 
        preid = dlist[0]
        fw.write(line)
   f.close()
   fw.close()
  else:
   name = tempfile.gettempdir() +"/result.res"
   bash('zcat ' + filename + '>' + name)
 elif re.match("", filename):
  name = filename
 output = bash(evalprog + ' ' + qrel + " " + name)
 print output
 
def evalDir(tdir):
 if not os.path.isdir(tdir):
  evalf(tdir) #print tdir
  return
 directories = [tdir]
 while len(directories)>0:
    directory = directories.pop()
    for name in os.listdir(directory):
        fullpath = os.path.join(directory,name)
        if os.path.isfile(fullpath):
            basename, extent = os.path.splitext(fullpath) 
            global ext
            if extent in ext :
              evalf(fullpath)  #print fullpath
            #else: 
            #  print extent, ext
              # That's a file. Do something with it.
        elif os.path.isdir(fullpath):
            directories.append(fullpath)  # It's a directory, store it.

def main():
 parser = OptionParser(usage="usage: %prog [options] [file/dir]", version="%prog 1.0")
 parser.add_option("-q", "--query",
                  action="store_true", dest="q", default=False,
                  help="In addition to summary evaluation, give evaluation for each query")
 parser.add_option("-r", "--reeval",
                  action="store_true", dest="r", default=False,
                  help="whether to re-eval the result file when eval-file exists")
 parser.add_option("-x", "--extract",
                  action="store_true", dest="x", default=False,
                  help="whether to cut the orginal result file")
 parser.add_option("-t", "--top",
                  action="store", dest="topk", default=1000, type="int",
                  help="extract top x docs to be evaluated (default = 1000)")
 #parser.set_description(des)
 (opt, args) = parser.parse_args()
 alen = len(args)
 print "options:", opt, " args:", args
 global qrel, evalprog, topxTag, top, reEval
 topxTag = opt.x
 top= opt.topk
 reEval = opt.r
#######################################################
 if opt.q :
  evalprog += " -q"
 if alen == 1:
  evalDir(args[0])
 elif alen == 2 : 
  qrel = args[0]
  evalDir(args[1]) 
 else : 
  evalDir(args[0])


if __name__ == '__main__':
 main()
