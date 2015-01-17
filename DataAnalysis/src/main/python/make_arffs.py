import collections
from utils import *
from pair_db import PairDb, merge_responses

db = PairDb()

PAIR_FIELDS = [
    # 'person',
    'lcs',
    'spherical',
    'relatedness',
    'geodetic',
    'countries',
    'states',
    'graph',
    'sr',
    'typeSr',
]

LOCATION_FIELDS = [
    'pfamiliarity',
    'familiarity',
    'valence',
    'popRank',
    'spherical',
    'geodetic',
    'countries',
    'states',
    'graph',
    # 'location',
]

overall_mean = []
by_question = collections.defaultdict(list)
by_person = collections.defaultdict(list)
for r in db.responses:
    if r.has_all_fields() and r.relatedness is not None and r.location1 and r.location2:
        by_question[r.get_location_key()].append(r)
        by_person[r.person].append(r)
        overall_mean.append(r.relatedness)
overall_mean = mean(overall_mean)

fields = (
    PAIR_FIELDS +
    [f + '1' for f in LOCATION_FIELDS] +
    [f + '2' for f in LOCATION_FIELDS] +
    ['umean', 'delta']
)

X = []
for all_qs in by_question.values():
    if len(all_qs) < 10:
        continue

    for q1 in all_qs:
        train = [q2 for q2 in all_qs if q2 != q1]
        utrain = [q2 for q2 in by_person[q1.person] if q2 != q1]
        umean = overall_mean
        if utrain:
            mean = merge_responses(utrain).relatedness
        merged = merge_responses(train)
        row = []
        for f in PAIR_FIELDS:
            row.append(q1.get(f))
        for f in LOCATION_FIELDS:
            row.append(q1.location1.get(f))
        for f in LOCATION_FIELDS:
            row.append(q1.location2.get(f))
        row.append(umean - overall_mean)
        row.append(q1.relatedness - merged.relatedness)
        X.append(row)

import arff
arff.dump(DAT + '/variation.arff', X, relation="pairs", names=fields)
