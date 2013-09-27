#!/usr/bin/env python
import subprocess
import optparse
import re
import time

#Create variables out of shell commands
#Note triple quotes can embed Bash

#You could add another bash command here
#HOLDING_SPOT="""fake_command"""

#Determines Home Directory Usage in Gigs
HOMEDIR_USAGE = """
du -sh $HOME | cut -f1
"""
#Determines IP Address

IPADDR = """
/sbin/ifconfig -a | awk '/(cast)/ { print $2 }' | cut -d':' -f2 | head -1
"""

#This function takes Bash commands and returns them
def bash(cmd):
    p = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE)
    out = p.stdout.read().strip()
    return out  #This is the stdout from the shell command


def waitForJobs(jobName, num=5, sleeps=20):
 bs='ps -ef |grep $USER|grep ' + jobName + '|egrep -v \'waitForJobs.sh| grep \'|wc -l'
 while True:
  currentnum = int(bash(bs))
  if currentnum < num :
   break
  else:
   time.sleep(sleeps)



 

VERBOSE=False
def report(output,cmdtype="UNIX COMMAND:"):
   #Notice the global statement allows input from outside of function
   if VERBOSE:
       print "%s: %s" % (cmdtype, output)
   else:
       print output

#Function to control option parsing in Python
def controller():
    global VERBOSE
    #Create instance of OptionParser Module, included in Standard Library
    p = optparse.OptionParser(description='Tools for using bash in python',
                                            prog='ibash.py',
                                            version='ibash 1.0',
                                            usage= '%prog [option]')
    p.add_option('--ip','-i', action="store_true", help='gets current IP Address')
    p.add_option('--usage', '-u', action="store_true", help='gets disk usage of homedir')
    p.add_option('--verbose', '-v',
                action = 'store_true',
                help='prints verbosely',
                default=False)

    #Option Handling passes correct parameter to runBash
    options, arguments = p.parse_args()
    if options.verbose:
        VERBOSE=True
    if options.ip:
        value = runBash(IPADDR)
        report(value,"IPADDR")
    elif options.usage:
        value = runBash(HOMEDIR_USAGE)
        report(value, "HOMEDIR_USAGE")
    else:
        p.print_help()

#Runs all the functions
def main():
    controller()

#This idiom means the below code only runs when executed from command line
if __name__ == '__main__':
   main()
