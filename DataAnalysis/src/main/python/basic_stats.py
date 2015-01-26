#!/usr/bin/python -O

import numpy as np

from scipy.stats import pearsonr

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
    'sr',
    'typeSr',
    ]



def analyze():
    tee(RES + '/basic.txt', 'w')

    db = pair_db.PairDb()
    analyze_validation(db)
    analyze_response_values(db)
    analyze_aggregation(db)

def analyze_validation(db):
    pair_counts = collections.defaultdict(lambda: [0, 0])
    user_counts = collections.defaultdict(lambda: [0, 0])

    for r in db.read_raw():
        if r.is_validation():
            k = r.get_location_key()
            if r.is_correct_validation():
                pair_counts[k][0] += 1
                user_counts[r.person][0] += 1
            else:
                pair_counts[k][1] += 1
                user_counts[r.person][1] += 1

    print '\nvalidation accuracy:'
    for (locations, responses) in pair_counts.items():
        print ('\tfor pair %s, %s, %d people were correct and %d were incorrect'
               % (locations[0], locations[1], responses[0], responses[1]))

    population_counts = collections.defaultdict(int)
    for c in user_counts.values():
        population_counts[tuple(c)] += 1

    for c in reversed(sorted(population_counts.keys(), key=population_counts.get)):
        n = population_counts.get(c)
        print '\t%d people got %d  validation questions correct and %d wrong' % (n, c[0], c[1])

def analyze_response_values(db):
    hist = collections.defaultdict(int)
    for r in db.responses:
        v = r.relatedness
        if type(v) == type(3.0):
            v = int(v)  # round down...
        hist[v] += 1
    print '\n\nhistogram of response values: '
    total = len(db.responses)
    for val in sorted(hist.keys()):
        print '\t%s: %.1f%% (n=%d of %d)' % (val, 100.0 * hist[val] / total, hist[val], total)

def analyze_aggregation(db):
    counts = collections.defaultdict(int)
    for r in db.responses:
        if r.has_response():
            counts[r.get_location_key()] += 1

    hist = collections.defaultdict(int)
    for c in counts.values():
        hist[c] += 1

    print '\n\nnumber of people with responses per concept pair:'
    total = sum(hist.values())
    cumm = 0
    for c in reversed(sorted(hist.keys())):
        cumm += hist[c]
        print '\t%d pairs had %d responses (%.1f%%), cummlative = %d (%.1f%%)' \
              % (hist[c], c, 100.0 * hist[c] / total, cumm, 100.0 * cumm / total)

if __name__ == '__main__':
    analyze()