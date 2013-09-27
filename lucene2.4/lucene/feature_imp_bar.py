from sklearn.ensemble import ExtraTreesClassifier
from pylab import *
import pylab as pl
# Build a forest and compute the feature importances
forest = ExtraTreesClassifier(n_estimators=250,
                              compute_importances=True,
                              random_state=0)

# x=frange(0,1,.01)


x = range(1,11)
#        0         8         9         2         3         4         5         6         7        1 
y  = [0.131026, 0.094010, 0.123959, 0.015813, 0.044785, 0.045511, 0.105831, 0.099095, 0.113181, 0.100042]
y1 = [0.142498, 0.151950, 0.139929, 0.019720, 0.030029, 0.029560, 0.118861, 0.128810, 0.128810, 0.128810]
y2 = [0.117890, 0.102176, 0.116800, 0.017164, 0.032594, 0.031945, 0.109981, 0.111665, 0.115154, 0.103175]
y3 = [0.112291, 0.100524, 0.136881, 0.010095, 0.013724, 0.014929, 0.116706, 0.116768, 0.121900, 0.111505]

# f=figure(figsize=(6, 2.5))
def featurn_imp_2_3grid():
    f = figure(figsize=(10, 6))
    subplot(231)
    # plt.subplots_adjust(left=0.1, right=0.2, top=0.2, bottom=0.1)
    pl.title("disk4&5")
    pl.bar(x, y, color="r", align="center")
    pl.xticks(x)
    pl.xlim([0, 11])
    pl.ylim([0, 0.152])
    
    subplot(232)
    pl.title("WT2G")
    pl.bar(x, y1, color="r", align="center")
    pl.xticks(x)
    pl.xlim([0, 11])
    pl.ylim([0, 0.152])
    
    subplot(233)
    pl.title("WT10G")
    pl.bar(x, y2, color="r", align="center")
    pl.xticks(x)
    pl.xlim([0, 11])
    pl.ylim([0, 0.152])
    
    subplot(234)
    pl.title("GOV2")
    pl.bar(x, y3, color="r", align="center")
    pl.xticks(x)
    pl.xlim([0, 11])
    pl.ylim([0, 0.152])
    
    subplot(235)
    pl.title("Robust04")
    pl.bar(x, y3, color="r", align="center")
    pl.xticks(x)
    pl.xlim([0, 11])
    pl.ylim([0, 0.152])

    #pl.savefig("/data/Dropbox/latex/sigir2013/feature_imp.eps", bbox_inches='tight')
    # tight_layout()
    #pl.show()

def featurn_imp_3_5grid():
    import pylab as pl
    import matplotlib.gridspec as gridspec
    
    pl.figure(figsize=(6, 4))
    G = gridspec.GridSpec(2, 6)
    
    axes_1 = pl.subplot(G[0, :2])
    pl.title("disk4&5")
    pl.bar(x, y, color="r", align="center")
    pl.xticks(x)
    pl.xlim([0, 11])
    pl.ylim([0, 0.152])

    
    axes_2 = pl.subplot(G[0, 2:4])
    pl.title("WT2G")
    pl.bar(x, y1, color="r", align="center")
    pl.xticks(x)
    pl.xlim([0, 11])
    pl.ylim([0, 0.152])
    
    axes_3 = pl.subplot(G[0, 4:])
    pl.title("WT10G")
    pl.bar(x, y2, color="r", align="center")
    pl.xticks(x)
    pl.xlim([0, 11])
    pl.ylim([0, 0.152])
    
    axes_4 = pl.subplot(G[-1, 1:3])
    pl.title("GOV2")
    pl.bar(x, y3, color="r", align="center")
    pl.xticks(x)
    pl.xlim([0, 11])
    pl.ylim([0, 0.152])
    
    axes_5 = pl.subplot(G[-1, 3:5])
    pl.title("Robust04")
    pl.bar(x, y3, color="r", align="center")
    pl.xticks(x)
    pl.xlim([0, 11])
    pl.ylim([0, 0.152])

    
    pl.tight_layout()
    pl.show()    

featurn_imp_3_5grid()

figure(figsize=(10, 2.5))
zipy = zip(y,y1,y2,y3)
print zipy
dim = len(zipy[0])
w=1.
dimw = 4.5

x = arange(len(zipy[0]))
print x
for i in range(len(zipy)):
    # y =[d[i] for d in zipy]
    y = zipy[i]
    print x + i * dimw, y
    b = bar(x + i * dimw, y, w, color=['r', 'b', 'g', 'black'], label='disk4&5') #label=['disk4&5', 'WT2G', 'WT10G', 'GOV2']

print type(b)
gca().set_xticks(x + w / 2)
xticks(arange(2,45, 4.5), (1,2,3,4,5,6,7,8,9,10))
legend((b[0], b[1], b[2], b[3]), ('disk4&5', 'WT2G', 'WT10G', 'GOV2'), loc=9, prop={'size':6})
# legend()
# gca().set_ylim( (0.001,0.2))
# show()
# pl.savefig("/data/Dropbox/latex/sigir2013/feature_imp1.eps", bbox_inches='tight')


# 

# for f in xrange(len(indices)):
#     print "%d. feature %d (%f)" % (f + 1, indices[f], importances[indices[f]])

# # Plot the feature importances of the forest
# import pylab as pl
# pl.figure()
# pl.title("Feature importances")
# pl.bar(xrange(len(indices)), importances[indices],
#        color="r", yerr=std[indices], align="center")
# pl.xticks(xrange(len(indices)), indices)
# pl.xlim([-1, 11])
# pl.show()


# gov2
# 10 (0.144677)
# 2. feature 9 (0.136881)
# 3. feature 7 (0.121900)
# 4. feature 6 (0.116768)
# 5. feature 5 (0.116706)
# 6. feature 0 (0.112291)
# 7. feature 1 (0.111505)
# 8. feature 8 (0.100524)
# 9. feature 4 (0.014929)
# 10. feature 3 (0.013724)
# 11. feature 2 (0.010095)

# wt10g

# 1. feature 10 (0.141457)
# 2. feature 0 (0.117890)
# 3. feature 9 (0.116800)
# 4. feature 7 (0.115154)
# 5. feature 6 (0.111665)
# 6. feature 5 (0.109981)
# 7. feature 1 (0.103175)
# 8. feature 8 (0.102176)
# 9. feature 3 (0.032594)
# 10. feature 4 (0.031945)
# 11. feature 2 (0.017164)

# disk45
# 1. feature 0 (0.131026)
# 2. feature 10 (0.126747)
# 3. feature 9 (0.123959)
# 4. feature 7 (0.113181)
# 5. feature 5 (0.105831)
# 6. feature 1 (0.100042)
# 7. feature 6 (0.099095)
# 8. feature 8 (0.094010)
# 9. feature 4 (0.045511)
# 10. feature 3 (0.044785)
# 11. feature 2 (0.015813)

# wt2g
# Feature ranking:
# 1. feature 8 (0.151950)
# 2. feature 0 (0.142498)
# 3. feature 9 (0.139929)
# 4. feature 6 (0.128810)
# 5. feature 7 (0.126345)
# 6. feature 5 (0.118861)
# 7. feature 1 (0.112298)
# 8. feature 3 (0.030029)
# 9. feature 4 (0.029560)
# 10. feature 2 (0.019720)