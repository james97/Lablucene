#########################################################################
# File Name: testQueryCutter.py
# Author: Jun M
# mail: warrior97@gmail.com
# Created Time: Mon 25 May 16:35:08 2015
#########################################################################
#!/usr/bin/env python

import unittest
from querycutter import QueryCutter

class QueryCutterTestCase(unittest.TestCase):
    def setUp(self):
        self.qc = QueryCutter("101-150", 25) 
    def testCutResult(self):
        onefold, nfolds = self.qc.generate_range_pairs() 
        self.assertEqual(cmp(onefold, ['101-125', '126-150']) , 0)
        self.assertEqual(cmp(nfolds, ['126-150', '101-125']) , 0)

def suite():  
    suite = unittest.TestSuite()  
    suite.addTest(QueryCutterTestCase("testCutResult"))  
    return suite  

if __name__ == "__main__":
    unittest.TextTestRunner().run(suite())      

