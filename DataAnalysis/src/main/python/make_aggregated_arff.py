import arff

import collections
from utils import *
from pair_db import PairDb, CONTAINMENT_CLASSES

db = PairDb()

FIELDS = [
    'lcs',
    'spherical',
    'geodetic',
    'countries',
    'states',
    'graph25',
    'graph100',
    'ordinal',
    'sr',
    'popDiff',
    'typeSr',
] + CONTAINMENT_CLASSES + ['relatedness']


responses = db.aggregated()

X = []
for q in responses:

    row = []
    for f in FIELDS:
        row.append(q.get(f))
    X.append(row)

arff.dump(DAT + '/aggregated.arff', X, relation="aggregated-pairs", names=FIELDS)

