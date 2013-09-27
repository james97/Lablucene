import pylab as pl
import matplotlib.gridspec as gridspec

pl.figure(figsize=(6, 4))
G = gridspec.GridSpec(2, 6)

axes_1 = pl.subplot(G[0, :2])
pl.xticks(())
pl.yticks(())
pl.text(0.5, 0.5, 'Axes 1', ha='center', va='center', size=24, alpha=.5)

axes_2 = pl.subplot(G[0, 2:4])
pl.xticks(())
pl.yticks(())
pl.text(0.5, 0.5, 'Axes 2', ha='center', va='center', size=24, alpha=.5)

axes_3 = pl.subplot(G[0, 4:])
pl.xticks(())
pl.yticks(())
pl.text(0.5, 0.5, 'Axes 3', ha='center', va='center', size=24, alpha=.5)

axes_4 = pl.subplot(G[-1, 1:3])
pl.xticks(())
pl.yticks(())
pl.text(0.5, 0.5, 'Axes 4', ha='center', va='center', size=24, alpha=.5)

axes_5 = pl.subplot(G[-1, 3:5])
pl.xticks(())
pl.yticks(())
pl.text(0.5, 0.5, 'Axes 5', ha='center', va='center', size=24, alpha=.5)

pl.tight_layout()
pl.show()
