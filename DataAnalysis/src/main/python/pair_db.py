import csv
import collections

from utils import *

from location_db import LocationDb

PERSONALIZED_FIELDS = [
    'familiarity1',
    'familiarity2',
    'valence1',
    'valence2',
    'relatedness'
]

FLOAT_FIELDS = [
    'lcs',
    'familiarity1',
    'familiarity2',
    'valence1',
    'valence2',
    'relatedness',
    'popRank1',
    'popRank2',
    'spherical',
    'geodetic',
    'countries',
    'states',
    'graph',
    'sr',
    'typeSr',
]



VALIDATION_PAIRS = [
    ('high', 'Great Britain', 'United Kingdom'),
    ('low', 'Florida', 'Hong Kong'),
    ('low', 'Bermuda Triangle', 'Minnesota'),
]

PAIRS_TO_SKIP = [
        (l1, l2) for (high_low, l1, l2) in VALIDATION_PAIRS
] + [('Harvard University', 'Princeton University')]

class PairDb:
    def __init__(self):
        self.locationDb = LocationDb()
        raw = self.read_raw()
        self.responses = self.filter_responses(raw)
        self.add_locations()

    def read_raw(self):
        f = open(DAT + '/pair-responses.tsv', 'r')
        responses = []
        for r in csv.DictReader(f, delimiter='\t'):
            responses.append(Pair(r))
        f.close()
        warn('read %d pair assessment' % (len(responses)))
        return responses

    def filter_responses(self, responses):
        validations = collections.defaultdict(set)
        filtered = []

        for r in responses:
            if r.is_validation() and r.is_correct_validation():
                validations[r.person].add(r.get_location_key())
            elif not r.should_skip():
                filtered.append(r)

        warn('%d users completed survey, %d passed validation'
             % (len(validations),
                len([u for u in validations if len(validations[u]) == len(VALIDATION_PAIRS)])))

        return merge_responses_by(filtered, lambda r: r.person + '@' + str(r.get_location_key()))

    def get_users(self):
        return set([r.person for r in self.responses])

    def get_merged(self, fn):
        return merge_responses_by(self.responses, fn)

    def add_locations(self):
        remaining = set(self.locationDb.responses.values())
        i = 0
        for r in self.responses:
            r.location1 = self.locationDb.get(r.person, r.location1Name)
            r.location2 = self.locationDb.get(r.person, r.location2Name)
            if r.location1: i += 1
            if r.location2: i += 1
            if r.location1 in remaining: remaining.remove(r.location1)
            if r.location2 in remaining: remaining.remove(r.location2)
            if r.location1:
                assert(r.location1.familiarity == r.familiarity1)
                assert(r.location1.valence == r.valence1)
                assert(r.location1.popRank == r.popRank1)
            if r.location2:
                assert(r.location2.familiarity == r.familiarity2)
                assert(r.location2.valence == r.valence2)
                assert(r.location2.popRank == r.popRank2)

        warn('%d of %d locations had a location response assigned to them (%d location responses orphaned)'
             % (i, len(self.responses)*2, len(remaining)))


def merge_responses_by(responses, fn):
    grouped = collections.defaultdict(list)
    for r in responses:
        grouped[fn(r)].append(r)
    merged = []
    for key in grouped:
        merged.append(merge_responses(grouped[key]))
    return merged

def merge_responses(responses):
    assert len(responses) > 0
    merged = {}
    for f in vars(responses[0]):
        if f in PERSONALIZED_FIELDS:
            merged[f] = []
        else:
            merged[f] = getattr(responses[0], f)
    for r in responses:
        for f in PERSONALIZED_FIELDS:
            if hasattr(r, f): merged[f].append(getattr(r, f))
    for f in PERSONALIZED_FIELDS:
        if not f in merged: continue
        vals = [v for v in merged[f] if v is not None]
        if vals:
            merged[f] = 1.0 * sum(vals) / len(vals)
        else:
            merged[f] = None
    merged['count'] = len(responses)
    return Pair(merged)

class Pair:
    def __init__(self, row):
        if 'location1Name' in row:
            swap = row['location1Name'] > row['location2Name']
        else:
            swap = row['location1'] > row['location2']
        # Set magic attributes
        for field, val in row.items():
            if swap and '1' in field:
                field = field.replace('1', '2')
            elif swap and '2' in field:
                field = field.replace('2', '1')

            if field == 'location1' and type(val) == type(''):
                field = 'location1Name'
            if field == 'location2' and type(val) == type(''):
                field = 'location2Name'
            if val in ('', 'NaN', 'null', None):
                val = None
            if val and field in FLOAT_FIELDS:
                val = float(val)
            if field == 'relatedness' and val <= 0:
                val = None
            if field.startswith('familiarity') and val <= 0:
                val = None
            if field.startswith('valence') and val <= 0:
                val = None
            setattr(self, field, val)
        self.lcs = longest_common_substring(self.location1Name.lower(), self.location2Name.lower())

    def get_location_key(self):
        assert(self.location1Name < self.location2Name)
        return (self.location1Name, self.location2Name)

    def get_relatedness_as_int(self):
        if not self.relatedness:
            return None
        else:
            return int(round(self.relatedness))

    def get_validation_code(self):
        for code, l1, l2 in VALIDATION_PAIRS:
            if self.get_location_key() == (l1, l2):
                return code
        return None

    def should_skip(self):
        for l1, l2 in PAIRS_TO_SKIP:
            if self.get_location_key() == (l1, l2):
                return True
        return False

    def is_validation(self):
        return self.get_validation_code() is not None

    def has_response(self):
        return self.relatedness is not None and self.relatedness > 0

    def is_correct_validation(self):
        code = self.get_validation_code()
        relatedness = self.get_relatedness_as_int()
        if  code == 'low':
            return relatedness not in (4, 5)
        elif code == 'high':
            return relatedness not in (1, 2)
        else:
            assert False


    def has_all_fields(self):
        for f in FLOAT_FIELDS:
            if getattr(self, f) is None:
                return False
        return self.location1 and self.location2 and self.location1.has_all_fields() and self.location2.has_all_fields()

    def get(self, field):
        if field.startswith('location1.'):
            if not self.location1:
                return None
            else:
                return self.location1.get(field[10:])
        elif field.startswith('location2.'):
            if not self.location2:
                return None
            else:
                return self.location2.get(field[10:])
        else:
            return getattr(self, field)

    def to_row(self, fields):
        return [ self.get(f) for f in fields ]

    def __str__(self):
        return str(self.__dict__)

    def __repr__(self):
        return str(self.__dict__)
