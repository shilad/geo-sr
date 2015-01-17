from location_db import LocationDb

from scipy.stats import spearmanr
from scipy.stats import pearsonr

from utils import *



FLOAT_FIELDS = [
    'familiarity',
    'valence',
    'popRank',
    'spherical',
    'geodetic',
    'countries',
    'states',
    'graph',
]

db = LocationDb()
for f1 in FLOAT_FIELDS:
    for f2 in FLOAT_FIELDS:
        if f1 == f2: continue
        X = []
        Y = []
        for r in db.responses.values():
            v1 = r.get(f1)
            v2 = r.get(f2)
            if v1 is not None and v2 is not None:
                X.append(v1)
                Y.append(v2)
        if X and Y:
            print '%s, %s: %.3f' % (f1, f2, spearmanr(X, Y)[0])