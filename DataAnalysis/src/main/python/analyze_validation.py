#!/usr/bin/python -O

import collections

import pair_response

from utils import *



pair_counts = collections.defaultdict(lambda: [0, 0])
user_counts = collections.defaultdict(lambda: [0, 0])

db = pair_response.ResponseDb()
for r in db.read_raw():
    if r.is_validation():
        k = r.get_location_key()
        if r.is_correct_validation():
            pair_counts[k][0] += 1
            user_counts[r.person][0] += 1
        else:
            pair_counts[k][1] += 1
            user_counts[r.person][1] += 1

tee('results/validation.txt', 'w')
for (locations, responses) in pair_counts.items():
    print locations, responses

population_counts = collections.defaultdict(int)
for c in user_counts.values():
    population_counts[tuple(c)] += 1

for c, n in  population_counts.items():
    print '%d got %d right and %d wrong' % (n, c[0], c[1])
