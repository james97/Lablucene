from flask import Flask, request, make_response
import barrister
from setup import LUCENE_HOME, DROPBOX_HOME
import sys
sys.path.append('.')
import numpy as np
from sklearn.datasets import load_svmlight_file
from sklearn import linear_model, svm, neighbors
from sklearn.cross_validation import LeaveOneOut, LeavePOut
from sklearn.linear_model import LinearRegression
from sklearn.preprocessing import StandardScaler
from sklearn.grid_search import GridSearchCV
from sklearn.metrics import r2_score, mean_squared_error
from sklearn.cross_validation import train_test_split
from sklearn.base import clone
from timeit import time
from LogisticRegressor import LogisticRegressor

inum = 1

def myr2_score(y_true, y_pred):
	global inum
	print "%d: %f, %f, %f" % (inum, y_pred.max(), y_pred.min(), y_pred.mean())
	inum += 1
	if len(y_true) == 1:
		return ((y_true - y_pred) ** 2).sum()
	numerator = ((y_true - y_pred) ** 2).sum()
	denominator = ((y_true - y_true.mean()) ** 2).sum()
	if denominator == 0.0:
	    if numerator == 0.0:
			return 1.0
        else:
            # arbitary set to zero to avoid -inf scores, having a constant
            # y_true is not interesting for scoring a regression anyway
            return 0.0
	return 1 - numerator / denominator

outfor = "{0:40s} {1}"
cp = 1000

tuned_parameters_svr = [{'kernel': ['rbf'], 'gamma': [0, 0.1, 0.2 , 0.3, 1e-2],
                     'C': [1, 10, 100, 10000]}]
tuned_parameters_logistic = [ { 'penalty': ['l1', 'l2'], 'C': [1, 10, 100, 10000] } ]
test_model = linear_model.LogisticRegression(tol=0.01)
svr = svm.SVR(C=1)



ir = LinearRegression()
tuned_parameters_knn = [ {'n_neighbors': [5, 10, 15], 'algorithm': ['auto', 'ball_tree', 'kd_tree']} ]
knn = neighbors.KNeighborsRegressor()

log = LogisticRegressor()
tuned_parameters_log = [ {'penalty': ['l1', 'l2']} ]

test_model = log
tuned_parameters = tuned_parameters_log
def coefficientR2(y, y_pred):
	"""it's the same as r2_score"""
	u = ((y - y_pred) ** 2).sum()
	v = ((y - y.mean()) ** 2).sum()
	return 1- u/v

myscore_func = myr2_score

def eval_print(model, x_train, y_train):
	# print model.predict(x_train) - y_train
	score = model.score(x_train, y_train)
	# mean = np.mean((model.predict(x_train) - y_train)**2)
	mean = myscore_func(y_train, model.predict(x_train))
	return "%f, %f" % (score, mean)

def printGrid(clf):
    for params, mean_score, scores in clf.grid_scores_:
    	print "%0.3f (+/-%0.03f) for %r" % (mean_score, scores.std() / 2, params)

def toNPArray(inst_str):
	return np.fromstring(inst_str, sep=' ')


class Predictor():
	def __init__(self, path, cprior=1):
		X_train, y_train = load_svmlight_file(path)
		x_train = X_train.toarray()
		y_train = 1/(y_train + 1)
		y_train = y_train.reshape(len(y_train), 1)
		print 'y_train shape' , y_train.shape
		self.scaler = StandardScaler()
		xs_train = self.scaler.fit_transform(x_train)
		Xnew_train, Xnew_test, ynew_train, ynew_test = train_test_split(
		    xs_train, y_train, test_size=0.2, random_state=0)
		self.model = svm.SVR(C=cprior, gamma=0.0)

		self.length = len(xs_train)
		even_slice = slice(0, self.length, 2)

		odd_slice = slice(1, self.length, 2)
		print 'even: ', len(xs_train[even_slice])
		print 'odd: ', len(xs_train[odd_slice])
		print time.ctime()
		# self.model_even = GridSearchCV(clone(test_model), tuned_parameters, score_func=myscore_func, iid=False, n_jobs=2)
		# self.model_even.fit(xs_train[even_slice], y_train[even_slice], cv=5)
		# # print self.model_even.grid_scores_
		

		# self.model_odd = GridSearchCV(clone(test_model), tuned_parameters, score_func=myscore_func, iid=False, n_jobs=2)
		# self.model_odd.fit(xs_train[odd_slice], y_train[odd_slice], cv=5)
		
		# # for params, mean_score, scores in self.clf.grid_scores_:
		# # print "%0.3f (+/-%0.03f) %r" % (mean_score, scores.std()/2, params)
		# # Split the dataset in two equal parts

		self.model = GridSearchCV(test_model, tuned_parameters, score_func=myscore_func, n_jobs=1, cv=LeaveOneOut(len(Xnew_train)))
		self.model.fit(Xnew_train, ynew_train)
		print time.ctime()

		# print outfor.format("even:odd svm reg Scaled:", eval_print(self.model_even, xs_train[odd_slice], y_train[odd_slice]))
		# print outfor.format("even:even svm reg Scaled:", eval_print(self.model_even, xs_train[even_slice], y_train[even_slice]))
		# print self.model_even.best_params_
		# # printGrid(self.model_even)
		# print 

		# print outfor.format("odd:even svm reg Scaled:", eval_print(self.model_odd, xs_train[even_slice], y_train[even_slice]))
		# print outfor.format("odd:odd svm reg Scaled:", eval_print(self.model_odd, xs_train[odd_slice], y_train[odd_slice]))
		# print self.model_odd.best_params_
		# # printGrid(self.model_odd)
		# print

		print outfor.format("svm reg Scaled:", eval_print(self.model, Xnew_train, ynew_train))
		print outfor.format("svm reg Scaled:", eval_print(self.model, Xnew_test, ynew_test))
		print self.model.best_params_
		# printGrid(self.model)

		ir.fit(Xnew_train, ynew_train)
		print outfor.format("IsotonicRegression reg Scaled:", eval_print(ir, Xnew_test, ynew_test))


		# return # init method never returns a value

	def predict(self, inst_str):
		score = self.model.predict(self.scaler.transform(toNPArray(inst_str)))
		print score[0]
		return score[0]

	def predict_id(self, inst_str, queryid):
		if queryid%2 ==0:
			score = self.model_odd.best_estimator_.predict(self.scaler.transform(toNPArray(inst_str)))
		else:
			score = self.model_even.best_estimator_.predict(self.scaler.transform(toNPArray(inst_str)))
		return score[0]

path_model = "%s/train.sample" % LUCENE_HOME
contract = barrister.contract_from_file("%s/python/lucene/Predictor.json" % DROPBOX_HOME)
server   = barrister.Server(contract)
server.add_handler("Predictor", Predictor(path_model, cprior=cp))


app = Flask(__name__)

@app.route("/predictor", methods=["POST"])
def predict():
    resp_data = server.call_json(request.data)
    resp = make_response(resp_data)
    resp.headers['Content-Type'] = 'application/json'
    return resp

print 'server is runing'
# app.run(host="127.0.0.1", port=60000)
