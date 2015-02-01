#!/usr/bin/python -O


import collections
from utils import *
from pair_db import PairDb, CONTAINMENT_CLASSES

db = PairDb()

responses = db.aggregated()
responses.sort(key=lambda r: r.relatedness)
f = open(DAT + '/pairs.txt', 'w')
for r in responses:
    f.write(r.location1Name + '\t' + r.location2Name + '\t' + str(r.relatedness) + '\n')
f.close()