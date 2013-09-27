"""
generating Collection.spec file for Terrier and LabLucene
"""
import glob, sys

if len(sys.argv) >1 :
	mypath = sys.argv[1]
else:
	mypath = '/media/disk/Collection/Blogs06Collection/'

gzlist = glob.glob(mypath + "/*/*.gz")

filtered_gzlist = gzlist
#filtered_gzlist = filter(lambda x : x.find('/bloghps') != -1, gzlist)
#filtered_gzlist = filter(lambda x : x.find('permalinks') != -1, gzlist)
filtered_gzlist = filter(lambda x : x.find('AP/') != -1, gzlist)

for line in filtered_gzlist:
    print line