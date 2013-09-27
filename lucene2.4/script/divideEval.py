#!/usr/bin/env python
import subprocess
import os,sys,re,gzip
from optparse import OptionParser
from optparse import OptionGroup
import tempfile
from array import array

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
 scores = array('f')
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

def divideeval(evalfile, num):
 qlen = len(num)
 f = open(evalfile, 'r')
 pos=0;
 start=36;
 scores = array('f')
 qid = array('i')
 for line in f:
  if pos > start:
   values = line.split()
   score = float(values[1])
   print (pos - start),line,
   qid.append(int(values[0]))
   scores.append(score)
  pos += 1
 
 pre = 0
 tave =0
 tzcount = scores.count(0.00)
 aves = array('f')
 for i in range(0, qlen): 
   print num[i]
   print pre, qid.index(num[i]) 
   lastid = qid.index(num[i])
   tb = scores[pre:qid.index(num[i])]
   #print scores[pre], scores[qid.index(num[i])]
   print 'sum', sum(tb)
   zcount = tb.count(0.00)
   print "zcount:", zcount
   ave = sum(tb)/len(tb)
   aves.append(ave)
   print 'ave', ave
   pre = qid.index(num[i]) + 1
   tave += ave
 print tzcount, tave,qlen, sum(scores),len(scores)
 print tzcount, tave/qlen, sum(scores)/len(scores)
 map = sum(scores)/len(scores)
 for i in range(0, qlen):
  print qlen * map * aves[i] / sum(aves)

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
 #print "options:", opt, " args:", args
 global qrel, evalprog, topxTag, top, reEval
 
#######################################################
 qnum = array('i')
 for i in range(1, alen):
  qnum.append(int(args[i]))  
 divideeval(args[0], qnum)


if __name__ == '__main__':
 main()
