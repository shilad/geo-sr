import collections
from utils import *
from pair_db import PairDb, merge_responses

db = PairDb()

FIELDS = [
    'location1Name',
    'location2Name',
    'lcs',
    'spherical',
    'relatedness',
    'geodetic',
    'countries',
    'states',
    'graph25',
    'graph100',
    'ordinal',
    'sr',
    'popDiff',
    'typeSr',
    'containsCategory',
]


responses = db.aggregated()

X = []
for q in responses:

    row = []
    for f in FIELDS:
        row.append(q.get(f))
    X.append(row)

import arff
arff.dump(DAT + '/aggregated.arff', X, relation="aggregated-pairs", names=FIELDS)

