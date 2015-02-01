#!/usr/bin/python -O

import numpy as np

from scipy.stats import pearsonr, spearmanr

from utils import *

import collections
import math
import location_db
import pair_db


METRICS = [
    'lcs',
    'relatedness',
    'spherical',
    'geodetic',
    'countries',
    'states',
    'graph25',
    'graph100',
    'popDiff',
    'ordinal',
    'sr',
    'typeSr',
]


def analyze():
    tee(RES + '/metrics.txt', 'w')
    db = pair_db.PairDb()
    print '\ncoverage:'
    for m in METRICS:
        analyze_metric(db, m)
    correlation(db)

def analyze_metric(db, m):
    aggregated = db.aggregated(impute=False)
    n1 = len([r for r in aggregated])
    n2 = len([r for r in aggregated if r.get(m) is not None])
    print '\t%s is %.1f%% (%d of %d)' % (m, 100.0 * n2 / n1, n2, n1)

def correlation(db):
    aggregated = db.aggregated()
    for i in range(2):
        vals = {}
        for m in METRICS:
            if i == 0:
                vals[m] = [r.get(m) for r in aggregated]
            else:
                vals[m] = [r.get(m) for r in aggregated if r.geodetic < 500000]

        if i == 0:
            print '\ncorrelations (with imputation)'
        else:
            print '\ncorrelations within 500 kms (n=%d)' % len(vals['lcs'])

        for m1 in METRICS:
            for m2 in METRICS:
                if m1 != m2:
                    X = vals[m1]
                    Y = vals[m2]
                    print '\t%s vs %s: pearson=%.3f, spearman=%.3f' \
                          % (m1, m2, pearsonr(X, Y)[0], spearmanr(X, Y)[0])


if __name__ == '__main__':
    analyze()