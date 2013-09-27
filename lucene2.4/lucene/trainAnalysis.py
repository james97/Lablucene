#!/usr/bin/python
"""
this is to analyse the training examples, print out import information
about the training data
"""
import os
from sklearn.datasets import load_svmlight_file
from sklearn import linear_model, svm
from sklearn.preprocessing import StandardScaler
from setup import LUCENE_HOME
import numpy as np
import logging

os.chdir(LUCENE_HOME)
scaler = StandardScaler()

X_train, y_train = load_svmlight_file("train.sample")
x_train = X_train.toarray()
xs_train = scaler.fit_transform(x_train)
outfor = "{0:40s} {1}"
cprior = 100.

def eval_print(model, x_train, y_train):
	model.fit(x_train, y_train)
	score = model.score(x_train, y_train)
	mean = np.mean((model.predict(x_train) - y_train)**2)
	return "%f, %f" % (score, mean)


lreg = linear_model.LinearRegression()
lrreg = linear_model.Ridge()
svr = svm.SVR(kernel='linear', C=cprior)
brreg = linear_model.BayesianRidge()
ardreg = linear_model.ARDRegression(compute_score=True)
logreg = linear_model.LogisticRegression(penalty='l2', C=cprior)


print outfor.format("LinearRegression:", eval_print(lreg, x_train, y_train))
print outfor.format("LinearRegression Scaled:", eval_print(lreg, xs_train, y_train))

print outfor.format("RedgeLinearRegression:", eval_print(lrreg, x_train, y_train))
print outfor.format("RedgeLinearRegression Scaled:", eval_print(lrreg, xs_train, y_train))


print outfor.format("svm reg:", eval_print(svr, x_train, y_train))
print outfor.format("svm reg Scaled:", eval_print(svr, xs_train, y_train))

print outfor.format("baye rreg:", eval_print(brreg, x_train, y_train))
print outfor.format("baye rreg Scaled:", eval_print(brreg, xs_train, y_train))

print outfor.format("ARDRegression rreg:", eval_print(ardreg, x_train, y_train))
print outfor.format("ARDRegression rreg Scaled:", eval_print(ardreg, xs_train, y_train))

print outfor.format("LogisticRegression rreg:", eval_print(logreg, x_train, y_train))
print outfor.format("LogisticRegression Scaled:", eval_print(logreg, xs_train, y_train))
