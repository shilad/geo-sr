#!/usr/bin/python -O

import arff
import collections
import sys

import pair_db
from utils import *

PAIR_FIELDS = [
    ''
]


db = pair_db.PairDb()

by_question = collections.defaultdict(list)
for r in db.responses:
    if r.has_all_fields() and r.relatedness is not None:
        by_question[r.get_location_key()].append(r)

fields = [f for f in FLOAT_FIELDS if f != 'relatedness']

X = []
for all_qs in by_question.values():
    if len(all_qs) < 10:
        continue
    for q1 in all_qs:
        train = [q2 for q2 in all_qs if q2 != q1]
        merged = merge_responses(train)
        X.append([q1.get(f) for f in fields] + [q1.relatedness - merged.relatedness])

arff.dump(DAT + '/variation.arff', X, relation="pairs", names=fields + ['delta'])
