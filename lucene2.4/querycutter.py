#!/bin/sh/env python

import os
import sys

class QueryCutter:

    def __init__(self, id_range_str, id_gap):
        self.id_ranges = id_range_str.split("-")
        self.id_gap = id_gap

    def get_id_ranges(self):
        return self.id_ranges

    def set_id_ranges(self, id_range_str):
         self.id_ranges = id_range_str.split("-")
    
    def get_id_gap(self):
        return self.id_gap

    def set_id_gap(self, id_gap):
        self.id_gap = id_gap

    def generate_range_pairs(self):
        '''
        Return two lists, one contains the queries for one fold, and the second
        one contains the according rest queries
        '''
        startid = int(self.id_ranges[0].strip())
        endid = int(self.id_ranges[1].strip())

        nMinus1fold = []
        onefold = []
        cursor = startid
        while cursor <= endid: 
            if cursor == startid:
	        onefold.append( str(cursor)+ "-" + str(cursor + self.id_gap - 1 ))
   	        nMinus1fold.append( str(cursor + self.id_gap) + "-" + str(endid))
            elif cursor + self.id_gap - 1 == endid:
                nMinus1fold.append(str(startid) + "-" + str(cursor - 1))
                onefold.append(str(cursor)+ "-" + str(cursor + self.id_gap - 1 ))
            else:
                onefold.append(str(cursor)+ "-" + str(cursor + self.id_gap - 1 ))
                nMinus1fold.append(str(startid) + "-" + str(cursor - 1) + "," +
                        str(cursor + self.id_gap) + "-" + str(endid))
            cursor += self.id_gap
        return onefold, nMinus1fold

