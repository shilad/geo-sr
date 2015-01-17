import copy
import collections
import csv

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


class LocationDb:
    def __init__(self):
        raw = collections.defaultdict(list)
        for r in self.read_raw():
            raw[r.get_key()].append(r)
        self.responses = {}
        for key, vals in raw.items():
            merged = copy.copy(vals[0])
            merged.familiarity = noneSafeMean([v.familiarity for v in vals])
            merged.valence= noneSafeMean([v.valence for v in vals])
            self.responses[key] = merged

    def get(self, personId, location):
        return self.responses.get(personId + '@' + location)

    def read_raw(self):
        f = open(DAT + '/location-responses.tsv', 'r')
        responses = []
        for r in csv.DictReader(f, delimiter='\t'):
            responses.append(Location(r))
        f.close()
        warn('read %d location assessments' % (len(responses)))
        return responses

def noneSafeMean(l):
    l = [x for x in l if x is not None]
    if not l:
        return None
    return 1.0 * sum(l) / len(l)

class Location:
    def __init__(self, row):
        # Set magic attributes
        for field, val in row.items():
            if val in ('', 'NaN', 'null', None):
                val = None
            if val and field in FLOAT_FIELDS:
                val = float(val)
            if (field == 'familiarity' or field == 'valence') and val <= 0.0:
                val = None
            setattr(self, field, val)

    def has_all_fields(self):
        for f in FLOAT_FIELDS:
            if getattr(self, f) is None:
                return False
        return True

    def get(self, field):
        return getattr(self, field)

    def get_key(self):
        return self.person + '@' + self.location



if __name__ == '__main__':
    db = LocationDb()
    print len(db.responses)
    print db.get('9', 'Massachusetts').familiarity
