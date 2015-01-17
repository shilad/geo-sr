__author__ = 'shilad'

from scipy.stats import spearmanr, pearsonr

import collections
import pair_db

from utils import *

db = pair_db.PairDb()

pair_ratings = collections.defaultdict(list)
location_ratings = collections.defaultdict(dict)

for r in db.responses:
    if not r.has_all_fields(): continue
    pair_ratings[r.person].append(r.relatedness)
    for l in r.location1, r.location2:
        location_ratings[r.person][l.location] = l.familiarity


X = []
Y = []
for p in pair_ratings:
    pmean = mean(pair_ratings[p])
    lmean = mean(location_ratings[p].values())
    X.append(pmean)
    Y.append(lmean)

print spearmanr(X, Y)
print pearsonr(X, Y)