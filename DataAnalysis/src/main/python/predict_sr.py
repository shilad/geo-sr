import collections
import random

from scipy.stats import spearmanr
from sklearn import cross_validation, linear_model
from sklearn.ensemble import GradientBoostingRegressor, RandomForestRegressor, ExtraTreesRegressor
from sklearn.svm import SVR, NuSVR
from sklearn.preprocessing import OneHotEncoder


import pair_db

from utils import *

def evaluate(db, fields):
    all = db.aggregated()
    random.shuffle(all)
    actual = []
    predictions = []
    for itrain, itest in cross_validation.KFold(len(all), n_folds=7):
        train = [all[i] for i in itrain]
        test = [all[i] for i in itest]
        predictions += predict_fields(train, test, fields)
        actual += [r.relatedness for r in test]

    return spearmanr(actual, predictions)[0]


def predict_fields(train, test, fields):
    uratings = collections.defaultdict(list)
    for r in train:
        uratings[r.person].append(r.relatedness)
    Ytrain = [r.relatedness for r in train]
    Xtrain = [ r.to_row(fields) for r in train ]
    Xtest = [ r.to_row(fields) for r in test ]

    # clf = linear_model.LinearRegression()
    clf = GradientBoostingRegressor(n_estimators=100, learning_rate=0.1, max_depth=1, random_state=0, loss='ls')
    # clf = SVR()
    # clf = RandomForestRegressor()
    clf.fit(Xtrain, Ytrain)
    return list(clf.predict([r for r in Xtest]))
FIELD_COMBOS = [
    ['sr'],
    ['typeSr'],
    ['sr', 'typeSr'],
    [ 'sr', ] + pair_db.CONTAINMENT_CLASSES,
    [ 'sr', 'typeSr' ] + pair_db.CONTAINMENT_CLASSES,
    [ 'sr', 'ordinal', ] + pair_db.CONTAINMENT_CLASSES,
    [ 'sr', 'ordinal', 'typeSr' ] + pair_db.CONTAINMENT_CLASSES,
    [ 'sr', 'ordinal', 'states', ] + pair_db.CONTAINMENT_CLASSES,
    [ 'sr', 'ordinal', 'states', 'typeSr' ] + pair_db.CONTAINMENT_CLASSES,
    [
        'lcs',
        'spherical',
        'countries',
        'states',
        'ordinal',
        'popDiff',
        ] + pair_db.CONTAINMENT_CLASSES,
    [
        'lcs',
        'spherical',
        'countries',
        'states',
        'ordinal',
        'popDiff',
        'typeSr',
        ] + pair_db.CONTAINMENT_CLASSES,
    [
        'lcs',
        'spherical',
        'countries',
        'states',
        'ordinal',
        'sr',
        'popDiff',
        'typeSr',
        ] + pair_db.CONTAINMENT_CLASSES,
]



tee(RES + '/geosr.txt', 'w')

db = pair_db.PairDb()

for fields in FIELD_COMBOS:
    print 'rho=%.3f for fields %s' % (evaluate(db, fields), ', '.join(fields))