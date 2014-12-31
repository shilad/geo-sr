#!/usr/bin/python -O

PERSONALIZED_FIELDS = [
    'person',
    'familiarity1',
    'familiarity2',
    'valence1',
    'valence2',
    'relatedness'
]

import csv

header = None

f = open('dat/pair-responses.tsv', 'r')
responses = []
for r in csv.DictReader(f, delimiter='\t'):
    oldRelatedness = r['relatedness']
    oldPopRank1 = r['popRank1']
    oldPopRank2 = r['popRank2']
    r['popRank1'] = oldRelatedness
    r['popRank2'] = oldPopRank1
    r['relatedness'] = int(float(oldPopRank2))
    responses.append(r)
    if len(responses) > 10000:
        break

all = {}
unfamiliar = {}
medium = {}
familiar = {}

for r in responses:
    strata = None
    if r['relatedness'] <= 0: continue
    elif r['relatedness'] in (1, 2):
        strata = unfamiliar
    elif r['relatedness'] in (3,):
        strata = medium
    elif r['relatedness'] in (4, 5):
        strata = familiar
    else:
        raise Exception("invalid relatedness in " + r)

    key = tuple(sorted([r['locationId1'], r['locationId2']]))
    for data in (all, strata):
        if not key in data:
            data[key] = dict(r)
            data[key]['count'] = 0
            for field in PERSONALIZED_FIELDS:
                data[key][field] = 0.0
        for field in PERSONALIZED_FIELDS:
            if f != 'person':
                data[key][field] += float(r[field])


