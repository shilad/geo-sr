import pair_db

from utils import *

db = pair_db.PairDb()

for r in db.aggregated():
    if r.states < 0.2 and r.get('cc-state-point-false') == '1':
        print r.states, r.location1Name, r.location2Name