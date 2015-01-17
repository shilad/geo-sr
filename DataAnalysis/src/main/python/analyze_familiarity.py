#!/usr/bin/python -O

import numpy as np

from scipy.stats import pearsonr
from sklearn import cross_validation, linear_model, svm, tree
from sklearn.ensemble import GradientBoostingRegressor

from utils import *

import collections
import math
import location_db
import pair_db

def predict_pair_db_familiarity(db):
    # Adds an inferred "pfamiliarity" field for each location

    responses = set()
    for r in db.responses:
        if r.location1 and r.location1.has_all_fields(): responses.add(r.location1)
        if r.location2 and r.location2.has_all_fields(): responses.add(r.location2)

    responses = list(responses)

    all = []
    by_person = collections.defaultdict(list)
    by_place = collections.defaultdict(list)
    for r in responses:
        by_person[r.person].append(r.familiarity)
        by_place[r.location].append(r.familiarity)
        all.append(r.familiarity)

    overall = mean(all)
    def mean2(vals, holdout):
        return 1.0 * (overall + sum(vals) - holdout) / len(vals)

    X = []
    Y = []
    for r in responses:
        umean = mean2(by_person[r.person], r.familiarity)
        pmean = mean2(by_place[r.location], r.familiarity)

        Y.append(r.familiarity)
        X.append([
            math.log(r.popRank + 1),
            r.spherical,
            r.geodetic,
            r.countries,
            r.states,
            r.graph,
            umean,
            pmean,
        ])

    X = np.array(X)
    Y = np.array(Y)


    vals = set()
    for train, test in cross_validation.KFold(len(responses), n_folds=7):
        X_train, X_test, Y_train, Y_test = X[train], X[test], Y[train], Y[test]
        #clf = GradientBoostingRegressor(n_estimators=100, learning_rate=0.1, max_depth=1, random_state=0, loss='ls')
        clf = linear_model.LinearRegression()
        # clf = tree.DecisionTreeRegressor()
        clf.fit(X_train, Y_train)
        predicted = clf.predict(X_test)
        for (i, j) in enumerate(test):
            vals.add(j)
            responses[j].pfamiliarity = predicted[i]
    p = pearsonr([r.familiarity for r in responses], [r.pfamiliarity for r in responses])
    warn('achieved pearson correlation for inferred familiarity fields of %.3f' % p[0])


def analyze():
    db = pair_db.PairDb()
    predict_pair_db_familiarity(db)
    all = [r for r in db.responses if r.has_all_fields()]
    by_pair = collections.defaultdict(lambda: [[],[]])
    for r in all:
        key = r.location1Name + ':' + r.location2Name
        # if r.location1.pfamiliarity <= 2.6 and r.location2.pfamiliarity <= 2.6:
        #     by_pair[key][0].append(r.relatedness)
        # elif r.location1.pfamiliarity >= 3.0 and r.location2.pfamiliarity >= 3.0:
        #     by_pair[key][1].append(r.relatedness)
        if r.location1.familiarity <= 2 and r.location2.familiarity <= 2:
            by_pair[key][0].append(r.relatedness)
        elif r.location1.familiarity >= 4 and r.location2.familiarity >= 4:
            by_pair[key][1].append(r.relatedness)

    print sum([len(l) for l, h in by_pair.values()]), sum([len(h) for l, h in by_pair.values()])

    direct = 0
    same = 0
    inverse = 0
    diffs = []

    for (low, high) in by_pair.values():
        if len(low) > 5 and len(high) > 5:
            lmean = mean(low)
            hmean = mean(high)
            if lmean < hmean:
                direct += 1
            elif lmean > hmean:
                inverse += 1
            else:
                same += 1
            diffs.append(hmean - lmean)

    print direct, same, inverse, mean(diffs)



if __name__ == '__main__':
    analyze()


