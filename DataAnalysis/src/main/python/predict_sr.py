import collections
import random

from scipy.stats import spearmanr
from sklearn import cross_validation, linear_model
from sklearn.ensemble import GradientBoostingRegressor, RandomForestRegressor, ExtraTreesRegressor
from sklearn.svm import SVR, NuSVR


import pair_db
import analyze_familiarity

from utils import *

def evaluate(db, predict_fn):
    all = [r for r in db.responses if r.has_all_fields()]
    random.shuffle(all)
    actual = []
    predictions = []
    for itrain, itest in cross_validation.KFold(len(all), n_folds=7):
        train = [all[i] for i in itrain]
        test = [all[i] for i in itest]
        predictions += predict_fn(train, test)
        actual += [r.relatedness for r in test]

    return spearmanr(actual, predictions)[0]

def predict_sr(train, test):
    return [r.sr for r in test]

def predict_all(train, test):
    fields = [
        'lcs',
        'location1.familiarity',
        'location2.familiarity',
        'popRank1',
        'popRank2',
        'spherical',
        'geodetic',
        'countries',
        'states',
        'graph',
        'sr',
        'typeSr',
    ]
    return predict_fields(train, test, fields)

def predict_predicted_familiarity(train, test):
    fields = [
        'lcs',
        'location1.pfamiliarity',
        'location2.pfamiliarity',
        'popRank1',
        'popRank2',
        'spherical',
        'geodetic',
        'countries',
        'states',
        'graph',
        'sr',
        'typeSr',
    ]
    return predict_fields(train, test, fields)

def predict_no_familiarity(train, test):
    fields = [
        'lcs',
        'popRank1',
        'popRank2',
        'spherical',
        'geodetic',
        'countries',
        'states',
        'graph',
        'sr',
        'typeSr',
    ]
    return predict_fields(train, test, fields)

def withheldMean(vals, withheld=None):
    if withheld is None:
        return (2.75 + sum(vals)) / (len(vals) + 1)
    else:
        return (2.75 + sum(vals) - withheld) / len(vals)

def predict_fields(train, test, fields):
    uratings = collections.defaultdict(list)
    for r in train:
        uratings[r.person].append(r.relatedness)
    Y = [r.relatedness for r in train]
    X = [
        r.to_row(fields) + [withheldMean(uratings[r.person], r.relatedness)]
        for r in train
    ]
    #clf = linear_model.LinearRegression()
    # clf = GradientBoostingRegressor(n_estimators=100, learning_rate=0.1, max_depth=1, random_state=0, loss='ls')
    clf = SVR()
    clf.fit(X, Y)
    return list(clf.predict([
        r.to_row(fields) + [withheldMean(uratings[r.person])]
        for r in test
    ]))

db = pair_db.PairDb()
analyze_familiarity.predict_pair_db_familiarity(db)

for f in predict_sr, predict_all, predict_no_familiarity, predict_predicted_familiarity:
    print f.__name__, evaluate(db, f)