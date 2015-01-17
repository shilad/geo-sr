#!/usr/bin/python -O

import collections

import pair_db

from utils import *



pair_counts = collections.defaultdict(lambda: [0, 0])
user_counts = collections.defaultdict(lambda: [0, 0])

db = pair_db.PairDb()
for r in db.read_raw():
    if r.is_validation():
        k = r.get_location_key()
        if r.is_correct_validation():
            pair_counts[k][0] += 1
            user_counts[r.person][0] += 1
        else:
            pair_counts[k][1] += 1
            user_counts[r.person][1] += 1

tee(RES + '/validation.txt', 'w')
for (locations, responses) in pair_counts.items():
    print ('for pair %s, %s, %d people were correct and %d were incorrect'
           % (locations[0], locations[1], responses[0], responses[1]))

population_counts = collections.defaultdict(int)
for c in user_counts.values():
    population_counts[tuple(c)] += 1

print '\n'

for c in reversed(sorted(population_counts.keys(), key=population_counts.get)):
    n = population_counts.get(c)
    print '%d people got %d  validation questions correct and %d wrong' % (n, c[0], c[1])
