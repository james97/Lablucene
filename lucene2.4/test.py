#!/usr/bin/env pypy

print "good"

def cmp():
	totle = 0;
	for i in range(20):
		for j in range(1000):
			i*j
			totle += i*j

	return totle

print( cmp())

def cmp1():
	f=0.345
	f1=5.6456
	for i in range(100000000):
		f*f1


cmp1()
